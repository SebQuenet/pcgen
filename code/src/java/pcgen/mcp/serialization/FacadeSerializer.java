package pcgen.mcp.serialization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pcgen.core.AbilityCategory;
import pcgen.core.Campaign;
import pcgen.core.Deity;
import pcgen.core.GameMode;
import pcgen.core.PCAlignment;
import pcgen.core.PCClass;
import pcgen.core.PCStat;
import pcgen.core.Race;
import pcgen.core.Skill;
import pcgen.facade.core.AbilityFacade;
import pcgen.facade.core.CharacterFacade;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.EquipmentFacade;
import pcgen.facade.core.TodoFacade;
import pcgen.facade.util.ListFacade;
import pcgen.facade.util.MapFacade;

public final class FacadeSerializer
{
	private FacadeSerializer()
	{
	}

	public static Map<String, Object> serializeGameMode(GameMode mode)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("name", mode.getName());
		result.put("displayName", mode.getDisplayName());
		return result;
	}

	public static Map<String, Object> serializeCampaign(Campaign campaign)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("key", campaign.getKeyName());
		result.put("name", campaign.getDisplayName());
		return result;
	}

	public static Map<String, Object> serializeRace(Race race)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("key", race.getKeyName());
		result.put("name", race.getDisplayName());
		return result;
	}

	public static Map<String, Object> serializePCClass(PCClass pcClass)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("key", pcClass.getKeyName());
		result.put("name", pcClass.getDisplayName());
		return result;
	}

	public static Map<String, Object> serializeStat(PCStat stat)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("key", stat.getKeyName());
		result.put("name", stat.getDisplayName());
		return result;
	}

	public static Map<String, Object> serializeAlignment(PCAlignment alignment)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("key", alignment.getKeyName());
		result.put("name", alignment.getDisplayName());
		return result;
	}

	public static Map<String, Object> serializeSkill(Skill skill)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("key", skill.getKeyName());
		result.put("name", skill.getDisplayName());
		return result;
	}

	public static Map<String, Object> serializeDeity(Deity deity)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("key", deity.getKeyName());
		result.put("name", deity.getDisplayName());
		return result;
	}

	public static Map<String, Object> serializeEquipment(EquipmentFacade equipment)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("key", equipment.toString());
		result.put("name", equipment.toString());
		return result;
	}

	public static Map<String, Object> serializeAbility(AbilityFacade ability)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("key", ability.toString());
		result.put("name", ability.toString());
		return result;
	}

	public static <T> List<Map<String, Object>> serializeList(ListFacade<T> list, java.util.function.Function<T, Map<String, Object>> serializer)
	{
		List<Map<String, Object>> result = new ArrayList<>();
		for (T item : list)
		{
			result.add(serializer.apply(item));
		}
		return result;
	}

	public static Map<String, Object> serializeCharacterSummary(CharacterFacade character)
	{
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("name", character.getNameRef().get());

		Race race = character.getRaceRef().get();
		result.put("race", race != null ? race.getDisplayName() : null);

		PCAlignment alignment = character.getAlignmentRef().get();
		result.put("alignment", alignment != null ? alignment.getDisplayName() : null);

		DataSetFacade dataSet = character.getDataSet();

		Map<String, Integer> abilityScores = new LinkedHashMap<>();
		for (PCStat stat : dataSet.getStats())
		{
			abilityScores.put(stat.getKeyName(), character.getScoreBase(stat));
		}
		result.put("abilityScores", abilityScores);

		Map<String, Integer> abilityModifiers = new LinkedHashMap<>();
		for (PCStat stat : dataSet.getStats())
		{
			abilityModifiers.put(stat.getKeyName(), character.getModTotal(stat));
		}
		result.put("abilityModifiers", abilityModifiers);

		List<Map<String, Object>> classes = new ArrayList<>();
		for (PCClass pcClass : dataSet.getClasses())
		{
			int level = character.getClassLevel(pcClass);
			if (level > 0)
			{
				Map<String, Object> classInfo = new LinkedHashMap<>();
				classInfo.put("name", pcClass.getDisplayName());
				classInfo.put("level", level);
				classes.add(classInfo);
			}
		}
		result.put("classes", classes);

		Number hp = character.getTotalHPRef().get();
		result.put("hp", hp != null ? hp.intValue() : 0);

		result.put("xp", character.getXPRef().get());
		result.put("xpForNextLevel", character.getXPForNextLevelRef().get());

		result.put("carriedWeight", character.getCarriedWeightRef().get());
		result.put("load", character.getLoadRef().get());

		Deity deity = character.getDeityRef().get();
		result.put("deity", deity != null ? deity.getDisplayName() : null);

		List<String> todoItems = new ArrayList<>();
		for (TodoFacade todo : character.getTodoList())
		{
			todoItems.add(todo.getMessageKey());
		}
		result.put("todoList", todoItems);

		return result;
	}
}
