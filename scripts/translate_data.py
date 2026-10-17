#!/usr/bin/env python3
"""Translate PCGen's English game text into a French translation table.

Reads the <pcc>.en.tsv files that `./gradlew exportTranslationSources` writes, sends the
texts not yet translated to Claude in batches, checks every answer, and appends the good
ones to <pcc>.fr.tsv beside the campaign file under data/. A line already in the table is
never translated again, so hand corrections survive a new run, and an interrupted run
resumes where it stopped.

Usage:
    python3 scripts/translate_data.py build/translation/pathfinder/.../core_rulebook.en.tsv
    python3 scripts/translate_data.py --limit 20 <source.en.tsv>    # a sample first

Needs the `anthropic` package and credentials (ANTHROPIC_API_KEY or `ant auth login`).
"""

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

DEFAULT_MODEL = "claude-sonnet-5-5"
DEFAULT_EFFORT = "medium"
GLOSSARY = Path("data/pathfinder/i18n/glossaire.fr.tsv")
SOURCE_ROOT = Path("build/translation")
DATA_ROOT = Path("data")
SOURCE_SUFFIX = ".en.tsv"
TARGET_SUFFIX = ".fr.tsv"
HEADER = "TYPE\tCATEGORY\tKEY\tFIELD\tTEXT"
BATCH_CHARACTERS = 12000
MAX_ATTEMPTS = 3
MAX_TOKENS = 32000
FALLBACK_BETA = "server-side-fallback-2026-07-01"
PLACEHOLDER = re.compile(r"%%|%\{?(\d+)\}?")

OUTPUT_SCHEMA = {
    "type": "object",
    "properties": {
        "translations": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {"id": {"type": "integer"}, "text": {"type": "string"}},
                "required": ["id", "text"],
                "additionalProperties": False,
            },
        }
    },
    "required": ["translations"],
    "additionalProperties": False,
}

INSTRUCTIONS = """You translate game text from the Pathfinder Roleplaying Game (first edition), \
as stored by the character generator PCGen, from English into French.

Rules:
- Use the terminology of the official French edition (Black Book Éditions). The glossary below is \
binding: when an English term of the glossary appears, use its French term.
- Keep every placeholder exactly: %1, %2, %{1}, %%. They are filled in later by the program.
- Convert measurements to metric as the French rulebooks do: 5 ft. = 1,5 m, 10 ft. = 3 m, \
30 ft. = 9 m, 1 mile = 1,5 km, 1 lb. = 0,5 kg. Write decimals with a comma.
- A NAME or OUTPUTNAME field is a name: translate it as a name, capitalised as in French \
(only the first word and proper nouns). Keep proper nouns of the setting (deities, places, people) \
as they are.
- An ABB field is an abbreviation: give the French abbreviation.
- Keep line breaks where the English has them.
- Return one translation for every id you receive, and nothing else.

Glossary (English -> French):
"""


@dataclass(frozen=True)
class Row:
    type: str
    category: str
    key: str
    field: str
    text: str
    # The English template a DESC or BENEFIT row translates, when the object has several
    english: str = ""

    def identity(self):
        return (self.type, self.category, self.key, self.field, self.english)


@dataclass(frozen=True)
class GlossaryTerm:
    english: str
    french: str
    strict: bool


def escape(text):
    return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")


def unescape(column):
    return re.sub(r"\\(.)", lambda m: {"n": "\n", "t": "\t"}.get(m.group(1), m.group(1)), column)


def read_table(path):
    """The rows of a translation table, or of a source file, which has the same columns."""
    if not path.exists():
        return []
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#") or line == HEADER:
            continue
        columns = line.split("\t")
        if len(columns) == 5:
            rows.append(Row(*columns[:4], unescape(columns[4])))
        elif len(columns) == 6:
            rows.append(Row(*columns[:4], unescape(columns[4]), unescape(columns[5])))
    return rows


def append_rows(path, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    is_new = not path.exists()
    with path.open("a", encoding="utf-8") as table:
        if is_new:
            table.write(HEADER + "\n")
        for row in rows:
            columns = [row.type, row.category, row.key, row.field, escape(row.text)]
            if row.english:
                columns.append(escape(row.english))
            table.write("\t".join(columns) + "\n")


def read_glossary(path):
    terms = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#") or line.startswith("EN\t"):
            continue
        columns = line.split("\t")
        terms.append(GlossaryTerm(columns[0], columns[1], len(columns) > 2 and columns[2] == "yes"))
    return terms


def placeholders(text):
    return sorted(match.group(1) for match in PLACEHOLDER.finditer(text) if match.group(1))


def contains_term(text, term):
    return re.search(r"(?<![\w])" + re.escape(term) + r"(?![\w])", text) is not None


def refusal_reason(source, translated, glossary):
    """Why a translation is refused, or None when it is accepted."""
    if not translated.strip():
        return "empty translation"
    if placeholders(source.text) != placeholders(translated):
        return "placeholders differ from " + source.text
    for term in glossary:
        if term.strict and contains_term(source.text, term.english) and not contains_term(translated, term.french):
            return f"glossary: {term.english} must be {term.french}"
    return None


def batches(rows, budget=BATCH_CHARACTERS):
    batch, size = [], 0
    for row in rows:
        if batch and size + len(row.text) > budget:
            yield batch
            batch, size = [], 0
        batch.append(row)
        size += len(row.text)
    if batch:
        yield batch


def system_prompt(glossary):
    lines = [f"{term.english} -> {term.french}" for term in glossary]
    return INSTRUCTIONS + "\n".join(lines)


def request_for(batch, glossary, model, effort):
    """The arguments of one Messages API call translating a batch."""
    items = [{"id": index, "type": row.type, "field": row.field, "text": row.text} for index, row in enumerate(batch)]
    return {
        "model": model,
        "max_tokens": MAX_TOKENS,
        "system": [{"type": "text", "text": system_prompt(glossary), "cache_control": {"type": "ephemeral"}}],
        "messages": [{"role": "user", "content": json.dumps(items, ensure_ascii=False)}],
        "output_config": {"effort": effort, "format": {"type": "json_schema", "schema": OUTPUT_SCHEMA}},
        "betas": [FALLBACK_BETA],
        "fallbacks": "default",
    }


def translations_in(response):
    """The translations a response holds by id, or None when it holds no usable answer."""
    if response.stop_reason != "end_turn":
        return None
    text = next((block.text for block in response.content if block.type == "text"), None)
    if text is None:
        return None
    try:
        answer = json.loads(text)
    except json.JSONDecodeError:
        return None
    return {item["id"]: item["text"] for item in answer.get("translations", [])}


def translate(source_rows, already_translated, client, glossary, model=DEFAULT_MODEL, effort=DEFAULT_EFFORT,
              on_accepted=lambda rows: None):
    """Translate the rows not yet in the table. Returns the rows still refused after every attempt,
    each with the last reason."""
    done = {row.identity() for row in already_translated}
    pending = [row for row in source_rows if row.identity() not in done]
    refused = {}
    for _ in range(MAX_ATTEMPTS):
        if not pending:
            break
        retry = []
        for batch in batches(pending):
            response = client.beta.messages.create(**request_for(batch, glossary, model, effort))
            answers = translations_in(response) or {}
            accepted = []
            for index, source in enumerate(batch):
                translated = answers.get(index)
                reason = "no answer" if translated is None else refusal_reason(source, translated, glossary)
                if reason is None:
                    accepted.append(Row(source.type, source.category, source.key, source.field, translated,
                                        source.english))
                    refused.pop(source.identity(), None)
                else:
                    retry.append(source)
                    refused[source.identity()] = (source, reason)
            on_accepted(accepted)
        pending = retry
    return list(refused.values())


def target_for(source_path):
    relative = source_path.resolve().relative_to(SOURCE_ROOT.resolve())
    return DATA_ROOT / str(relative).replace(SOURCE_SUFFIX, TARGET_SUFFIX)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("sources", nargs="+", type=Path, help="<pcc>.en.tsv files from exportTranslationSources")
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--effort", default=DEFAULT_EFFORT, choices=["low", "medium", "high"])
    parser.add_argument("--limit", type=int, help="translate at most this many texts per file")
    arguments = parser.parse_args(argv)

    import anthropic  # only when calling the API: the tests never do

    client = anthropic.Anthropic()
    glossary = read_glossary(GLOSSARY)
    for source_path in arguments.sources:
        target = target_for(source_path)
        source_rows = read_table(source_path)[: arguments.limit]
        refused = translate(source_rows, read_table(target), client, glossary, arguments.model, arguments.effort,
                            on_accepted=lambda rows, path=target: append_rows(path, rows))
        print(f"{target}: {len(source_rows) - len(refused)} translated or already there, {len(refused)} refused")
        for source, reason in refused:
            print(f"  {source.type} {source.key} {source.field}: {reason}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
