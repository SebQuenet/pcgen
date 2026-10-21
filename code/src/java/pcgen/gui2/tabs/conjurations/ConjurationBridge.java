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
package pcgen.gui2.tabs.conjurations;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import pcgen.core.conjurations.AverageDamage;
import pcgen.core.conjurations.CreatureStats;
import pcgen.facade.core.BonusChoice;
import pcgen.facade.core.ConjurationSheetFacade;
import pcgen.gui2.tools.DesktopBrowserLauncher;
import pcgen.io.conjurations.PathfinderSummonPortraits;
import pcgen.util.Logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * What the conjurations page can call back into. Planted on the page's window
 * object so that conjuring, wounding and dismissing a creature reaches the
 * character.
 *
 * <p>
 * Public, with public methods, because JavaScript running in the WebView
 * reaches it by reflection. The same page opened in an ordinary browser finds
 * no bridge and keeps the fight to itself.
 */
public final class ConjurationBridge implements AutoCloseable
{

	/** Shows one of the published references to the user. */
	@FunctionalInterface
	public interface ReferenceBrowser
	{
		void browse(URI reference) throws IOException;
	}

	/** What the page passes for a bonus that asks no value. */
	public static final int NO_VALUE = -1;

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final String PENDING = "{\"pending\":true}";
	private static final Set<String> REFERENCE_HOSTS = Set.of("rpgbot.net", "legacy.aonprd.com",
		"aonprd.com", "www.aonprd.com", "www.d20pfsrd.com");

	private final ConjurationSheetFacade sheet;
	private final Function<String, Boolean> selectPublished;
	private final BooleanSupplier visible;
	private final ReferenceBrowser browser;
	private final ExecutorService catalogWorker = Executors.newSingleThreadExecutor(task -> {
		Thread worker = new Thread(task, "pcgen-conjuration-catalog");
		worker.setDaemon(true);
		worker.setPriority(Thread.MIN_PRIORITY);
		return worker;
	});
	private final Map<String, Future<Optional<CreatureStats>>> catalogJobs = new HashMap<>();
	private String catalogSource = "";

	/**
	 * @param sheet the conjurations to write through. Never null.
	 */
	public ConjurationBridge(ConjurationSheetFacade sheet)
	{
		this(sheet, ignored -> false, () -> true);
	}

	/** @param selectPublished records a chosen published option in the source editor. */
	public ConjurationBridge(ConjurationSheetFacade sheet, Function<String, Boolean> selectPublished)
	{
		this(sheet, selectPublished, () -> true);
	}

	/** @param visible whether the page is currently shown to the user. */
	public ConjurationBridge(ConjurationSheetFacade sheet, Function<String, Boolean> selectPublished,
		BooleanSupplier visible)
	{
		this(sheet, selectPublished, visible, DesktopBrowserLauncher::viewInBrowser);
	}

	/** @param browser shows a published reference to the user. */
	public ConjurationBridge(ConjurationSheetFacade sheet, Function<String, Boolean> selectPublished,
		BooleanSupplier visible, ReferenceBrowser browser)
	{
		if (sheet == null)
		{
			throw new IllegalArgumentException("A conjuration bridge needs conjurations to write through");
		}
		this.sheet = sheet;
		this.selectPublished = selectPublished;
		this.visible = visible;
		this.browser = browser;
	}

	/** Selects one of the fixed choices offered by Expanded Summon Monster. */
	public boolean selectPublished(String creature)
	{
		return selectPublished.apply(creature);
	}

	/**
	 * Called from the page when a creature of the catalog is conjured.
	 *
	 * @param creature the name of its stat line.
	 * @return the number it was given.
	 */
	public int conjure(String creature)
	{
		return sheet.conjure(creature);
	}

	/**
	 * Called from the page when a creature in play is harmed or healed.
	 *
	 * @param creature its name.
	 * @param number   its number.
	 * @param damage   the total damage it has taken.
	 */
	public void setDamage(String creature, int number, int damage)
	{
		sheet.setDamage(creature, number, damage);
	}

	/**
	 * Called from the page when the rounds left to a creature are corrected.
	 *
	 * @param creature its name.
	 * @param number   its number.
	 * @param rounds   the rounds it has left.
	 */
	public void setRoundsLeft(String creature, int number, int rounds)
	{
		sheet.setRoundsLeft(creature, number, rounds);
	}

	/**
	 * Called from the page when the fight moves on a round.
	 */
	public void nextRound()
	{
		sheet.nextRound();
	}

	/**
	 * Called from the page when a creature is sent away.
	 *
	 * @param creature its name.
	 * @param number   its number.
	 */
	public void dismiss(String creature, int number)
	{
		sheet.dismiss(creature, number);
	}

	/**
	 * Called from the page when every creature is sent away.
	 */
	public void dismissAll()
	{
		sheet.dismissAll();
	}

	/**
	 * Called from the page when the duration given to new creatures changes.
	 *
	 * @param rounds how many rounds a new creature stays, 0 not to count them.
	 */
	public void setDuration(int rounds)
	{
		sheet.setDuration(rounds);
	}

	/**
	 * @return the group bonuses, as JSON: a list of {key, label}.
	 */
	public String groupBonuses()
	{
		return json(sheet.groupBonuses());
	}

	/**
	 * @return what may be laid on every creature, as JSON: a list of {key, label, range}.
	 */
	public String groupBonusChoices()
	{
		return json(sheet.groupBonusChoices().stream().map(ConjurationBridge::choiceOf).toList());
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @return what may be laid on that creature, as JSON: a list of {key, label, range}.
	 */
	public String bonusChoices(String creature, int number)
	{
		return json(sheet.bonusChoices(creature, number).stream().map(ConjurationBridge::choiceOf).toList());
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @return the creature as JSON, or "null" when it is not in play.
	 */
	public String view(String creature, int number)
	{
		return json(sheet.viewOf(creature, number).orElse(null));
	}

	/** Records a creature's selected alternate form on the character. */
	public boolean setForm(String creature, int number, String form)
	{
		return sheet.setForm(creature, number, form);
	}

	/** Resets the daily use of Skeleton Summoner after resting. */
	public void resetSkeletonUse()
	{
		sheet.resetSkeletonUse();
	}

	/** @return computed catalog metrics against the chosen AC, or null for a handwritten entry. */
	public String catalogStats(String creature, int targetArmorClass)
	{
		String source = sheet.getCatalogSource();
		if (!source.equals(catalogSource))
		{
			catalogJobs.values().forEach(job -> job.cancel(false));
			catalogJobs.clear();
			catalogSource = source;
		}
		if (!visible.getAsBoolean())
		{
			return PENDING;
		}
		Future<Optional<CreatureStats>> job;
		try
		{
			job = catalogJobs.computeIfAbsent(creature,
				ignored -> catalogWorker.submit(() -> sheet.catalogStatsOf(creature)));
		}
		catch (RejectedExecutionException closed)
		{
			return PENDING;
		}
		if (!job.isDone())
		{
			return PENDING;
		}
		try
		{
			Optional<CreatureStats> stats = job.get();
			catalogJobs.remove(creature);
			return json(stats.map(value -> catalogMetrics(value, targetArmorClass)).orElse(null));
		}
		catch (CancellationException | ExecutionException | InterruptedException error)
		{
			catalogJobs.remove(creature);
			if (error instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			Logging.errorPrint("Could not calculate conjuration catalog stats", error);
			return "null";
		}
	}

	@Override
	public void close()
	{
		catalogWorker.shutdownNow();
	}

	/** Computed metrics for one creature in play, including its active bonuses. */
	public String inPlayStats(String creature, int number, int targetArmorClass)
	{
		return json(sheet.viewOf(creature, number)
			.map(view -> catalogMetrics(view.stats(), targetArmorClass)).orElse(null));
	}

	/** The race's illustration as a data URI, or an empty string when it has none. */
	public String portrait(String race)
	{
		return PathfinderSummonPortraits.dataUriFor(race).orElse("");
	}

	/**
	 * Opens one of the published rules or guide references in the system browser.
	 * The browser starts on its own thread: on Linux, AWT's Desktop called from the
	 * JavaFX thread never returns and freezes the whole application.
	 */
	public void openReference(String address)
	{
		URI reference;
		try
		{
			reference = URI.create(address);
		}
		catch (IllegalArgumentException error)
		{
			Logging.errorPrint("Could not open conjuration reference", error);
			return;
		}
		if ("https".equals(reference.getScheme()) && reference.getHost() != null
			&& REFERENCE_HOSTS.contains(reference.getHost()))
		{
			Thread.ofPlatform().daemon().name("pcgen-conjuration-reference").start(() -> {
				try
				{
					browser.browse(reference);
				}
				catch (IOException error)
				{
					Logging.errorPrint("Could not open conjuration reference", error);
				}
			});
		}
	}

	private static Map<String, Object> catalogMetrics(CreatureStats stats, int targetArmorClass)
	{
		Map<String, Object> values = new LinkedHashMap<>();
		OptionalDouble damage = AverageDamage.expectedAgainst(stats.attacks(), targetArmorClass);
		values.put("hitDice", stats.hitDice());
		values.put("expectedDamagePerRound", damage.isPresent() ? damage.getAsDouble() : null);
		values.put("armorClass", stats.armorClass());
		values.put("touchArmorClass", stats.touchArmorClass());
		values.put("flatFootedArmorClass", stats.flatFootedArmorClass());
		values.put("combatManeuverBonus", stats.combatManeuverBonus());
		values.put("combatManeuverDefense", stats.combatManeuverDefense());
		values.put("maximumHitPoints", stats.maximumHitPoints());
		values.put("speed", stats.speed());
		values.put("attacks", stats.attacks());
		values.put("reflex", stats.reflex());
		values.put("fortitude", stats.fortitude());
		values.put("will", stats.will());
		values.put("initiative", stats.initiative());
		return values;
	}

	/**
	 * @param key   the key of the bonus's source
	 * @param value the value chosen, or {@value #NO_VALUE} for a bonus that asks none
	 * @return every creature in play, refreshed, as JSON.
	 */
	public String addGroupBonus(String key, int value)
	{
		return json(sheet.addGroupBonus(key, valueOf(value)));
	}

	/**
	 * @param key the key of a group bonus's source
	 * @return every creature in play, refreshed, as JSON.
	 */
	public String removeGroupBonus(String key)
	{
		return json(sheet.removeGroupBonus(key));
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param key      the key of the bonus's source
	 * @param value    the value chosen, or {@value #NO_VALUE} for a bonus that asks none
	 * @return that creature, refreshed, as JSON.
	 */
	public String addBonus(String creature, int number, String key, int value)
	{
		return json(sheet.addBonus(creature, number, key, valueOf(value)));
	}

	/**
	 * @param creature the creature's name
	 * @param number   its number
	 * @param key      the key of that creature's own bonus's source
	 * @return that creature, refreshed, as JSON.
	 */
	public String removeBonus(String creature, int number, String key)
	{
		return json(sheet.removeBonus(creature, number, key));
	}

	private static OptionalInt valueOf(int value)
	{
		return (value == NO_VALUE) ? OptionalInt.empty() : OptionalInt.of(value);
	}

	private static Map<String, Object> choiceOf(BonusChoice choice)
	{
		Map<String, Object> written = new LinkedHashMap<>();
		written.put("key", choice.key());
		written.put("label", choice.label());
		written.put("range", choice.range().orElse(null));
		return written;
	}

	private static String json(Object value)
	{
		try
		{
			return JSON.writeValueAsString(value);
		}
		catch (JsonProcessingException e)
		{
			Logging.errorPrint("Could not write conjurations for the page", e);
			return "null";
		}
	}
}
