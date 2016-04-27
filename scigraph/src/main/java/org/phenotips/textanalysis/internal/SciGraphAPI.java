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

import java.io.InputStream;
import java.util.Map;

/**
 * Interacts with the scigraph rest api.
 *
 * @version $Id$
 */
@Role
public interface SciGraphAPI
{
    /**
     * Execute a post request to the method given, taking content to be the json body of the request.
     * @param method the method
     * @param content the content, to be interpreted as containing a json object
     * @return the response
     * @throws SciGraphException if there's an error accessing the method
     */
    InputStream postJson(String method, InputStream content) throws SciGraphException;

    /**
     * Post the form given to the method given.
     * @param method the method
     * @param params the form parameters
     * @return the response
     * @throws SciGraphException if there's an error accessing the method
     */
    InputStream postForm(String method, Map<String, String> params) throws SciGraphException;

    /**
     * Send an empty post to the method given.
     * @param method the method
     * @return the response
     * @throws SciGraphException if there's an error accessing the method
     */
    InputStream postEmpty(String method) throws SciGraphException;

    /**
     * Send an empty get to the method given.
     * @param method the method
     * @return the response
     * @throws SciGraphException if there's an error accessing the method
     */
    InputStream getEmpty(String method) throws SciGraphException;

    /**
     * An exception returned by SciGraph.
     *
     * @version $Id$
     */
    class SciGraphException extends Exception
    {
        /**
         * CTOR.
         * @param message the message
         */
        public SciGraphException(String message)
        {
            super(message);
        }

        /**
         * CTOR with cause.
         * @param message the message
         * @param cause the cause.
         */
        public SciGraphException(String message, Exception cause)
        {
            super(message, cause);
        }
    }
}
