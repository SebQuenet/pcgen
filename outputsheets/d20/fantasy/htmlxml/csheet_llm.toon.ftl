<#ftl encoding="UTF-8" strip_whitespace=true >
<#-- LLM-optimized TOON (Token-Oriented Object Notation) character sheet export -->
<#-- TOON spec v3.0 - https://github.com/toon-format/spec -->
<#-- Uses YAML-like objects + CSV-style tabular arrays for maximum token efficiency -->

<#-- TOON quoting function: quote strings that contain special chars -->
<#function tq val>
<#if val == "" || val == "true" || val == "false" || val == "null" || val?matches("^-?\\d") || val?contains(",") || val?contains(":") || val?contains("\"") || val?contains("\\") || val?starts_with("-") || val?contains("[") || val?contains("]") || val?contains("{") || val?contains("}") || val?contains("\n") || val?contains("\r") || val?contains("\t")>
<#return '"' + val?replace('\\', '\\\\')?replace('"', '\\"')?replace('\n', '\\n')?replace('\r', '\\r')?replace('\t', '\\t') + '"'>
<#else>
<#return val>
</#if>
</#function>
name: ${tq(pcstring('NAME'))}
race: ${tq(pcstring('RACE'))}
alignment: ${tq(pcstring('ALIGNMENT'))}
totalLevel: ${pcstring('TOTALLEVELS')}
ecl: ${pcstring('ECL')}
cr: ${tq(pcstring('CR'))}
hp: ${pcstring('HP')}
hitDice: ${tq(pcstring('HITDICE'))}
xp: ${pcstring('EXP.CURRENT')}
xpNext: ${pcstring('EXP.NEXT')}
size: ${pcstring('SIZELONG')}
speed: ${tq(pcstring('MOVEMENT'))}
reach: ${pcstring('REACH')}
vision: ${tq(pcstring('VISION'))}
gender: ${pcstring('GENDER.LONG')}
age: ${pcstring('AGE')}
height: ${tq(pcstring('HEIGHT'))}
weight: ${tq(pcstring('WEIGHT'))}
<#if (pcstring('DEITY') != '' && pcstring('DEITY') != 'None')>
deity: ${tq(pcstring('DEITY'))}
deityAlignment: ${tq(pcstring('DEITY.ALIGNMENT'))}
</#if>
<#assign classItems = [] >
<@loop from=0 to=pcvar('COUNT[CLASSES]-1') ; class, class_has_next>
<#if (pcvar(pcstring('CLASS.${class}.LEVEL')) > 0) >
<#assign classItems = classItems + [class] >
</#if>
</@loop>
classes[${classItems?size}]{name,level}:
<#list classItems as class>
  ${tq(pcstring('CLASS.${class}'))},${pcstring('CLASS.${class}.LEVEL')}
</#list>
<#assign statCount = pcvar('COUNT[STATS]')?int >
abilityScores[${statCount}]{name,score,mod,base,baseMod}:
<@loop from=0 to=statCount-1 ; stat, stat_has_next>
  ${pcstring('STAT.${stat}.NAME')},${pcstring('STAT.${stat}')},${pcstring('STAT.${stat}.MOD')},${pcstring('STAT.${stat}.NOTEMP.NOEQUIP')},${pcstring('STAT.${stat}.MOD.NOTEMP.NOEQUIP')}
</@loop>
initiative: ${pcstring('INITIATIVEMOD')}
ac:
  total: ${pcstring('AC.Total')}
  touch: ${pcstring('AC.Touch')}
  flatFooted: ${pcstring('AC.Flatfooted')}
  armor: ${pcstring('AC.Armor')}
  shield: ${pcstring('AC.Shield')}
  dex: ${pcstring('AC.Ability')}
  size: ${pcstring('AC.Size')}
  natural: ${pcstring('AC.NaturalArmor')}
  deflection: ${pcstring('AC.Deflection')}
  dodge: ${pcstring('AC.Dodge')}
  misc: ${pcstring('AC.Misc')}
  maxDex: ${pcstring('MAXDEX')}
  spellFailure: ${pcstring('SPELLFAILURE')}
  armorCheck: ${pcstring('ACCHECK')}
attack:
  bab: ${tq(pcstring('ATTACK.MELEE.BASE'))}
  melee: ${tq(pcstring('ATTACK.MELEE.TOTAL'))}
  ranged: ${tq(pcstring('ATTACK.RANGED.TOTAL'))}
<#if (pchasvar('CMB') || pcboolean('VAR.HASFEAT:CMB Output')) >
  cmb: ${pcstring('VAR.CMB.INTVAL.SIGN')}
  cmd: ${pcstring('VAR.CMD.INTVAL')}
</#if>
<#if (pcstring('DR') != '')>
dr: ${tq(pcstring('DR'))}
</#if>
<#if (pcstring('SR') != '' && pcstring('SR') != '0')>
sr: ${pcstring('SR')}
</#if>
<#assign checkCount = 0 >
<#list pc.checks as check>
<#assign checkCount = checkCount + 1 >
</#list>
saves[${checkCount}]{name,total,base,ability,magic,misc}:
<#assign checknum = 0 />
<#list pc.checks as check>
  ${tq(pcstring('CHECK.${checknum}.NAME'))},${pcstring('CHECK.${checknum}.TOTAL')},${pcstring('CHECK.${checknum}.BASE')},${pcstring('CHECK.${checknum}.STATMOD')},${pcstring('CHECK.${checknum}.MAGIC')},${pcstring('CHECK.${checknum}.MISC.NOMAGIC.NOSTAT')}
<#assign checknum = checknum + 1 />
</#list>
<#-- Skills: only trained -->
<#assign skillCount = pcvar('count("SKILLSIT", "VIEW=VISIBLE_EXPORT")')?int >
<#assign trainedSkills = [] >
<@loop from=0 to=skillCount-1 ; skill, skill_has_next>
<#if (pcstring('SKILLSIT.${skill}.RANK') != "0.0" && pcstring('SKILLSIT.${skill}.RANK') != "0")>
<#assign trainedSkills = trainedSkills + [skill] >
</#if>
</@loop>
<#if (trainedSkills?size > 0)>
skills[${trainedSkills?size}]{name,total,ranks,abilMod,ability,misc}:
<#list trainedSkills as skill>
  ${tq(pcstring('SKILLSIT.${skill}'))},${pcstring('SKILLSIT.${skill}.TOTAL')},${pcstring('SKILLSIT.${skill}.RANK')},${pcstring('SKILLSIT.${skill}.ABMOD')},${pcstring('SKILLSIT.${skill}.ABILITY')},${pcstring('SKILLSIT.${skill}.MISC')}
</#list>
</#if>
<#-- Feats -->
<#assign featCount = pcvar('COUNT[FEATS.VISIBLE]')?int >
<#if (featCount > 0)>
feats[${featCount}]{name,type,description}:
<@loop from=0 to=featCount-1 ; feat, feat_has_next>
  ${tq(pcstring('FEAT.VISIBLE.${feat}'))},${tq(pcstring('FEAT.VISIBLE.${feat}.TYPE')?split(".")?first)},${tq(pcstring('FEAT.VISIBLE.${feat}.DESC'))}
</@loop>
</#if>
<#-- Auto feats -->
<#assign autoFeatCount = pcvar('COUNT[FEATSAUTO.VISIBLE]')?int >
<#if (autoFeatCount > 0)>
autoFeats[${autoFeatCount}]{name,type}:
<@loop from=0 to=autoFeatCount-1 ; feat, feat_has_next>
  ${tq(pcstring('FEATAUTO.VISIBLE.${feat}'))},${tq(pcstring('FEATAUTO.VISIBLE.${feat}.TYPE')?split(".")?first)}
</@loop>
</#if>
<#-- Special Abilities -->
<#assign saCount = pcvar('COUNT[SA]')?int >
<#if (saCount > 0)>
specialAbilities[${saCount}]{name,description}:
<@loop from=0 to=saCount-1 ; sa, sa_has_next>
  ${tq(pcstring('SPECIALABILITY.${sa}'))},${tq(pcstring('SPECIALABILITY.${sa}.DESCRIPTION'))}
</@loop>
</#if>
<#-- Ability sections: [outputKey, category, typeFilter] -->
<#-- Use CATEGORY=Special Ability + TYPE for reliable output; ABILITYCATEGORY names don't resolve in countdistinct -->
<#assign abilSections = [
  ["traits", "Special Ability", "Trait"],
  ["racialTraits", "Special Ability", "RacialTrait"],
  ["classFeatures", "Special Ability", "ClassFeatures"],
  ["domainPowers", "Special Ability", "DomainPower"],
  ["specialAttacks", "Special Ability", "SpecialAttack"],
  ["auras", "Special Ability", "Aura"],
  ["archetypes", "Archetype", ""],
  ["mythicAbilities", "Special Ability", "Mythic Ability"],
  ["mythicTiers", "Special Ability", "Mythic Tier"],
  ["mythicPaths", "Special Ability", "Mythic Path"],
  ["mythicPathAbilities", "Special Ability", "Mythic Path Ability"],
  ["mythicAbilityBonuses", "Special Ability", "Mythic Ability Bonus"],
  ["championStrikes", "Special Ability", "Champion Path Feature"],
  ["guardianCalls", "Special Ability", "Guardian Path Feature"],
  ["marshalOrders", "Special Ability", "Marshal Path Feature"],
  ["tricksterAttacks", "Special Ability", "Trickster Path Feature"],
  ["archmageArcanas", "Special Ability", "Archmage Path Feature"],
  ["hierophantSurges", "Special Ability", "Hierophant Path Feature"]
] >
<#list abilSections as sec>
<#assign typeFilter = (sec[2] != "")?then(',"TYPE=${sec[2]}"', '') >
<#assign typeSuffix = (sec[2] != "")?then('.TYPE=${sec[2]}', '') >
<#assign secCount = pcvar('countdistinct("ABILITIES","CATEGORY=${sec[1]}"${typeFilter},"VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")')?int >
<#if (secCount > 0)>
${sec[0]}[${secCount}]{name,description}:
<@loop from=0 to=secCount-1 ; ability, ability_has_next>
  ${tq(pcstring('ABILITYALL.${sec[1]}.VISIBLE.${ability}${typeSuffix}'))},${tq(pcstring('ABILITYALL.${sec[1]}.VISIBLE.${ability}${typeSuffix}.DESC'))}
</@loop>
</#if>
</#list>
<#-- Tracked resources (mythic pool, channel energy, etc.) -->
<#assign resourceVars = [
  ["MythicTierLevel", "mythicTier"],
  ["MythicSurgeDieSize", "mythicSurgeDie"],
  ["MythicPowerTimes", "mythicPoolPerDay"],
  ["ClericChannelEnergyTimes", "channelEnergyPerDay"],
  ["ClericChannelPositiveEnergyDice", "channelPositiveDice"],
  ["ClericChannelPositiveEnergyDieSize", "channelPositiveDieSize"],
  ["ClericChannelPositiveEnergyDC", "channelPositiveDC"],
  ["PaladinChannelPerDay", "paladinChannelPerDay"],
  ["PaladinChannelDC", "paladinChannelDC"],
  ["LayOnHandsTimes", "layOnHandsPerDay"],
  ["SmiteEvilTimes", "smiteEvilPerDay"],
  ["KiPool", "kiPool"],
  ["RageDuration", "rageRoundsPerDay"],
  ["BardicMusicDuration", "bardicPerformanceRounds"],
  ["WildShapeTimes", "wildShapePerDay"],
  ["SneakAttackDice", "sneakAttackDice"],
  ["StunningFistCount", "stunningFistPerDay"],
  ["TouchofGoodTimes", "touchOfGoodPerDay"],
  ["TouchofEvilTimes", "touchOfEvilPerDay"],
  ["TouchofLawTimes", "touchOfLawPerDay"],
  ["TouchofChaosTimes", "touchOfChaosPerDay"],
  ["HolyLanceTimes", "holyLancePerDay"],
  ["SunNimbusOfLightRounds", "nimbusOfLightRounds"],
  ["BleedingTouchTimes", "bleedingTouchPerDay"],
  ["CalmingTouchTimes", "calmingTouchPerDay"],
  ["TouchofGloryTimes", "touchOfGloryPerDay"],
  ["ResistantTouchTimes", "resistantTouchPerDay"]
] >
<#assign hasResources = false >
<#list resourceVars as rv>
<#if pchasvar(rv[0]) && (pcvar(rv[0]) > 0)>
<#if !hasResources>
resources:
<#assign hasResources = true >
</#if>
  ${rv[1]}: ${pcvar(rv[0])?int}
</#if>
</#list>
<#-- Conditional modifiers (save, combat, skill bonuses) -->
<#assign condSaveCount = pcvar('countdistinct("ABILITIES","ASPECT=SaveBonus")')?int >
<#assign condCombatCount = pcvar('countdistinct("ABILITIES","ASPECT=CombatBonus")')?int >
<#assign condSkillCount = pcvar('countdistinct("ABILITIES","ASPECT=SkillBonus")')?int >
<#if (condSaveCount + condCombatCount + condSkillCount > 0)>
conditionalModifiers:
<#if (condSaveCount > 0)>
  saves:
<@loop from=0 to=condSaveCount-1 ; cm, cm_has_next>
    - ${tq(pcstring('ABILITYALL.ANY.${cm}.ASPECT=SaveBonus.ASPECT.SaveBonus'))}
</@loop>
</#if>
<#if (condCombatCount > 0)>
  combat:
<@loop from=0 to=condCombatCount-1 ; cm, cm_has_next>
    - ${tq(pcstring('ABILITYALL.ANY.${cm}.ASPECT=CombatBonus.ASPECT.CombatBonus'))}
</@loop>
</#if>
<#if (condSkillCount > 0)>
  skills:
<@loop from=0 to=condSkillCount-1 ; cm, cm_has_next>
    - ${tq(pcstring('ABILITYALL.ANY.${cm}.ASPECT=SkillBonus.ASPECT.SkillBonus'))}
</@loop>
</#if>
</#if>
<#-- Weapons: MERGENONE to show each weapon instance separately (e.g. dual-wield) -->
<#-- BASEHIT for primary/equipped, TOTALHIT for off-hand (includes TWF penalty) -->
<#assign weapCount = pcvar('COUNT[EQTYPE.MERGENONE.WEAPON]')?int >
<#if (weapCount > 0)>
weapons[${weapCount}]{name,toHit,damage,crit,range,type,hand,special}:
<@loop from=0 to=weapCount-1 ; weap, weap_has_next>
<#assign weapHand = pcstring('WEAPON.MERGENONE.${weap}.HAND')?lower_case >
<#assign weapHit = (weapHand?contains("non") || weapHand?contains("off") || weapHand?contains("secondary"))?then(pcstring('WEAPON.MERGENONE.${weap}.TOTALHIT'), pcstring('WEAPON.MERGENONE.${weap}.BASEHIT')) >
  ${tq(pcstring('WEAPON.MERGENONE.${weap}.NAME'))},${tq(weapHit)},${tq(pcstring('WEAPON.MERGENONE.${weap}.DAMAGE'))},${tq(pcstring('WEAPON.MERGENONE.${weap}.CRIT') + '/x' + pcstring('WEAPON.MERGENONE.${weap}.MULT'))},${tq(pcstring('WEAPON.MERGENONE.${weap}.RANGE'))},${tq(pcstring('WEAPON.MERGENONE.${weap}.TYPE'))},${tq(pcstring('WEAPON.MERGENONE.${weap}.HAND'))},${tq(pcstring('WEAPON.MERGENONE.${weap}.SPROP'))}
</@loop>
</#if>
<#-- Natural Attacks -->
<#assign natAttackCount = pcvar('countdistinct("ABILITIES","CATEGORY=Natural Attack","TYPE=NaturalAttack")')?int >
<#if (natAttackCount > 0)>
naturalAttacks[${natAttackCount}]{name,toHit,damage,type,crit}:
<@loop from=0 to=natAttackCount-1 ; na, na_has_next>
  ${tq(pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackName'))},${tq(pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackToHit'))},${tq(pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackDamage'))},${tq(pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackType'))},${tq(pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackThreatRange') + pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackCritMult'))}
</@loop>
</#if>
<#-- Equipment -->
<#assign equipCount = pcvar('COUNT[EQUIPMENT.MERGELOC]')?int >
<#if (equipCount > 0)>
equipment[${equipCount}]{name,qty,weight,cost,location,special}:
<@loop from=0 to=equipCount-1 ; equip, equip_has_next>
  ${tq(pcstring('EQ.MERGELOC.${equip}.NAME'))},${pcstring('EQ.MERGELOC.${equip}.QTY')},${pcstring('EQ.MERGELOC.${equip}.WT')},${pcstring('EQ.MERGELOC.${equip}.COST')},${tq(pcstring('EQ.MERGELOC.${equip}.LOCATION'))},${tq(pcstring('EQ.MERGELOC.${equip}.SPROP'))}
</@loop>
</#if>
gold: ${pcstring('GOLD')}
carriedWeight: ${tq(pcstring('WEIGHT.CARRIED'))}
load: ${tq(pcstring('WEIGHT.LOAD'))}
weightLimits:
  light: ${tq(pcstring('WEIGHT.LIGHT'))}
  medium: ${tq(pcstring('WEIGHT.MEDIUM'))}
  heavy: ${tq(pcstring('WEIGHT.HEAVY'))}
<#-- Domains -->
<#if (pcvar("COUNT[DOMAINS]") > 0) >
<#assign domainCount = pcvar('COUNT[DOMAINS]')?int >
domains[${domainCount}]{name,description}:
<@loop from=1 to=domainCount ; domain, domain_has_next>
  ${tq(pcstring('DOMAIN.${domain}'))},${tq(pcstring('DOMAIN.${domain}.DESCRIPTION'))}
</@loop>
</#if>
<#-- Spells per class -->
<@loop from=pcvar('COUNT[SPELLRACE]') to=pcvar('COUNT[SPELLRACE]+COUNT[CLASSES]-1') ; class, class_has_next>
<#if (pcstring("SPELLLISTCLASS.${class}") != '') >
spells.${pcstring('SPELLLISTCLASS.${class}')?replace(" ", "")}:
  casterLevel: ${pcstring('SPELLLISTCLASS.${class}.CASTERLEVEL')}
  concentration: ${pcstring('SPELLLISTCLASS.${class}.CONCENTRATION')}
  type: ${pcstring('SPELLLISTTYPE.${class}')}
<@loop from=0 to=pcvar('MAXSPELLLEVEL.${class}') ; level, level_has_next>
<#assign spellCount = pcvar('COUNT[SPELLSINBOOK.${class}.0.${level}]')?int >
<#if (spellCount > 0)>
  level${level}:
    perDay: ${pcstring('SPELLLISTCAST.${class}.${level}')}
    known: ${pcstring('SPELLLISTKNOWN.${class}.${level}')}
    spells[${spellCount}]{name,dc,school,range,duration,saveInfo}:
<@loop from=0 to=spellCount-1 ; spell, spell_has_next>
      ${tq(pcstring('SPELLMEM.${class}.0.${level}.${spell}.NAME'))},${pcstring('SPELLMEM.${class}.0.${level}.${spell}.DC')},${tq(pcstring('SPELLMEM.${class}.0.${level}.${spell}.SCHOOL'))},${tq(pcstring('SPELLMEM.${class}.0.${level}.${spell}.RANGE'))},${tq(pcstring('SPELLMEM.${class}.0.${level}.${spell}.DURATION'))},${tq(pcstring('SPELLMEM.${class}.0.${level}.${spell}.SAVEINFO'))}
</@loop>
</#if>
</@loop>
</#if>
</@loop>
<#-- Languages -->
<#assign langCount = pcvar('COUNT[LANGUAGES]')?int >
<#if (langCount > 0)>
languages[${langCount}]: <@loop from=0 to=langCount-1 ; lang, lang_has_next>${tq(pcstring('LANGUAGES.${lang}'))}<#if lang_has_next>,</#if></@loop>
</#if>
<#-- Biography -->
bio:
  player: ${tq(pcstring('PLAYERNAME'))}
  raceType: ${tq(pcstring('RACETYPE'))}
  hair: ${tq(pcstring('COLOR.HAIR'))}
  eyes: ${tq(pcstring('COLOR.EYE'))}
  skin: ${tq(pcstring('COLOR.SKIN'))}
  handed: ${tq(pcstring('HANDED'))}
