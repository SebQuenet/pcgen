/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.persistence.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import pcgen.cdom.base.CDOMObject;
import pcgen.core.Ability;
import pcgen.core.AbilityCategory;
import pcgen.core.Campaign;
import pcgen.core.spell.Spell;
import pcgen.gui2.util.treetable.TreeTableNode;
import pcgen.rules.context.AbstractReferenceContext;
import pcgen.rules.context.ConsolidatedListCommitStrategy;
import pcgen.rules.context.RuntimeLoadContext;
import pcgen.rules.context.RuntimeReferenceContext;
import pcgen.util.Comparators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behaviour of finding the translation table that sits beside each loaded campaign.
 */
class TranslationLoaderTest
{
	@TempDir
	Path dataFolder;

	private AbstractReferenceContext references;
	private Spell magicMissile;
	private Campaign coreRulebook;

	@BeforeEach
	void setUp()
	{
		references = new RuntimeLoadContext(RuntimeReferenceContext.createRuntimeReferenceContext(),
			new ConsolidatedListCommitStrategy()).getReferenceContext();
		magicMissile = references.constructCDOMObject(Spell.class, "Magic Missile");
		coreRulebook = new Campaign();
		coreRulebook.setSourceURI(dataFolder.resolve("core_rulebook.pcc").toUri());
	}

	private void writeTable(String fileName, String content) throws IOException
	{
		Files.writeString(dataFolder.resolve(fileName), content, StandardCharsets.UTF_8);
	}

	@Test
	void readsTheTableNamedAfterTheCampaignAndTheLanguage() throws IOException
	{
		writeTable("core_rulebook.fr.tsv", "SPELL\t\tMagic Missile\tNAME\tProjectile magique\n");

		TranslationLoader.load(List.of(coreRulebook), "fr", references);

		assertEquals("Projectile magique", magicMissile.toString());
	}

	@Test
	void ignoresATableInAnotherLanguage() throws IOException
	{
		writeTable("core_rulebook.de.tsv", "SPELL\t\tMagic Missile\tNAME\tMagisches Geschoss\n");

		TranslationLoader.load(List.of(coreRulebook), "fr", references);

		assertEquals("Magic Missile", magicMissile.toString());
	}

	@Test
	void reportsNothingForACampaignWithoutATable()
	{
		List<String> problems = TranslationLoader.load(List.of(coreRulebook), "fr", references);

		assertTrue(problems.isEmpty());
	}

	@Test
	void namesTheFileAndLineOfEachProblem() throws IOException
	{
		writeTable("core_rulebook.fr.tsv", "SPELL\t\tFireball\tNAME\tBoule de feu\n");

		List<String> problems = TranslationLoader.load(List.of(coreRulebook), "fr", references);

		assertTrue(problems.getFirst().contains("core_rulebook.fr.tsv:1"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void findsFeatNamesInAnotherPathfinderBookAndSortsByFrenchName() throws IOException
	{
		Path guide = dataFolder.resolve("pathfinder/paizo/roleplaying_game/adventurers_guide");
		Path agents = dataFolder.resolve("pathfinder/paizo/player_companion/agents_of_evil");
		Files.createDirectories(guide);
		Files.createDirectories(agents);
		Files.writeString(agents.resolve("_agents_of_evil.fr.tsv"),
			"ABILITY\tFEAT\tAspis Partner\tNAME\tPartenaire de l'Aspis\n", StandardCharsets.UTF_8);
		Campaign adventurersGuide = new Campaign();
		adventurersGuide.setSourceURI(guide.resolve("_adventurers_guide.pcc").toUri());
		references.importObject(AbilityCategory.FEAT);
		Ability aspisPartner = AbilityCategory.FEAT.newInstance();
		aspisPartner.setName("Aspis Partner");
		references.importObject(aspisPartner);
		Ability aberrantTumor = AbilityCategory.FEAT.newInstance();
		aberrantTumor.setName("Aberrant Tumor");
		aberrantTumor.setLocalizedName("Tumeur aberrante");
		references.importObject(aberrantTumor);

		TranslationLoader.load(List.of(adventurersGuide), "fr", references);

		assertEquals("Partenaire de l'Aspis", aspisPartner.toString());
		assertEquals("Aspis Partner", aspisPartner.getKeyName());
		assertTrue(CDOMObject.P_OBJECT_NAME_COMP.compare(aspisPartner, aberrantTumor) < 0);
		Comparator<Object> treeComparator = (Comparator<Object>) (Comparator<?>)
			Comparators.getComparatorFor(TreeTableNode.class);
		assertTrue(treeComparator.compare(aspisPartner, aberrantTumor) < 0);
	}

	@Test
	void loadedBookKeepsItsNameWhenAnotherBookUsesADifferentTranslation() throws IOException
	{
		Path guide = dataFolder.resolve("pathfinder/paizo/roleplaying_game/adventurers_guide");
		Path agents = dataFolder.resolve("pathfinder/paizo/player_companion/agents_of_evil");
		Files.createDirectories(guide);
		Files.createDirectories(agents);
		Files.writeString(guide.resolve("_adventurers_guide.fr.tsv"),
			"SPELL\t\tMagic Missile\tNAME\tProjectile magique\n", StandardCharsets.UTF_8);
		Files.writeString(agents.resolve("_agents_of_evil.fr.tsv"),
			"SPELL\t\tMagic Missile\tNAME\tTrait magique\n", StandardCharsets.UTF_8);
		Campaign adventurersGuide = new Campaign();
		adventurersGuide.setSourceURI(guide.resolve("_adventurers_guide.pcc").toUri());

		TranslationLoader.load(List.of(adventurersGuide), "fr", references);

		assertEquals("Projectile magique", magicMissile.toString());
	}
}
