package pcgen.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import pcgen.facade.core.ChooserFacade;
import pcgen.facade.core.InfoFacade;
import pcgen.facade.util.ListFacade;
import pcgen.system.ConsoleUIDelegate;
import pcgen.util.Logging;

/**
 * UIDelegate for MCP server mode. Has two chooser modes:
 * - Interactive (default): queues choices for resolve_choice tool, blocks until resolved
 * - Auto-select: picks first available options automatically (used during level-up etc.)
 *
 * Use setAutoChoose(true) before operations that may trigger choosers on the same thread
 * (like addCharacterLevels), then setAutoChoose(false) after.
 */
public class McpUIDelegate extends ConsoleUIDelegate
{
	private final Map<String, CompletableFuture<List<String>>> pendingFutures = new ConcurrentHashMap<>();
	private final Map<String, PendingChoice> pendingChoices = new ConcurrentHashMap<>();
	private final AtomicBoolean autoChoose = new AtomicBoolean(true);
	private volatile List<String> preSelectedChoices = null;
	private volatile boolean captureMode = false;
	private volatile List<String> capturedOptions = null;

	public void setAutoChoose(boolean auto)
	{
		autoChoose.set(auto);
	}

	public boolean isAutoChoose()
	{
		return autoChoose.get();
	}

	/**
	 * Set specific choices to auto-select when the next chooser triggers.
	 * Pass null to clear. When set, the chooser will match these names
	 * against available options instead of picking the first N.
	 */
	public void setPreSelectedChoices(List<String> choices)
	{
		this.preSelectedChoices = choices;
		if (choices != null)
		{
			autoChoose.set(true);
		}
	}

	public void clearPreSelectedChoices()
	{
		this.preSelectedChoices = null;
	}

	/**
	 * Enable capture mode: the next chooser will record available options
	 * and cancel (rollback) instead of selecting anything.
	 */
	public void enableCaptureMode()
	{
		this.captureMode = true;
		this.capturedOptions = null;
	}

	public void disableCaptureMode()
	{
		this.captureMode = false;
	}

	/**
	 * Returns the options captured during the last capture-mode chooser, or null.
	 */
	public List<String> getCapturedOptions()
	{
		return capturedOptions;
	}

	public PendingChoice getLatestPendingChoice()
	{
		return pendingChoices.values().stream().findFirst().orElse(null);
	}

	public Map<String, PendingChoice> getPendingChoices()
	{
		return Map.copyOf(pendingChoices);
	}

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
		if (captureMode)
		{
			return captureOptions(chooserFacade);
		}
		if (autoChoose.get())
		{
			return autoSelect(chooserFacade);
		}
		return interactiveSelect(chooserFacade);
	}

	private boolean captureOptions(ChooserFacade chooserFacade)
	{
		ListFacade<InfoFacade> available = chooserFacade.getAvailableList();
		List<String> options = new ArrayList<>();
		for (InfoFacade item : available)
		{
			options.add(item.toString());
		}
		this.capturedOptions = options;
		captureMode = false;

		Logging.log(Level.INFO, "MCP capture-chooser: " + chooserFacade.getName()
			+ " — captured " + options.size() + " options");

		chooserFacade.rollback();
		return false;
	}

	private boolean autoSelect(ChooserFacade chooserFacade)
	{
		ListFacade<InfoFacade> available = chooserFacade.getAvailableList();
		int remaining = chooserFacade.getRemainingSelections().get();
		List<String> preSelected = this.preSelectedChoices;

		Logging.log(Level.INFO, "MCP auto-chooser: " + chooserFacade.getName()
			+ " (" + available.getSize() + " options, selecting " + remaining
			+ (preSelected != null ? ", pre-selected: " + preSelected : "") + ")");

		if (preSelected != null && !preSelected.isEmpty())
		{
			for (String sel : preSelected)
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
			this.preSelectedChoices = null;
		}
		else
		{
			int count = 0;
			for (InfoFacade item : available)
			{
				if (count >= remaining)
				{
					break;
				}
				chooserFacade.addSelected(item);
				count++;
			}
		}
		chooserFacade.commit();
		return true;
	}

	private boolean interactiveSelect(ChooserFacade chooserFacade)
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

		Logging.log(Level.INFO, "MCP chooser pending: " + chooserFacade.getName()
			+ " (id=" + choiceId + ", " + options.size() + " options, " + remaining + " selections)");

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
