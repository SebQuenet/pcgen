package pcgen.mcp;

import java.io.File;
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
import pcgen.util.chooser.ChooserFactory;

public class McpSessionManager
{
	private DataSetFacade currentDataSet;
	private String currentSourceSetId;
	private final Map<String, CharacterFacade> characters = new LinkedHashMap<>();
	private final Map<String, McpUIDelegate> characterDelegates = new LinkedHashMap<>();

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

	public synchronized String loadSources(String gameModeName, List<String> campaignKeys)
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

		String canonicalKey = gameModeName + ":" + campaignKeys.stream().sorted().collect(Collectors.joining(","));

		if (canonicalKey.equals(currentSourceSetId) && currentDataSet != null)
		{
			return currentSourceSetId;
		}

		McpUIDelegate delegate = new McpUIDelegate();
		SourceFileLoader loader = new SourceFileLoader(delegate, new DefaultListFacade<>(campaigns), gameMode.getName());
		loader.run();

		currentDataSet = loader.getDataSetFacade();
		currentSourceSetId = canonicalKey;

		return currentSourceSetId;
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
		return characterId;
	}

	public synchronized String openCharacter(File file)
	{
		if (currentDataSet == null)
		{
			throw new IllegalStateException("No sources loaded. Call load_sources first.");
		}

		McpUIDelegate delegate = new McpUIDelegate();
		CharacterFacade character = CharacterManager.openCharacter(file, delegate, currentDataSet);
		if (character == null)
		{
			throw new IllegalArgumentException("Failed to open character from: " + file.getPath());
		}

		String characterId = UUID.randomUUID().toString();
		characters.put(characterId, character);
		characterDelegates.put(characterId, delegate);
		return characterId;
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
