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
package pcgen.gui2.facade;

import java.text.Collator;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import pcgen.cdom.base.CDOMObject;
import pcgen.cdom.facet.FacetLibrary;
import pcgen.cdom.facet.input.CampaignFacet;
import pcgen.core.PlayerCharacter;
import pcgen.core.bonus.TemporaryBonusApplier;
import pcgen.core.bonus.TemporaryBonusSources;
import pcgen.core.conjurations.AppliedBonus;
import pcgen.core.conjurations.BonusSourceKey;
import pcgen.core.conjurations.BuiltCreature;
import pcgen.core.conjurations.CatalogResolution;
import pcgen.core.conjurations.ConjurationSession;
import pcgen.core.conjurations.ConjuredCreature;
import pcgen.core.conjurations.ConjuredCreatures;
import pcgen.core.conjurations.CreatureStats;
import pcgen.core.conjurations.PathfinderSummonOptions;
import pcgen.core.conjurations.SummonCatalog;
import pcgen.core.conjurations.SummonCatalogParser;
import pcgen.core.conjurations.SummonedCreatureBuilder;
import pcgen.core.conjurations.SummonForms;
import pcgen.core.conjurations.SummonParseError;
import pcgen.core.conjurations.SummonParseFailure;
import pcgen.core.conjurations.SummonParseSuccess;
import pcgen.core.conjurations.SummonSelections;
import pcgen.core.conjurations.SummonTemplate;
import pcgen.facade.core.BonusChoice;
import pcgen.facade.core.BonusLabel;
import pcgen.facade.core.ConjurationSheetFacade;
import pcgen.facade.core.ConjuredCreatureView;
import pcgen.io.conjurations.ConjurationSpreadsheet;
import pcgen.session.HeadlessUIDelegate;

/**
 * Edits the conjurations of a character on behalf of the Conjurations tab and
 * the page it shows.
 *
 * <p>
 * The character is the source of truth: every change is written straight
 * through to it. The one thing kept here is each creature in play built as a
 * character, since building one takes seconds; it is brought back in line
 * with the character's fight every time it is shown.
 */
class ConjurationSheetFacadeImpl implements ConjurationSheetFacade
{

	private final PlayerCharacter theCharacter;
	private final ConjuredCreatures creatures;
	private final SummonedCreatureBuilder builder;
	private final Map<String, CatalogCreature> catalogCreatures = new ConcurrentHashMap<>();

	private record CatalogCreature(SummonTemplate summon, boolean augmented, CreatureStats stats)
	{
	}

	ConjurationSheetFacadeImpl(PlayerCharacter pc)
	{
		theCharacter = pc;
		builder = new SummonedCreatureBuilder(
			FacetLibrary.getFacet(CampaignFacet.class).getSet(pc.getCharID()), new HeadlessUIDelegate());
		creatures = new ConjuredCreatures(builder);
	}

	@Override
	public String getCatalogSource()
	{
		return theCharacter.getDisplay().getConjurationCatalog().orElse("");
	}

	@Override
	public void setCatalogSource(String source)
	{
		catalogCreatures.clear();
		if (source == null || source.isBlank())
		{
			theCharacter.clearConjurationCatalog();
			return;
		}
		theCharacter.setConjurationCatalog(source);
	}

	@Override
	public Optional<CreatureStats> catalogStatsOf(String creature)
	{
		return catalog().find(creature).flatMap(this::catalogStatsOf);
	}

	@Override
	public byte[] catalogWorkbook(BiConsumer<Integer, Integer> progress)
	{
		SummonCatalog catalog = catalog();
		int total = (int) catalog.groups().stream().flatMap(group -> group.summons().stream())
			.filter(summon -> !summon.hasAuthoredStats()).count();
		AtomicInteger built = new AtomicInteger();
		progress.accept(0, total);
		return ConjurationSpreadsheet.workbookOf(catalog,
			(summon, level) -> PathfinderSummonOptions.maximumCount(theCharacter, summon, level), summon -> {
				Optional<CreatureStats> stats = catalogStatsOf(summon);
				progress.accept(built.incrementAndGet(), total);
				return stats;
			});
	}

	private Optional<CreatureStats> catalogStatsOf(SummonTemplate summon)
	{
		if (summon.hasAuthoredStats() || !CatalogResolution.unresolvedIn(summon).isEmpty())
		{
			return Optional.empty();
		}
		boolean augmented = SummonedCreatureBuilder.augments(theCharacter);
		CatalogCreature cached = catalogCreatures.get(summon.name());
		if (cached == null || !cached.summon().equals(summon) || cached.augmented() != augmented)
		{
			CreatureStats stats = SummonForms.statsOf(summon, "",
				builder.statsOf(builder.build(summon, augmented), summon));
			cached = new CatalogCreature(summon, augmented, stats);
			catalogCreatures.put(summon.name(), cached);
		}
		return Optional.of(cached.stats());
	}

	@Override
	public Optional<String> selectionSource(String creature)
	{
		Optional<SummonTemplate> option = catalog().find(creature);
		if (option.isEmpty() || !CatalogResolution.unresolvedIn(option.get()).isEmpty())
		{
			return Optional.empty();
		}
		SummonTemplate summon = option.get();
		if ("Versatile SM".equals(summon.metadata().get("family"))
			&& "true".equals(summon.metadata().get("choiceRequired")))
		{
			String source = getCatalogSource();
			String template = summon.templateKeys().getFirst();
			var chosen = SummonSelections.versatileMonster(source);
			return chosen.size() < 2 && !chosen.contains(template)
				? Optional.of(SummonSelections.versatileMonsterLine(template)) : Optional.empty();
		}
		if (!"Expanded".equals(summon.metadata().get("family")))
		{
			return Optional.empty();
		}
		String groupName = "Expanded Summon Monster " + summon.metadata().get("level");
		long alreadyChosen = theCharacter.getDisplay().getConjurationCatalog()
			.flatMap(SummonCatalogParser::catalogOf).stream().flatMap(found -> found.groups().stream())
			.filter(group -> group.title().equals(groupName)).mapToLong(group -> group.summons().size()).sum();
		if (alreadyChosen >= 2)
		{
			return Optional.empty();
		}
		StringBuilder source = new StringBuilder("## ")
			.append(groupName).append('\n').append("summon: ").append(summon.name())
			.append('\n').append("  race: ").append(summon.sourceRaceKey()).append('\n');
		if (!summon.templateKeys().isEmpty())
		{
			source.append("  template: ").append(String.join(", ", summon.templateKeys())).append('\n');
		}
		if (summon.metadata().containsKey("kit"))
		{
			source.append("  kit: ").append(summon.metadata().get("kit")).append('\n');
		}
		source.append("  source: ").append(summon.metadata().get("source")).append('\n');
		source.append("  level: ").append(summon.metadata().get("level")).append('\n');
		return Optional.of(source.toString());
	}

	@Override
	public List<SummonParseError> errorsIn(String source)
	{
		if (source == null || source.isBlank())
		{
			return List.of();
		}
		return switch (SummonCatalogParser.parse(source))
		{
			case SummonParseSuccess ignored -> List.of();
			case SummonParseFailure failure -> failure.errors();
		};
	}

	@Override
	public int conjure(String creature)
	{
		Optional<SummonTemplate> summon = catalog().find(creature);
		if (summon.isEmpty() || "true".equals(summon.get().metadata().get("choiceRequired"))
			|| "true".equals(summon.get().metadata().get("dailyUsed"))
			|| !CatalogResolution.unresolvedIn(summon.get()).isEmpty())
		{
			return 0;
		}
		ConjurationSession fight = theCharacter.getDisplay().getConjurationSession();
		ConjuredCreature arriving = fight.arrival(creature);
		ConjurationSession updated = fight.with(arriving);
		if ("Skeleton 1/day".equals(summon.get().metadata().get("family")))
		{
			updated = updated.withSkeletonUsed(true);
		}
		theCharacter.setConjurationSession(updated);
		return arriving.number();
	}

	@Override
	public void resetSkeletonUse()
	{
		change(fight -> fight.withSkeletonUsed(false));
	}

	@Override
	public void setDamage(String creature, int number, int damage)
	{
		change(fight -> fight.withDamage(creature, number, Math.max(damage, 0)));
	}

	@Override
	public void setRoundsLeft(String creature, int number, int rounds)
	{
		change(fight -> fight.withRoundsLeft(creature, number, Math.max(rounds, 0)));
	}

	@Override
	public boolean setForm(String creature, int number, String form)
	{
		Optional<SummonTemplate> summon = catalog().find(creature);
		if (inPlay(creature, number).isEmpty() || summon.isEmpty() || !SummonForms.supports(summon.get(), form))
		{
			return false;
		}
		change(fight -> fight.withForm(creature, number, form));
		return true;
	}

	@Override
	public void nextRound()
	{
		change(ConjurationSession::nextRound);
	}

	@Override
	public void dismiss(String creature, int number)
	{
		change(fight -> fight.dismiss(creature, number));
		creatures.forget(creature, number);
	}

	@Override
	public void dismissAll()
	{
		change(ConjurationSession::dismissAll);
		creatures.forgetAll();
	}

	@Override
	public void setDuration(int rounds)
	{
		change(fight -> fight.withDuration(Math.max(rounds, 0)));
	}

	@Override
	public List<BonusLabel> groupBonuses()
	{
		return labelsOf(fight().groupBonuses());
	}

	@Override
	public List<BonusChoice> groupBonusChoices()
	{
		Collator alphabetical = Collator.getInstance(Locale.getDefault());
		return choicesOf(TemporaryBonusSources.castableOnOthers(), theCharacter).stream()
			.sorted((left, right) -> alphabetical.compare(left.label(), right.label())).toList();
	}

	@Override
	public List<BonusChoice> bonusChoices(String creature, int number)
	{
		return built(creature, number)
			.map(found -> choicesOf(TemporaryBonusSources.availableTo(found.character()), found.character()))
			.orElse(List.of());
	}

	@Override
	public Optional<ConjuredCreatureView> viewOf(String creature, int number)
	{
		SummonCatalog catalog = catalog();
		boolean augmented = SummonedCreatureBuilder.augments(theCharacter);
		return inPlay(creature, number).flatMap(found -> catalog.find(found.creature())
			.filter(summon -> !summon.hasAuthoredStats()).map(summon -> {
				BuiltCreature built = creatures.creature(summon, augmented, found, fight().groupBonuses());
				return new ConjuredCreatureView(found.creature(), found.number(), built.stats(), built.baseStats(),
					labelsOf(found.bonuses()),
					creatures.templateDifferences(summon, augmented, found.form(), built.baseStats()));
			}));
	}

	@Override
	public List<ConjuredCreatureView> addGroupBonus(String key, OptionalInt value)
	{
		Optional<AppliedBonus> bonus = bonusOf(key, value);
		if (bonus.isEmpty())
		{
			return List.of();
		}
		change(fight -> fight.withGroupBonus(bonus.get()));
		return everyView();
	}

	@Override
	public List<ConjuredCreatureView> removeGroupBonus(String key)
	{
		Optional<BonusSourceKey> source = BonusSourceKey.parse(key);
		source.ifPresent(found -> change(fight -> fight.withoutGroupBonus(found)));
		return everyView();
	}

	@Override
	public List<ConjuredCreatureView> addBonus(String creature, int number, String key, OptionalInt value)
	{
		Optional<AppliedBonus> bonus = bonusOf(key, value);
		if (bonus.isEmpty() || inPlay(creature, number).isEmpty())
		{
			return List.of();
		}
		change(fight -> fight.withBonus(creature, number, bonus.get()));
		return viewOf(creature, number).stream().toList();
	}

	@Override
	public List<ConjuredCreatureView> removeBonus(String creature, int number, String key)
	{
		Optional<BonusSourceKey> source = BonusSourceKey.parse(key);
		source.ifPresent(found -> change(fight -> fight.withoutBonus(creature, number, found)));
		return viewOf(creature, number).stream().toList();
	}

	private ConjurationSession fight()
	{
		return theCharacter.getDisplay().getConjurationSession();
	}

	private Optional<ConjuredCreature> inPlay(String creature, int number)
	{
		return fight().conjured().stream().filter(found -> found.is(creature, number)).findFirst();
	}

	private Optional<BuiltCreature> built(String creature, int number)
	{
		SummonCatalog catalog = catalog();
		return inPlay(creature, number).flatMap(found -> catalog.find(found.creature())
			.filter(summon -> !summon.hasAuthoredStats())
			.map(summon -> creatures.creature(summon, SummonedCreatureBuilder.augments(theCharacter), found,
				fight().groupBonuses())));
	}

	private SummonCatalog catalog()
	{
		return PathfinderSummonOptions.withPublishedOptions(theCharacter,
			theCharacter.getDisplay().getConjurationCatalog().flatMap(SummonCatalogParser::catalogOf));
	}

	private List<ConjuredCreatureView> everyView()
	{
		List<ConjuredCreatureView> views = new ArrayList<>();
		for (ConjuredCreature creature : fight().conjured())
		{
			viewOf(creature.creature(), creature.number()).ifPresent(views::add);
		}
		return views;
	}

	private static Optional<AppliedBonus> bonusOf(String key, OptionalInt value)
	{
		return BonusSourceKey.parse(key).filter(source -> TemporaryBonusSources.find(source).isPresent())
			.map(source -> new AppliedBonus(source, value));
	}

	private static List<BonusLabel> labelsOf(List<AppliedBonus> bonuses)
	{
		return bonuses.stream()
			.map(bonus -> new BonusLabel(bonus.source().text(), TemporaryBonusSources.labelOf(bonus.source()))).toList();
	}

	private static List<BonusChoice> choicesOf(List<CDOMObject> sources, PlayerCharacter reader)
	{
		return sources.stream().map(source -> new BonusChoice(TemporaryBonusSources.keyOf(source).text(),
			source.getDisplayName(), TemporaryBonusApplier.choiceRange(reader, source))).toList();
	}

	private void change(UnaryOperator<ConjurationSession> change)
	{
		theCharacter.setConjurationSession(change.apply(theCharacter.getDisplay().getConjurationSession()));
	}
}
