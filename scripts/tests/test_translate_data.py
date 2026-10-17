"""Tests of scripts/translate_data.py. They replay recorded API responses and never call the API."""

import json
import sys
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import translate_data  # noqa: E402

FIXTURES = Path(__file__).parent / "fixtures" / "translate_data"
FIREBALL_NAME = translate_data.Row("SPELL", "", "Fireball", "NAME", "Fireball")
FIREBALL_DESC = translate_data.Row("SPELL", "", "Fireball", "DESC",
                                   "Deals %1d6 points of fire damage in a 20-ft. radius (Reflex DC %2 halves).")
GLOSSARY = [translate_data.GlossaryTerm("DC", "DD", True), translate_data.GlossaryTerm("Reflex", "Réflexes", False)]


def recorded(name):
    """A recorded response, with the attribute access of the SDK's objects."""
    return json.loads((FIXTURES / name).read_text(encoding="utf-8"), object_hook=lambda fields: SimpleNamespace(**fields))


class ReplayingClient:
    """Answers each call with the next recorded response, and keeps the requests."""

    def __init__(self, *fixture_names):
        self.responses = [recorded(name) for name in fixture_names]
        self.requests = []
        self.beta = SimpleNamespace(messages=SimpleNamespace(create=self.create))

    def create(self, **request):
        self.requests.append(request)
        return self.responses.pop(0)


def run(client, rows=(FIREBALL_NAME, FIREBALL_DESC), already=()):
    accepted = []
    refused = translate_data.translate(list(rows), list(already), client, GLOSSARY, on_accepted=accepted.extend)
    return accepted, refused


class TranslateTest(unittest.TestCase):

    def test_passes_on_every_accepted_translation(self):
        accepted, _ = run(ReplayingClient("fireball_ok.json"))

        self.assertEqual(["Boule de feu", "Inflige %1d6 points de dégâts de feu dans un rayon de 6 m "
                                          "(Réflexes DD %2 pour réduire de moitié)."], [row.text for row in accepted])

    def test_keeps_the_key_and_field_of_the_source(self):
        accepted, _ = run(ReplayingClient("fireball_ok.json"))

        self.assertEqual(FIREBALL_DESC.identity(), accepted[1].identity())

    def test_keeps_the_english_text_that_tells_two_descriptions_apart(self):
        warped = translate_data.Row("SPELL", "", "Fireball", "DESC", FIREBALL_DESC.text, FIREBALL_DESC.text)
        accepted, _ = run(ReplayingClient("fireball_ok.json"), rows=(FIREBALL_NAME, warped))

        self.assertEqual(FIREBALL_DESC.text, accepted[1].english)

    def test_sends_again_a_translation_that_lost_a_placeholder(self):
        accepted, refused = run(ReplayingClient("fireball_lost_placeholder.json", "fireball_desc_retry_ok.json"))

        self.assertEqual((2, []), (len(accepted), refused))

    def test_refuses_a_translation_that_breaks_the_glossary(self):
        _, refused = run(ReplayingClient("fireball_wrong_abbreviation.json", "fireball_desc_wrong_abbreviation.json",
                                         "fireball_desc_wrong_abbreviation.json"))

        self.assertEqual(["glossary: DC must be DD"], [reason for _, reason in refused])

    def test_retries_a_batch_whose_answer_was_cut_off(self):
        accepted, _ = run(ReplayingClient("truncated.json", "fireball_ok.json"))

        self.assertEqual(2, len(accepted))

    def test_does_not_send_what_the_table_already_holds(self):
        client = ReplayingClient()

        run(client, already=[translate_data.Row("SPELL", "", "Fireball", "NAME", "Boule de feu"),
                             translate_data.Row("SPELL", "", "Fireball", "DESC", "Déjà traduit %1 %2 DD")])

        self.assertEqual([], client.requests)

    def test_asks_for_answers_in_the_output_schema(self):
        client = ReplayingClient("fireball_ok.json")

        run(client)

        self.assertEqual(translate_data.OUTPUT_SCHEMA, client.requests[0]["output_config"]["format"]["schema"])


class TableTest(unittest.TestCase):

    def test_reads_back_what_it_appends_with_line_breaks_and_tabs(self):
        row = translate_data.Row("SPELL", "", "Fireball", "DESC", "Ligne un\nligne deux\tet \\ fin")
        with tempfile.TemporaryDirectory() as folder:
            table = Path(folder) / "core.fr.tsv"

            translate_data.append_rows(table, [row])

            self.assertEqual([row], translate_data.read_table(table))

    def test_reads_back_the_english_text_of_one_description_among_several(self):
        row = translate_data.Row("ABILITY", "Special Ability", "Aberrant", "DESC", "Sang déformé", "Warped blood")
        with tempfile.TemporaryDirectory() as folder:
            table = Path(folder) / "core.fr.tsv"

            translate_data.append_rows(table, [row])

            self.assertEqual([row], translate_data.read_table(table))

    def test_splits_batches_by_size(self):
        rows = [translate_data.Row("SPELL", "", str(n), "DESC", "x" * 600) for n in range(5)]

        self.assertEqual([2, 2, 1], [len(batch) for batch in translate_data.batches(rows, budget=1300)])

    def test_reads_the_glossary_strict_column(self):
        self.assertTrue(next(term for term in translate_data.read_glossary(translate_data.GLOSSARY)
                             if term.english == "DC").strict)


if __name__ == "__main__":
    unittest.main()
