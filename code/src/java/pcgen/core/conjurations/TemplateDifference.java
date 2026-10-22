/*
 * Copyright 2026 (C) PCGen contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package pcgen.core.conjurations;

/**
 * One statistic a creature's templates change.
 *
 * @param stat             the statistic's key, as the page labels it ({@code ac}, {@code init}, ...,
 *                         or {@code abilities} for the special abilities they add)
 * @param withoutTemplates its value on the same creature without templates
 * @param withTemplates    its value with them
 */
public record TemplateDifference(String stat, String withoutTemplates, String withTemplates)
{
}
