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

import edu.sdsc.scigraph.annotation.EntityAnnotation;
import edu.sdsc.scigraph.annotation.EntityFormatConfiguration;

/**
 * Wraps scigraph initialization and calls, offering a single interface to annotate text.
 */
@Role
public interface SciGraphWrapper {

    /**
     * Annotates text as per the EntityFormatConfiguration given.
     * @param config the entity format configuration to follow
     * @return List of annotations
     */
    public List<EntityAnnotation> annotate(EntityFormatConfiguration config) throws IOException;
}
