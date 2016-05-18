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

import org.phenotips.textanalysis.TermAnnotationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Wrapper component for the SciGraph annotation service.
 *
 * @version $Id$
 */
@Component
@Singleton
public class SciGraphWrapperImpl implements SciGraphWrapper, Initializable
{
    /**
     * The object for API interaction with scigraph.
     */
    @Inject
    private SciGraphAPI api;

    /**
     * The object mapper to use for json parsing.
     */
    private ObjectMapper mapper;

    @Override
    public void initialize() throws InitializationException {
        mapper = new ObjectMapper();
    }

    @Override
    public List<SciGraphAnnotation> annotate(String text) throws TermAnnotationService.AnnotationException {
        try {
            Map<String, String> params = new HashMap<>(2);
            params.put("content", text);
            params.put("includeCat", "phenotype");
            InputStream is = api.postForm("annotations/entities", params);
            TypeReference reference = new TypeReference<List<SciGraphAnnotation>>() { };
            return mapper.readValue(is, reference);
        } catch (IOException | SciGraphAPI.SciGraphException e) {
            throw new TermAnnotationService.AnnotationException(e.getMessage(), e);
        }
    }
}
