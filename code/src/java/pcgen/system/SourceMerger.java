package pcgen.system;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import pcgen.core.Campaign;
import pcgen.core.GameMode;
import pcgen.facade.core.SourceSelectionFacade;
import pcgen.facade.util.ListFacade;

/**
 * Utility class for merging source selections.
 * Allows multiple characters with different source sets to coexist
 * by computing the union of their required campaigns.
 */
public final class SourceMerger
{
	private SourceMerger()
	{
		// Utility class
	}

	/**
	 * Merge two source selections into one containing the union of their campaigns.
	 * Both selections must share the same GameMode.
	 *
	 * @param current the currently loaded source selection
	 * @param other   the source selection required by the new character
	 * @return a merged SourceSelectionFacade, or null if game modes differ
	 */
	public static SourceSelectionFacade merge(SourceSelectionFacade current, SourceSelectionFacade other)
	{
		if (current == null)
		{
			return other;
		}
		if (other == null)
		{
			return current;
		}

		GameMode currentMode = current.getGameMode().get();
		GameMode otherMode = other.getGameMode().get();

		// GameModes are singletons — use reference equality
		if (currentMode != otherMode)
		{
			return null;
		}

		Set<Campaign> merged = new LinkedHashSet<>();
		for (Campaign c : current.getCampaigns())
		{
			merged.add(c);
		}
		for (Campaign c : other.getCampaigns())
		{
			merged.add(c);
		}

		return FacadeFactory.createSourceSelection(currentMode, new ArrayList<>(merged));
	}

	/**
	 * Merge multiple source selections into one containing the union of all campaigns.
	 * All selections must share the same GameMode.
	 *
	 * @param selections the source selections to merge
	 * @return a merged SourceSelectionFacade, or null if game modes differ or list is empty
	 */
	public static SourceSelectionFacade mergeAll(List<SourceSelectionFacade> selections)
	{
		if (selections == null || selections.isEmpty())
		{
			return null;
		}

		SourceSelectionFacade result = selections.getFirst();
		for (int i = 1; i < selections.size(); i++)
		{
			result = merge(result, selections.get(i));
			if (result == null)
			{
				return null;
			}
		}
		return result;
	}

	/**
	 * Check if all campaigns in {@code subset} are contained in {@code superset}.
	 * When this returns true, the character can be opened without a source merge.
	 *
	 * @param subset   the source selection to check (typically from a character file)
	 * @param superset the source selection to check against (typically currently loaded)
	 * @return true if superset contains all campaigns of subset
	 */
	public static boolean isSubsetOf(SourceSelectionFacade subset, SourceSelectionFacade superset)
	{
		if (subset == null || superset == null)
		{
			return false;
		}

		GameMode subMode = subset.getGameMode().get();
		GameMode superMode = superset.getGameMode().get();
		if (subMode != superMode)
		{
			return false;
		}

		ListFacade<Campaign> superCampaigns = superset.getCampaigns();
		for (Campaign c : subset.getCampaigns())
		{
			if (!superCampaigns.containsElement(c))
			{
				return false;
			}
		}
		return true;
	}
}
