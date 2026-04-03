package pcgen.mcp;

import java.util.List;

public record PendingChoice(
	String choiceId,
	String title,
	List<String> availableOptions,
	int remainingSelections,
	boolean requireCompleteSelection
)
{
}
