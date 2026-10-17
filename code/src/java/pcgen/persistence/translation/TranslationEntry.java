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
package pcgen.persistence.translation;

/**
 * One translated field of one game object.
 *
 * @param line     the one based line it was read from, so a problem can point at it
 * @param type     the object type, as LST files name it (ABILITY, SPELL, EQUIPMENT...)
 * @param category the ability category, empty for every other type
 * @param key      the object's key, which never changes with the language
 * @param field    the field translated
 * @param text     the translated text
 */
public record TranslationEntry(int line, String type, String category, String key, TranslatedField field, String text)
{
}
