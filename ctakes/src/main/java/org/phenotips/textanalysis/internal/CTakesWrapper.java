/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.textanalysis.internal;

import org.phenotips.vocabulary.VocabularyTerm;
import org.xwiki.component.annotation.Role;

import java.util.List;

import org.apache.ctakes.typesystem.type.textsem.EntityMention;


@Role
public interface CTakesWrapper
{
    /**
     * Return a list of uima annotations.
     * @param text the text to annotate
     */
    public List<EntityMention> annotate(String text) throws CTakesException;

    /**
     * Wrapper class for any Exception thrown by CTakes.
     */
    public static class CTakesException extends Exception
    {
        /**
         * CTOR.
         * @param message the message
         * @param cause the exception that caused this one.
         */
        public CTakesException(String message, Exception cause)
        {
            super(message, cause);
        }
    }
}
