<#ftl encoding="UTF-8" strip_whitespace=true >
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<!--
Pathfinder Statblock Template
==============================
Author: Stefan Radermacher (Zaister)
Email: stefanATzaisterDOTde

$Revision: 15961 $
$Author: zaister $
$Date: 2012-02-02 13:23:59 +0100 (Do, 02 Feb 2012) $
-->
<#include "common/misc.ftl" />
<head>
	<title>${pcstring('NAME')}</title>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
	<style type="text/css">
		body  {
			font-family: Nexus, Gandhi Sans, Arial, sans-serif;
			font-size: medium;
			text-align: left;
			color: black;
			background: white;
			font-weight: normal;
			margin: 0px;
			padding: 5px;
		}
		p {
			text-indent: -1em;
			margin-left: 1em;
			margin-top: 0;
			margin-bottom: 0;
		}
		p.xp {
			font-size: large;
		}
		p.spells {
			margin-left: 2em;
		}
		p.sa {
			text-indent: 1em;
		}
		table.section {
			width: 100%;
			font-size: small;
			font-weight: bold;
			border-top-width: 1px;
			border-top-color: black;
			border-top-style: solid;
			border-bottom-width: 1px;
			border-bottom-color: black;
			border-bottom-style: solid;
			margin-top: 2px;
			margin-bottom: 2px;
		}
		table.name {
			width: 100%;
			color: white;
			background: black;
			font-weight: bold;
		}
		td.name {
			font-variant: small-caps;
			padding-left: 5px;
			padding-right: 5px;
		}
	</style>
</head>

<body>
<table class="name">
	<tr>
		<td class="name">${pcstring('TEXT.UPPER.NAME')}</td>
		<td class="name" align="right">${i18n("in_statblock_cr")}
<@compress single_line=true>
<#if (pcstring("CR") = "0")>
&mdash;
<#else>
${pcstring('CR')}
</#if>
<#if (pcvar("MR") != 0)>
/MR${" "}${pcstring("VAR.MR.INTVAL")}
</#if>
</@compress>

		</td>
	</tr>
</table>

<!-- xp award -->
<p class="xp">
<b>${i18n("in_statblock_xp")} ${pcstring('XPAWARD')}</b>
</p>

<!-- gender, classes -->
<p>
<#if (pcstring("NAME") = pcstring("RACE") && pcvar("COUNT[TEMPLATES]") = 0)><#--  |IIF(NAME:RACE.AND.VAR.COUNT[TEMPLATES]:0.0)| -->
<#else>
${pcstring('GENDER.LONG')}
<@loop from=0 to=pcvar('COUNT[TEMPLATES]-1') ; template , template_has_next>
${pcstring('TEXT.LOWERCASE.TEMPLATE.${template}.APPLIEDNAME')}
</@loop>
<#if (pcstring("AGE.CATEGORY") = "Adult")>
<#else>
${pcstring('TEXT.LOWERCASE.AGE.CATEGORY')}
</#if>
${pcstring('TEXT.LOWERCASE.RACE')}
<@loop from=0 to=pcvar('COUNT[CLASSES]-1') ; class , class_has_next>
<#if (pcstring("CLASS.${class}.ISMONSTER") = "N")>
${pcstring('TEXT.LOWERCASE.CLASS.${class}')}
<#if (pcstring("CLASS.${class}")?lower_case?contains("cleric"))>
${i18n("in_statblock_ofDeity", pcstring('DEITY'))}
</#if>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Archetype","TYPE=Archetype","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; archetype , archetype_has_next>
<#if (pcstring("ABILITYALL.Archetype.VISIBLE.${archetype}.TYPE=Archetype.TYPE")?lower_case?contains(pcstring("CLASS.${class}")?lower_case)) >
(${pcstring("TEXT.LOWERCASE.ABILITYALL.Archetype.VISIBLE.${archetype}.TYPE=Archetype")})
</#if>
</@loop>

${pcstring('CLASS.${class}.LEVEL')}
<#if (class_has_next) >
/
</#if>
</#if>
</@loop>
</#if>
</p>

<!-- alignment, size, race -->
<p>
<#if (pcstring("ALIGNMENT.SHORT") = "TN")>
N
<#else>
${pcstring('ALIGNMENT.SHORT')}
</#if>
${pcstring('SIZELONG')}
<#if (pcstring("RACETYPE") = "None")>
${pcstring('TEXT.LOWER.TYPE')}
<#else>
${pcstring('TEXT.LOWER.RACETYPE')}
</#if>
<#if (pcvar("COUNT[RACESUBTYPES]")= 0)>
<#else>
(<#t>
<@loop from=0 to=pcvar('COUNT[RACESUBTYPES]-1') ; subtype , subtype_has_next>
${pcstring('TEXT.LOWER.RACESUBTYPE.${subtype}')}<#if (subtype_has_next)>, </#if><#t>
</@loop>
)
</#if>
</p>

<!-- initiative, senses -->
<p>
<b>${i18n("in_statblock_init")}</b> ${pcstring('INITIATIVEMOD')};
<b>${i18n("in_statblock_senses")}</b>
<!-- Sense and Vision TYPE Abilities --><#t>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=Sense[or]TYPE=Vision","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; specialAbilities , specialAbilities_has_next>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Sense.TYPE=Vision.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Sense.TYPE=Vision.ASPECT.Ability Bonus')} <#t>
</#if>
<#t>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Sense.TYPE=Vision.HASASPECT.Vision") = "Y")>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Sense.TYPE=Vision.ASPECT.Vision')}<#t>
<#else>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Sense.TYPE=Vision.TYPE")?lower_case?contains("spelllike"))>
<i>${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Sense.TYPE=Vision')}</i><#t>
<#else>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Sense.TYPE=Vision')}<#t>
</#if>
</#if>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Sense.TYPE=Vision.HASASPECT.Ability Benefit") = "Y")>
 ${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Sense.TYPE=Vision.ASPECT.Ability Benefit')}<#t>
</#if>
, <#t>
</@loop>
<!-- End Sense TYPE Abilities -->
Perception ${pcstring('SKILL.Perception.TOTAL.SIGN')}<#lt>
</p>

<!-- auras -->
<#if (pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=Aura","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<p>
<!-- Aura TYPE Abilities -->
<b>${i18n("in_statblock_aura")} </b><#t>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=Aura","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; specialAbilities , specialAbilities_has_next>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Aura.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Aura.ASPECT.Ability Bonus')} <#t>
</#if>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Aura.TYPE")?lower_case?contains("spelllike"))>
<i>${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Aura')}</i><#t>
<#else>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Aura')}<#t>
</#if>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Aura.HASASPECT.Ability Benefit") = "Y")>
 ${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Aura.ASPECT.Ability Benefit')}<#t>
</#if>
<#if (specialAbilities_has_next) >
,${' '}<#t>
</#if>
</@loop>

<!-- End Aura TYPE Abilities -->
</p>
</#if>

<table class="section">
  <tr>
    <td>${i18n("in_statblock_defense")}</td>
  </tr>
</table>

<!-- armor class -->
<p>
<b>${i18n("in_statblock_ac")}</b> ${pcstring('AC.Total')}, <b>${i18n("in_statblock_touch")}</b> ${pcstring('AC.Touch')}, <b>${i18n("in_statblock_flatFooted")}</b> ${pcstring('AC.Flatfooted')} (<#t>
<#t>
<#if (pcstring("AC.Armor") = "0")>
<#else>
${pcstring('AC.Armor.SIGN')} ${i18n("in_statblock_armor")}<#t>
</#if>
<#if (pcstring("AC.Armor") = "0")>
<#else>
<#if (pcvar("AC.Deflection") = 0 && pcvar("AC.Ability") = 0 && pcvar("AC.Dodge") = 0 && pcvar("AC.NaturalArmor") = 0 && pcvar("AC.Size") = 0 && pcvar("AC.Shield") = 0 && pcvar("AC.Misc") = 0) >
<#else>
, <#t>
</#if>
</#if>
<#t>
<#if (pcstring("AC.Deflection") = "0")>
<#else>
${pcstring('AC.Deflection.SIGN')} ${i18n("in_statblock_deflection")}<#t>
</#if>
<#if (pcstring("AC.Deflection") = "0")>
<#else>
<#if (pcvar("AC.Ability") = 0 && pcvar("AC.Dodge") = 0 && pcvar("AC.NaturalArmor") = 0 && pcvar("AC.Size") = 0 && pcvar("AC.Shield") = 0 && pcvar("AC.Misc") = 0) >
<#else>
, <#t>
</#if>
</#if>
<#t>
<#if (pcstring("AC.Ability") = "0")>
<#else>
${pcstring('AC.Ability.SIGN')} Dex<#t>
</#if>
<#if (pcstring("AC.Ability") = "0")>
<#else>
<#if (pcvar("AC.Dodge") = 0 && pcvar("AC.NaturalArmor") = 0 && pcvar("AC.Size") = 0 && pcvar("AC.Shield") = 0 && pcvar("AC.Misc") = 0) >
<#else>
, <#t>
</#if>
</#if>
<#t>
<#if (pcstring("AC.Dodge") = "0")>
<#else>
${pcstring('AC.Dodge.SIGN')} ${i18n("in_statblock_dodge")}<#t>
</#if>
<#if (pcstring("AC.Dodge") = "0")>
<#else>
<#if (pcvar("AC.NaturalArmor") = 0 && pcvar("AC.Size") = 0 && pcvar("AC.Shield") = 0 && pcvar("AC.Misc") = 0) >
<#else>
, <#t>
</#if>
</#if>
<#t>
<#if (pcstring("AC.NaturalArmor") = "0")>
<#else>
${pcstring('AC.NaturalArmor.SIGN')} ${i18n("in_statblock_natural")}<#t>
</#if>
<#if (pcstring("AC.NaturalArmor") = "0")>
<#else>
<#if (pcvar("AC.Size") = 0 && pcvar("AC.Shield") = 0 && pcvar("AC.Misc") = 0) >
<#else>
, <#t>
</#if>
</#if>
<#t>
<#if (pcstring("AC.Size") = "0")>
<#else>
${pcstring('AC.Size.SIGN')} ${i18n("in_statblock_size")}<#t>
</#if>
<#if (pcstring("AC.Size") = "0")>
<#else>
<#if (pcvar("AC.Shield") = 0 && pcvar("AC.Misc") = 0) >
<#else>
, <#t>
</#if>
</#if>
<#t>
<#if (pcstring("AC.Shield") = "0")>
<#else>
${pcstring('AC.Shield.SIGN')} ${i18n("in_statblock_shield")}<#t>
</#if>
<#if (pcstring("AC.Shield") = "0")>
<#else>
<#if (pcstring("AC.Misc") = "0")>
<#else>
, <#t>
</#if>
</#if>
<#t>
<#if (pcstring("AC.Misc") = "0")>
<#else>
${pcstring('AC.Misc.SIGN')} ${i18n("in_statblock_misc")}
</#if>
)<#lt>
</p>

<!-- hit points -->
<b>${i18n("in_statblock_hp")}</b> ${pcstring('HP')} (${pcstring('HITDICE.MEDIUM')})<#t>
<#t>
<!-- ModifyHP TYPE Abilities --><#t>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=ModifyHP","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; specialHitPoints , specialHitPoints_has_next>
<#if (specialHitPoints_has_next) >
; <#lt>
</#if>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialHitPoints}.TYPE=ModifyHP.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialHitPoints}.TYPE=ModifyHP.ASPECT.Ability Bonus')} <#t>
</#if>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${specialHitPoints}.TYPE=ModifyHP')}<#t>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialHitPoints}.TYPE=ModifyHP.HASASPECT.Ability Benefit") = "Y")>
 ${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialHitPoints}.TYPE=ModifyHP.ASPECT.Ability Benefit')}<#t>
</#if>
<#if (specialHitPoints_has_next) >
; <#lt>
</#if>
</@loop>
<!-- End ModifyHP TYPE Abilities -->
<br>
<#t>
<b>${i18n("in_statblock_fort")}</b> ${pcstring('CHECK.FORTITUDE.TOTAL')}, <b>${i18n("in_statblock_ref")}</b> ${pcstring('CHECK.REFLEX.TOTAL')}, <b>${i18n("in_statblock_will")}</b> ${pcstring('CHECK.2.TOTAL')}<#t>
<#t>
<!-- SaveBonus TYPE Abilities --><#t>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","ASPECT=SaveBonus","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; savebonus , savebonus_has_next>
, ${pcstring('ABILITYALL.ANY.VISIBLE.${savebonus}.ASPECT=SaveBonus.ASPECT.SaveBonus')}<#t>
</@loop>
<br>
<!-- End SaveBonus TYPE Abilities -->

<!--- defensive abilities, damager reduction, immune, resist, SR --->
<p>
<!-- Defensive TYPE Abilities -->
<#if (pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=Defensive","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<@compress single_line=true>
<b>${i18n("in_statblock_defensiveAbilities")}</b>${" "}<#t>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=Defensive","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; defensiveAbilities , defensiveAbilities_has_next>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${defensiveAbilities}.TYPE=Defensive.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${defensiveAbilities}.TYPE=Defensive.ASPECT.Ability Bonus')} <#t>
</#if>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${defensiveAbilities}.TYPE=Defensive')}<#t>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${defensiveAbilities}.TYPE=Defensive.HASASPECT.Ability Benefit") = "Y")>
 ${pcstring('ABILITYALL.Special Ability.VISIBLE.${defensiveAbilities}.TYPE=Defensive.ASPECT.Ability Benefit')}<#t>
</#if>
<#if (defensiveAbilities_has_next) >
,${' '}<#t>
</#if>
</@loop>
</@compress>
; 
</#if>
<!-- End Defensive TYPE Abilities -->

<@compress single_line=true>
<#assign dr = pcstring('DR') />
<#if (dr != '')>
<b>${i18n("in_statblock_dr")}</b> ${dr?lower_case}<#t>
<#if (pcvar('countdistinct("ABILITIES","TYPE=Immunity","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0 || pcvar('countdistinct("ABILITIES","TYPE=Resistance","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0 || pcvar('SR') > 0) >
;${" "}<#t>
</#if>
</#if>

<!-- Immunity TYPE Abilities -->
<@compress single_line=true>
<#if (pcvar('countdistinct("ABILITIES","TYPE=Immunity","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<b>${i18n("in_statblock_immune")} </b><#t>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=Immunity","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; immunities , immunities_has_next>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${immunities}.TYPE=Immunity.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${immunities}.TYPE=Immunity.ASPECT.Ability Bonus')} <#t>
</#if>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${immunities}.TYPE=Immunity.HASASPECT.Immunity") = "Y")>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${immunities}.TYPE=Immunity.ASPECT.Immunity')}<#t>
<#else>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${immunities}.TYPE=Immunity')}<#t>
</#if>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${immunities}.TYPE=Immunity.HASASPECT.Ability Benefit") = "Y")>
 ${pcstring('ABILITYALL.Special Ability.VISIBLE.${immunities}.TYPE=Immunity.ASPECT.Ability Benefit')}<#t>
</#if>
<#if (immunities_has_next) >
,${" "}
<#else>
<#if (pcvar('countdistinct("ABILITIES","TYPE=Resistance","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0 || pcvar('SR') > 0) >
;${" "}
</#if>
</#if>
</@loop>
</#if>
</@compress>

<!-- End Immunity TYPE Abilities -->

<!-- Resistance TYPE Abilities -->
<#if (pcvar('countdistinct("ABILITIES","TYPE=Resistance","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<b>${i18n("in_statblock_resist")} </b>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=Resistance","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; resistances , resistances_has_next>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${resistances}.TYPE=Resistance.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${resistances}.TYPE=Resistance.ASPECT.Ability Bonus')} 
</#if>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${resistances}.TYPE=Resistance.HASASPECT.Resistance") = "Y")>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${resistances}.TYPE=Resistance.ASPECT.Resistance')}
<#else>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${resistances}.TYPE=Resistance')}
</#if>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${resistances}.TYPE=Resistance.HASASPECT.Ability Benefit") = "Y")>
  ${pcstring('ABILITYALL.Special Ability.VISIBLE.${resistances}.TYPE=Resistance.ASPECT.Ability Benefit')}
</#if>
<#if (resistances_has_next) >
,${' '}
<#else>
<#if (pcstring("SR") = "0")>
<#else>
;${' '}
</#if>
</#if>
</@loop>
</#if>
<!-- End Resistance TYPE Abilities -->

<#if (pcstring("SR") = "0")>
<#else>
<b>${i18n("in_statblock_sr")}</b> ${pcstring('SR')}
</#if>
</@compress>

</p>

<!-- Weakness TYPE Abilities -->
<#if (pcvar('countdistinct("ABILITIES","TYPE=Weakness","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<@compress single_line=true>
<b>${i18n("in_statblock_weaknesses")} </b><#t>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=Weakness","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; weaknesses , weaknesses_has_next>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${weaknesses}.TYPE=Weakness.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${weaknesses}.TYPE=Weakness.ASPECT.Ability Bonus')} <#t>
</#if>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${weaknesses}.TYPE=Weakness')}<#t>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${weaknesses}.TYPE=Weakness.HASASPECT.Ability Benefit") = "Y")>
  ${pcstring('ABILITYALL.Special Ability.VISIBLE.${weaknesses}.TYPE=Weakness.ASPECT.Ability Benefit')}<#t>
</#if>
,${' '}<#t>
</@loop>
<br><#t>
</@compress>
</#if>
<!-- End Weakness TYPE Abilities -->

<!-- Defensive Gear -->
<#if (pcvar('COUNT[EQTYPE.DefensiveGear]') > 0) >
<b>${i18n("in_statblock_defensiveGear")} </b>
<@loop from=0 to=pcvar('COUNT[EQTYPE.DefensiveGear]-1') ; defensiveGear , defensiveGear_has_next> <#-- TODO: Loop was of early exit type 1 -->
<i>${pcstring('TEXT.LOWER.EQ.IS.DefensiveGear.${defensiveGear}.NAME')}; </i>
</@loop>
<br>
</#if>
<!-- End Defensive Gear -->

<table class="section">
  <tr>
    <td>${i18n("in_statblock_offense")}</td>
  </tr>
</table>
<#t>
<@compress single_line=true>
<b>${i18n("in_statblock_speed")}</b>${" "}<#t>
<@loop from=0 to=pcvar('COUNT[MOVE]-1') ; movement , movement_has_next>
<#if (pcstring("MOVE.0.NAME") = "Walk")>
<#if (pcstring("MOVE.${movement}.NAME") = "Walk")>
${pcstring('MOVE.${movement}.RATE')}<#t>
<#else>
${pcstring('TEXT.LOWERCASE.MOVE.${movement}.NAME')} ${pcstring('MOVE.${movement}.RATE')}<#t>
</#if>
<#else>
${pcstring('TEXT.LOWERCASE.MOVE.${movement}.NAME')} ${pcstring('MOVE.${movement}.RATE')}<#t>
</#if>
<#if (pcstring("MOVE.${movement}.NAME") = "Fly")>
${" "}(${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.HIDDEN.0.TYPE=Maneuverability.ASPECT.Maneuverability')})<#t>
</#if>
<#if movement_has_next>
,${" "}<#t>
</#if>
</@loop>
<#t>
<!-- ModifyMovement TYPE Abilities --><#t>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Feat","TYPE=ModifyMovement","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; movementAbilities , movementAbilities_has_next>
,${' '}<#t>
<#if (pcstring("ABILITYALL.Feat.VISIBLE.${movementAbilities}.TYPE=ModifyMovement.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Feat.VISIBLE.${movementAbilities}.TYPE=ModifyMovement.ASPECT.Ability Bonus')} <#t>
</#if>
${pcstring('ABILITYALL.Feat.VISIBLE.${movementAbilities}.TYPE=ModifyMovement')}
<#if (pcstring("ABILITYALL.Feat.VISIBLE.${movementAbilities}.TYPE=ModifyMovement.HASASPECT.Ability Benefit") = "Y")>
 ${pcstring('ABILITYALL.Feat.VISIBLE.${movementAbilities}.TYPE=ModifyMovement.ASPECT.Ability Benefit')}<#t>
</#if>
</@loop>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=ModifyMovement","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; movementAbilities , movementAbilities_has_next>
,${' '}<#t>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${movementAbilities}.TYPE=ModifyMovement.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${movementAbilities}.TYPE=ModifyMovement.ASPECT.Ability Bonus')} <#t>
</#if>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${movementAbilities}.TYPE=ModifyMovement')}
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${movementAbilities}.TYPE=ModifyMovement.HASASPECT.Ability Benefit") = "Y")>
 ${pcstring('ABILITYALL.Special Ability.VISIBLE.${movementAbilities}.TYPE=ModifyMovement.ASPECT.Ability Benefit')}<#t>
</#if>
</@loop>
</@compress>

<!-- End ModifyMovement TYPE Abilities -->

<!-- Attacks -->
<br>
<@compress single_line=true>
<@loop from=0 to=pcvar('COUNT[EQTYPE.Weapon]-1') ; weap , weap_has_next>
<#if (pcstring("WEAPON.${weap}.HAND") != "Equipped")>
<#if (pcstring("WEAPON.${weap}.NAME") = "Swarm")>
<b>${i18n("in_statblock_melee")} </b><i>${pcstring('TEXT.LOWER.WEAPON.${weap}.NAME.NOSTAR')} </i> (${pcstring('WEAPON.${weap}.DAMAGE')})<#t>
<#else>
<#if (pcboolean("WEAPON.${weap}.ISTYPE.Ranged")) >
<b>${i18n("in_statblock_ranged")} </b><#t>
<#else>
<b>${i18n("in_statblock_melee")} </b><#t>
</#if>
<#if (pcstring("WEAPON.${weap}.TYPE")?lower_case?contains("magic"))>
<i>${pcstring('TEXT.LOWER.WEAPON.${weap}.NAME.NOSTAR')} </i><#t>
<#else>
${pcstring('TEXT.LOWER.WEAPON.${weap}.NAME.NOSTAR')} <#t>
</#if>
<#if (pcstring("WEAPON.${weap}.BASEHIT") = pcstring("INVALIDTEXT.TOHIT"))>
(${i18n("in_statblock_twoHanded")}) ${pcstring('WEAPON.${weap}.THHIT')} ((${i18n("in_statblock_twoHanded")}) ${pcstring('WEAPON.${weap}.THDAMAGE')}<#t>
<#if (pcstring('WEAPON.${weap}.CRIT')?length = 2)>
<#else>
/${pcstring('WEAPON.${weap}.CRIT')}<#t>
</#if>
<#if (pcstring("WEAPON.${weap}.MULT") = "2")>
<#else>
/x${pcstring('WEAPON.${weap}.MULT')}<#t>
</#if>
)<#t>
<#else>
${pcstring('WEAPON.${weap}.BASEHIT')} <#t>

<#if (pcstring("WEAPON.${weap}.TYPE")?lower_case?contains("ranged")) >
(${pcstring('WEAPON.${weap}.RANGELIST.1.DAMAGE')}<#t>
<#else>
(${pcstring('WEAPON.${weap}.DAMAGE')}<#t>
</#if>
<#if (pcstring('WEAPON.${weap}.CRIT')?length = 2)>
<#else>
/${pcstring('WEAPON.${weap}.CRIT')}<#t>
</#if>
<#if (pcstring("WEAPON.${weap}.MULT") = "2")>
<#else>
/x${pcstring('WEAPON.${weap}.MULT')}<#t>
</#if>
)<#t>
<#if (pcstring("WEAPON.${weap}.TYPE")?lower_case?contains("ranged") && (pcboolean('VAR.HASFEAT:Point-Blank Shot') || pcboolean('VAR.HASFEAT:Point Blank Shot'))) >
, ${i18n("in_statblock_within30Ft")} ${pcstring('WEAPON.${weap}.RANGELIST.0.BASEHIT')} (${pcstring('WEAPON.${weap}.RANGELIST.0.DAMAGE')})<#t>
</#if>
</#if>
</#if>
<br><#t>
</#if>
</@loop>
</@compress>

<!-- End Attacks -->

<!-- Space and Reach -->
<#if (pcstring("FACE") != "5 ft. by 5 ft." || pcstring("REACH") != "5 ft.") >
<p>
<b>${i18n("in_statblock_space")}</b> ${pcstring('FACE')}; <b>${i18n("in_statblock_reach")}</b> ${pcstring('REACH')}
</p>
</#if>

<!-- SpecialAttack TYPE Abilities -->
<#if (pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=SpecialAttack","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<p>
<@compress single_line=true>
<b>${i18n("in_statblock_specialAttacks")} </b><#t>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=SpecialAttack","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; specialActions , specialActions_has_next>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpecialAttack.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpecialAttack.ASPECT.Ability Bonus')} <#t>
</#if>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpecialAttack')}<#t>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpecialAttack.HASASPECT.Ability Benefit") = "Y")>
 ${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpecialAttack.ASPECT.Ability Benefit')}<#t>
</#if>
<#if (specialActions_has_next) >
,${' '}<#t>
</#if>
</@loop>
</p>
</@compress>
</#if>

<!-- End SpecialAttack TYPE Abilities -->

<!-- Innate Spell-Like Abilities -->
<#if (pcvar("COUNT[SPELLSINBOOK.0.1.0]") > 0)>
<b>${i18n("in_statblock_innateSpellLikeAbilities")}:</b>
<#assign spellbook=1/>
<#assign class=0/>
<#assign level=0/>
<@loop from=0 to=pcvar('COUNT[SPELLSINBOOK.${class}.${spellbook}.${level}]-1') ; spell , spell_has_next>
<i>${pcstring('TEXT.LOWER.SPELLMEM.${class}.${spellbook}.${level}.${spell}.NAME')}</i> (
<#if (!pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.SAVEINFO")?contains("None")
 && !pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.SAVEINFO")?contains("Harmless"))>
${i18n("in_statblock_dc")} ${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.DC')},
</#if>
<#if (pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES") = "At Will")>
${i18n("in_statblock_atWill")})
<#else>
${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES')}/${pcstring('TEXT.LOWER.SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMEUNIT')})
</#if>
</@loop>
</#if>
<!-- End Innate Spell-Like Abilities -->

<!-- Other Spell-Like Abilities -->
<@loop from=2 to=pcvar('COUNT[SPELLBOOKS]-1') ; spellbook , spellbook_has_next>
<#if (pcstring("SPELLBOOK.${spellbook}.TYPE") = "Innate Spell List")>
<#if (pcvar("COUNT[SPELLSINBOOK.0.${spellbook}.0]") > 0)>
<b>${i18n("in_statblock_spellLikeAbilitiesOf", pcstring('SPELLBOOK.${spellbook}.NAME'))}</b>
<@compress single_line=true>
<#assign class = 0/>
<#assign level = 0/>
<@loop from=0 to=pcvar('COUNT[SPELLSINBOOK.${class}.${spellbook}.${level}]-1') ; spell , spell_has_next>
<i>${pcstring('TEXT.LOWER.SPELLMEM.${class}.${spellbook}.${level}.${spell}.NAME')}</i> (<#t>
<#assign saveinfo = pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.SAVEINFO")?lower_case />
<#if (saveinfo?contains("none") || saveinfo?contains("harmless") || pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.RANGE")?lower_case?contains("personal"))>
<#else>
${i18n("in_statblock_dc")} ${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.DC')},<#t>
</#if>
<#if (pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES")?contains("Will"))>
${i18n("in_statblock_atWill")})<#t>
<#else>
${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES')}/${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMEUNIT')})<#t>
</#if>
<#if (spell_has_next) >
,${" "}<#t>
</#if>
</@loop>
</@compress>
</#if>
</#if>
</@loop>
<!-- End Other Spell-Like Abilities -->

<!-- Domain Power Spell-Like Abilities -->
<#if (pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=SpellLike.DomainPower","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<br><b>${i18n("in_statblock_domainPowerSpellLikeAbilities")}</b>${' '}
<@compress single_line=true>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=SpellLike.DomainPower","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; specialActions , specialActions_has_next>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpellLike.DomainPower.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpellLike.DomainPower.ASPECT.Ability Bonus')} <#t>
</#if>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpellLike.DomainPower')}<#t>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpellLike.DomainPower.HASASPECT.Ability Benefit") = "Y")>
 ${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialActions}.TYPE=SpellLike.DomainPower.ASPECT.Ability Benefit')}<#t>
</#if>
<#if specialActions_has_next >
,${' '}<#t>
</#if>
</@loop>
</@compress>
</#if>


<!-- Known Spells -->
<@loop from=pcvar('COUNT[SPELLRACE]') to=pcvar('COUNT[SPELLRACE]+COUNT[CLASSES]-1') ; class , class_has_next>
<#if (pcstring("SPELLLISTMEMORIZE.${class}") = "false")>
<br><b>${i18n("in_statblock_knownSpellsOf", pcstring('SPELLLISTCLASS.${class}'))}</b>
<#if (pcstring('SPELLLISTCLASS.${class}.CONCENTRATION') = "")>
(${i18n("in_statblock_cl")} <@suffixnum num=pcvar('SPELLLISTCLASS.${class}.CASTERLEVEL') />
<#else>
(${i18n("in_statblock_cl")} <@suffixnum num=pcvar('SPELLLISTCLASS.${class}.CASTERLEVEL') />, ${i18n("in_statblock_concentration")} ${pcstring('SPELLLISTCLASS.${class}.CONCENTRATION')}):
</#if>
<@loop from=9 to=0 step=-1; level, level_has_next >
<@loop from=pcvar('COUNT[SPELLSINBOOK.${class}.0.${level}]') to=pcvar('COUNT[SPELLSINBOOK.${class}.0.${level}]') ; spelllevelcount , spelllevelcount_has_next>
<#if (spelllevelcount = 0)>
<!-- no memorized spells for SPELLSINBOOK.${class} 0 ${level} -->
<#else>
<p class="spells"><@compress single_line=true>
<#if (level = 0)>
0 (${i18n("in_statblock_atWill")})<#t>
<#else>
<@suffixnum num=level/> (${pcstring('SPELLLISTCAST.${class}.${level}')}/${i18n("in_statblock_day")})<#t>
</#if>
&mdash;<#t>
<@loop from=0 to=pcvar('COUNT[SPELLSINBOOK.${class}.0.${level}]-2') ; spell , spell_has_next>
<i>${pcstring('TEXT.LOWER.SPELLMEM.${class}.0.${level}.${spell}.NAME')}</i><#t>
 <#assign saveinfo = pcstring('SPELLMEM.${class}.0.${level}.${spell}.SAVEINFO')?lower_case /> 
 <#if (saveinfo?contains("none") || saveinfo?contains("harmless") || pcstring("SPELLMEM.${class}.0.${level}.${spell}.RANGE")?lower_case?contains("personal"))>
 <#else>
 (${i18n("in_statblock_dc")} ${pcstring('SPELLMEM.${class}.0.${level}.${spell}.DC')})<#t>
 </#if>
, <#t>
</@loop>
<@loop from=pcvar('COUNT[SPELLSINBOOK.${class}.0.${level}]-1') to=pcvar('COUNT[SPELLSINBOOK.${class}.0.${level}]-1') ; spell , spell_has_next>
${pcstring('SPELLMEM.${class}.0.${level}.${spell}.BONUSSPELL')}<i>${pcstring('TEXT.LOWER.SPELLMEM.${class}.0.${level}.${spell}.NAME')}</i><#t>
 <#assign saveinfo = pcstring('SPELLMEM.${class}.0.${level}.${spell}.SAVEINFO')?lower_case /> 
 <#if (saveinfo?contains("none") || saveinfo?contains("harmless") || pcstring("SPELLMEM.${class}.0.${level}.${spell}.RANGE")?lower_case?contains("personal"))>
 <#else>
 (${i18n("in_statblock_dc")} ${pcstring('SPELLMEM.${class}.0.${level}.${spell}.DC')})<#t>
 </#if>
</@loop>
</@compress>

</p>
</#if>
</@loop>
</@loop>
</#if>
</@loop>
<!-- End Known Spells -->

<!-- Prepared Spells -->
<@loop from=2 to=pcvar('COUNT[SPELLBOOKS]-1') ; spellbook , spellbook_has_next>
<#if (pcstring("SPELLBOOK.${spellbook}.TYPE") = "Prepared Spell List" && pcvar("SPELLBOOK.${spellbook}.NUMPAGES") = 0) >
<@loop from=pcvar('COUNT[SPELLRACE]') to=pcvar('COUNT[SPELLRACE]+COUNT[CLASSES]-1') ; class , class_has_next>
<#if (pcboolean("SPELLLISTMEMORIZE.${class}"))>
<p><b>${i18n("in_statblock_spellsPreparedOf", pcstring('SPELLLISTCLASS.${class}'))}</b>
(${i18n("in_statblock_cl")} <@suffixnum num=pcvar('SPELLLISTCLASS.${class}.LEVEL') />, ${i18n("in_statblock_concentration")} ${pcstring('SPELLLISTCLASS.${class}.CONCENTRATION')}):
</p>
<@loop from=9 to=0 step=-1; level, level_has_next >
 <#assign spelllevelcount = pcvar('COUNT[SPELLSINBOOK.${class}.${spellbook}.${level}]') />
 <#if (spelllevelcount = 0)>
<!-- no memorized spells for SPELLSINBOOK.${class} ${spellbook} ${level} -->
 <#else>
<p class="spells"><@compress single_line=true>
  <#if (level = 0)>
0${' '}(${i18n("in_statblock_atWill")})<#t>
  <#else>
<@suffixnum num=level /><#t>
  </#if>
&mdash;<#t>
  <@loop from=0 to=pcvar('COUNT[SPELLSINBOOK.${class}.${spellbook}.${level}]-1') ; spell , spell_has_next>
${pcstring('TEXT.LOWER.SPELLMEM.${class}.${spellbook}.${level}.${spell}.APPLIEDNAME')}<#t>
<i>${pcstring('TEXT.LOWER.SPELLMEM.${class}.${spellbook}.${level}.${spell}.BASENAME')}</i><#t>
   <#if (pcstring("DOMAIN.1") != "") >
<sup>&#8201;${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.BONUSSPELLD')}</sup><#t>
   </#if>
   <#if (pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.SOURCESHORT") = "CR")>
   <#else>
<sup>&#8201;${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.SOURCESHORT')}</sup><#t>
   </#if>
   <#if (pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES") = "1")>
   <#else>
${' '}(${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES')})<#t>
   </#if>
   <#assign saveinfo = pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.SAVEINFO')?lower_case /> 
   <#if (saveinfo?contains("none") || saveinfo?contains("harmless") || pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.RANGE")?lower_case?contains("personal"))>
   <#else>
 ${' '}(${i18n("in_statblock_dc")} ${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.DC')})<#t>
   </#if>
   <#if (spell_has_next)>
,${' '}<#t>
   </#if>
  </@loop>
 </@compress>

</p>
</#if>
</@loop>
</#if>
</@loop>
<#else>
</#if>
</@loop>
<!-- End Prepared Spells -->

<!-- domains -->
<#if (pcvar("COUNT[DOMAINS]") > 0) >
<p class="spells">
<b>D</b> ${i18n("in_statblock_domainSpell")};
<b>${i18n("in_statblock_domains")}</b>
<@compress single_line=true>
<@loop from=1 to=pcvar('COUNT[DOMAINS]') ; domain , domain_has_next>
${pcstring('DOMAIN.${domain}')}<#t>
<#if (domain_has_next)>
,${' '}<#t>
</#if>
</@loop>
</@compress>
</p>
</#if>
<!-- End Domain list -->

<table class="section">
  <tr>
    <td>${i18n("in_statblock_statistics")}</td>
  </tr>
</table>

<@compress single_line=true>
<@loop from=0 to=pcvar('COUNT[STATS]-1') ; stat , stat_has_next> <#-- TODO: Loop was of early exit type 1 -->
<b>${pcstring('TEXT.TITLECASE.STAT.${stat}.NAME')}</b>${' '}<#t>
<#if (pcstring("STAT.${stat}.ISNONABILITY") = "Y")>
&mdash;<#t>
<#else>
${pcstring('STAT.${stat}')}<#t>
</#if>
<#if (stat_has_next)>
,${' '}<#t>
</#if>
</@loop>
</@compress>

<br>

<@compress single_line=true>
<b>${i18n("in_statblock_baseAtk")}</b> ${pcstring('ATTACK.MELEE.BASE')}; <#t>
<#if (pcvar("UseCombatManueverBonus") = 1)>
 <#assign useAPGCombatManuevers = (pcvar("UseAPGCombatManuevers") = 1) />
 <b>${i18n("in_statblock_cmb")}</b> ${pcstring('VAR.CMB.INTVAL.SIGN')}<#t>
 <#if (pcvar("CMB") != pcvar("CMB_BullRush")) >
 (${pcstring('VAR.CMB_BullRush.INTVAL.SIGN')} ${i18n("in_statblock_bullRush")})<#t>
</#if>
 <#if (useAPGCombatManuevers && pcvar("CMB") != pcvar("CMB_DirtyTricks")) >
 (${pcstring('VAR.CMB_DirtyTricks.INTVAL.SIGN')} ${i18n("in_statblock_dirtyTricks")})<#t>
 </#if>
 <#if (pcvar("CMB") != pcvar("CMB_Disarm")) >
 (${pcstring('VAR.CMB_Disarm.INTVAL.SIGN')} ${i18n("in_statblock_disarm")})<#t>
 </#if>
 <#if (useAPGCombatManuevers && pcvar("CMB") != pcvar("CMB_Drag")) >
 (${pcstring('VAR.CMB_Drag.INTVAL.SIGN')} ${i18n("in_statblock_drag")})<#t>
 </#if>
 <#if (pcvar("CMB") != pcvar("CMB_Grapple")) >
 (${pcstring('VAR.CMB_Grapple.INTVAL.SIGN')} ${i18n("in_statblock_grapple")})<#t>
 </#if>
 <#if (pcvar("CMB") != pcvar("CMB_Overrun")) >
 (${pcstring('VAR.CMB_Overrun.INTVAL.SIGN')} ${i18n("in_statblock_overrun")})<#t>
 </#if>
 <#if (useAPGCombatManuevers && pcvar("CMB") != pcvar("CMB_Reposition"))>
 (${pcstring('VAR.CMB_Reposition.INTVAL.SIGN')} ${i18n("in_statblock_reposition")})<#t>
 </#if>
 <#if (useAPGCombatManuevers && pcvar("CMB") != pcvar("CMB_Steal"))>
 (${pcstring('VAR.CMB_Steal.INTVAL.SIGN')} ${i18n("in_statblock_steal")})<#t>
 </#if>
 <#if (pcvar("CMB") != pcvar("CMB_Sunder")) >
 (${pcstring('VAR.CMB_Sunder.INTVAL.SIGN')} ${i18n("in_statblock_sunder")})<#t>
 </#if>
 <#if (pcvar("CMB") != pcvar("CMB_Trip")) >
 (${pcstring('VAR.CMB_Trip.INTVAL.SIGN')} ${i18n("in_statblock_trip")})<#t>
 </#if>
 <#if (pcvar("CMD") > 0) >
<b>; ${i18n("in_statblock_cmd")}</b> ${pcstring('VAR.CMD.INTVAL')}<#t>
  <#if (pcvar("CMD") != pcvar("CMD_BullRush")) >
 (${pcstring('VAR.CMD_BullRush.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_bullRush")})<#t>
  </#if>
  <#if (useAPGCombatManuevers && pcvar("CMD") != pcvar("CMD_DirtyTricks")) >
 (${pcstring('VAR.CMD_DirtyTricks.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_dirtyTricks")})<#t>
  </#if>
  <#if (pcvar("CMD") != pcvar("CMD_Disarm")) >
 (${pcstring('VAR.CMD_Disarm.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_disarm")})<#t>
  </#if>
  <#if (useAPGCombatManuevers && pcvar("CMD") != pcvar("CMD_Drag")) >
 (${pcstring('VAR.CMD_Drag.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_drag")})<#t>
  </#if>
  <#if (pcvar("CMD") != pcvar("CMD_Grapple")) >
 (${pcstring('VAR.CMD_Grapple.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_grapple")})<#t>
  </#if>
  <#if (pcvar("CMD") != pcvar("CMD_Overrun")) >
 (${pcstring('VAR.CMD_Overrun.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_overrun")})<#t>
  </#if>
  <#if (useAPGCombatManuevers && pcvar("CMD") != pcvar("CMD_Reposition")) >
 (${pcstring('VAR.CMD_Reposition.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_reposition")})<#t>
  </#if>
  <#if (useAPGCombatManuevers && pcvar("CMD") != pcvar("CMD_Steal")) >
 (${pcstring('VAR.CMD_Steal.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_steal")})<#t>
  </#if>
  <#if (pcvar("CMD") != pcvar("CMD_Sunder")) >
 (${pcstring('VAR.CMD_Sunder.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_sunder")})<#t>
  </#if>
  <#if (pcvar("CantBeTripped") != 0) >
 (${i18n("in_statblock_cantBeTripped")})<#t>
  <#elseif (pcvar("CMD") != pcvar("CMD_Trip")) >
 (${pcstring('VAR.CMD_Trip.INTVAL')} ${i18n("in_statblock_versus")} ${i18n("in_statblock_trip")})<#t>
  </#if>
 </#if>
<#else>
<b>${i18n("in_statblock_grp")}</b>
 <#if (pcvar("CanNotGrapple")=0)>
${pcvar('VAR.(STR+BAB+(SIZE-4)+(SIZE-4)+(SIZE-4)+(SIZE-4)+(ATTACK.GRAPPLE.MISC)).INTVAL.SIGN')}<#t>
 <#else>
&mdash;<#t>
 </#if>
</#if>
</@compress>

<br>

<p>
<@compress single_line=true>
<#if (pcvar("COUNT[FEATSALL.VISIBLE]") > 0) >
<b>${i18n("in_statblock_feats")} </b><#t>
<@loop from=0 to=pcvar('COUNT[FEATSALL.VISIBLE]-1') ; feat , feat_has_next>
${pcstring("FEATALL.VISIBLE.${feat}")}<#t>
<#if (feat_has_next) >
,${" "}
</#if>
</@loop>
</#if>
</@compress>
</p>

<p>
<b>${i18n("in_statblock_skills")}</b>
<@compress single_line=true>
<@loop from=0 to=pcvar('count("SKILLSIT","NONDEFAULT")')-1 ; skill, skill_has_next >
${pcstring('SKILLSIT.${skill}.NONDEFAULT')}${' '}<#t>
${pcstring('SKILLSIT.${skill}.NONDEFAULT.TOTAL.INTVAL.SIGN')}${' '}<#t>
<#if (skill_has_next)>
,${' '}<#t>
</#if>
</@loop>
</@compress>

</p>

<#macro communicationBlock>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=Communicate","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; specialAbilities , specialAbilities_has_next>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Communicate.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Communicate.ASPECT.Ability Bonus')} <#t>
</#if>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Communicate')}<#t>
<#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Communicate.HASASPECT.Ability Benefit") = "Y")>
  ${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=Communicate.ASPECT.Ability Benefit')}<#t>
</#if>
,${' '}<#t>
</@loop>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Feat","TYPE=Communicate","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; specialAbilities , specialAbilities_has_next>
<#if (pcstring("ABILITYALL.Feat.VISIBLE.${specialAbilities}.TYPE=Communicate.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Feat.VISIBLE.${specialAbilities}.TYPE=Communicate.ASPECT.Ability Bonus')} <#t>
</#if>
${pcstring('ABILITYALL.Feat.VISIBLE.${specialAbilities}.TYPE=Communicate')}<#t>
<#if (pcstring("ABILITYALL.Feat.VISIBLE.${specialAbilities}.TYPE=Communicate.HASASPECT.Ability Benefit") = "Y")>
  ${pcstring('ABILITYALL.Feat.VISIBLE.${specialAbilities}.TYPE=Communicate.ASPECT.Ability Benefit')}<#t>
</#if>
,${' '}<#t>
</@loop>
</#macro>

<!-- Languages and Communicate TYPE Abilities -->
<@compress single_line=true>
<#if pcstring("LANGUAGES")?length = 0 >
 <#if (pcvar('countdistinct("ABILITIES","TYPE=Communicate","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<b>${i18n("in_statblock_communication")}</b> <#t>
 <@communicationBlock />
<br><#t>
 </#if>
<#else>
<p><#t>
<b>${i18n("in_statblock_languages")}</b> ${pcstring('LANGUAGES')}<#t>
 <#if (pcvar('countdistinct("ABILITIES","TYPE=Communicate","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
,${' '}<#t>
 </#if>
 <@communicationBlock />
</p>
</#if>
</@compress>

<!-- End Languages Communicate TYPE Abilities -->

<!-- SpecialQuality TYPE Abilities -->
<p>
<#if (pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=SpecialQuality","EXCLUDETYPE=Implicit.Immunity.Defensive.Weakness.Communicate.Vision","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
 <@compress single_line=true>
<b>${i18n("in_statblock_sq")}</b>${' '}<#t>
  <@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=SpecialQuality","EXCLUDETYPE=Implicit.Immunity.Defensive.Weakness.Communicate.Vision","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; specialAbilities , specialAbilities_has_next>
   <#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=SpecialQuality.EXCLUDETYPE=Implicit;Immunity;Defensive;Weakness;Communicate;Vision.HASASPECT.Ability Bonus") = "Y")>
${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=SpecialQuality.EXCLUDETYPE=Implicit;Immunity;Defensive;Weakness;Communicate;VisionASPECT.Ability Bonus')}<#t>
   </#if>
${pcstring('TEXT.LOWERCASE.ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=SpecialQuality.EXCLUDETYPE=Implicit;Immunity;Defensive;Weakness;Communicate;Vision')}<#t>
   <#if (pcstring("ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=SpecialQuality.EXCLUDETYPE=Implicit;Immunity;Defensive;Weakness;Communicate;Vision.HASASPECT.Ability Benefit") = "Y")>
${' '}${pcstring('ABILITYALL.Special Ability.VISIBLE.${specialAbilities}.TYPE=SpecialQuality.EXCLUDETYPE=Implicit;Immunity;Defensive;Weakness;Communicate;Vision.ASPECT.Ability Benefit')}<#t>
   </#if>
   <#if (specialAbilities_has_next) >
,${' '}<#t>
   </#if>
  </@loop>
 </@compress>
</#if>
</p>
<!-- End SpecialQuality TYPE Abilities -->

<!-- Animal Tricks TYPE Abilities -->
<@compress single_line=true>
<#if (pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","TYPE=AnimalTrick","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<b>${i18n("in_statblock_animalTricks")}</b>${' '}${pcstring('ABILITYALLLIST.Special Ability.VISIBLE.TYPE=AnimalTrick')}<br><#t>
</#if>
</@compress>
<!-- End Animal Tricks TYPE Abilities -->

<#if (pcvar("COUNT[SA]") > 0) >
<b>${i18n("in_statblock_specialAbilities")} </b> ${pcstring('SPECIALLIST')}<br>
</#if>

<!-- Afflictions Start -->
<#if (pcvar('countdistinct("ABILITIES","CATEGORY=Afflictions","TYPE=Affliction","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")') > 0)>
<b>${i18n("in_statblock_afflictions")} </b>
<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Afflictions","TYPE=Affliction","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; affliction , affliction_has_next>
${pcstring('ABILITYALL.Afflictions.VISIBLE.${affliction}.TYPE=Affliction')}
(${pcstring('ABILITYALL.Afflictions.VISIBLE.${affliction}.TYPE=Affliction.DESC')}),
</@loop>
<br>
</#if>
<!-- Afflictions End -->

<!-- Equipment -->
<#if (pcvar('COUNT[EQUIPMENT]') > 0)>
<p>
<@compress single_line=true><#-- TODO: Add <#t> at the end of each following line with output to ensure no extra spaces are output. -->
<!-- Combat Gear --><#t>
<#if (pcvar('var("COUNT[EQUIPMENT.IS.Consumable]")') > 0)>
<b>${i18n("in_statblock_combatGear")}</b>${' '}<#t>
<@loop from=0 to=pcvar('COUNT[EQUIPMENT.IS.Consumable]-1') ; equip , equip_has_next> 
<#if (pcstring("EQTYPE.Consumable.${equip}.TYPE")?lower_case?contains("magic"))>
<i>${pcstring('TEXT.LOWER.EQTYPE.Consumable.${equip}.NAME')}</i><#t>
<#else>
${pcstring('TEXT.LOWER.EQTYPE.Consumable.${equip}.NAME')}<#t>
</#if>
<#if (pcvar('EQTYPE.Consumable.${equip}.QTY') > 1)>
${' '}(${pcstring('EQTYPE.Consumable.${equip}.QTY')})<#t>
</#if>
<#if (equip_has_next)>
,${' '}<#t>
</#if>
</@loop>
;${' '}<b>${i18n("in_statblock_other")} </b><#t>
</#if>
<b>${i18n("in_statblock_gear")}</b>${' '}<#t>
<!-- Armor -->
<#if (pcvar('COUNT[EQUIPMENT.IS.Armor]') > 0)>
<@loop from=0 to=pcvar('COUNT[EQUIPMENT.IS.Armor]-1') ; equip , equip_has_next>
<#if (pcstring("EQTYPE.Armor.${equip}.TYPE")?lower_case?contains("magic"))>
<i>${pcstring('TEXT.LOWER.EQTYPE.Armor.${equip}.NAME')}</i><#t>
<#else>
${pcstring('TEXT.LOWER.EQTYPE.Armor.${equip}.NAME')}<#t>
</#if>
<#if (equip_has_next)>
,${' '}<#t>
</#if>
</@loop>
<#if (pcvar('COUNT[EQTYPE.Weapon]') > 0 || pcvar('COUNT[EQUIPMENT.IS.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable]') > 0)>
,${' '}<#t>
</#if>
</#if>
<!-- Weapons -->
<#if (pcvar('var("COUNT[EQUIPMENT.IS.Weapon.NOT.Unarmed]")') > 0)>
<@loop from=0 to=pcvar('COUNT[EQUIPMENT.IS.Weapon.NOT.Unarmed]-1') ; equip , equip_has_next>
<#if (pcstring("EQTYPE.Weapon.NOT.Unarmed.${equip}.TYPE")?lower_case?contains("magic"))>
<i>${pcstring('TEXT.LOWER.EQ.IS.Weapon.NOT.Unarmed.${equip}.NAME')}</i><#t>
<#else>
${pcstring('TEXT.LOWER.EQ.IS.Weapon.NOT.Unarmed.${equip}.NAME')}<#t>
</#if>
<#if (pcvar('EQ.IS.Weapon.NOT.Unarmed.${equip}.QTY') > 1)>
${' '}(${pcstring('EQ.IS.Weapon.NOT.Unarmed.${equip}.QTY')})<#t>
</#if>
<#if (equip_has_next)>
,${' '}<#t>
</#if>
</@loop>
<#if (pcvar('COUNT[EQUIPMENT.NOT.Armor.NOT.Weapon.NOT.Consumable]') > 0)>
,${' '}<#t>
</#if>
</#if>
<!-- Magic Items -->
<#if (pcvar("COUNT[EQUIPMENT.IS.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable]") > 0)>
<@loop from=0 to=pcvar('COUNT[EQUIPMENT.IS.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable]-1') ; equip , equip_has_next> <#-- TODO: Loop was of early exit type 1 -->
<#if (pcstring("EQ.IS.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.TYPE")?lower_case?contains("magic"))>
<i>${pcstring('TEXT.LOWER.EQ.IS.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.NAME')}</i><#t>
<#else>
${pcstring('TEXT.LOWER.EQ.IS.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.NAME')}<#t>
</#if>
<#if (pcvar('EQ.IS.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.QTY') > 1)>
${' '}(${pcstring('EQ.IS.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.QTY')})<#t>
</#if>
<#if (equip_has_next)>
,${' '}<#t>
</#if>
</@loop>
<#if (pcvar('COUNT[EQUIPMENT.NOT.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable]') > 0)>
,${' '}<#t>
</#if>
</#if>
<!-- Remaining Equipment -->
<#if (pcvar('COUNT[EQUIPMENT.NOT.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable]') > 0)>
<@loop from=0 to=pcvar('COUNT[EQUIPMENT.NOT.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable]-1') ; equip , equip_has_next> <#-- TODO: Loop was of early exit type 1 -->
<#if (pcstring("EQ.NOT.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.TYPE")?lower_case?contains("magic"))>
<i>${pcstring('TEXT.LOWER.EQ.NOT.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.NAME')}</i><#t>
<#else>
${pcstring('TEXT.LOWER.EQ.NOT.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.NAME')}<#t>
</#if>
<#if (pcvar('EQ.NOT.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.QTY') > 1)>
${' '}(${pcstring('EQ.NOT.Magic.NOT.Armor.NOT.Weapon.NOT.Consumable.${equip}.QTY')})<#t>
</#if>
<#if (equip_has_next)>
,${' '}<#t>
</#if>
</@loop>
</#if>
</@compress>
</p>
</#if>
<!-- Equipment End -->

<!-- SpellBooks -->
<@loop from=2 to=pcvar('COUNT[SPELLBOOKS]-1') ; spellbook , spellbook_has_next>
<#if (pcstring("SPELLBOOK.${spellbook}.TYPE") = "Spell Book")>
<br><b>${pcstring('SPELLBOOKNAME.${spellbook}')}</b>
<@loop from=pcvar('COUNT[SPELLRACE]') to=pcvar('COUNT[SPELLRACE]+COUNT[CLASSES]') ; class , class_has_next> <#-- TODO: Loop was of early exit type 1 -->
<#if (pcstring("SPELLLISTMEMORIZE.${class}") = "false")>
<#else>
<@loop from=0 to=9 ; level , level_has_next>
<@loop from=pcvar('COUNT[SPELLSINBOOK.${class}.${spellbook}.${level}]') to=pcvar('COUNT[SPELLSINBOOK.${class}.${spellbook}.${level}]') ; spelllevelcount , spelllevelcount_has_next>
<#if (spelllevelcount = 0)>
<!-- no memorized spells for SPELLSINBOOK.${class} ${spellbook} ${level} -->
<#else>
<br><@suffixnum num=level />
<@loop from=0 to=pcvar('COUNT[SPELLSINBOOK.${class}.${spellbook}.${level}]-2') ; spell , spell_has_next>
<i>${pcstring('TEXT.LOWER.SPELLMEM.${class}.${spellbook}.${level}.${spell}.NAME')}</i>
<#if (pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES") = "1")>
<#else>
(${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES')})
</#if>
<#if (pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.SAVEINFO")?contains("None"))>
<#else>
(${i18n("in_statblock_dc")} ${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.DC')})
</#if>
,
</@loop>
<@loop from=pcvar('COUNT[SPELLSINBOOK.${class}.${spellbook}.${level}]-1') to=pcvar('COUNT[SPELLSINBOOK.${class}.${spellbook}.${level}]-1') ; spell , spell_has_next>
${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.BONUSSPELL')}<i>${pcstring('TEXT.LOWER.SPELLMEM.${class}.${spellbook}.${level}.${spell}.NAME')}</i>
<#if (pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES") = "1")>
<#else>
(${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.TIMES')})
</#if>
<#if (pcstring("SPELLMEM.${class}.${spellbook}.${level}.${spell}.SAVEINFO")?contains("None"))>
<#else>
(${i18n("in_statblock_dc")} ${pcstring('SPELLMEM.${class}.${spellbook}.${level}.${spell}.DC')})
</#if>
</@loop>
</#if>
</@loop>
</@loop>
</#if>
</@loop>
</#if>
</@loop>
<!-- End SpellBooks -->

<p></p>
<table class="section">
  <tr>
    <td>${i18n("in_statblock_specialAbilitiesCaps")}</td>
  </tr>
</table>

<@loop from=0 to=pcvar('countdistinct("ABILITIES","CATEGORY=Special Ability","VISIBILITY=DEFAULT[or]VISIBILITY=OUTPUT_ONLY")-1') ; allAbilities , allAbilities_has_next>
<#assign typeOfAbility = pcstring("ABILITYALL.Special Ability.VISIBLE.${allAbilities}.TYPE")?lower_case />
<#if (typeOfAbility = "Implicit")>
<#else>
<p><b>${pcstring('ABILITYALL.Special Ability.VISIBLE.${allAbilities}')}
<@typeOfAbilitySuffix typeOfAbility=typeOfAbility />
</b> ${pcstring('TEXT.REPLACEALL{<p>,<p class="sa">}.TEXT.REPLACEFIRST{<p>,}.ABILITYALL.Special Ability.VISIBLE.${allAbilities}.DESC')}</p>
</#if>
</@loop>

<!-- Start of Temporary Bonuses Added -->
<p></p>
<#if (pcvar("COUNT[TEMPBONUSNAMES]") > 0) >
<table class="section">
  <tr>
    <td>${i18n("in_statblock_temporaryBonusesCaps")}</td>
  </tr>
</table>
<b>${i18n("in_statblock_temporaryBonusesApplied")}</b>
<@loop from=0 to=pcvar('COUNT[TEMPBONUSNAMES]-1') ; temp , temp_has_next>
${pcstring('TEMPBONUS.${temp}')},
</@loop>
<br>
</#if>
<!-- End of Temporary Bonuses Added -->

</body>
</html>

