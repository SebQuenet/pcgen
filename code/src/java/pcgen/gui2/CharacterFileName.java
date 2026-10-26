package pcgen.gui2;

import pcgen.cdom.base.Constants;
import pcgen.facade.core.CharacterFacade;

/**
 * The file name proposed when a character is saved: its name, its level and, for a
 * mythic character, its tier, such as Drakès-13-04.pcg. Two digits keep the files of
 * one character in level order.
 */
final class CharacterFileName
{
	/** Raised by each mythic tier in the Pathfinder Mythic Adventures data. */
	private static final String MYTHIC_TIER_VARIABLE = "MythicTierLevel";
	private static final String NUMBER_FORMAT = "-%02d";

	private CharacterFileName()
	{
	}

	static String suggestFor(CharacterFacade character)
	{
		int mythicTier = character.hasVariable(MYTHIC_TIER_VARIABLE)
			? Math.round(character.getVariable(MYTHIC_TIER_VARIABLE, true))
			: 0;
		return suggest(character.getNameRef().get(), character.getCharacterLevelsFacade().getSize(), mythicTier);
	}

	static String suggest(String characterName, int level, int mythicTier)
	{
		String mythicPart = mythicTier > 0 ? String.format(NUMBER_FORMAT, mythicTier) : "";
		return characterName + String.format(NUMBER_FORMAT, level) + mythicPart + Constants.EXTENSION_CHARACTER_FILE;
	}
}
