package pcgen.mcp;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import pcgen.cdom.enumeration.ListKey;
import pcgen.core.Campaign;
import pcgen.core.Globals;
import pcgen.core.prereq.Prerequisite;
import pcgen.core.prereq.PrerequisiteOperator;
import pcgen.facade.util.ListFacade;
import pcgen.persistence.PersistenceManager;
import pcgen.util.Logging;

/**
 * Automatically resolves campaign prerequisites by analyzing PRECAMPAIGN tags
 * and adding required campaigns from the available sources for the game mode.
 *
 * <p>This handles the common prerequisite patterns:
 * <ul>
 *   <li>{@code INCLUDES=CampaignName} - requires a specific campaign by key/name</li>
 *   <li>{@code INCLUDESBOOKTYPE=BookType} - requires any campaign with a given book type</li>
 *   <li>{@code BOOKTYPE=BookType} - requires any campaign with a given book type</li>
 *   <li>Bare campaign name - direct key/name match</li>
 * </ul>
 */
public final class CampaignDependencyResolver
{
	private static final int MAX_ITERATIONS = 20;

	private CampaignDependencyResolver()
	{
	}

	/**
	 * Result of dependency resolution.
	 */
	public static final class ResolvedCampaigns
	{
		private final List<Campaign> campaigns;
		private final List<Campaign> autoAdded;
		private final List<String> warnings;

		ResolvedCampaigns(List<Campaign> campaigns, List<Campaign> autoAdded, List<String> warnings)
		{
			this.campaigns = List.copyOf(campaigns);
			this.autoAdded = List.copyOf(autoAdded);
			this.warnings = List.copyOf(warnings);
		}

		public List<Campaign> getCampaigns()
		{
			return campaigns;
		}

		public List<Campaign> getAutoAdded()
		{
			return autoAdded;
		}

		public List<String> getWarnings()
		{
			return warnings;
		}
	}

	/**
	 * Resolves all campaign prerequisites, automatically adding missing dependencies.
	 *
	 * @param requested the campaigns explicitly requested by the user
	 * @param available all campaigns available for the game mode
	 * @return the resolved set including auto-added dependencies
	 */
	public static ResolvedCampaigns resolve(List<Campaign> requested, ListFacade<Campaign> available)
	{
		List<Campaign> availableList = StreamSupport
			.stream(available.spliterator(), false)
			.toList();

		Set<Campaign> currentSet = new LinkedHashSet<>(requested);
		List<Campaign> autoAdded = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++)
		{
			List<Campaign> newDeps = findMissingDependencies(currentSet, availableList, warnings);
			if (newDeps.isEmpty())
			{
				break;
			}
			autoAdded.addAll(newDeps);
			currentSet.addAll(newDeps);
		}

		// Final validation using the same mechanism as the GUI
		if (!passesPrereqs(new ArrayList<>(currentSet)))
		{
			warnings.add("Some prerequisites could not be automatically resolved. "
				+ "The sources may load with errors.");
		}

		return new ResolvedCampaigns(new ArrayList<>(currentSet), autoAdded, warnings);
	}

	/**
	 * Finds campaigns that are required by the current set but not yet included.
	 */
	private static List<Campaign> findMissingDependencies(
		Set<Campaign> currentSet, List<Campaign> available, List<String> warnings)
	{
		List<Campaign> newDeps = new ArrayList<>();
		Set<URI> currentURIs = buildURISet(currentSet);

		for (Campaign campaign : currentSet)
		{
			List<Prerequisite> prereqs = campaign.getPrerequisiteList();
			for (Prerequisite prereq : prereqs)
			{
				collectMissingFromPrereq(prereq, currentSet, currentURIs,
					available, newDeps, warnings);
			}
		}
		return newDeps;
	}

	/**
	 * Recursively walks the prerequisite tree to find missing campaigns.
	 */
	private static void collectMissingFromPrereq(
		Prerequisite prereq, Set<Campaign> currentSet, Set<URI> currentURIs,
		List<Campaign> available, List<Campaign> newDeps, List<String> warnings)
	{
		if (prereq.getKind() == null)
		{
			// PREMULT node — walk children
			// For OR logic (operand < children count), we only need to satisfy 'operand' of them.
			// For AND logic (operand == children count), we need all.
			// Strategy: try to satisfy as many as possible, starting with already-satisfied ones.
			int required = parseOperand(prereq);
			List<Prerequisite> children = prereq.getPrerequisites();
			int alreadySatisfied = 0;
			List<Prerequisite> unsatisfied = new ArrayList<>();

			for (Prerequisite child : children)
			{
				if (isPrereqSatisfied(child, currentSet, currentURIs))
				{
					alreadySatisfied++;
				}
				else
				{
					unsatisfied.add(child);
				}
			}

			int stillNeeded = required - alreadySatisfied;
			if (stillNeeded <= 0)
			{
				return; // Already satisfied
			}

			// Try to resolve 'stillNeeded' of the unsatisfied children
			int resolved = 0;
			for (Prerequisite child : unsatisfied)
			{
				if (resolved >= stillNeeded)
				{
					break;
				}
				int sizeBefore = newDeps.size();
				collectMissingFromPrereq(child, currentSet, currentURIs,
					available, newDeps, warnings);
				if (newDeps.size() > sizeBefore)
				{
					resolved++;
				}
			}
			return;
		}

		if (!"campaign".equalsIgnoreCase(prereq.getKind()))
		{
			return; // Not a campaign prerequisite
		}

		// Skip negated prerequisites (exclusions like !PRECAMPAIGN)
		if (prereq.getOperator() == PrerequisiteOperator.LT)
		{
			return;
		}

		String key = prereq.getKey();
		if (key == null || key.isEmpty())
		{
			return;
		}

		// Check if already satisfied
		if (isKeyAlreadySatisfied(key, currentSet, currentURIs))
		{
			return;
		}

		// Also check against already-planned new deps
		Set<Campaign> extendedSet = new LinkedHashSet<>(currentSet);
		extendedSet.addAll(newDeps);
		Set<URI> extendedURIs = buildURISet(extendedSet);
		if (isKeyAlreadySatisfied(key, extendedSet, extendedURIs))
		{
			return;
		}

		// Try to find a campaign that satisfies this prerequisite
		Campaign match = findCampaignForKey(key, available, currentSet, newDeps);
		if (match != null)
		{
			newDeps.add(match);
			Logging.log(Logging.INFO, "Auto-resolved dependency: " + match.getKeyName()
				+ " (required by prerequisite: " + key + ")");
		}
		else
		{
			warnings.add("Could not auto-resolve prerequisite: " + key);
		}
	}

	/**
	 * Checks if a specific prerequisite key is already satisfied by the current set.
	 */
	private static boolean isKeyAlreadySatisfied(String key, Set<Campaign> currentSet, Set<URI> currentURIs)
	{
		if (key.startsWith("BOOKTYPE="))
		{
			String bookType = key.substring(9);
			return hasCampaignWithBookType(currentSet, bookType, false);
		}
		else if (key.startsWith("INCLUDESBOOKTYPE="))
		{
			String bookType = key.substring(17);
			return hasCampaignWithBookType(currentSet, bookType, true);
		}
		else if (key.startsWith("INCLUDES="))
		{
			String name = key.substring(9);
			return hasCampaignByName(currentSet, name, true);
		}
		else
		{
			return hasCampaignByName(currentSet, key, false);
		}
	}

	/**
	 * Checks if a single prerequisite node is already satisfied.
	 */
	private static boolean isPrereqSatisfied(
		Prerequisite prereq, Set<Campaign> currentSet, Set<URI> currentURIs)
	{
		if (prereq.getKind() == null)
		{
			// PREMULT — check children
			int required = parseOperand(prereq);
			int satisfied = 0;
			for (Prerequisite child : prereq.getPrerequisites())
			{
				if (isPrereqSatisfied(child, currentSet, currentURIs))
				{
					satisfied++;
				}
			}
			return satisfied >= required;
		}

		if (!"campaign".equalsIgnoreCase(prereq.getKind()))
		{
			return true; // Not a campaign prereq, assume satisfied
		}

		if (prereq.getOperator() == PrerequisiteOperator.LT)
		{
			return true; // Negated prereq — not a dependency to resolve
		}

		String key = prereq.getKey();
		return key != null && isKeyAlreadySatisfied(key, currentSet, currentURIs);
	}

	/**
	 * Finds a campaign from the available list that matches the given prerequisite key.
	 */
	private static Campaign findCampaignForKey(
		String key, List<Campaign> available, Set<Campaign> currentSet, List<Campaign> newDeps)
	{
		Set<Campaign> excluded = new LinkedHashSet<>(currentSet);
		excluded.addAll(newDeps);

		if (key.startsWith("BOOKTYPE=") || key.startsWith("INCLUDESBOOKTYPE="))
		{
			String bookType;
			boolean includesSub;
			if (key.startsWith("INCLUDESBOOKTYPE="))
			{
				bookType = key.substring(17);
				includesSub = true;
			}
			else
			{
				bookType = key.substring(9);
				includesSub = false;
			}
			return findCampaignByBookType(available, bookType, includesSub, excluded);
		}
		else
		{
			String name = key.startsWith("INCLUDES=") ? key.substring(9) : key;
			return findCampaignByName(available, name, excluded);
		}
	}

	/**
	 * Checks if any campaign in the set has the given book type.
	 */
	private static boolean hasCampaignWithBookType(
		Set<Campaign> campaigns, String bookType, boolean includeSubCampaigns)
	{
		for (Campaign campaign : campaigns)
		{
			List<Campaign> toCheck;
			if (includeSubCampaigns)
			{
				toCheck = getFullCampaignList(campaign);
			}
			else
			{
				toCheck = Collections.singletonList(campaign);
			}
			for (Campaign c : toCheck)
			{
				for (String bt : c.getSafeListFor(ListKey.BOOK_TYPE))
				{
					if (bookType.equalsIgnoreCase(bt))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if any campaign in the set matches the given name/key.
	 */
	private static boolean hasCampaignByName(
		Set<Campaign> campaigns, String name, boolean includeSubCampaigns)
	{
		Campaign target = Globals.getCampaignKeyedSilently(name);
		if (target == null)
		{
			return false;
		}
		for (Campaign campaign : campaigns)
		{
			if (includeSubCampaigns)
			{
				for (Campaign c : getFullCampaignList(campaign))
				{
					if (c.equals(target))
					{
						return true;
					}
				}
			}
			else
			{
				if (campaign.equals(target))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Finds a campaign with the given book type from the available list.
	 * Prefers campaigns that have fewer prerequisites themselves (simpler/more fundamental).
	 */
	private static Campaign findCampaignByBookType(
		List<Campaign> available, String bookType, boolean includeSubCampaigns,
		Set<Campaign> excluded)
	{
		Campaign best = null;
		int bestPrereqCount = Integer.MAX_VALUE;

		for (Campaign campaign : available)
		{
			if (excluded.contains(campaign))
			{
				continue;
			}
			List<Campaign> toCheck;
			if (includeSubCampaigns)
			{
				toCheck = getFullCampaignList(campaign);
			}
			else
			{
				toCheck = Collections.singletonList(campaign);
			}
			for (Campaign c : toCheck)
			{
				for (String bt : c.getSafeListFor(ListKey.BOOK_TYPE))
				{
					if (bookType.equalsIgnoreCase(bt))
					{
						int prereqCount = campaign.getPrerequisiteCount();
						if (best == null || prereqCount < bestPrereqCount)
						{
							best = campaign;
							bestPrereqCount = prereqCount;
						}
					}
				}
			}
		}
		return best;
	}

	/**
	 * Finds a campaign by name/key from the available list.
	 */
	private static Campaign findCampaignByName(
		List<Campaign> available, String name, Set<Campaign> excluded)
	{
		Campaign target = Globals.getCampaignKeyedSilently(name);
		if (target == null)
		{
			return null;
		}
		for (Campaign campaign : available)
		{
			if (excluded.contains(campaign))
			{
				continue;
			}
			if (campaign.equals(target))
			{
				return campaign;
			}
		}
		return null;
	}

	/**
	 * Builds a set of URIs for the given campaigns.
	 */
	private static Set<URI> buildURISet(Set<Campaign> campaigns)
	{
		Set<URI> uris = new LinkedHashSet<>();
		for (Campaign campaign : campaigns)
		{
			uris.add(campaign.getSourceURI());
		}
		return uris;
	}

	/**
	 * Gets a campaign and all its sub-campaigns recursively.
	 */
	private static List<Campaign> getFullCampaignList(Campaign campaign)
	{
		List<Campaign> result = new ArrayList<>();
		addChildrenRecursively(result, campaign);
		return result;
	}

	private static void addChildrenRecursively(List<Campaign> list, Campaign campaign)
	{
		list.add(campaign);
		for (Campaign sub : campaign.getSubCampaigns())
		{
			addChildrenRecursively(list, sub);
		}
	}

	private static int parseOperand(Prerequisite prereq)
	{
		try
		{
			return Integer.parseInt(prereq.getOperand());
		}
		catch (NumberFormatException e)
		{
			return 1;
		}
	}

	/**
	 * Checks if all campaigns in the list pass their prerequisites,
	 * using the same mechanism as the GUI ({@code FacadeFactory.passesPrereqs}).
	 */
	private static boolean passesPrereqs(List<Campaign> campaigns)
	{
		PersistenceManager pman = PersistenceManager.getInstance();
		List<URI> oldList = pman.getChosenCampaignSourcefiles();
		List<URI> uris = new ArrayList<>();
		for (Campaign campaign : campaigns)
		{
			uris.add(campaign.getSourceURI());
		}
		pman.setChosenCampaignSourcefiles(uris);
		try
		{
			for (Campaign campaign : campaigns)
			{
				if (!campaign.qualifies(null, campaign))
				{
					return false;
				}
			}
			return true;
		}
		finally
		{
			pman.setChosenCampaignSourcefiles(oldList);
		}
	}
}
