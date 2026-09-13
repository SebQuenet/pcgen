<#ftl output_format="HTML">
<#--
  The tactical sheet.

  output_format="HTML" above makes FreeMarker escape every interpolation, so
  text written by an AI agent cannot inject markup or script into a page that
  holds a bridge to Java. Do not add ?no_esc to an authored field.

  The plan comes from the `tactics` data model (pcgen.io.tactics.TacticalOutputModel);
  everything the character already knows comes from pcstring and pcvar, as in
  every other output sheet.
-->
<#assign hitPoints = pcstring('HP')?trim />
<style>
:root{
  --bg:#0b0d0c; --bg2:#121614; --bg3:#1b211d; --border:#2b3a30;
  --text:#dce4dc; --text-dim:#7a8a7c;
  --gold:#c9a03a; --gold-dim:#6e5820; --gold-bright:#f0d47a;
  --atk:#d98030; --dmg:#cc4a40; --def:#5a9abf; --def-dim:#1a3a50;
  --myth:#b070d8; --myth-dim:#3a1e50; --grn:#4aaa6a; --grn-dim:#1a4028;
  --holy:#e8dc9a; --buff:#4ac0b0; --warn:#d0a860; --iron:#6a7a88;
}
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Source Sans 3','Segoe UI',system-ui,sans-serif;background:var(--bg);color:var(--text);
  font-size:13px;line-height:1.45}
.sheet{max-width:1020px;margin:0 auto;padding:12px}
h1,h2,.pip-label,.block-head{font-family:Cinzel,'Palatino Linotype',Georgia,serif}

header{display:flex;flex-wrap:wrap;align-items:baseline;gap:6px 14px;
  padding-bottom:10px;margin-bottom:10px;border-bottom:2px solid var(--gold-dim)}
h1{font-size:22px;letter-spacing:.06em;color:var(--gold-bright);font-weight:900}
.subtitle{color:var(--text-dim)}

.vitals{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:12px}
.vital{background:var(--bg2);border:1px solid var(--border);border-radius:3px;padding:5px 9px;min-width:78px}
.vital .name{display:block;font-size:10px;letter-spacing:.08em;text-transform:uppercase;color:var(--text-dim)}
.vital .value{display:block;font-size:16px;font-weight:600}
.vital.ac .value{color:var(--def)}
.vital.hp .value{color:var(--grn)}
.vital.atk .value{color:var(--atk)}

.hp-track{display:flex;align-items:center;gap:8px;background:var(--bg2);border:1px solid var(--border);
  border-radius:3px;padding:6px 9px;margin-bottom:12px}
.hp-track .bar{flex:1;height:9px;background:var(--bg3);border-radius:5px;overflow:hidden}
.hp-track .bar span{display:block;height:100%;background:var(--grn);transition:width .15s}
.hp-track button{font:inherit;background:var(--bg3);color:var(--text);border:1px solid var(--border);
  border-radius:3px;padding:2px 7px;cursor:pointer}
.hp-track button:hover{border-color:var(--gold)}
.hp-track .readout{font-variant-numeric:tabular-nums;min-width:78px;text-align:center}

section{background:var(--bg2);border:1px solid var(--border);border-radius:4px;margin-bottom:10px}
section > h2{font-size:13px;letter-spacing:.1em;text-transform:uppercase;color:var(--gold);
  padding:8px 10px;cursor:pointer;display:flex;justify-content:space-between;gap:8px;
  border-bottom:1px solid var(--border)}
section > h2 .chevron{color:var(--text-dim);font-size:11px}
section.closed > h2{border-bottom:none}
section.closed > .body{display:none}
.body{padding:8px 10px;display:flex;flex-direction:column;gap:8px}

.block{border-left:2px solid var(--border);padding-left:8px}
.block-head{font-size:12px;letter-spacing:.05em;color:var(--gold-bright)}
.block.step{border-left-color:var(--iron)}
.block.note{border-left-color:var(--warn)}
.block.resource{border-left-color:var(--myth)}
.block.attack{border-left-color:var(--atk)}
.block.attack + .block.attack{border-top:none;margin-top:-6px}
.block.attack table{table-layout:fixed}
.pips{gap:3px 10px}
.action-cost{margin-left:auto}
.block.creature{border-left-color:var(--grn)}
.block.spells{border-left-color:var(--def)}

.step .trigger{color:var(--warn)}
.step .actions{display:block}
.step .note,.attack .note{color:var(--text-dim);font-size:12px}
.note p{margin-bottom:3px}

.pips{display:flex;flex-wrap:wrap;gap:3px;align-items:center}
.pip{width:13px;height:13px;border:1px solid var(--myth);border-radius:2px;cursor:pointer;background:transparent}
.pip.spent{background:var(--myth)}
.pip-label{color:var(--myth);font-size:12px;letter-spacing:.05em}
.pip-count{color:var(--text-dim);font-variant-numeric:tabular-nums}
.action-cost{color:var(--text-dim);font-size:11px;text-transform:uppercase;letter-spacing:.06em}

table{border-collapse:collapse;width:100%;font-size:12px}
th{text-align:left;font-weight:600;color:var(--text-dim);font-size:10px;letter-spacing:.08em;
  text-transform:uppercase;padding:2px 6px 2px 0}
td{padding:2px 6px 2px 0;vertical-align:top}
.attack .hit{color:var(--atk);font-weight:600;font-variant-numeric:tabular-nums}
.attack .dmg{color:var(--dmg);font-variant-numeric:tabular-nums}

.targets{display:flex;flex-wrap:wrap;gap:4px;margin:3px 0}
.target-tab{font:inherit;font-size:11px;background:var(--bg3);color:var(--text-dim);
  border:1px solid var(--border);border-radius:3px;padding:1px 7px;cursor:pointer}
.target-tab.on{color:var(--holy);border-color:var(--gold-dim);background:var(--bg)}
.target-effect{color:var(--holy);font-size:12px}

.rows{display:grid;grid-template-columns:max-content 1fr;gap:2px 8px;font-size:12px}
.rows dt{color:var(--def);font-weight:600}

.filters{display:flex;flex-wrap:wrap;gap:4px;margin-bottom:5px}
.spell{display:flex;flex-wrap:wrap;gap:6px;align-items:baseline;font-size:12px}
.spell .level{color:var(--def);font-variant-numeric:tabular-nums}
.spell .tag{color:var(--buff);font-size:10px;text-transform:uppercase;letter-spacing:.06em}
.spell.hidden{display:none}

.unresolved{color:var(--dmg);font-size:11px}
.empty,.errors{color:var(--text-dim);padding:14px 0}
.errors li{color:var(--dmg);margin-left:18px}

@media (max-width:520px){
  .vitals{gap:4px}
  .vital{min-width:64px}
  .rows{grid-template-columns:1fr}
}
</style>

<div class="sheet" data-every-tag="${tactics.labels.everyTag}">
<header>
  <h1>${pcstring('NAME')}</h1>
  <span class="subtitle">${pcstring('CLASSLIST')} &middot; ${pcstring('RACE')} &middot; ${pcstring('ALIGNMENT.SHORT')}</span>
</header>

<div class="vitals">
  <div class="vital hp"><span class="name">HP</span><span class="value">${hitPoints}</span></div>
  <div class="vital ac"><span class="name">AC</span><span class="value">${pcstring('AC.Total')}</span></div>
  <div class="vital"><span class="name">Touch</span><span class="value">${pcstring('AC.Touch')}</span></div>
  <div class="vital"><span class="name">Flat</span><span class="value">${pcstring('AC.Flatfooted')}</span></div>
  <div class="vital"><span class="name">Init</span><span class="value">${pcstring('INITIATIVEMOD')}</span></div>
  <div class="vital atk"><span class="name">Melee</span><span class="value">${pcstring('ATTACK.MELEE.TOTAL')}</span></div>
  <div class="vital atk"><span class="name">Ranged</span><span class="value">${pcstring('ATTACK.RANGED.TOTAL')}</span></div>
<@loop from=0 to=pcvar('COUNT[CHECKS]-1') ; check , check_has_next>
  <div class="vital"><span class="name">${pcstring('CHECK.${check}.NAME')}</span><span class="value">${pcstring('CHECK.${check}.TOTAL')}</span></div>
</@loop>
<@loop from=0 to=pcvar('COUNT[MOVE]-1') ; movement , movement_has_next>
  <div class="vital"><span class="name">${pcstring('MOVE.${movement}.NAME')}</span><span class="value">${pcstring('MOVE.${movement}.RATE')}</span></div>
</@loop>
</div>

<div class="hp-track" data-max="${hitPoints}">
  <span class="pip-label">${tactics.labels.hitPoints}</span>
  <button type="button" data-heal="10">&minus;10</button>
  <button type="button" data-heal="1">&minus;1</button>
  <span class="readout"><span class="current">${hitPoints}</span> / ${hitPoints}</span>
  <button type="button" data-harm="1">+1</button>
  <button type="button" data-harm="10">+10</button>
  <div class="bar"><span style="width:100%"></span></div>
</div>

<#if !tactics.present>
  <p class="empty">${tactics.labels.empty}</p>
<#elseif tactics.errors?size gt 0>
  <div class="errors">
    <p>${tactics.labels.doesNotRead}</p>
    <ul>
    <#list tactics.errors as error>
      <li>${error.text}</li>
    </#list>
    </ul>
  </div>
<#else>
<#list tactics.sections as section>
  <section>
    <h2>${section.title}<span class="chevron">&#9660;</span></h2>
    <div class="body">
    <#list section.blocks as block>
      <#if block.kind == 'step'>
        <div class="block step">
          <div class="block-head trigger">${block.trigger}</div>
          <span class="actions">${block.actions}</span>
          <#if block.note?has_content><span class="note">${block.note}</span></#if>
        </div>
      <#elseif block.kind == 'note'>
        <div class="block note">
          <div class="block-head">${block.title}</div>
          <#list block.prose as line><p>${line}</p></#list>
        </div>
      <#elseif block.kind == 'resource'>
        <div class="block resource">
          <div class="pips" data-resource="${block.label}" data-spent="${block.spent}">
            <span class="pip-label">${block.label}</span>
            <#if block.pips gt 0>
              <#list 1..block.pips as pip>
                <button type="button" class="pip<#if pip lte block.spent> spent</#if>" data-pip="${pip}"></button>
              </#list>
              <#if block.pips gt 1><span class="pip-count">${block.spent} / ${block.pips}</span></#if>
            <#elseif block.unresolved>
              <span class="unresolved">${tactics.labels.noVariable?replace("{0}", "'" + block.missing + "'")}</span>
            <#else>
              <span class="pip-count">${block.maximum}</span>
            </#if>
            <#if block.action?has_content><span class="action-cost">${block.action}</span></#if>
          </div>
        </div>
      <#elseif block.kind == 'attack'>
        <#assign opensTable = (block?index == 0) || (section.blocks[block?index - 1].kind != 'attack') />
        <div class="block attack">
          <table>
            <#if opensTable>
            <tr><th>${tactics.labels.attack}</th><th>${tactics.labels.hit}</th><th>${tactics.labels.damage}</th><th>${tactics.labels.critical}</th></tr>
            </#if>
            <tr>
              <td class="block-head">${block.name}</td>
              <td class="hit">${block.toHit}</td>
              <td class="dmg">${block.damage}</td>
              <td>${block.critical}</td>
            </tr>
          </table>
          <#if block.unresolved>
            <span class="unresolved">${tactics.labels.noWeapon?replace("{0}", "'" + block.missing + "'")}</span>
          </#if>
          <#if block.variants?size gt 0>
            <div class="targets">
              <button type="button" class="target-tab on" data-effect="">${tactics.labels.normalTarget}</button>
              <#list block.variants as variant>
                <button type="button" class="target-tab" data-effect="${variant.effect}">${variant.label}</button>
              </#list>
            </div>
            <span class="target-effect"></span>
          </#if>
          <#if block.note?has_content><span class="note">${block.note}</span></#if>
        </div>
      <#elseif block.kind == 'creature'>
        <div class="block creature">
          <div class="block-head">${block.name}<#if block.source?has_content> &middot; ${block.source}</#if><#if block.duration?has_content> &middot; ${block.duration}</#if></div>
          <dl class="rows">
          <#list block.rows as row>
            <dt>${row.label}</dt><dd>${row.content}</dd>
          </#list>
          </dl>
        </div>
      <#elseif block.kind == 'spells'>
        <div class="block spells">
          <div class="block-head"><#if block.source == 'prepared'>${tactics.labels.prepared}<#else>${tactics.labels.known}</#if></div>
          <#if block.spells?size gt 0>
            <div class="filters"></div>
            <#list block.spells as spell>
              <div class="spell" data-tags="<#list spell.tags as tag>${tag}<#sep>,</#sep></#list>">
                <span class="level">Lv ${spell.level}</span>
                <span class="name">${spell.name}</span>
                <#if spell.times gt 1><span class="pip-count">&times;${spell.times}</span></#if>
                <#list spell.tags as tag><span class="tag">${tag}</span></#list>
              </div>
            </#list>
          <#else>
            <span class="note">${tactics.labels.noSpells}</span>
          </#if>
        </div>
      </#if>
    </#list>
    </div>
  </section>
</#list>
</#if>
</div>

<script>
(function () {
  "use strict";

  var everyTag = (document.querySelector(".sheet") || document.body).getAttribute("data-every-tag") || "All";

  // Inside PCGen a bridge to Java is planted on the window once the document is
  // loaded; in a plain browser there is none, and the page keeps its state to
  // itself. Read lazily, so it is found whenever it arrives.
  function bridge() {
    return window.pcgen || null;
  }

  function on(selector, event, handler) {
    Array.prototype.forEach.call(document.querySelectorAll(selector), function (node) {
      node.addEventListener(event, handler);
    });
  }

  on("section > h2", "click", function (event) {
    event.currentTarget.parentNode.classList.toggle("closed");
  });

  var track = document.querySelector(".hp-track");
  if (track) {
    var maximum = parseInt(track.getAttribute("data-max"), 10) || 0;
    var damage = 0;
    var paint = function () {
      var current = Math.max(maximum - damage, 0);
      track.querySelector(".current").textContent = current;
      track.querySelector(".bar span").style.width =
        (maximum > 0 ? (current * 100 / maximum) : 0) + "%";
      var link = bridge();
      if (link && link.setDamage) {
        link.setDamage(damage);
      }
    };
    on(".hp-track button", "click", function (event) {
      var heal = parseInt(event.currentTarget.getAttribute("data-heal"), 10);
      var harm = parseInt(event.currentTarget.getAttribute("data-harm"), 10);
      damage = Math.min(Math.max(damage + (harm || 0) - (heal || 0), 0), maximum);
      paint();
    });
    paint();
  }

  Array.prototype.forEach.call(document.querySelectorAll(".pips"), function (row) {
    var label = row.getAttribute("data-resource");
    var pips = Array.prototype.slice.call(row.querySelectorAll(".pip"));
    var count = row.querySelector(".pip-count");
    var spend = function (spent) {
      pips.forEach(function (pip, index) {
        pip.classList.toggle("spent", index < spent);
      });
      if (count) {
        count.textContent = spent + " / " + pips.length;
      }
      row.setAttribute("data-spent", spent);
      var link = bridge();
      if (link && link.spend) {
        link.spend(label, spent);
      }
    };
    pips.forEach(function (pip, index) {
      pip.addEventListener("click", function () {
        var spent = parseInt(row.getAttribute("data-spent"), 10) || 0;
        spend(index + 1 === spent ? index : index + 1);
      });
    });
  });

  Array.prototype.forEach.call(document.querySelectorAll(".block.attack"), function (attack) {
    var readout = attack.querySelector(".target-effect");
    if (!readout) {
      return;
    }
    Array.prototype.forEach.call(attack.querySelectorAll(".target-tab"), function (tab) {
      tab.addEventListener("click", function () {
        Array.prototype.forEach.call(attack.querySelectorAll(".target-tab"), function (other) {
          other.classList.remove("on");
        });
        tab.classList.add("on");
        readout.textContent = tab.getAttribute("data-effect");
      });
    });
  });

  Array.prototype.forEach.call(document.querySelectorAll(".block.spells"), function (block) {
    var filters = block.querySelector(".filters");
    var spells = Array.prototype.slice.call(block.querySelectorAll(".spell"));
    if (!filters) {
      return;
    }
    var tags = [];
    spells.forEach(function (spell) {
      (spell.getAttribute("data-tags") || "").split(",").forEach(function (tag) {
        if (tag && tags.indexOf(tag) < 0) {
          tags.push(tag);
        }
      });
    });
    if (!tags.length) {
      return;
    }
    var show = function (wanted) {
      spells.forEach(function (spell) {
        var owned = (spell.getAttribute("data-tags") || "").split(",");
        spell.classList.toggle("hidden", wanted !== "" && owned.indexOf(wanted) < 0);
      });
    };
    ["", ].concat(tags).forEach(function (tag) {
      var button = document.createElement("button");
      button.type = "button";
      button.className = "target-tab" + (tag === "" ? " on" : "");
      button.textContent = tag === "" ? everyTag : tag;
      button.addEventListener("click", function () {
        Array.prototype.forEach.call(filters.querySelectorAll(".target-tab"), function (other) {
          other.classList.remove("on");
        });
        button.classList.add("on");
        show(tag);
      });
      filters.appendChild(button);
    });
  });
}());
</script>
