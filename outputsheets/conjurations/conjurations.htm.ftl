<#ftl output_format="HTML">
<#--
  The conjurations page: the catalog of what the character can conjure on top,
  the creatures conjured into the fight underneath.

  output_format="HTML" above makes FreeMarker escape every interpolation, so
  text written by an AI agent cannot inject markup or script into a page that
  holds a bridge to Java. Do not add ?no_esc to an authored field, and keep the
  script writing through textContent and attributes only.

  The catalog and the fight come from the `conjurations` data model
  (pcgen.io.conjurations.ConjurationOutputModel); the character's name comes
  from pcstring, as in every other output sheet.
-->
<#assign labels = conjurations.labels />
<#macro text value><#list value?split("\n") as line>${line}<#sep><br></#sep></#list></#macro>
<#-- A bonus laid on creatures; own marks a creature's own, against the group's. -->
<#macro chip bonus own><span class="chip<#if own> own</#if>" data-bonus="${bonus.key}">${bonus.label} <button type="button" class="chip-remove" data-action="<#if own>remove-bonus<#else>remove-group-bonus</#if>">&times;</button></span></#macro>
<#-- Where the player picks a bonus to lay: a name to type, and a value when the bonus asks one. -->
<#macro adder action><span class="adder"><select class="bonus-select" data-action="${action}" aria-label="${labels.addBonus}"><option value="">${labels.addBonus}</option></select><input type="number" class="bonus-value" placeholder="${labels.value}" aria-label="${labels.value}" hidden /><button type="button" class="bonus-apply" hidden>${labels.applyBonus}</button></span></#macro>
<#--
  One creature in play. The page renders the creatures already in play with it,
  and keeps one copy per catalog creature in a <template> for the script to
  clone when a new one arrives.
-->
<#macro card name number damage counted rounds found summon missing bonuses form="">
<#-- The maximum hit points arrive with the numbers PCGen computes, through the bridge -->
<#local authored = found && summon.authored />
<article class="card<#if counted && rounds == 0> gone</#if>" data-creature="${name}" data-number="${number?c}" data-damage="${damage?c}" data-max="${(authored?then(summon.hpMax, 0))?c}" data-counted="${counted?c}" data-rounds="${rounds?c}" data-authored="${authored?c}" data-form="${form}">
  <header>
    <span class="card-name"><#if found>${summon.displayName}<#else>${name}</#if> <span class="card-number">${number?c}</span><#if found><button type="button" class="inspect-creature" data-creature="${name}" title="${labels.details}" aria-label="${labels.details} : ${summon.displayName}">&#128269;</button></#if></span>
    <span class="rounds"<#if !counted> hidden</#if>>
      <button type="button" data-action="rounds-down">&minus;</button>
      <span class="rounds-left">${rounds?c}</span> ${labels.rounds}
      <button type="button" data-action="rounds-up">+</button>
      <span class="gone-tag">${labels.gone}</span>
    </span>
    <button type="button" class="dismiss" data-action="dismiss">${labels.dismiss}</button>
  </header>
  <div class="hp">
    <#if authored && summon.hpMax gt 0>
    <span class="hp-readout"><span class="hp-current">${(summon.hpMax - damage)?c}</span> / <span class="hp-maximum">${summon.hpMax?c}</span></span><span class="bar"><span></span></span>
    <#else>
    <span class="hp-readout">${labels.damage} <span class="hp-damage">${damage?c}</span></span>
    </#if>
  </div>
  <div class="hp-controls">
    <input type="number" class="amount" min="1" placeholder="1" />
    <button type="button" data-action="harm">${labels.harm}</button>
    <button type="button" data-action="heal">${labels.heal}</button>
  </div>
  <#if found>
  <#if summon.forms?has_content><label>${labels.form} <select class="form-select"><option value="">${labels.normalForm}</option><#list summon.forms as alternative><option value="${alternative.key}"<#if form == alternative.key> selected</#if>>${alternative.label}</option></#list></select></label></#if>
  <#if authored>
  <p class="building">${labels.authoredStats}</p>
  <dl class="stats"><#list conjurations.metadataFields as field><#if summon.metadata[field]?has_content><dt>${conjurations.fieldLabels[field]}</dt><dd><@text summon.metadata[field]/></dd></#if></#list></dl>
  <#else>
  <div class="own-bonuses"><span class="lbl">${labels.ownBonuses}</span><span class="chips"><#list bonuses as bonus><@chip bonus=bonus own=true/></#list></span><@adder action="add-bonus"/></div>
  <p class="building">${labels.building}</p>
  <dl class="stats"></dl>
  </#if>
  <#elseif missing?has_content>
  <p class="unresolved">${missing}</p>
  </#if>
</article>
</#macro>
<style>
:root{
  --bg:#ffffff; --bg2:#f5f6f3; --bg3:#e5e9e3; --border:#c3cbc2;
  --text:#1a1f1b; --text-dim:#545e56;
  --gold:#7f5d0b; --gold-dim:#d6c08a; --gold-bright:#5e4400;
  --atk:#a14700; --dmg:#b0241c; --def:#1d5a85; --grn:#1c6f37; --buff:#0a6b62; --warn:#8a5a00;
  --myth:#7236a0; --holy:#806800;
}
*{margin:0;padding:0;box-sizing:border-box}
[hidden]{display:none!important}
html,body{height:100%}
body{font-family:'Source Sans 3','Segoe UI',system-ui,sans-serif;background:var(--bg);color:var(--text);
  font-size:13px;line-height:1.4}
h1,h2,.card-name{font-family:Cinzel,'Palatino Linotype',Georgia,serif}

.page{display:flex;flex-direction:column;height:100vh}
.title{flex:0 0 auto;display:flex;flex-wrap:wrap;align-items:baseline;gap:4px 12px;padding:8px 12px;
  border-bottom:2px solid var(--gold-dim)}
h1{font-size:19px;letter-spacing:.06em;color:var(--gold-bright);font-weight:900}
.subtitle{color:var(--text-dim)}
.catalog{flex:0 0 58%;overflow:auto;padding:6px 12px 10px;min-height:60px}
.divider{flex:0 0 7px;cursor:row-resize;background:var(--bg3);border-top:1px solid var(--border);
  border-bottom:1px solid var(--border)}
.divider:hover{background:var(--gold-dim)}
.combat{flex:1 1 auto;overflow:auto;padding:8px 12px;min-height:60px}

h2{font-size:12px;letter-spacing:.1em;text-transform:uppercase;color:var(--gold);margin:10px 0 3px}
button{font:inherit;font-size:11px;background:var(--bg3);color:var(--text);border:1px solid var(--border);
  border-radius:3px;padding:1px 7px;cursor:pointer}
button:hover{border-color:var(--gold)}
.filters{display:flex;flex-wrap:wrap;gap:4px;margin:4px 0}
.filter{color:var(--text-dim)}
.filter.on{color:var(--gold-bright);border-color:var(--gold-dim);background:var(--bg)}

table{border-collapse:collapse;width:100%;min-width:950px;font-size:12px}
col.c-conjure{width:78px}
col.c-name{width:14%}
col.c-speed{width:6%}
col.c-hp{width:4%}
col.c-ac{width:9%}
col.c-attack{width:13%}
col.c-damage{width:15%}
col.c-cmb{width:8%}
col.c-cmd{width:7%}
col.c-ref,col.c-fort,col.c-will,col.c-init{width:4.5%}
th{position:sticky;top:0;background:var(--bg);text-align:left;font-weight:600;color:var(--text-dim);
  font-size:10px;letter-spacing:.06em;text-transform:uppercase;padding:3px 6px 3px 0}
td{padding:2px 6px 2px 0;vertical-align:top;overflow-wrap:anywhere}
tbody.summon{border-top:1px solid var(--bg3)}
td.name{color:var(--gold-bright);font-weight:600}
td.hp{color:var(--grn)}
td.ac{color:var(--def)}
td.attack{color:var(--atk)}
td.damage{color:var(--dmg)}
td.metric{text-align:right;font-variant-numeric:tabular-nums;white-space:nowrap}
th button.sort{border:0;background:transparent;padding:0;text-align:inherit;color:inherit;font:inherit}
th button.sort[aria-pressed="true"]{color:var(--gold-bright)}
tr.details td{color:var(--text-dim);font-size:11px;padding-bottom:4px}
.detail{margin-right:12px}
.detail b{color:var(--def);font-weight:600}
.tag{color:var(--buff);font-size:10px;text-transform:uppercase;letter-spacing:.06em;margin-right:6px}
.rating{font-size:11px;font-weight:600;white-space:nowrap}
.rating.red{color:#a32626}.rating.orange{color:#9c5200}.rating.green{color:#187338}.rating.blue{color:#1d5a85}
.conjure{color:var(--grn);white-space:nowrap}
.inspect-creature{border:0;background:transparent;color:var(--def);padding:0 3px;font-size:14px;line-height:1}
.inspect-creature:hover{color:var(--gold-bright)}
.drawer-shade{position:fixed;inset:0;background:rgba(15,23,19,.35);z-index:30}
.creature-drawer{position:fixed;right:0;top:0;bottom:0;width:min(480px,94vw);z-index:31;
  overflow:auto;background:var(--bg);box-shadow:-6px 0 22px rgba(0,0,0,.25);padding:14px 18px;
  transform:translateX(100%);transition:transform .2s ease}
.creature-drawer.open{transform:translateX(0)}
.drawer-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;
  border-bottom:1px solid var(--border);padding-bottom:8px}
.drawer-head h2{font-size:17px;letter-spacing:0;text-transform:none;margin:0}
.drawer-close{font-size:18px;line-height:1}
.drawer-portrait{display:block;width:250px;height:250px;margin:10px auto 0;background:#fff;border-radius:4px}
.drawer-portrait[hidden]{display:none}
.drawer-body h3{font-size:12px;color:var(--def);margin:14px 0 3px}
.drawer-body p{margin:3px 0 8px;white-space:normal}
.drawer-meta{color:var(--text-dim);font-size:11px;margin-top:8px}
.drawer-stats{display:grid;grid-template-columns:auto 1fr;gap:2px 8px;font-size:12px}
.drawer-stats dt{font-weight:600;color:var(--text-dim)}
.drawer-stats dd{margin:0}
.drawer-sources{margin-top:14px;font-size:11px;display:flex;gap:12px;flex-wrap:wrap}
.drawer-sources a{color:var(--def)}

.combat-head{display:flex;flex-wrap:wrap;align-items:center;gap:6px 10px;margin-bottom:6px}
.combat-head h2{margin:0}
.duration{color:var(--text-dim);font-size:11px}
.duration input,.amount{width:52px;font:inherit;font-size:12px;background:var(--bg);color:var(--text);
  border:1px solid var(--border);border-radius:3px;padding:1px 4px}
.nobody,.empty,.errors{color:var(--text-dim);padding:10px 0}
.errors li{color:var(--dmg);margin-left:18px}

.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(270px,1fr));gap:8px}
.card{background:var(--bg2);border:1px solid var(--border);border-left:3px solid var(--grn);border-radius:4px;
  padding:6px 8px;display:flex;flex-direction:column;gap:4px}
.card header{display:flex;flex-wrap:wrap;align-items:center;gap:4px 8px}
.card-name{color:var(--gold-bright);font-size:13px;margin-right:auto}
.card-number{color:var(--gold)}
.rounds{display:inline-flex;align-items:center;gap:3px;color:var(--text-dim);font-size:11px}
.rounds-left{color:var(--buff);font-weight:600}
.gone-tag{display:none;color:var(--warn);text-transform:uppercase;font-size:10px;letter-spacing:.06em}
.card.gone{opacity:.55;border-left-color:var(--warn)}
.card.gone .gone-tag{display:inline}
.card.down{border-left-color:var(--dmg)}
.hp{display:flex;align-items:center;gap:4px 8px}
.hp-controls{display:flex;align-items:center;gap:4px}
.hp-readout{font-variant-numeric:tabular-nums;min-width:62px}
.hp-current{color:var(--grn);font-weight:600}
.card.down .hp-current{color:var(--dmg)}
.bar{display:block;flex:1 1 60px;height:7px;background:var(--bg3);border-radius:4px;overflow:hidden}
.bar span{display:block;height:100%;width:100%;background:var(--grn)}
.card.down .bar span{background:var(--dmg)}
.stats{display:grid;grid-template-columns:max-content 1fr;gap:1px 8px;font-size:11px}
.stats dt{color:var(--def);font-weight:600}
.unresolved{color:var(--dmg);font-size:11px}

.group-bonuses,.own-bonuses{display:flex;flex-wrap:wrap;align-items:center;gap:4px 6px}
.group-bonuses{padding:5px 8px;margin-bottom:8px;background:var(--bg2);border:1px solid var(--border);border-radius:4px}
.lbl{font-size:10px;text-transform:uppercase;letter-spacing:.06em;color:var(--text-dim)}
.chips{display:inline-flex;flex-wrap:wrap;gap:4px}
.chip{display:inline-flex;align-items:center;gap:3px;font-size:11px;padding:0 2px 0 7px;border:1px solid var(--buff);
  color:var(--buff);border-radius:10px;background:var(--bg)}
.chip.own{border-color:var(--myth);color:var(--myth)}
.chip-remove{border:none;background:transparent;color:var(--text-dim);padding:0 4px}
.bonus-select{max-width:240px;font:inherit;font-size:11px;background:var(--bg);color:var(--text);border:1px dashed var(--text-dim);
  border-radius:10px;padding:1px 7px}
.bonus-value{width:52px;font:inherit;font-size:11px}
.building{color:var(--text-dim);font-size:11px}
.was{color:var(--text-dim);text-decoration:line-through;margin-right:4px}
.buffed{color:var(--holy);font-weight:600}

@media print{
  .page{display:block;height:auto}
  .catalog,.combat{overflow:visible}
  .divider,button,input,select,.duration,.adder,.drawer-shade,.creature-drawer{display:none}
}
</style>

<div class="page">
<header class="title">
  <h1>${pcstring('NAME')}</h1>
  <span class="subtitle">${pcstring('CLASSLIST')} &middot; ${pcstring('RACE')} &middot; ${pcstring('ALIGNMENT.SHORT')}</span>
</header>

<section class="catalog">
<#if !conjurations.present>
  <p class="empty">${labels.empty}</p>
<#elseif conjurations.errors?size gt 0>
  <div class="errors">
    <p>${labels.doesNotRead}</p>
    <ul>
    <#list conjurations.errors as error>
      <li>${error.text}</li>
    </#list>
    </ul>
  </div>
<#else>
  <label class="target-ac">${labels.targetArmor} <input type="number" min="1" max="99" value="20" /></label>
  <table class="catalog-table">
    <thead><tr>
      <th></th>
      <th><button class="sort" type="button" data-sort="level">${labels.level}</button></th>
      <th><button class="sort" type="button" data-sort="name">${labels.creature}</button></th>
      <th>${labels.role}</th>
      <th><button class="sort" type="button" data-sort="ranking" title="${labels.ranking}">${labels.ranking}</button></th>
      <th><button class="sort" type="button" data-sort="hitDice">${labels.hitDice}</button></th>
      <th><button class="sort" type="button" data-sort="expectedDamagePerRound" title="${labels.expectedDamage}">${labels.expectedDamage}</button></th>
      <th><button class="sort" type="button" data-sort="armorClass">${conjurations.statLabels.ac}</button></th>
      <th><button class="sort" type="button" data-sort="touchArmorClass">${labels.touchArmor}</button></th>
      <th><button class="sort" type="button" data-sort="flatFootedArmorClass">${labels.flatArmor}</button></th>
      <th><button class="sort" type="button" data-sort="combatManeuverBonus">${conjurations.statLabels.cmb}</button></th>
      <th><button class="sort" type="button" data-sort="combatManeuverDefense">${conjurations.statLabels.cmd}</button></th>
    </tr></thead>
    <#list conjurations.summons as summon>
    <tbody class="summon" data-creature="${summon.name}" data-group="${summon.group}" data-family="${summon.family}" data-level="${summon.levelRank?c}" data-order="${summon?index?c}" data-authored="${summon.authored?c}" data-available="${summon.available?c}"<#if summon.ranking.score??> data-sort-ranking="${summon.ranking.score?c}"</#if>>
      <tr>
        <td><#if (summon.family == "Expanded" || summon.family == "Versatile SM") && summon.choiceRequired><button type="button" class="select-choice" data-creature="${summon.name}" title="${summon.choiceHint}">${labels.selectChoice}</button><#else><button type="button" class="conjure" data-creature="${summon.name}"<#if !summon.available> disabled title="${summon.unavailableHint}"</#if>>${labels.conjure}</button></#if></td>
        <td class="metric">${summon.displayGroup}</td>
        <td class="name">${summon.displayName} <button type="button" class="inspect-creature" data-creature="${summon.name}" title="${labels.details}" aria-label="${labels.details} : ${summon.displayName}">&#128269;</button></td>
        <td><#list summon.roles as role><span class="tag">${role}</span></#list></td>
        <td><#if summon.ranking.color?has_content><a class="rating ${summon.ranking.color}" data-reference="true" href="${summon.ranking.source}" title="${labels.ranking} : ${labels['rating_' + summon.ranking.color + '_meaning']}" target="_blank" rel="noopener noreferrer">${labels['rating_' + summon.ranking.color]}</a><#else>${labels.unranked}</#if></td>
        <#list ["hitDice", "expectedDamagePerRound", "armorClass", "touchArmorClass", "flatFootedArmorClass", "combatManeuverBonus", "combatManeuverDefense"] as metric>
        <td class="metric" data-metric="${metric}">…</td>
        </#list>
      </tr>
      <#if summon.authored>
      <tr class="details"><td></td><td colspan="11">
        <span class="detail">${labels.authoredStats}</span>
      </td></tr>
      </#if>
    </tbody>
    </#list>
  </table>
</#if>
</section>

<div class="divider"></div>

<section class="combat">
  <div class="combat-head">
    <h2>${labels.inPlay} <span class="count"></span></h2>
    <button type="button" data-action="next-round">${labels.nextRound}</button>
    <button type="button" data-action="dismiss-all">${labels.dismissAll}</button>
    <#if conjurations.skeletonFeat><button type="button" data-action="reset-skeleton" data-used-title="${labels.skeletonUsed}">${labels.skeletonReset}</button></#if>
    <label class="duration">${labels.duration} <input type="number" min="0" value="${conjurations.duration?c}" /></label>
  </div>
  <div class="group-bonuses"><span class="lbl">${labels.group}</span><span class="chips"><#list conjurations.groupBonuses as bonus><@chip bonus=bonus own=false/></#list></span><@adder action="add-group-bonus"/></div>
  <dl class="stat-labels" hidden><#list conjurations.statLabels as key, label><dt data-stat="${key}">${label}</dt></#list></dl>
  <p class="nobody"<#if conjurations.conjured?size gt 0> hidden</#if>>${labels.nobody}</p>
  <div class="cards">
  <#list conjurations.conjured as conjured>
    <@card name=conjured.name number=conjured.number damage=conjured.damage counted=conjured.counted rounds=conjured.rounds found=conjured.found summon=conjured.summon missing=conjured.missing bonuses=conjured.bonuses form=conjured.form/>
  </#list>
  </div>
  <#list conjurations.summons as summon>
  <template data-creature="${summon.name}"><@card name=summon.name number=1 damage=0 counted=false rounds=0 found=true summon=summon missing="" bonuses=[]/></template>
  </#list>
</section>
</div>

<div class="drawer-shade" hidden></div>
<aside class="creature-drawer" role="dialog" aria-modal="true" aria-label="${labels.details}" data-hit-dice-label="${labels.hitDice}" data-expected-damage-label="${labels.expectedDamage}" data-building-label="${labels.building}" hidden>
  <div class="drawer-head"><h2 class="drawer-name"></h2><button type="button" class="drawer-close" aria-label="${labels.closeDetails}">&times;</button></div>
  <img class="drawer-portrait" alt="" hidden>
  <div class="drawer-body"></div>
</aside>
<#list conjurations.summons as summon>
<template class="creature-details" data-creature="${summon.name}" data-race="${summon.race}" data-display-name="${summon.displayName}">
  <p class="drawer-meta">${summon.displayGroup}<#if summon.roles?has_content> · <#list summon.roles as role>${role}<#sep>, </#sep></#list></#if></p>
  <p class="drawer-meta">${labels.sourceRace} : ${summon.displayRace}<#if summon.displayTemplates?has_content> · ${labels.templates} : <#list summon.displayTemplates as template>${template}<#sep>, </#sep></#list></#if></p>
  <p class="drawer-meta">${labels.ranking} : <#if summon.ranking.color?has_content><a class="rating ${summon.ranking.color}" data-reference="true" href="${summon.ranking.source}" title="${labels['rating_' + summon.ranking.color + '_meaning']}" target="_blank" rel="noopener noreferrer">${labels['rating_' + summon.ranking.color]}</a><#else>${labels.unranked}</#if></p>
  <#if summon.guide.interest?has_content><h3>${labels.interest}</h3><p><@text summon.guide.interest/></p></#if>
  <#if summon.guide.usage?has_content><h3>${labels.usage}</h3><p><@text summon.guide.usage/></p></#if>
  <#if summon.guide.limits?has_content><h3>${labels.limits}</h3><p><@text summon.guide.limits/></p></#if>
  <#if summon.metadata.special?has_content><h3>${labels.abilities}</h3><p><@text summon.metadata.special/></p></#if>
  <#if summon.metadata.spells?has_content><h3>${labels.spells}</h3><p><@text summon.metadata.spells/></p></#if>
  <#if summon.note?has_content><h3><#if summon.metadata.publishedName?has_content>${labels.rulesNote}<#else>${labels.authoredNote}</#if></h3><p><@text summon.note/></p></#if>
  <#if summon.forms?has_content><h3>${labels.form}</h3><#list summon.forms as alternative><p><b>${alternative.label}</b> : ${alternative.description} <a data-reference="true" href="${alternative.source}" target="_blank" rel="noopener noreferrer">${labels.publishedRules}</a></p></#list></#if>
  <#if summon.collectiveForm.label?has_content><h3>${summon.collectiveForm.label}</h3><p>${summon.collectiveForm.description} <a data-reference="true" href="${summon.collectiveForm.source}" target="_blank" rel="noopener noreferrer">${labels.publishedRules}</a></p></#if>
  <#if summon.choiceRequired><p>${summon.choiceHint}</p></#if>
  <#if !summon.available && !summon.choiceRequired><p>${summon.unavailableHint}</p></#if>
  <h3>${labels.fullStats}</h3>
  <#if summon.authored><dl class="drawer-stats"><#list conjurations.metadataFields as field><#if summon.metadata[field]?has_content && field != "note" && field != "special" && field != "spells"><dt>${conjurations.fieldLabels[field]}</dt><dd><@text summon.metadata[field]/></dd></#if></#list></dl></#if>
  <#if !summon.authored><p class="drawer-stat-status">${labels.computedInApp}</p><dl class="drawer-stats"></dl></#if>
  <div class="drawer-sources"><#if summon.guide.bestiary?has_content><a data-reference="true" href="${summon.guide.bestiary}" target="_blank" rel="noopener noreferrer">${labels.bestiarySource}</a></#if><#if summon.ranking.source?has_content><a data-reference="true" href="${summon.ranking.source}" target="_blank" rel="noopener noreferrer">${labels.guideSource}</a></#if><#if summon.metadata.source?has_content><a data-reference="true" href="${summon.metadata.source}" target="_blank" rel="noopener noreferrer">${labels.publishedRules}</a><#else><a data-reference="true" href="https://legacy.aonprd.com/coreRulebook/spells/summonMonster.html" target="_blank" rel="noopener noreferrer">${labels.rulesSource}</a></#if></div>
</template>
</#list>

<script>
(function () {
  "use strict";

  // Inside PCGen a bridge to Java is planted on the window once the document
  // is loaded; in a plain browser there is none, and the page keeps the fight
  // to itself. Read lazily, so it is found whenever it arrives.
  function bridge() {
    return window.pcgen || null;
  }

  function each(nodes, action) {
    Array.prototype.forEach.call(nodes, action);
  }

  function whole(text, fallback) {
    var parsed = parseInt(text, 10);
    return isNaN(parsed) ? fallback : parsed;
  }

  var page = document.querySelector(".page");
  var cards = document.querySelector(".cards");
  var nobody = document.querySelector(".nobody");
  var counter = document.querySelector(".combat-head .count");
  var durationInput = document.querySelector(".combat-head .duration input");

  function nameOf(card) {
    return card.getAttribute("data-creature");
  }

  function numberOf(card) {
    return whole(card.getAttribute("data-number"), 0);
  }

  // A card's numbers live in its data attributes; painting turns them into
  // what the card shows.
  function paint(card) {
    var damage = whole(card.getAttribute("data-damage"), 0);
    var maximum = whole(card.getAttribute("data-max"), 0);
    var counted = card.getAttribute("data-counted") === "true";
    var rounds = whole(card.getAttribute("data-rounds"), 0);
    var current = card.querySelector(".hp-current");
    if (current) {
      current.textContent = String(maximum - damage);
    }
    var damageTaken = card.querySelector(".hp-damage");
    if (damageTaken) {
      damageTaken.textContent = String(damage);
    }
    var bar = card.querySelector(".bar span");
    if (bar && maximum > 0) {
      bar.style.width = Math.max(0, Math.min(100, (maximum - damage) * 100 / maximum)) + "%";
    }
    card.querySelector(".rounds").hidden = !counted;
    card.querySelector(".rounds-left").textContent = String(rounds);
    card.classList.toggle("down", maximum > 0 && damage >= maximum);
    card.classList.toggle("gone", counted && rounds === 0);
  }

  function refreshCount() {
    var count = cards.querySelectorAll(".card").length;
    counter.textContent = count > 0 ? "(" + count + ")" : "";
    nobody.hidden = count > 0;
  }

  function lowestFreeNumber(name) {
    var taken = {};
    each(cards.querySelectorAll(".card"), function (card) {
      if (nameOf(card).toLowerCase() === name.toLowerCase()) {
        taken[numberOf(card)] = true;
      }
    });
    var number = 1;
    while (taken[number]) {
      number++;
    }
    return number;
  }

  function templateFor(name) {
    var found = null;
    each(document.querySelectorAll("template[data-creature]"), function (candidate) {
      if (!found && candidate.getAttribute("data-creature") === name) {
        found = candidate;
      }
    });
    return found;
  }

  function conjure(name, family) {
    var template = templateFor(name);
    if (!template || !template.content) {
      return;
    }
    var number = 0;
    var link = bridge();
    if (link && link.conjure) {
      try {
        number = whole(link.conjure(name), 0);
      } catch (refused) {
        number = 0;
      }
      if (number < 1) {
        return;
      }
    }
    if (number < 1) {
      number = lowestFreeNumber(name);
    }
    var duration = Math.max(whole(durationInput.value, 0), 0);
    var card = document.importNode(template.content.querySelector(".card"), true);
    card.setAttribute("data-number", String(number));
    card.setAttribute("data-damage", "0");
    card.setAttribute("data-counted", duration > 0 ? "true" : "false");
    card.setAttribute("data-rounds", String(duration));
    card.querySelector(".card-number").textContent = String(number);
    cards.appendChild(card);
    if (family === "Skeleton 1/day") {
      each(document.querySelectorAll('.catalog tbody[data-family="Skeleton 1/day"] .conjure'), function (button) {
        button.disabled = true;
        button.title = document.querySelector("[data-action=reset-skeleton]").getAttribute("data-used-title");
      });
    }
    paint(card);
    refreshCount();
    if (card.scrollIntoView) {
      card.scrollIntoView(false);
    }
    refreshCardsLater([card]);
  }

  each(document.querySelectorAll(".catalog .conjure"), function (button) {
    button.addEventListener("click", function () {
      conjure(button.getAttribute("data-creature"), button.closest("tbody").getAttribute("data-family"));
    });
  });

  each(document.querySelectorAll(".catalog .select-choice"), function (button) {
    button.addEventListener("click", function () {
      var link = bridge();
      if (link && link.selectPublished && link.selectPublished(button.getAttribute("data-creature"))) {
        button.disabled = true;
      }
    });
  });

  cards.addEventListener("change", function (event) {
    if (!event.target.classList || !event.target.classList.contains("form-select")) {
      return;
    }
    var card = event.target.closest(".card");
    var selected = event.target.value;
    var link = bridge();
    if (link && link.setForm && !link.setForm(nameOf(card), numberOf(card), selected)) {
      event.target.value = card.getAttribute("data-form") || "";
      return;
    }
    card.setAttribute("data-form", selected);
    refreshCardsLater([card]);
  });

  cards.addEventListener("click", function (event) {
    var button = event.target.closest("[data-action]");
    var card = button ? button.closest(".card") : null;
    if (!card) {
      return;
    }
    var link = bridge();
    var name = nameOf(card);
    var number = numberOf(card);
    var action = button.getAttribute("data-action");
    if (action === "harm" || action === "heal") {
      var amount = Math.max(whole(card.querySelector(".amount").value, 1), 1);
      var damage = whole(card.getAttribute("data-damage"), 0);
      damage = (action === "harm") ? damage + amount : Math.max(damage - amount, 0);
      card.setAttribute("data-damage", String(damage));
      paint(card);
      if (link && link.setDamage) {
        link.setDamage(name, number, damage);
      }
    } else if (action === "rounds-down" || action === "rounds-up") {
      var step = (action === "rounds-up") ? 1 : -1;
      var rounds = Math.max(whole(card.getAttribute("data-rounds"), 0) + step, 0);
      card.setAttribute("data-rounds", String(rounds));
      paint(card);
      if (link && link.setRoundsLeft) {
        link.setRoundsLeft(name, number, rounds);
      }
    } else if (action === "remove-bonus") {
      if (link && link.removeBonus) {
        paintViews(link.removeBonus(name, number, button.closest(".chip").getAttribute("data-bonus")));
      }
    } else if (action === "dismiss") {
      cards.removeChild(card);
      refreshCount();
      if (link && link.dismiss) {
        link.dismiss(name, number);
      }
    }
  });

  document.querySelector(".combat-head").addEventListener("click", function (event) {
    var button = event.target.closest("[data-action]");
    if (!button) {
      return;
    }
    var link = bridge();
    var action = button.getAttribute("data-action");
    if (action === "next-round") {
      each(cards.querySelectorAll(".card"), function (card) {
        if (card.getAttribute("data-counted") === "true") {
          card.setAttribute("data-rounds", String(Math.max(whole(card.getAttribute("data-rounds"), 0) - 1, 0)));
          paint(card);
        }
      });
      if (link && link.nextRound) {
        link.nextRound();
      }
    } else if (action === "dismiss-all") {
      each(cards.querySelectorAll(".card"), function (card) {
        cards.removeChild(card);
      });
      refreshCount();
      if (link && link.dismissAll) {
        link.dismissAll();
      }
    } else if (action === "reset-skeleton") {
      if (link && link.resetSkeletonUse) {
        link.resetSkeletonUse();
      }
      each(document.querySelectorAll('.catalog tbody[data-family="Skeleton 1/day"] .conjure'), function (conjureButton) {
        conjureButton.disabled = false;
        conjureButton.title = "";
      });
    }
  });

  durationInput.addEventListener("change", function () {
    var rounds = Math.max(whole(durationInput.value, 0), 0);
    durationInput.value = String(rounds);
    var link = bridge();
    if (link && link.setDuration) {
      link.setDuration(rounds);
    }
  });

  // PCGen builds a creature as a character of its own and computes its
  // numbers; they reach the page only through the bridge, as JSON.
  function parsed(json) {
    try {
      return JSON.parse(json);
    } catch (unreadable) {
      return null;
    }
  }

  var catalogTable = document.querySelector(".catalog-table");
  var targetArmorInput = document.querySelector(".target-ac input");
  var catalogSort = null;
  var catalogAscending = false;
  var catalogRevision = 0;

  function sortCatalog() {
    if (!catalogTable || !catalogSort) {
      return;
    }
    var rows = Array.prototype.slice.call(catalogTable.querySelectorAll("tbody.summon"));
    rows.sort(function (left, right) {
      var a, b;
      if (catalogSort === "name") {
        a = left.getAttribute("data-creature");
        b = right.getAttribute("data-creature");
        var named = a.localeCompare(b);
        return (catalogAscending ? named : -named) ||
          whole(left.getAttribute("data-order"), 0) - whole(right.getAttribute("data-order"), 0);
      }
      if (catalogSort === "level") {
        a = Number(left.getAttribute("data-level"));
        b = Number(right.getAttribute("data-level"));
      } else {
        var rawA = left.getAttribute("data-sort-" + catalogSort);
        var rawB = right.getAttribute("data-sort-" + catalogSort);
        a = rawA === null ? NaN : Number(rawA);
        b = rawB === null ? NaN : Number(rawB);
      }
      var missingA = !isFinite(a);
      var missingB = !isFinite(b);
      if (missingA || missingB) {
        return missingA === missingB ? 0 : (missingA ? 1 : -1);
      }
      return (catalogAscending ? a - b : b - a) ||
        whole(left.getAttribute("data-order"), 0) - whole(right.getAttribute("data-order"), 0);
    });
    rows.forEach(function (row) {
      catalogTable.appendChild(row);
    });
  }

  if (catalogTable) {
    each(catalogTable.querySelectorAll("button.sort"), function (button) {
      button.addEventListener("click", function () {
        var key = button.getAttribute("data-sort");
        catalogAscending = (catalogSort === key) ? !catalogAscending : key === "name";
        catalogSort = key;
        each(catalogTable.querySelectorAll("button.sort"), function (other) {
          other.setAttribute("aria-pressed", other === button ? "true" : "false");
        });
        sortCatalog();
      });
    });
  }

  function paintCatalogStats(row, stats) {
    each(row.querySelectorAll("[data-metric]"), function (cell) {
      var key = cell.getAttribute("data-metric");
      var value = stats ? stats[key] : null;
      cell.textContent = value === null || value === undefined ? "—" :
        (key === "expectedDamagePerRound" ? Number(value).toFixed(1) : String(value));
      if (typeof value === "number") {
        row.setAttribute("data-sort-" + key, String(value));
      } else if (key === "combatManeuverBonus" && value !== null) {
        row.setAttribute("data-sort-" + key, String(Number(value)));
      } else {
        row.removeAttribute("data-sort-" + key);
      }
    });
    sortCatalog();
  }

  function refreshCatalogLater() {
    if (!catalogTable || !bridge()) {
      return;
    }
    var queue = Array.prototype.slice.call(catalogTable.querySelectorAll("tbody.summon:not([hidden])"));
    var revision = ++catalogRevision;
    var targetArmor = Math.max(1, Math.min(99, whole(targetArmorInput.value, 20)));
    targetArmorInput.value = String(targetArmor);
    function next() {
      if (!queue.length || revision !== catalogRevision) {
        return;
      }
      var row = queue[0];
      var stats = null;
      if (row.getAttribute("data-authored") !== "true" && row.getAttribute("data-available") === "true") {
        try {
          stats = parsed(bridge().catalogStats(row.getAttribute("data-creature"), targetArmor));
        } catch (unavailable) {
          stats = null;
        }
        if (stats && stats.pending) {
          window.setTimeout(next, 100);
          return;
        }
      }
      queue.shift();
      paintCatalogStats(row, stats);
      window.setTimeout(next, 0);
    }
    window.setTimeout(next, 0);
  }
  if (targetArmorInput) {
    targetArmorInput.addEventListener("change", refreshCatalogLater);
  }
  refreshCatalogLater();

  var statLabels = {};
  each(document.querySelectorAll(".stat-labels dt"), function (label) {
    statLabels[label.getAttribute("data-stat")] = label.textContent;
  });

  function cardOf(name, number) {
    var found = null;
    each(cards.querySelectorAll(".card"), function (card) {
      if (!found && nameOf(card) === name && numberOf(card) === number) {
        found = card;
      }
    });
    return found;
  }

  // A value a bonus changed shows the old one struck through beside the new.
  function valueCell(now, before) {
    var cell = document.createElement("dd");
    if (before !== undefined && String(before) !== String(now)) {
      var was = document.createElement("span");
      was.className = "was";
      was.textContent = String(before);
      cell.appendChild(was);
      var buffed = document.createElement("span");
      buffed.className = "buffed";
      buffed.textContent = String(now);
      cell.appendChild(buffed);
    } else {
      cell.textContent = String(now);
    }
    return cell;
  }

  function row(list, label, now, before) {
    var term = document.createElement("dt");
    term.textContent = label;
    list.appendChild(term);
    list.appendChild(valueCell(now, before));
  }

  function armorOf(stats) {
    return stats.armorClass + " / " + stats.touchArmorClass + " / " + stats.flatFootedArmorClass;
  }

  function paintStats(card, view) {
    var list = card.querySelector(".stats");
    if (!list || !view) {
      return;
    }
    var now = view.stats;
    var base = view.baseStats || {};
    var baseAttacks = base.attacks || [];
    while (list.firstChild) {
      list.removeChild(list.firstChild);
    }
    row(list, statLabels.speed, now.speed, base.speed);
    row(list, statLabels.ac, armorOf(now), base.armorClass === undefined ? undefined : armorOf(base));
    (now.attacks || []).forEach(function (attack, index) {
      var before = baseAttacks[index];
      row(list, attack.name, attack.toHit + " · " + attack.damage,
        before ? before.toHit + " · " + before.damage : undefined);
    });
    row(list, statLabels.cmb, now.combatManeuverBonus, base.combatManeuverBonus);
    row(list, statLabels.cmd, now.combatManeuverDefense, base.combatManeuverDefense);
    row(list, statLabels.ref + " / " + statLabels.fort + " / " + statLabels.will,
      now.reflex + " / " + now.fortitude + " / " + now.will,
      base.reflex === undefined ? undefined : base.reflex + " / " + base.fortitude + " / " + base.will);
    row(list, statLabels.init, now.initiative, base.initiative);
    card.setAttribute("data-max", String(now.maximumHitPoints));
    var building = card.querySelector(".building");
    if (building) {
      building.hidden = true;
    }
    paint(card);
  }

  var drawer = document.querySelector(".creature-drawer");
  var drawerShade = document.querySelector(".drawer-shade");
  var drawerBody = drawer.querySelector(".drawer-body");
  var drawerRevision = 0;
  var drawerTrigger = null;

  function paintDrawerStats(name, number, revision) {
    if (revision !== drawerRevision || drawer.hidden) {
      return;
    }
    var link = bridge();
    if (!link || !link.catalogStats) {
      return;
    }
    var targetArmor = targetArmorInput ? Math.max(1, Math.min(99, whole(targetArmorInput.value, 20))) : 20;
    var stats = parsed(number > 0 && link.inPlayStats ? link.inPlayStats(name, number, targetArmor) :
      link.catalogStats(name, targetArmor));
    if (stats && stats.pending) {
      window.setTimeout(function () { paintDrawerStats(name, number, revision); }, 100);
      return;
    }
    var list = drawerBody.querySelector(".drawer-stats");
    if (!stats || !list) {
      return;
    }
    while (list.firstChild) {
      list.removeChild(list.firstChild);
    }
    row(list, drawer.getAttribute("data-hit-dice-label"), stats.hitDice);
    var card = number > 0 ? cardOf(name, number) : null;
    row(list, statLabels.hp, card ? String(stats.maximumHitPoints - whole(card.getAttribute("data-damage"), 0)) +
      " / " + stats.maximumHitPoints : stats.maximumHitPoints);
    row(list, statLabels.speed, stats.speed);
    row(list, statLabels.ac, armorOf(stats));
    (stats.attacks || []).forEach(function (attack) {
      row(list, attack.name, attack.toHit + " · " + attack.damage);
    });
    row(list, statLabels.cmb, stats.combatManeuverBonus);
    row(list, statLabels.cmd, stats.combatManeuverDefense);
    row(list, statLabels.ref + " / " + statLabels.fort + " / " + statLabels.will,
      stats.reflex + " / " + stats.fortitude + " / " + stats.will);
    row(list, statLabels.init, stats.initiative);
    if (stats.expectedDamagePerRound !== null) {
      row(list, drawer.getAttribute("data-expected-damage-label"),
        Number(stats.expectedDamagePerRound).toFixed(1));
    }
    drawerBody.querySelector(".drawer-stat-status").hidden = true;
  }

  function closeDetails() {
    drawerRevision++;
    drawer.classList.remove("open");
    drawerShade.hidden = true;
    window.setTimeout(function () {
      if (!drawer.classList.contains("open")) {
        drawer.hidden = true;
      }
    }, 220);
    if (drawerTrigger) {
      drawerTrigger.focus();
    }
  }

  function openDetails(name, trigger) {
    var detail = null;
    each(document.querySelectorAll("template.creature-details"), function (candidate) {
      if (!detail && candidate.getAttribute("data-creature") === name) {
        detail = candidate;
      }
    });
    if (!detail || !detail.content) {
      return;
    }
    drawerRevision++;
    var revision = drawerRevision;
    var card = trigger.closest(".card");
    var number = card ? numberOf(card) : 0;
    drawer.setAttribute("data-creature", name);
    drawer.setAttribute("data-number", String(number));
    drawerTrigger = trigger;
    while (drawerBody.firstChild) {
      drawerBody.removeChild(drawerBody.firstChild);
    }
    drawerBody.appendChild(document.importNode(detail.content, true));
    var pending = drawerBody.querySelector(".drawer-stat-status");
    if (pending && bridge()) {
      pending.textContent = drawer.getAttribute("data-building-label");
    }
    drawer.querySelector(".drawer-name").textContent = detail.getAttribute("data-display-name") + (number > 0 ? " #" + number : "");
    var portrait = drawer.querySelector(".drawer-portrait");
    var portraitAddress = bridge() && bridge().portrait ? bridge().portrait(detail.getAttribute("data-race")) : "";
    portrait.hidden = !portraitAddress;
    if (portraitAddress) {
      portrait.setAttribute("src", portraitAddress);
    } else {
      portrait.removeAttribute("src");
    }
    drawer.hidden = false;
    drawerShade.hidden = false;
    window.setTimeout(function () { drawer.classList.add("open"); }, 0);
    drawer.querySelector(".drawer-close").focus();
    window.setTimeout(function () { paintDrawerStats(name, number, revision); }, 0);
  }

  document.addEventListener("click", function (event) {
    var reference = event.target.closest("a[data-reference]");
    var link = bridge();
    if (reference && link && link.openReference) {
      event.preventDefault();
      link.openReference(reference.href);
      return;
    }
    var inspect = event.target.closest(".inspect-creature");
    if (inspect) {
      openDetails(inspect.getAttribute("data-creature"), inspect);
    }
  });
  drawer.querySelector(".drawer-close").addEventListener("click", closeDetails);
  drawerShade.addEventListener("click", closeDetails);
  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape" && !drawer.hidden) {
      closeDetails();
    } else if (event.key === "Tab" && !drawer.hidden) {
      var focusable = drawer.querySelectorAll("button, a[href]");
      var first = focusable[0];
      var last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  });
  if (targetArmorInput) {
    targetArmorInput.addEventListener("change", function () {
      if (!drawer.hidden) {
        paintDrawerStats(drawer.getAttribute("data-creature"),
          whole(drawer.getAttribute("data-number"), 0), drawerRevision);
      }
    });
  }

  // The hit point line is drawn for a known maximum; a card cloned or rendered
  // before PCGen built its creature counts damage only until then.
  function ensureHitPointBar(card) {
    var readout = card.querySelector(".hp-readout");
    if (card.querySelector(".hp-current") || !readout) {
      return;
    }
    while (readout.firstChild) {
      readout.removeChild(readout.firstChild);
    }
    var current = document.createElement("span");
    current.className = "hp-current";
    readout.appendChild(current);
    var maximum = document.createElement("span");
    maximum.className = "hp-maximum";
    readout.appendChild(document.createTextNode(" / "));
    readout.appendChild(maximum);
    var bar = document.createElement("span");
    bar.className = "bar";
    bar.appendChild(document.createElement("span"));
    readout.parentNode.appendChild(bar);
  }

  function chipFor(bonus, own) {
    var chip = document.createElement("span");
    chip.className = own ? "chip own" : "chip";
    chip.setAttribute("data-bonus", bonus.key);
    chip.appendChild(document.createTextNode(bonus.label + " "));
    var remove = document.createElement("button");
    remove.type = "button";
    remove.className = "chip-remove";
    remove.setAttribute("data-action", own ? "remove-bonus" : "remove-group-bonus");
    remove.textContent = "\u00d7";
    chip.appendChild(remove);
    return chip;
  }

  function paintChips(holder, bonuses, own) {
    if (!holder) {
      return;
    }
    while (holder.firstChild) {
      holder.removeChild(holder.firstChild);
    }
    (bonuses || []).forEach(function (bonus) {
      holder.appendChild(chipFor(bonus, own));
    });
  }

  function paintView(view) {
    var card = view ? cardOf(view.name, view.number) : null;
    if (!card) {
      return;
    }
    ensureHitPointBar(card);
    paintStats(card, view);
    var maximum = card.querySelector(".hp-maximum");
    if (maximum) {
      maximum.textContent = String(view.stats.maximumHitPoints);
    }
    paintChips(card.querySelector(".own-bonuses .chips"), view.bonuses, true);
  }

  function paintViews(json) {
    var views = parsed(json);
    if (Array.isArray(views)) {
      views.forEach(paintView);
    }
  }

  function refreshCard(card) {
    if (card.getAttribute("data-authored") === "true") {
      return;
    }
    var link = bridge();
    if (link && link.view) {
      paintView(parsed(link.view(nameOf(card), numberOf(card))));
      choicesFor(card.querySelector(".bonus-select"));
    }
  }

  // Building a creature takes seconds: let the page draw before asking.
  function refreshCardsLater(list) {
    var queue = Array.prototype.slice.call(list);
    function next() {
      if (queue.length) {
        refreshCard(queue.shift());
        window.setTimeout(next, 0);
      }
    }
    window.setTimeout(next, 0);
  }

  // Ask PCGen for the bonuses available to this creature or to the group.
  function choicesFor(select) {
    if (select.choices) {
      return select.choices;
    }
    var link = bridge();
    var card = select.closest(".card");
    var json = null;
    if (link && card && link.bonusChoices) {
      json = link.bonusChoices(nameOf(card), numberOf(card));
    } else if (link && !card && link.groupBonusChoices) {
      json = link.groupBonusChoices();
    }
    if (json === null) {
      return [];
    }
    var choices = parsed(json) || [];
    choices.forEach(function (choice) {
      var option = document.createElement("option");
      option.value = choice.key;
      option.textContent = choice.label;
      select.appendChild(option);
    });
    select.choices = choices;
    return choices;
  }

  function chosen(select) {
    var found = null;
    (select.choices || []).forEach(function (choice) {
      if (!found && choice.key === select.value) {
        found = choice;
      }
    });
    return found;
  }

  function lay(select, choice, value) {
    var link = bridge();
    var card = select.closest(".card");
    if (!link) {
      return;
    }
    if (card && link.addBonus) {
      paintViews(link.addBonus(nameOf(card), numberOf(card), choice.key, value));
    } else if (!card && link.addGroupBonus) {
      paintViews(link.addGroupBonus(choice.key, value));
      paintChips(document.querySelector(".group-bonuses .chips"), parsed(link.groupBonuses()), false);
    }
    select.value = "";
    var valueInput = select.parentNode.querySelector(".bonus-value");
    valueInput.hidden = true;
    valueInput.value = "";
    select.parentNode.querySelector(".bonus-apply").hidden = true;
  }

  document.addEventListener("focusin", function (event) {
    if (event.target.classList && event.target.classList.contains("bonus-select")) {
      choicesFor(event.target);
    }
  });

  document.addEventListener("change", function (event) {
    var target = event.target;
    if (!target.classList) {
      return;
    }
    if (target.classList.contains("bonus-select")) {
      var choice = chosen(target);
      if (!choice) {
        target.parentNode.querySelector(".bonus-value").hidden = true;
        target.parentNode.querySelector(".bonus-apply").hidden = true;
        return;
      }
      var valueInput = target.parentNode.querySelector(".bonus-value");
      var apply = target.parentNode.querySelector(".bonus-apply");
      if (choice.range) {
        valueInput.min = String(choice.range.minimum);
        valueInput.max = String(choice.range.maximum);
        valueInput.value = String(choice.range.minimum);
        valueInput.title = choice.range.title;
        valueInput.hidden = false;
        apply.hidden = false;
        valueInput.focus();
      } else {
        apply.hidden = true;
        lay(target, choice, -1);
      }
    }
  });

  document.addEventListener("click", function (event) {
    if (!event.target.classList || !event.target.classList.contains("bonus-apply")) {
      return;
    }
    var select = event.target.parentNode.querySelector(".bonus-select");
    var choice = chosen(select);
    var valueInput = event.target.parentNode.querySelector(".bonus-value");
    if (choice && choice.range && valueInput.value !== "" && valueInput.checkValidity()) {
      lay(select, choice, Number(valueInput.value));
    } else {
      valueInput.focus();
    }
  });

  document.querySelector(".group-bonuses").addEventListener("click", function (event) {
    var button = event.target.closest("[data-action=remove-group-bonus]");
    var link = bridge();
    if (!button || !link || !link.removeGroupBonus) {
      return;
    }
    var chip = button.closest(".chip");
    paintViews(link.removeGroupBonus(chip.getAttribute("data-bonus")));
    chip.parentNode.removeChild(chip);
  });

  // The bar between the two halves moves the boundary between them.
  var catalog = document.querySelector(".catalog");
  var divider = document.querySelector(".divider");
  var dragging = false;
  divider.addEventListener("mousedown", function (event) {
    dragging = true;
    event.preventDefault();
  });
  document.addEventListener("mousemove", function (event) {
    if (!dragging) {
      return;
    }
    var top = catalog.getBoundingClientRect().top;
    var height = Math.max(60, Math.min(event.clientY - top, window.innerHeight - top - 80));
    catalog.style.flexBasis = height + "px";
  });
  document.addEventListener("mouseup", function () {
    dragging = false;
  });

  each(cards.querySelectorAll(".card"), paint);
  refreshCount();
  function startStats(attempt) {
    if (bridge() && bridge().catalogStats) {
      choicesFor(document.querySelector(".group-bonuses .bonus-select"));
      refreshCatalogLater();
      refreshCardsLater(cards.querySelectorAll(".card"));
    } else if (attempt < 30) {
      window.setTimeout(function () { startStats(attempt + 1); }, 100);
    }
  }
  startStats(0);
}());
</script>
