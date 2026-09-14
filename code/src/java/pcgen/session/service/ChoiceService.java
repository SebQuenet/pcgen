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
import java.util.Map;

import pcgen.session.PcgenSession;
import pcgen.session.PendingChoice;
import pcgen.session.service.model.ChoiceResolved;
import pcgen.session.service.model.PendingChoiceSummary;

/**
 * The questions PCGen stops to ask, and the answers sent back to it.
 *
 * <p>
 * These two calls are the only ones that may run while another is in flight. The
 * call that opened a choice is blocked waiting on the answer, so if answering had
 * to queue behind it, neither would ever finish.
 */
public final class ChoiceService
{
	private final PcgenSession session;

	public ChoiceService(PcgenSession session)
	{
		this.session = session;
	}

	public ServiceResult<List<PendingChoiceSummary>> getPendingChoices()
	{
		List<PendingChoiceSummary> waiting = new ArrayList<>();
		for (Map.Entry<String, PendingChoice> pending : session.getAllPendingChoices().entrySet())
		{
			PendingChoice choice = pending.getValue();
			waiting.add(new PendingChoiceSummary(choice.choiceId(), choice.title(),
				List.copyOf(choice.availableOptions()), choice.remainingSelections(),
				choice.requireCompleteSelection()));
		}
		return ServiceResult.success(List.copyOf(waiting));
	}

	public ServiceResult<ChoiceResolved> resolveChoice(String choiceId, List<String> selections)
	{
		return session.resolveChoice(choiceId, selections)
			? ServiceResult.success(new ChoiceResolved(choiceId))
			: ServiceResult.failure(new ServiceError.EntryNotFound("Choice", choiceId));
	}
}
