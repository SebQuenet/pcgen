<#ftl encoding="UTF-8" strip_whitespace=true >
<#-- LLM-optimized Markdown character sheet export -->
<#-- Produces compact, token-efficient output for AI consumption -->

# ${pcstring('NAME')} — ${pcstring('RACE')} ${pcstring('ALIGNMENT')}

## Identity
<#assign classCount = pcvar('COUNT[CLASSES]-1') >
- **Classes:** <@loop from=0 to=classCount ; class, class_has_next><#if (pcvar(pcstring('CLASS.${class}.LEVEL')) > 0) >${pcstring('CLASS.${class}')} ${pcstring('CLASS.${class}.LEVEL')}<#if class_has_next> / </#if></#if></@loop>
- **Total Level:** ${pcstring('TOTALLEVELS')} | **ECL:** ${pcstring('ECL')} | **CR:** ${pcstring('CR')}
- **XP:** ${pcstring('EXP.CURRENT')} / ${pcstring('EXP.NEXT')}
- **HP:** ${pcstring('HP')} | **Hit Dice:** ${pcstring('HITDICE')}
<#if (pcstring('DEITY') != '')>
- **Deity:** ${pcstring('DEITY')} (${pcstring('DEITY.ALIGNMENT')}) | **Domains:** ${pcstring('DEITY.DOMAINLIST')}
</#if>
- **Size:** ${pcstring('SIZELONG')} | **Speed:** ${pcstring('MOVEMENT')} | **Reach:** ${pcstring('REACH')} ft.
- **Vision:** ${pcstring('VISION')}
- **Gender:** ${pcstring('GENDER.LONG')} | **Age:** ${pcstring('AGE')} | **Height:** ${pcstring('HEIGHT')} | **Weight:** ${pcstring('WEIGHT')}

## Ability Scores

| Stat | Score | Mod | Base | Base Mod |
|------|-------|-----|------|----------|
<@loop from=0 to=pcvar('COUNT[STATS]-1') ; stat, stat_has_next>
| ${pcstring('STAT.${stat}.NAME')} | ${pcstring('STAT.${stat}')} | ${pcstring('STAT.${stat}.MOD')} | ${pcstring('STAT.${stat}.NOTEMP.NOEQUIP')} | ${pcstring('STAT.${stat}.MOD.NOTEMP.NOEQUIP')} |
</@loop>

## Combat

- **Initiative:** ${pcstring('INITIATIVEMOD')}
- **AC:** ${pcstring('AC.Total')} (Touch ${pcstring('AC.Touch')}, Flat-Footed ${pcstring('AC.Flatfooted')})
  - Armor ${pcstring('AC.Armor')} | Shield ${pcstring('AC.Shield')} | Dex ${pcstring('AC.Ability')} | Size ${pcstring('AC.Size')} | Natural ${pcstring('AC.NaturalArmor')} | Deflection ${pcstring('AC.Deflection')} | Dodge ${pcstring('AC.Dodge')} | Misc ${pcstring('AC.Misc')}
- **BAB:** ${pcstring('ATTACK.MELEE.BASE')}
- **Melee:** ${pcstring('ATTACK.MELEE.TOTAL')} | **Ranged:** ${pcstring('ATTACK.RANGED.TOTAL')}
<#if (pchasvar('CMB') || pcboolean('VAR.HASFEAT:CMB Output')) >
- **CMB:** ${pcstring('VAR.CMB.INTVAL.SIGN')} | **CMD:** ${pcstring('VAR.CMD.INTVAL')}
</#if>
<#if (pcstring('DR') != '')>
- **DR:** ${pcstring('DR')}
</#if>
<#if (pcstring('SR') != '' && pcstring('SR') != '0')>
- **SR:** ${pcstring('SR')}
</#if>
- **Max Dex:** ${pcstring('MAXDEX')} | **Spell Failure:** ${pcstring('SPELLFAILURE')}% | **Armor Check:** ${pcstring('ACCHECK')}

## Saving Throws

| Save | Total | Base | Ability | Magic | Misc |
|------|-------|------|---------|-------|------|
<#assign checknum = 0 />
<#list pc.checks as check>
| ${pcstring('CHECK.${checknum}.NAME')} | ${pcstring('CHECK.${checknum}.TOTAL')} | ${pcstring('CHECK.${checknum}.BASE')} | ${pcstring('CHECK.${checknum}.STATMOD')} | ${pcstring('CHECK.${checknum}.MAGIC')} | ${pcstring('CHECK.${checknum}.MISC.NOMAGIC.NOSTAT')} |
<#assign checknum = checknum + 1 />
</#list>

<#-- Conditional save modifiers -->
<#if (pcvar('countdistinct("ABILITIES","ASPECT=SaveBonus")') > 0) >
**Save Modifiers:** <@loop from=0 to=pcvar('countdistinct("ABILITIES","ASPECT=SaveBonus")-1') ; ability, ability_has_next>${pcstring('ABILITYALL.ANY.${ability}.ASPECT=SaveBonus.ASPECT.SaveBonus')}<#if ability_has_next>; </#if></@loop>
</#if>

## Weapons

<#if (pcvar('COUNT[EQTYPE.WEAPON]') > 0)>
| Weapon | To Hit | Damage | Crit | Range | Type | Special |
|--------|--------|--------|------|-------|------|---------|
<@loop from=0 to=pcvar('COUNT[EQTYPE.WEAPON]-1') ; weap, weap_has_next>
| ${pcstring('WEAPON.${weap}.NAME')} | ${pcstring('WEAPON.${weap}.BASEHIT')} | ${pcstring('WEAPON.${weap}.DAMAGE')} | ${pcstring('WEAPON.${weap}.CRIT')}/x${pcstring('WEAPON.${weap}.MULT')} | ${pcstring('WEAPON.${weap}.RANGE')} | ${pcstring('WEAPON.${weap}.TYPE')} | ${pcstring('WEAPON.${weap}.SPROP')} |
</@loop>
</#if>

<#-- Natural Attacks -->
<#if (pcvar('countdistinct("ABILITIES","CATEGORY=Natural Attack","TYPE=NaturalAttack")') > 0) >
### Natural Attacks

| Attack | To Hit | Damage | Type | Crit |
|--------|--------|--------|------|------|
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Natural Attack","TYPE=NaturalAttack")-1') ; na, na_has_next>
| ${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackName')} | ${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackToHit')} | ${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackDamage')} | ${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackType')} | ${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackThreatRange')}${pcstring('ABILITYALL.Natural Attack.${na}.TYPE=NaturalAttack.ASPECT.NaturalAttackCritMult')} |
</@loop>
</#if>

## Skills

<#assign skillCount = pcvar('count("SKILLSIT", "VIEW=VISIBLE_EXPORT")') >
<#if (skillCount > 0)>
| Skill | Total | Ranks | Ability | Misc | Trained |
|-------|-------|-------|---------|------|---------|
<@loop from=0 to=skillCount-1 ; skill, skill_has_next>
<#if (pcstring('SKILLSIT.${skill}.RANK') != "0.0" && pcstring('SKILLSIT.${skill}.RANK') != "0")>
| ${pcstring('SKILLSIT.${skill}')} | ${pcstring('SKILLSIT.${skill}.TOTAL')} | ${pcstring('SKILLSIT.${skill}.RANK')} | ${pcstring('SKILLSIT.${skill}.ABMOD')} (${pcstring('SKILLSIT.${skill}.ABILITY')}) | ${pcstring('SKILLSIT.${skill}.MISC')} | Yes |
</#if>
</@loop>
</#if>

<#-- Conditional skill modifiers -->
<#if (pcvar('countdistinct("ABILITIES","ASPECT=SkillBonus")') > 0) >
**Skill Modifiers:** <@loop from=0 to=pcvar('countdistinct("ABILITIES","ASPECT=SkillBonus")-1') ; ability, ability_has_next>${pcstring('ABILITYALL.ANY.${ability}.ASPECT=SkillBonus.ASPECT.SkillBonus')}<#if ability_has_next>; </#if></@loop>
</#if>

## Feats

<#if (pcvar('COUNT[FEATS.VISIBLE]') > 0)>
<@loop from=0 to=pcvar('COUNT[FEATS.VISIBLE]-1') ; feat, feat_has_next>
- **${pcstring('FEAT.VISIBLE.${feat}')}**<#if (pcstring('FEAT.VISIBLE.${feat}.ASSOCIATED') != '')> (${pcstring('FEAT.VISIBLE.${feat}.ASSOCIATED')})</#if><#if (pcstring('FEAT.VISIBLE.${feat}.DESC') != '')> — ${pcstring('FEAT.VISIBLE.${feat}.DESC')}</#if>
</@loop>
</#if>

<#if (pcvar('COUNT[FEATSAUTO.VISIBLE]') > 0)>
### Automatic Feats
<@loop from=0 to=pcvar('COUNT[FEATSAUTO.VISIBLE]-1') ; feat, feat_has_next>
- ${pcstring('FEATAUTO.VISIBLE.${feat}')}<#if (pcstring('FEATAUTO.VISIBLE.${feat}.ASSOCIATED') != '')> (${pcstring('FEATAUTO.VISIBLE.${feat}.ASSOCIATED')})</#if>
</@loop>
</#if>

<#if (pcvar('COUNT[VFEATS.VISIBLE]') > 0)>
### Granted Feats
<@loop from=0 to=pcvar('COUNT[VFEATS.VISIBLE]-1') ; feat, feat_has_next>
- ${pcstring('VFEAT.VISIBLE.${feat}')} (Granted)<#if (pcstring('VFEAT.VISIBLE.${feat}.ASSOCIATED') != '')> (${pcstring('VFEAT.VISIBLE.${feat}.ASSOCIATED')})</#if>
</@loop>
</#if>

## Special Abilities

<#if (pcvar('COUNT[SA]') > 0)>
<@loop from=0 to=pcvar('COUNT[SA]-1') ; sa, sa_has_next>
- **${pcstring('SPECIALABILITY.${sa}')}**<#if (pcstring('SPECIALABILITY.${sa}.DESCRIPTION') != '')> — ${pcstring('SPECIALABILITY.${sa}.DESCRIPTION')}</#if>
</@loop>
</#if>

<#-- Ability sections: use CATEGORY=Special Ability + TYPE for reliable output -->
<#assign abilSections = [
  ["Traits", "Special Ability", "Trait"],
  ["Racial Traits", "Special Ability", "RacialTrait"],
  ["Class Features", "Special Ability", "ClassFeatures"],
  ["Special Attacks", "Special Ability", "SpecialAttack"],
  ["Archetypes", "Archetype", ""]
] >
<#list abilSections as sec>
<#assign typeFilter = (sec[2] != "")?then(',"TYPE=${sec[2]}"', '') >
<#assign typeSuffix = (sec[2] != "")?then('.TYPE=${sec[2]}', '') >
<#assign secCount = pcvar('countdistinct("ABILITIES","CATEGORY=${sec[1]}"${typeFilter},"VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")')?int >
<#if (secCount > 0)>

### ${sec[0]}
<@loop from=0 to=secCount-1 ; ability, ability_has_next>
- **${pcstring('ABILITYALL.${sec[1]}.VISIBLE.${ability}${typeSuffix}')}**<#if (pcstring('ABILITYALL.${sec[1]}.VISIBLE.${ability}${typeSuffix}.DESC') != '')> — ${pcstring('ABILITYALL.${sec[1]}.VISIBLE.${ability}${typeSuffix}.DESC')}</#if>
</@loop>
</#if>
</#list>

## Armor & Protection

<#if (pcvar('COUNT[EQTYPE.ARMOR]') > 0)>
<@loop from=0 to=pcvar('COUNT[EQTYPE.ARMOR]-1') ; armor, armor_has_next>
- **${pcstring('ARMOR.EQUIPPED.${armor}.NAME')}** — AC ${pcstring('ARMOR.EQUIPPED.${armor}.TOTALAC')} | Max Dex ${pcstring('ARMOR.EQUIPPED.${armor}.MAXDEX')} | Check ${pcstring('ARMOR.EQUIPPED.${armor}.ACCHECK')} | Spell Fail ${pcstring('ARMOR.EQUIPPED.${armor}.SPELLFAIL')}%
</@loop>
</#if>

## Equipment

<#assign equipCount = pcvar('COUNT[EQUIPMENT.MERGELOC]') >
<#if (equipCount > 0)>
| Item | Qty | Weight | Cost | Location |
|------|-----|--------|------|----------|
<@loop from=0 to=equipCount-1 ; equip, equip_has_next>
| ${pcstring('EQ.MERGELOC.${equip}.NAME')} | ${pcstring('EQ.MERGELOC.${equip}.QTY')} | ${pcstring('EQ.MERGELOC.${equip}.WT')} | ${pcstring('EQ.MERGELOC.${equip}.COST')} | ${pcstring('EQ.MERGELOC.${equip}.LOCATION')} |
</@loop>

- **Gold:** ${pcstring('GOLD')} | **Total Weight:** ${pcstring('WEIGHT.CARRIED')} | **Load:** ${pcstring('WEIGHT.LOAD')}
- **Light:** ${pcstring('WEIGHT.LIGHT')} | **Medium:** ${pcstring('WEIGHT.MEDIUM')} | **Heavy:** ${pcstring('WEIGHT.HEAVY')}
</#if>

<#-- Domains -->
<#if (pcvar("COUNT[DOMAINS]") > 0) >
## Domains

<@loop from=1 to=pcvar('COUNT[DOMAINS]') ; domain, domain_has_next>
- **${pcstring('DOMAIN.${domain}')}** — ${pcstring('DOMAIN.${domain}.DESCRIPTION')}
</@loop>
</#if>

## Spells

<#-- Known/Prepared Spells per class -->
<@loop from=pcvar('COUNT[SPELLRACE]') to=pcvar('COUNT[SPELLRACE]+COUNT[CLASSES]-1') ; class, class_has_next>
<#if (pcstring("SPELLLISTCLASS.${class}") != '') >

### ${pcstring('SPELLLISTCLASS.${class}')} (CL ${pcstring('SPELLLISTCLASS.${class}.CASTERLEVEL')}, Concentration ${pcstring('SPELLLISTCLASS.${class}.CONCENTRATION')}, ${pcstring('SPELLLISTTYPE.${class}')})

<@loop from=0 to=pcvar('MAXSPELLLEVEL.${class}') ; level, level_has_next>
<#assign spellCount = pcvar('COUNT[SPELLSINBOOK.${class}.0.${level}]') >
<#if (spellCount > 0)>
**Level ${level}** (${pcstring('SPELLLISTCAST.${class}.${level}')} /day, DC ${pcstring('SPELLMEM.${class}.0.${level}.0.DC')}+):
<@loop from=0 to=spellCount-1 ; spell, spell_has_next>
- ${pcstring('SPELLMEM.${class}.0.${level}.${spell}.NAME')}<#if (pcstring('SPELLMEM.${class}.0.${level}.${spell}.TIMES') != "1")> x${pcstring('SPELLMEM.${class}.0.${level}.${spell}.TIMES')}</#if> [${pcstring('SPELLMEM.${class}.0.${level}.${spell}.SCHOOL')}<#if (pcstring('SPELLMEM.${class}.0.${level}.${spell}.SUBSCHOOL') != '')>/${pcstring('SPELLMEM.${class}.0.${level}.${spell}.SUBSCHOOL')}</#if>] — ${pcstring('SPELLMEM.${class}.0.${level}.${spell}.RANGE')}<#if (pcstring('SPELLMEM.${class}.0.${level}.${spell}.DURATION') != '')>, ${pcstring('SPELLMEM.${class}.0.${level}.${spell}.DURATION')}</#if><#if (pcstring('SPELLMEM.${class}.0.${level}.${spell}.SAVEINFO') != '' && pcstring('SPELLMEM.${class}.0.${level}.${spell}.SAVEINFO') != 'None')> (${pcstring('SPELLMEM.${class}.0.${level}.${spell}.SAVEINFO')})</#if><#if (pcstring('SPELLMEM.${class}.0.${level}.${spell}.SR') != '' && pcstring('SPELLMEM.${class}.0.${level}.${spell}.SR') != 'No')> [SR: ${pcstring('SPELLMEM.${class}.0.${level}.${spell}.SR')}]</#if>
</@loop>
</#if>
</@loop>
</#if>
</@loop>

## Languages

${pcstring('LANGUAGES')}

## Biography

- **Player:** ${pcstring('PLAYERNAME')}
- **Race Type:** ${pcstring('RACETYPE')}
- **Hair:** ${pcstring('COLOR.HAIR')} | **Eyes:** ${pcstring('COLOR.EYE')} | **Skin:** ${pcstring('COLOR.SKIN')}
- **Handed:** ${pcstring('HANDED')}
<#if (pcstring('PERSONALITY1') != '')>
- **Personality:** ${pcstring('PERSONALITY1')}<#if (pcstring('PERSONALITY2') != '')>; ${pcstring('PERSONALITY2')}</#if>
</#if>
<#if (pcstring('BIO') != '')>
- **Background:** ${pcstring('BIO')}
</#if>
