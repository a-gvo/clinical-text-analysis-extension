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

import org.xwiki.component.annotation.Role;

/**
 * Loads the hpo into a scigraph.
 *
 * @version $Id$
 */
@Role
public interface SciGraphLoader
{
    /**
     * Load the HPO into a scigraph.
     * @throws LoadException if the load goes wrong
     */
    void load() throws LoadException;

    /**
     * Return whether the load has happened.
     * Really just checks if the directory where the database is supposed to be exists.
     * @return boolean whether the neo4j database is loaded.
     */
    boolean isLoaded();

    /**
     * Exception thrown when an owl load goes wrong.
     *
     * @version $Id$
     */
    class LoadException extends Exception
    {
        private static final long serialVersionUID = 738151762;

        /**
         * Constructor.
         * @param message the message.
         */
        public LoadException(String message) {
            super(message);
        }

        /**
         * Constructor with cause.
         * @param message the exception message
         * @param cause the cause.
         */
        public LoadException(String message, Exception cause)
        {
            super(message, cause);
        }
    }
}
