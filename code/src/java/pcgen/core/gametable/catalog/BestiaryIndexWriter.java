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
package pcgen.core.gametable.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import pcgen.core.Campaign;
import pcgen.core.Globals;
import pcgen.core.Kit;
import pcgen.core.Race;
import pcgen.core.gametable.CombatantSheets;
import pcgen.core.gametable.ForgedMonster;
import pcgen.core.gametable.MonsterForge;
import pcgen.core.gametable.MonsterRecipe;
import pcgen.core.gametable.MonsterSource;
import pcgen.facade.util.ListFacades;
import pcgen.session.HeadlessBootstrap;
import pcgen.session.HeadlessUIDelegate;
import pcgen.session.PcgenSession;
import pcgen.util.GracefulExit;

/**
 * Computes the index of the data's races and NPC kits that PCGen delivers, so that the bestiary filters every race
 * without building it on the game master's computer.
 *
 * <pre>java pcgen.core.gametable.catalog.BestiaryIndexWriter &lt;character file&gt; &lt;index file&gt; [extra source...]</pre>
 *
 * The character file names the sources to load, as {@code characters/tous-les-monstres.pcg} does.
 */
public final class BestiaryIndexWriter
{
	private static final String GAME_MODE = "Pathfinder_RPG";
	private static final String CAMPAIGN_LINE = "CAMPAIGN:";
	private static final int PROGRESS_EVERY = 100;

	private BestiaryIndexWriter()
	{
	}

	public static void main(String... args) throws IOException
	{
		if (args.length < 2)
		{
			System.err.println("Usage: BestiaryIndexWriter <character file> <index file> [extra source...]");
			GracefulExit.exit(2);
			return;
		}
		List<String> campaigns = new ArrayList<>(campaignsOf(Path.of(args[0])));
		campaigns.addAll(Arrays.asList(args).subList(2, args.length));
		HeadlessBootstrap.start(HeadlessBootstrap.settingsDirFrom());
		PcgenSession session = new PcgenSession();
		session.loadSources(GAME_MODE, campaigns);
		Collection<Campaign> loaded = ListFacades.wrap(session.getCurrentDataSet().getCampaigns());
		MonsterForge forge = new MonsterForge(loaded, new HeadlessUIDelegate());
		CombatantSheets sheets = new CombatantSheets(forge.builder());

		Path output = Path.of(args[1]);
		// A pass stopped midway resumes: what it already indexed is kept
		BestiaryIndex index = BestiaryIndex.read(output);
		List<String> failures = new ArrayList<>();
		List<Race> races = Globals.getContext().getReferenceContext().getConstructedCDOMObjects(Race.class).stream()
			.filter(race -> !race.isUnselected()).toList();
		List<Kit> kits = Globals.getContext().getReferenceContext().getConstructedCDOMObjects(Kit.class).stream()
			.filter(kit -> kit.isType("NPC")).toList();
		int done = 0;
		int total = races.size() + kits.size();
		for (Race race : races)
		{
			if (index.facts(new IndexKey(IndexKey.RACE, race.getKeyName())).isPresent())
			{
				report(++done, total, index, output);
				continue;
			}
			EntryFacts facts = built(forge, sheets, new MonsterSource.BestiaryRace(race.getKeyName()),
				new EntryFacts(DirectFacts.ofRace(race), Optional.empty()));
			facts.buildFailure().ifPresent(failures::add);
			index = index.with(new IndexKey(IndexKey.RACE, race.getKeyName()), facts, 0);
			report(++done, total, index, output);
		}
		for (Kit kit : kits)
		{
			if (index.facts(new IndexKey(IndexKey.KIT, kit.getKeyName())).isPresent())
			{
				report(++done, total, index, output);
				continue;
			}
			EntryFacts facts = built(forge, sheets, new MonsterSource.NpcKit(kit.getKeyName()),
				new EntryFacts(DirectFacts.ofKit(kit), Optional.empty()));
			facts.buildFailure().ifPresent(failures::add);
			index = index.with(new IndexKey(IndexKey.KIT, kit.getKeyName()), facts, 0);
			report(++done, total, index, output);
		}
		index.write(output);
		System.out.println(races.size() + " races and " + kits.size() + " NPC kits indexed in " + output + ", "
			+ failures.size() + " not built in this pass:");
		failures.forEach(failure -> System.out.println("  " + failure));
		GracefulExit.exit(0);
	}

	private static EntryFacts built(MonsterForge forge, CombatantSheets sheets, MonsterSource source, EntryFacts direct)
	{
		return switch (forge.forge(MonsterRecipe.of(source)))
		{
			case ForgedMonster.Forged forged ->
			{
				try
				{
					yield direct.with(ComputedFacts.of(forged.character(), sheets));
				}
				catch (RuntimeException unreadable)
				{
					yield new EntryFacts(direct.values(), Optional.of(source + ": " + unreadable));
				}
				finally
				{
					MonsterForge.release(forged.character());
				}
			}
			case ForgedMonster.Missing missing -> new EntryFacts(direct.values(), Optional.of(missing.reason()));
		};
	}

	private static List<String> campaignsOf(Path characterFile) throws IOException
	{
		List<String> campaigns = new ArrayList<>();
		for (String line : Files.readAllLines(characterFile))
		{
			if (line.startsWith(CAMPAIGN_LINE))
			{
				for (String part : line.split("\\|"))
				{
					if (part.startsWith(CAMPAIGN_LINE))
					{
						campaigns.add(part.substring(CAMPAIGN_LINE.length()));
					}
				}
			}
		}
		return campaigns;
	}

	private static void report(int done, int total, BestiaryIndex index, Path output) throws IOException
	{
		if (done % PROGRESS_EVERY == 0 || done == total)
		{
			index.write(output);
			System.out.println(done + " / " + total);
		}
	}
}
