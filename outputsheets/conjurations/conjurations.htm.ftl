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
<#--
  One creature in play. The page renders the creatures already in play with it,
  and keeps one copy per catalog creature in a <template> for the script to
  clone when a new one arrives.
-->
<#macro card name number damage counted rounds found summon missing>
<#local maximum = (found && summon.hasMax)?then(summon.hpMax, 0) />
<article class="card<#if counted && rounds == 0> gone</#if><#if maximum gt 0 && damage gte maximum> down</#if>" data-creature="${name}" data-number="${number?c}" data-damage="${damage?c}" data-max="${maximum?c}" data-counted="${counted?c}" data-rounds="${rounds?c}">
  <header>
    <span class="card-name">${name} <span class="card-number">${number?c}</span></span>
    <span class="rounds"<#if !counted> hidden</#if>>
      <button type="button" data-action="rounds-down">&minus;</button>
      <span class="rounds-left">${rounds?c}</span> ${labels.rounds}
      <button type="button" data-action="rounds-up">+</button>
      <span class="gone-tag">${labels.gone}</span>
    </span>
    <button type="button" class="dismiss" data-action="dismiss">${labels.dismiss}</button>
  </header>
  <div class="hp">
    <#if maximum gt 0>
    <span class="hp-readout"><span class="hp-current">${(maximum - damage)?c}</span> / ${maximum?c}</span>
    <span class="bar"><span></span></span>
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
  <dl class="stats">
  <#list conjurations.cardFields as field>
    <#if summon.fields[field.key]?has_content><dt>${field.label}</dt><dd><@text summon.fields[field.key]/></dd></#if>
  </#list>
  </dl>
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

table{border-collapse:collapse;width:100%;font-size:12px;table-layout:fixed}
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
tr.details td{color:var(--text-dim);font-size:11px;padding-bottom:4px}
.detail{margin-right:12px}
.detail b{color:var(--def);font-weight:600}
.tag{color:var(--buff);font-size:10px;text-transform:uppercase;letter-spacing:.06em;margin-right:6px}
.conjure{color:var(--grn);white-space:nowrap}

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

@media print{
  .page{display:block;height:auto}
  .catalog,.combat{overflow:visible}
  .divider,button,input,.filters,.duration{display:none}
}
</style>

<div class="page" data-every-role="${labels.everyRole}">
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
  <div class="filters" data-roles="${conjurations.roles?join(",")}"></div>
  <#list conjurations.groups as group>
  <div class="group">
    <h2>${group.title}</h2>
    <table>
      <colgroup><col class="c-conjure" /><col class="c-name" /><#list conjurations.columns as column><col class="c-${column.key}" /></#list></colgroup>
      <thead>
        <tr><th></th><th>${labels.creature}</th><#list conjurations.columns as column><th>${column.label}</th></#list></tr>
      </thead>
      <#list group.summons as summon>
      <#assign detailed = summon.roles?has_content />
      <#list conjurations.details as detail><#if summon.fields[detail.key]?has_content><#assign detailed = true /></#if></#list>
      <tbody class="summon" data-row-roles="${summon.roles?join(",")?lower_case}">
        <tr>
          <td><button type="button" class="conjure" data-creature="${summon.name}">${labels.conjure}</button></td>
          <td class="name">${summon.name}</td>
          <#list conjurations.columns as column><td class="${column.key}"><@text summon.fields[column.key]/></td></#list>
        </tr>
        <#if detailed>
        <tr class="details">
          <td></td>
          <td colspan="${(conjurations.columns?size + 1)?c}">
            <#list summon.roles as role><span class="tag">${role}</span></#list>
            <#list conjurations.details as detail><#if summon.fields[detail.key]?has_content><span class="detail"><b>${detail.label}</b> <@text summon.fields[detail.key]/></span></#if></#list>
          </td>
        </tr>
        </#if>
      </tbody>
      </#list>
    </table>
  </div>
  </#list>
</#if>
</section>

<div class="divider"></div>

<section class="combat">
  <div class="combat-head">
    <h2>${labels.inPlay} <span class="count"></span></h2>
    <button type="button" data-action="next-round">${labels.nextRound}</button>
    <button type="button" data-action="dismiss-all">${labels.dismissAll}</button>
    <label class="duration">${labels.duration} <input type="number" min="0" value="${conjurations.duration?c}" /></label>
  </div>
  <p class="nobody"<#if conjurations.conjured?size gt 0> hidden</#if>>${labels.nobody}</p>
  <div class="cards">
  <#list conjurations.conjured as conjured>
    <@card name=conjured.name number=conjured.number damage=conjured.damage counted=conjured.counted rounds=conjured.rounds found=conjured.found summon=conjured.summon missing=conjured.missing/>
  </#list>
  </div>
  <#list conjurations.summons as summon>
  <template data-creature="${summon.name}"><@card name=summon.name number=1 damage=0 counted=false rounds=0 found=true summon=summon missing=""/></template>
  </#list>
</section>
</div>

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

  // The filter bar is built from the roles the catalog declares; a role hides
  // every creature that does not play it, and every group left empty.
  each(document.querySelectorAll(".filters"), function (bar) {
    var roles = (bar.getAttribute("data-roles") || "").split(",").filter(function (role) {
      return role !== "";
    });
    if (!roles.length) {
      return;
    }
    var show = function (wanted) {
      each(document.querySelectorAll("tbody.summon"), function (summon) {
        var played = (summon.getAttribute("data-row-roles") || "").split(",");
        summon.hidden = wanted !== "" && played.indexOf(wanted) < 0;
      });
      each(document.querySelectorAll(".group"), function (group) {
        group.hidden = !group.querySelector("tbody.summon:not([hidden])");
      });
    };
    [""].concat(roles).forEach(function (role) {
      var button = document.createElement("button");
      button.type = "button";
      button.className = "filter" + (role === "" ? " on" : "");
      button.textContent = role === "" ? (page.getAttribute("data-every-role") || "All") : role;
      button.addEventListener("click", function () {
        each(bar.querySelectorAll(".filter"), function (other) {
          other.classList.remove("on");
        });
        button.classList.add("on");
        show(role.toLowerCase());
      });
      bar.appendChild(button);
    });
  });

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

  function conjure(name) {
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
    paint(card);
    refreshCount();
    if (card.scrollIntoView) {
      card.scrollIntoView(false);
    }
  }

  each(document.querySelectorAll(".catalog .conjure"), function (button) {
    button.addEventListener("click", function () {
      conjure(button.getAttribute("data-creature"));
    });
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
}());
</script>
