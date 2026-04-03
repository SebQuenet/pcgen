package pcgen.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import pcgen.facade.core.ChooserFacade;
import pcgen.facade.core.InfoFacade;
import pcgen.facade.util.ListFacade;
import pcgen.system.ConsoleUIDelegate;
import pcgen.util.Logging;

/**
 * UIDelegate for MCP server mode. Extends ConsoleUIDelegate with two-phase
 * chooser interaction: when a chooser is triggered, the pending choice is
 * queued and can be resolved via the resolve_choice MCP tool.
 */
public class McpUIDelegate extends ConsoleUIDelegate
{
	private final Map<String, CompletableFuture<List<String>>> pendingFutures = new ConcurrentHashMap<>();
	private final Map<String, PendingChoice> pendingChoices = new ConcurrentHashMap<>();

	/**
	 * Returns the most recent pending choice, or null if none.
	 */
	public PendingChoice getLatestPendingChoice()
	{
		return pendingChoices.values().stream().findFirst().orElse(null);
	}

	public Map<String, PendingChoice> getPendingChoices()
	{
		return Map.copyOf(pendingChoices);
	}

	/**
	 * Resolve a pending choice by providing selections.
	 */
	public boolean resolveChoice(String choiceId, List<String> selections)
	{
		CompletableFuture<List<String>> future = pendingFutures.get(choiceId);
		if (future == null)
		{
			return false;
		}
		future.complete(selections);
		return true;
	}

	@Override
	public boolean showGeneralChooser(ChooserFacade chooserFacade)
	{
		String choiceId = UUID.randomUUID().toString();

		ListFacade<InfoFacade> available = chooserFacade.getAvailableList();
		List<String> options = new ArrayList<>();
		for (InfoFacade item : available)
		{
			options.add(item.toString());
		}

		int remaining = chooserFacade.getRemainingSelections().get();
		boolean requireComplete = chooserFacade.isRequireCompleteSelection();

		PendingChoice pending = new PendingChoice(choiceId, chooserFacade.getName(), options, remaining, requireComplete);
		CompletableFuture<List<String>> future = new CompletableFuture<>();

		pendingChoices.put(choiceId, pending);
		pendingFutures.put(choiceId, future);

		Logging.log(Level.INFO, "MCP chooser pending: " + chooserFacade.getName() + " (id=" + choiceId + ", " + options.size() + " options, " + remaining + " selections)");

		try
		{
			List<String> selections = future.get(300, TimeUnit.SECONDS);

			for (String sel : selections)
			{
				for (InfoFacade item : available)
				{
					if (item.toString().equalsIgnoreCase(sel) || item.getKeyName().equalsIgnoreCase(sel))
					{
						chooserFacade.addSelected(item);
						break;
					}
				}
			}
			chooserFacade.commit();
			return true;
		}
		catch (Exception e)
		{
			Logging.log(Level.WARNING, "MCP chooser timed out or failed: " + chooserFacade.getName());
			chooserFacade.rollback();
			return false;
		}
		finally
		{
			pendingChoices.remove(choiceId);
			pendingFutures.remove(choiceId);
		}
	}
}
