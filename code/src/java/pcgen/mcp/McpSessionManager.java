package pcgen.mcp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import pcgen.core.Campaign;
import pcgen.core.GameMode;
import pcgen.core.Globals;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.SourceSelectionFacade;
import pcgen.facade.util.DefaultListFacade;
import pcgen.facade.util.ListFacade;
import pcgen.persistence.SourceFileLoader;
import pcgen.system.CharacterManager;
import pcgen.system.FacadeFactory;
import pcgen.system.SourceMerger;
import pcgen.util.Logging;
import pcgen.util.chooser.ChooserFactory;

public class McpSessionManager
{
	private DataSetFacade currentDataSet;
	private String currentSourceSetId;
	private SourceSelectionFacade currentSourceSelection;
	private final Map<String, CharacterFacade> characters = new LinkedHashMap<>();
	private final Map<String, McpUIDelegate> characterDelegates = new LinkedHashMap<>();
	private final Map<String, SourceSelectionFacade> characterSources = new LinkedHashMap<>();

	public ListFacade<GameMode> getGameModes()
	{
		return FacadeFactory.getGameModes();
	}

	public ListFacade<Campaign> getSupportedCampaigns(GameMode gameMode)
	{
		return FacadeFactory.getSupportedCampaigns(gameMode);
	}

	public ListFacade<SourceSelectionFacade> getSourceSelections()
	{
		return FacadeFactory.getSourceSelections();
	}

	public GameMode findGameMode(String name)
	{
		ListFacade<GameMode> modes = getGameModes();
		for (GameMode mode : modes)
		{
			if (mode.getName().equalsIgnoreCase(name))
			{
				return mode;
			}
		}
		return null;
	}

	public synchronized LoadSourcesResult loadSources(String gameModeName, List<String> campaignKeys)
	{
		GameMode gameMode = findGameMode(gameModeName);
		if (gameMode == null)
		{
			throw new IllegalArgumentException("Game mode not found: " + gameModeName);
		}

		List<Campaign> campaigns = new ArrayList<>();
		for (String key : campaignKeys)
		{
			Campaign campaign = Globals.getCampaignKeyed(key);
			if (campaign != null)
			{
				campaigns.add(campaign);
			}
		}
		if (campaigns.isEmpty())
		{
			throw new IllegalArgumentException("No valid campaigns found for keys: " + campaignKeys);
		}

		// Auto-resolve prerequisites
		CampaignDependencyResolver.ResolvedCampaigns resolved =
			CampaignDependencyResolver.resolve(campaigns, getSupportedCampaigns(gameMode));
		campaigns = new ArrayList<>(resolved.getCampaigns());

		String canonicalKey = gameModeName + ":" + campaigns.stream()
			.map(Campaign::getKeyName)
			.sorted()
			.collect(Collectors.joining(","));

		if (canonicalKey.equals(currentSourceSetId) && currentDataSet != null)
		{
			return new LoadSourcesResult(currentSourceSetId, resolved);
		}

		McpUIDelegate delegate = new McpUIDelegate();
		SourceFileLoader loader = new SourceFileLoader(delegate, new DefaultListFacade<>(campaigns), gameMode.getName());
		loader.run();

		currentDataSet = loader.getDataSetFacade();
		currentSourceSetId = canonicalKey;
		currentSourceSelection = FacadeFactory.createSourceSelection(gameMode, campaigns);

		return new LoadSourcesResult(currentSourceSetId, resolved);
	}

	/**
	 * Result of loading sources, including information about auto-resolved dependencies.
	 */
	public static final class LoadSourcesResult
	{
		private final String sourceSetId;
		private final CampaignDependencyResolver.ResolvedCampaigns resolved;

		LoadSourcesResult(String sourceSetId, CampaignDependencyResolver.ResolvedCampaigns resolved)
		{
			this.sourceSetId = sourceSetId;
			this.resolved = resolved;
		}

		public String getSourceSetId()
		{
			return sourceSetId;
		}

		public CampaignDependencyResolver.ResolvedCampaigns getResolved()
		{
			return resolved;
		}
	}

	public DataSetFacade getCurrentDataSet()
	{
		return currentDataSet;
	}

	public String getCurrentSourceSetId()
	{
		return currentSourceSetId;
	}

	public synchronized String createCharacter(String name)
	{
		if (currentDataSet == null)
		{
			throw new IllegalStateException("No sources loaded. Call load_sources first.");
		}

		McpUIDelegate delegate = new McpUIDelegate();
		ChooserFactory.setDelegate(delegate);
		CharacterFacade character = CharacterManager.createNewCharacter(delegate, currentDataSet);
		if (name != null && !name.isBlank())
		{
			character.setName(name);
		}

		String characterId = UUID.randomUUID().toString();
		characters.put(characterId, character);
		characterDelegates.put(characterId, delegate);
		if (currentSourceSelection != null)
		{
			characterSources.put(characterId, currentSourceSelection);
		}
		return characterId;
	}

	/**
	 * Open a character file. If the character requires different sources than
	 * what is currently loaded (but same game mode), automatically merges sources
	 * by saving all open characters, reloading with the union of sources,
	 * and reopening everything.
	 *
	 * @param file the .pcg character file
	 * @return result containing the new character ID and any reopened character mappings
	 */
	public synchronized OpenCharacterResult openCharacter(File file)
	{
		if (currentDataSet == null)
		{
			throw new IllegalStateException("No sources loaded. Call load_sources first.");
		}

		// Read required sources from the character file
		McpUIDelegate tempDelegate = new McpUIDelegate();
		SourceSelectionFacade requiredSources = CharacterManager.getRequiredSourcesForCharacter(file, tempDelegate);

		boolean needsMerge = false;
		if (requiredSources != null && currentSourceSelection != null
			&& !requiredSources.getCampaigns().isEmpty())
		{
			if (!SourceMerger.isSubsetOf(requiredSources, currentSourceSelection))
			{
				// Check game mode compatibility
				GameMode requiredMode = requiredSources.getGameMode().get();
				GameMode currentMode = currentSourceSelection.getGameMode().get();
				if (requiredMode != currentMode)
				{
					throw new IllegalArgumentException(
						"Cannot open character: game mode '" + requiredMode.getName()
							+ "' differs from currently loaded '" + currentMode.getName()
							+ "'. Different game modes cannot be merged.");
				}
				needsMerge = true;
			}
		}

		if (needsMerge)
		{
			return mergeAndOpen(file, requiredSources);
		}

		// Simple open — no merge needed
		return new OpenCharacterResult(openCharacterSimple(file), false, Map.of());
	}

	private String openCharacterSimple(File file)
	{
		McpUIDelegate delegate = new McpUIDelegate();
		CharacterFacade character = CharacterManager.openCharacter(file, delegate, currentDataSet);
		if (character == null)
		{
			throw new IllegalArgumentException("Failed to open character from: " + file.getPath());
		}

		String characterId = UUID.randomUUID().toString();
		characters.put(characterId, character);
		characterDelegates.put(characterId, delegate);
		if (currentSourceSelection != null)
		{
			characterSources.put(characterId, currentSourceSelection);
		}
		return characterId;
	}

	/**
	 * Perform a source merge: save all open characters, reload with merged sources,
	 * reopen all characters plus the new one.
	 */
	private OpenCharacterResult mergeAndOpen(File newCharFile, SourceSelectionFacade requiredSources)
	{
		SourceSelectionFacade merged = SourceMerger.merge(currentSourceSelection, requiredSources);
		if (merged == null)
		{
			throw new IllegalStateException("Failed to merge sources — incompatible game modes.");
		}

		// Snapshot: save all open characters and record their files
		Map<String, File> savedFiles = new LinkedHashMap<>();
		List<File> tempFiles = new ArrayList<>();

		for (Map.Entry<String, CharacterFacade> entry : characters.entrySet())
		{
			CharacterFacade ch = entry.getValue();
			File charFile = ch.getFileRef().get();

			if (CharacterManager.characterFilenameValid(ch))
			{
				CharacterManager.saveCharacter(ch);
				savedFiles.put(entry.getKey(), charFile);
			}
			else
			{
				// Never-saved character — save to temp file
				try
				{
					File tmpFile = Files.createTempFile("pcgen_merge_", ".pcg").toFile();
					ch.setFile(tmpFile);
					CharacterManager.saveCharacter(ch);
					savedFiles.put(entry.getKey(), tmpFile);
					tempFiles.add(tmpFile);
				}
				catch (IOException e)
				{
					Logging.errorPrint("Failed to save unsaved character to temp file", e);
				}
			}
		}

		// Close all characters
		characters.clear();
		characterDelegates.clear();
		characterSources.clear();
		CharacterManager.removeAllCharacters();

		// Reload with merged sources
		GameMode gameMode = merged.getGameMode().get();
		List<Campaign> mergedCampaigns = new ArrayList<>();
		for (Campaign c : merged.getCampaigns())
		{
			mergedCampaigns.add(c);
		}

		McpUIDelegate loaderDelegate = new McpUIDelegate();
		SourceFileLoader loader = new SourceFileLoader(loaderDelegate,
			new DefaultListFacade<>(mergedCampaigns), gameMode.getName());
		loader.run();

		currentDataSet = loader.getDataSetFacade();
		currentSourceSelection = merged;
		currentSourceSetId = gameMode.getName() + ":" + mergedCampaigns.stream()
			.map(Campaign::getKeyName)
			.sorted()
			.collect(Collectors.joining(","));

		// Reopen all previously open characters
		Map<String, String> reopenedMap = new LinkedHashMap<>();
		for (Map.Entry<String, File> entry : savedFiles.entrySet())
		{
			String oldId = entry.getKey();
			File charFile = entry.getValue();
			try
			{
				String newId = openCharacterSimple(charFile);
				reopenedMap.put(oldId, newId);
			}
			catch (Exception e)
			{
				Logging.errorPrint("Failed to reopen character from " + charFile.getPath()
					+ " after source merge", e);
			}
		}

		// Open the new character
		String newCharId = openCharacterSimple(newCharFile);

		// Cleanup temp files
		for (File tmp : tempFiles)
		{
			String reopenedId = null;
			for (Map.Entry<String, String> re : reopenedMap.entrySet())
			{
				File origFile = savedFiles.get(re.getKey());
				if (origFile != null && origFile.equals(tmp))
				{
					reopenedId = re.getValue();
					break;
				}
			}
			// Restore "unsaved" state for the reopened character
			if (reopenedId != null)
			{
				CharacterFacade ch = characters.get(reopenedId);
				if (ch != null)
				{
					ch.setFile(new File(""));
				}
			}
			tmp.delete();
		}

		return new OpenCharacterResult(newCharId, true, reopenedMap);
	}

	/**
	 * Result of opening a character, which may have triggered a source merge.
	 */
	public static final class OpenCharacterResult
	{
		private final String characterId;
		private final boolean sourcesMerged;
		private final Map<String, String> reopenedCharacters;

		OpenCharacterResult(String characterId, boolean sourcesMerged, Map<String, String> reopenedCharacters)
		{
			this.characterId = characterId;
			this.sourcesMerged = sourcesMerged;
			this.reopenedCharacters = reopenedCharacters;
		}

		public String getCharacterId()
		{
			return characterId;
		}

		public boolean isSourcesMerged()
		{
			return sourcesMerged;
		}

		public Map<String, String> getReopenedCharacters()
		{
			return reopenedCharacters;
		}
	}

	public CharacterFacade getCharacter(String characterId)
	{
		CharacterFacade character = characters.get(characterId);
		if (character == null)
		{
			throw new IllegalArgumentException("Character not found: " + characterId);
		}
		McpUIDelegate delegate = characterDelegates.get(characterId);
		if (delegate != null)
		{
			ChooserFactory.setDelegate(delegate);
		}
		return character;
	}

	public pcgen.core.PlayerCharacter getPlayerCharacter(String characterId)
	{
		CharacterFacade facade = getCharacter(characterId);
		pcgen.cdom.enumeration.CharID charId = facade.getCharID();
		for (pcgen.core.PlayerCharacter pc : pcgen.core.Globals.getPCList())
		{
			if (pc.getCharID().equals(charId))
			{
				return pc;
			}
		}
		throw new IllegalStateException("PlayerCharacter not found for CharID: " + charId);
	}

	public McpUIDelegate getDelegate(String characterId)
	{
		return characterDelegates.get(characterId);
	}

	/**
	 * Returns all pending choices across all characters.
	 */
	public Map<String, PendingChoice> getAllPendingChoices()
	{
		Map<String, PendingChoice> all = new LinkedHashMap<>();
		for (McpUIDelegate delegate : characterDelegates.values())
		{
			all.putAll(delegate.getPendingChoices());
		}
		return all;
	}

	/**
	 * Resolve a pending choice by id, searching across all character delegates.
	 */
	public boolean resolveChoice(String choiceId, List<String> selections)
	{
		for (McpUIDelegate delegate : characterDelegates.values())
		{
			if (delegate.resolveChoice(choiceId, selections))
			{
				return true;
			}
		}
		return false;
	}

	public synchronized boolean saveCharacter(String characterId)
	{
		CharacterFacade character = getCharacter(characterId);
		return CharacterManager.saveCharacter(character);
	}

	public synchronized void closeCharacter(String characterId)
	{
		CharacterFacade character = characters.remove(characterId);
		characterDelegates.remove(characterId);
		characterSources.remove(characterId);
		if (character != null)
		{
			CharacterManager.removeCharacter(character);
		}
	}

	public Map<String, CharacterFacade> getOpenCharacters()
	{
		return Map.copyOf(characters);
	}
}
