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

import org.phenotips.textanalysis.TermAnnotation;
import org.phenotips.textanalysis.TermAnnotationService;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.io.StringReader;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import edu.sdsc.scigraph.annotation.EntityProcessor;
import edu.sdsc.scigraph.annotation.EntityAnnotation;
import edu.sdsc.scigraph.annotation.Entity;
import edu.sdsc.scigraph.annotation.EntityFormatConfiguration;

/**
 * Implementation of {@link TermAnnotationService} using SciGraph.
 */
@Component
@Named("scigraph")
@Singleton
public class SciGraphAnnotationService implements TermAnnotationService
{
    /**
     * The ontology used to look up phenotypes.
     */
    @Inject
    private VocabularyManager vocabularies;

    /**
     * The scigraph wrapper that will actually interact with the library.
     */
    @Inject
    private SciGraphWrapper wrapper;

    /**
     * The categories to which annotations must belong to.
     * In this case it's just "phenotype"
     */
    private static final Set<String> CATEGORIES = new HashSet<String>(Arrays.asList("phenotype"));

    @Override
    public List<TermAnnotation> annotate(String text) throws AnnotationException
    {
        List<EntityAnnotation> entities = sciGraphAnnotate(text);
        List<TermAnnotation> annotations = new LinkedList<>();
        for(EntityAnnotation ea : entities) {
            Entity entity = ea.getToken();
            String termId = entity.getId().replace("hpo:", "");
            VocabularyTerm term = this.vocabularies.resolveTerm(termId);
            if(term != null) {
                long start = ea.getStart();
                long end = ea.getEnd();
                annotations.add(new TermAnnotation(start, end, term));
            }
        }
        return annotations;
    }

    /**
     * Get a list of scigraph EntityAnnotations for the given text.
     */
    private List<EntityAnnotation> sciGraphAnnotate(String text) throws AnnotationException
    {
        List<EntityAnnotation> entities;
        try {
            StringReader reader = new StringReader(text);
            EntityFormatConfiguration.Builder builder = new EntityFormatConfiguration.Builder(reader);
            builder.includeCategories(CATEGORIES);
            builder.longestOnly(false);
            builder.includeAbbreviations(false);
            builder.includeAncronyms(false);
            builder.includeNumbers(false);
            entities = wrapper.annotate(builder.get());
        } catch(IOException e) {
            throw new AnnotationException(e.getMessage());
        }
        return entities;
    }
}
