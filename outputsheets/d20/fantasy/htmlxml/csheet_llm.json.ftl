<#ftl encoding="UTF-8" strip_whitespace=true >
<#-- LLM-optimized JSON character sheet export -->
<#-- Produces structured, machine-parseable JSON for programmatic consumption -->
{
  "name": "${pcstring('NAME')?json_string}",
  "race": "${pcstring('RACE')?json_string}",
  "alignment": "${pcstring('ALIGNMENT')?json_string}",
  "totalLevel": ${pcstring('TOTALLEVELS')},
  "ecl": ${pcstring('ECL')},
  "cr": "${pcstring('CR')?json_string}",
  "hp": ${pcstring('HP')},
  "hitDice": "${pcstring('HITDICE')?json_string}",
  "xp": ${pcstring('EXP.CURRENT')},
  "xpNext": ${pcstring('EXP.NEXT')},
  "size": "${pcstring('SIZELONG')?json_string}",
  "speed": "${pcstring('MOVEMENT')?json_string}",
  "reach": ${pcstring('REACH')},
  "vision": "${pcstring('VISION')?json_string}",
  "gender": "${pcstring('GENDER.LONG')?json_string}",
  "age": ${pcstring('AGE')},
  "height": "${pcstring('HEIGHT')?json_string}",
  "weight": "${pcstring('WEIGHT')?json_string}",
  "deity": "${pcstring('DEITY')?json_string}",
  "deityAlignment": "${pcstring('DEITY.ALIGNMENT')?json_string}",
  "domains": "${pcstring('DEITY.DOMAINLIST')?json_string}",
  "classes": [
<@loop from=0 to=pcvar('COUNT[CLASSES]-1') ; class, class_has_next>
<#if (pcvar(pcstring('CLASS.${class}.LEVEL')) > 0) >
    {"name": "${pcstring('CLASS.${class}')?json_string}", "level": ${pcstring('CLASS.${class}.LEVEL')}}<#if class_has_next>,</#if>
</#if>
</@loop>
  ],
  "abilityScores": {
<@loop from=0 to=pcvar('COUNT[STATS]-1') ; stat, stat_has_next>
    "${pcstring('STAT.${stat}.NAME')?json_string}": {"score": ${pcstring('STAT.${stat}')}, "mod": ${pcstring('STAT.${stat}.MOD')}, "base": ${pcstring('STAT.${stat}.NOTEMP.NOEQUIP')}, "baseMod": ${pcstring('STAT.${stat}.MOD.NOTEMP.NOEQUIP')}}<#if stat_has_next>,</#if>
</@loop>
  },
  "initiative": ${pcstring('INITIATIVEMOD')},
  "ac": {
    "total": ${pcstring('AC.Total')},
    "touch": ${pcstring('AC.Touch')},
    "flatFooted": ${pcstring('AC.Flatfooted')},
    "armor": ${pcstring('AC.Armor')},
    "shield": ${pcstring('AC.Shield')},
    "dex": ${pcstring('AC.Ability')},
    "size": ${pcstring('AC.Size')},
    "natural": ${pcstring('AC.NaturalArmor')},
    "deflection": ${pcstring('AC.Deflection')},
    "dodge": ${pcstring('AC.Dodge')},
    "misc": ${pcstring('AC.Misc')},
    "maxDex": ${pcstring('MAXDEX')},
    "spellFailure": ${pcstring('SPELLFAILURE')},
    "armorCheck": ${pcstring('ACCHECK')}
  },
  "attack": {
    "bab": "${pcstring('ATTACK.MELEE.BASE')?json_string}",
    "melee": "${pcstring('ATTACK.MELEE.TOTAL')?json_string}",
    "ranged": "${pcstring('ATTACK.RANGED.TOTAL')?json_string}"<#if (pchasvar('CMB') || pcboolean('VAR.HASFEAT:CMB Output')) >,
    "cmb": ${pcstring('VAR.CMB.INTVAL')},
    "cmd": ${pcstring('VAR.CMD.INTVAL')}</#if>
  },
  "dr": "${pcstring('DR')?json_string}",
  "sr": "${pcstring('SR')?json_string}",
  "saves": [
<#assign checknum = 0 />
<#list pc.checks as check>
    {"name": "${pcstring('CHECK.${checknum}.NAME')?json_string}", "total": ${pcstring('CHECK.${checknum}.TOTAL')}, "base": ${pcstring('CHECK.${checknum}.BASE')}, "ability": ${pcstring('CHECK.${checknum}.STATMOD')}, "magic": ${pcstring('CHECK.${checknum}.MAGIC')}, "misc": ${pcstring('CHECK.${checknum}.MISC.NOMAGIC.NOSTAT')}}<#assign checknum = checknum + 1 /><#if check_has_next>,</#if>
</#list>
  ],
  "skills": [
<#assign skillCount = pcvar('count("SKILLSIT", "VIEW=VISIBLE_EXPORT")')?int >
<#assign first = true >
<@loop from=0 to=skillCount-1 ; skill, skill_has_next>
<#if (pcstring('SKILLSIT.${skill}.RANK') != "0.0" && pcstring('SKILLSIT.${skill}.RANK') != "0")>
<#if !first>,
</#if>
    {"name": "${pcstring('SKILLSIT.${skill}')?json_string}", "total": ${pcstring('SKILLSIT.${skill}.TOTAL')}, "ranks": ${pcstring('SKILLSIT.${skill}.RANK')}, "abilMod": ${pcstring('SKILLSIT.${skill}.ABMOD')}, "ability": "${pcstring('SKILLSIT.${skill}.ABILITY')?json_string}", "misc": ${pcstring('SKILLSIT.${skill}.MISC')}}<#assign first = false>
</#if>
</@loop>

  ],
  "feats": [
<@loop from=0 to=pcvar('COUNT[FEATS.VISIBLE]-1') ; feat, feat_has_next>
    {"name": "${pcstring('FEAT.VISIBLE.${feat}')?json_string}", "type": "${pcstring('FEAT.VISIBLE.${feat}.TYPE')?json_string}", "description": "${pcstring('FEAT.VISIBLE.${feat}.DESC')?json_string}", "associated": "${pcstring('FEAT.VISIBLE.${feat}.ASSOCIATED')?json_string}"}<#if feat_has_next>,</#if>
</@loop>
  ],
  "autoFeats": [
<@loop from=0 to=pcvar('COUNT[FEATSAUTO.VISIBLE]-1') ; feat, feat_has_next>
    {"name": "${pcstring('FEATAUTO.VISIBLE.${feat}')?json_string}", "type": "${pcstring('FEATAUTO.VISIBLE.${feat}.TYPE')?json_string}"}<#if feat_has_next>,</#if>
</@loop>
  ],
  "specialAbilities": [
<@loop from=0 to=pcvar('COUNT[SA]-1') ; sa, sa_has_next>
    {"name": "${pcstring('SPECIALABILITY.${sa}')?json_string}", "description": "${pcstring('SPECIALABILITY.${sa}.DESCRIPTION')?json_string}"}<#if sa_has_next>,</#if>
</@loop>
  ],
<#-- Ability sections: use CATEGORY=Special Ability + TYPE for reliable output -->
<#assign abilSections = [["traits", "Special Ability", "Trait"], ["racialTraits", "Special Ability", "RacialTrait"], ["classFeatures", "Special Ability", "ClassFeatures"], ["specialAttacks", "Special Ability", "SpecialAttack"], ["archetypes", "Archetype", ""]] >
<#list abilSections as sec>
<#assign typeFilter = (sec[2] != "")?then(',"TYPE=${sec[2]}"', '') >
<#assign typeSuffix = (sec[2] != "")?then('.TYPE=${sec[2]}', '') >
<#assign secCount = pcvar('countdistinct("ABILITIES","CATEGORY=${sec[1]}"${typeFilter},"VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")')?int >
  "${sec[0]}": [
<#if (secCount > 0)>
<@loop from=0 to=secCount-1 ; ability, ability_has_next>
    {"name": "${pcstring('ABILITYALL.${sec[1]}.VISIBLE.${ability}${typeSuffix}')?json_string}", "description": "${pcstring('ABILITYALL.${sec[1]}.VISIBLE.${ability}${typeSuffix}.DESC')?json_string}"}<#if ability_has_next>,</#if>
</@loop>
</#if>
  ]<#if sec_has_next>,</#if>
</#list>,
  "weapons": [
<@loop from=0 to=pcvar('COUNT[EQTYPE.WEAPON]-1') ; weap, weap_has_next>
    {"name": "${pcstring('WEAPON.${weap}.NAME')?json_string}", "toHit": "${pcstring('WEAPON.${weap}.BASEHIT')?json_string}", "damage": "${pcstring('WEAPON.${weap}.DAMAGE')?json_string}", "crit": "${pcstring('WEAPON.${weap}.CRIT')?json_string}/x${pcstring('WEAPON.${weap}.MULT')}", "range": "${pcstring('WEAPON.${weap}.RANGE')?json_string}", "type": "${pcstring('WEAPON.${weap}.TYPE')?json_string}", "special": "${pcstring('WEAPON.${weap}.SPROP')?json_string}"}<#if weap_has_next>,</#if>
</@loop>
  ],
  "naturalAttacks": [
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Natural Attack","TYPE=NaturalAttack")-1') ; na, na_has_next>
    {"name": "${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackName')?json_string}", "toHit": "${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackToHit')?json_string}", "damage": "${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackDamage')?json_string}", "type": "${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackType')?json_string}"}<#if na_has_next>,</#if>
</@loop>
  ],
  "equipment": [
<@loop from=0 to=pcvar('COUNT[EQUIPMENT.MERGELOC]-1') ; equip, equip_has_next>
    {"name": "${pcstring('EQ.MERGELOC.${equip}.NAME')?json_string}", "qty": ${pcstring('EQ.MERGELOC.${equip}.QTY')}, "weight": "${pcstring('EQ.MERGELOC.${equip}.WT')?json_string}", "cost": "${pcstring('EQ.MERGELOC.${equip}.COST')?json_string}", "location": "${pcstring('EQ.MERGELOC.${equip}.LOCATION')?json_string}"}<#if equip_has_next>,</#if>
</@loop>
  ],
  "gold": "${pcstring('GOLD')?json_string}",
  "carriedWeight": "${pcstring('WEIGHT.CARRIED')?json_string}",
  "load": "${pcstring('WEIGHT.LOAD')?json_string}",
  "weightLimits": {"light": "${pcstring('WEIGHT.LIGHT')?json_string}", "medium": "${pcstring('WEIGHT.MEDIUM')?json_string}", "heavy": "${pcstring('WEIGHT.HEAVY')?json_string}"},
  "spells": {
<#assign firstClass = true >
<@loop from=pcvar('COUNT[SPELLRACE]') to=pcvar('COUNT[SPELLRACE]+COUNT[CLASSES]-1') ; class, class_has_next>
<#if (pcstring("SPELLLISTCLASS.${class}") != '') >
<#if !firstClass>,
</#if>
    "${pcstring('SPELLLISTCLASS.${class}')?json_string}": {
      "casterLevel": ${pcstring('SPELLLISTCLASS.${class}.CASTERLEVEL')},
      "concentration": ${pcstring('SPELLLISTCLASS.${class}.CONCENTRATION')},
      "type": "${pcstring('SPELLLISTTYPE.${class}')?json_string}",
      "levels": {
<#assign firstLevel = true >
<@loop from=0 to=pcvar('MAXSPELLLEVEL.${class}') ; level, level_has_next>
<#assign spellCount = pcvar('COUNT[SPELLSINBOOK.${class}.0.${level}]')?int >
<#if (spellCount > 0)>
<#if !firstLevel>,
</#if>
        "${level}": {
          "perDay": "${pcstring('SPELLLISTCAST.${class}.${level}')?json_string}",
          "known": "${pcstring('SPELLLISTKNOWN.${class}.${level}')?json_string}",
          "spells": [
<@loop from=0 to=spellCount-1 ; spell, spell_has_next>
            {"name": "${pcstring('SPELLMEM.${class}.0.${level}.${spell}.NAME')?json_string}", "dc": ${pcstring('SPELLMEM.${class}.0.${level}.${spell}.DC')}, "school": "${pcstring('SPELLMEM.${class}.0.${level}.${spell}.SCHOOL')?json_string}", "range": "${pcstring('SPELLMEM.${class}.0.${level}.${spell}.RANGE')?json_string}", "duration": "${pcstring('SPELLMEM.${class}.0.${level}.${spell}.DURATION')?json_string}", "saveInfo": "${pcstring('SPELLMEM.${class}.0.${level}.${spell}.SAVEINFO')?json_string}", "components": "${pcstring('SPELLMEM.${class}.0.${level}.${spell}.COMPONENTS')?json_string}", "description": "${pcstring('SPELLMEM.${class}.0.${level}.${spell}.DESCRIPTION')?json_string}"}<#if spell_has_next>,</#if>
</@loop>
          ]
        }<#assign firstLevel = false>
</#if>
</@loop>

      }
    }<#assign firstClass = false>
</#if>
</@loop>

  },
  "languages": [
<@loop from=0 to=pcvar('COUNT[LANGUAGES]-1') ; lang, lang_has_next>
    "${pcstring('LANGUAGES.${lang}')?json_string}"<#if lang_has_next>,</#if>
</@loop>
  ],
  "bio": {
    "player": "${pcstring('PLAYERNAME')?json_string}",
    "raceType": "${pcstring('RACETYPE')?json_string}",
    "hair": "${pcstring('COLOR.HAIR')?json_string}",
    "eyes": "${pcstring('COLOR.EYE')?json_string}",
    "skin": "${pcstring('COLOR.SKIN')?json_string}",
    "handed": "${pcstring('HANDED')?json_string}",
    "personality": "${pcstring('PERSONALITY1')?json_string}",
    "background": "${pcstring('BIO')?json_string}"
  }
}
