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

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.phenotips.textanalysis.TermAnnotationService;

/**
 * Wraps scigraph initialization and calls, offering a single interface to annotate text.
 *
 * @version $Id$
 */
@Role
public interface SciGraphWrapper
{

    /**
     * Annotates text as per the EntityFormatConfiguration given.
     * @param string the string to annotate
     * @return List of annotations
     * @throws AnnotationException if something goes wrong
     */
    List<SciGraphAnnotation> annotate(String text) throws TermAnnotationService.AnnotationException;
    

    /**
     * A scigraph annotation as returned by scigraph.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SciGraphAnnotation
    {
        /**
         * The token corresponding to the annotation; in phenotype terms, this is the term.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static final class Token
        {
            /**
             * The token's id.
             */
            private String id;
            
            /**
             * Return the id.
             */
            public String getId()
            {
                return id;
            }

            /**
             * Set the id.
             */
            public void setId(String id)
            {
                this.id = id;
            }
        }

        /**
         * The token for this annotation.
         */
        private Token token;

        /**
         * The end of the annotation.
         */
        private int end;

        /**
         * The start of the annotation.
         */
        private int start;

        /**
         * Get the token.
         */
        public Token getToken()
        {
            return token;
        }

        /**
         * Set the token.
         */
        public void setToken(Token token)
        {
            this.token = token;
        }

        /**
         * Get the end of the annotation.
         */
        public int getEnd()
        {
            return end;
        }

        /**
         * Set the end of the annotation.
         */
        public void setEnd(int end)
        {
            this.end = end;
        }

        /**
         * Get the start of the annotation.
         */
        public int getStart()
        {
            return start;
        }
        
        /**
         * Set the start of the annotation.
         */
        public void setStart(int start)
        {
            this.start = start;
        }
    }
}
