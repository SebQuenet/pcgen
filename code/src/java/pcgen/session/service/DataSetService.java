/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.session.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pcgen.cdom.base.Identified;
import pcgen.facade.core.DataSetFacade;
import pcgen.facade.core.EquipmentFacade;
import pcgen.session.PcgenSession;
import pcgen.session.service.model.NamedEntry;

/**
 * What the loaded data holds, so that a caller can offer a choice instead of
 * asking someone to type a key they have no way of knowing.
 *
 * <p>
 * Equipment runs to thousands of entries, so every listing takes a filter and a
 * ceiling rather than handing back everything and hoping.
 */
public final class DataSetService
{
	private static final int DEFAULT_LIMIT = 200;

	private final PcgenSession session;

	public DataSetService(PcgenSession session)
	{
		this.session = session;
	}

	/**
	 * The entries of one kind the loaded data holds.
	 *
	 * @param kind race, class, skill, deity, alignment, stat, equipment, template or kit
	 * @param nameContains keeps only entries whose key or name contains this, when given
	 * @param limit how many to return at most
	 */
	public ServiceResult<List<NamedEntry>> listEntries(String kind, String nameContains, int limit)
	{
		DataSetFacade dataSet = session.getCurrentDataSet();
		if (dataSet == null)
		{
			return ServiceResult.failure(new ServiceError.NoSourcesLoaded());
		}
		return entriesOfKind(dataSet, kind).map(entries -> matching(entries, nameContains, limit));
	}

	private static ServiceResult<List<NamedEntry>> entriesOfKind(DataSetFacade dataSet, String kind)
	{
		return switch (kind.toLowerCase(Locale.ROOT))
		{
			case "race" -> ServiceResult.success(named(dataSet.getRaces()));
			case "class" -> ServiceResult.success(named(dataSet.getClasses()));
			case "skill" -> ServiceResult.success(named(dataSet.getSkills()));
			case "deity" -> ServiceResult.success(named(dataSet.getDeities()));
			case "alignment" -> ServiceResult.success(named(dataSet.getAlignments()));
			case "stat" -> ServiceResult.success(named(dataSet.getStats()));
			case "template" -> ServiceResult.success(named(dataSet.getTemplates()));
			case "kit" -> ServiceResult.success(named(dataSet.getKits()));
			case "equipment" -> ServiceResult.success(equipment(dataSet));
			default -> ServiceResult.failure(new ServiceError.InvalidArgument("kind",
				"unknown kind '" + kind + "'. Use: race, class, skill, deity, alignment, stat, "
					+ "equipment, template, kit"));
		};
	}

	private static List<NamedEntry> matching(List<NamedEntry> entries, String nameContains, int limit)
	{
		int ceiling = limit > 0 ? limit : DEFAULT_LIMIT;
		String wanted = nameContains == null ? null : nameContains.toLowerCase(Locale.ROOT);
		List<NamedEntry> kept = new ArrayList<>();
		for (NamedEntry entry : entries)
		{
			if (wanted != null && !containsIgnoringCase(entry, wanted))
			{
				continue;
			}
			kept.add(entry);
			if (kept.size() == ceiling)
			{
				break;
			}
		}
		return List.copyOf(kept);
	}

	private static boolean containsIgnoringCase(NamedEntry entry, String wanted)
	{
		return entry.key().toLowerCase(Locale.ROOT).contains(wanted)
			|| entry.name().toLowerCase(Locale.ROOT).contains(wanted);
	}

	private static List<NamedEntry> named(Iterable<? extends Identified> entries)
	{
		List<NamedEntry> named = new ArrayList<>();
		for (Identified entry : entries)
		{
			named.add(new NamedEntry(entry.getKeyName(), entry.getDisplayName()));
		}
		return named;
	}

	/** Equipment is a facade rather than an Identified, and reads its name from toString. */
	private static List<NamedEntry> equipment(DataSetFacade dataSet)
	{
		List<NamedEntry> named = new ArrayList<>();
		for (EquipmentFacade item : dataSet.getEquipment())
		{
			named.add(new NamedEntry(item.getKeyName(), item.toString()));
		}
		return named;
	}
}
