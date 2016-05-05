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

import java.util.List;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.ctakes.typesystem.type.refsem.Entity;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Annotates free text using apache ctakes.
 *
 * @version $Id$
 */
@Component
@Named("ctakes")
@Singleton
public class CTakesAnnotationService implements TermAnnotationService
{
    /**
     * The ontology manager.
     */
    @Inject
    private VocabularyManager vocabularies;

    /**
     * The wrapper to actually interact with ctakes.
     */
    @Inject
    private CTakesWrapper wrapper;

    @Override
    public List<TermAnnotation> annotate(String text) throws AnnotationException
    {
        try {
            List<EntityMention> annotations = wrapper.annotate(text);
            List<TermAnnotation> retval = new ArrayList<>(annotations.size());
            for (EntityMention annotation : annotations) {
                VocabularyTerm term = getVocabularyTerm(annotation);
                if (term != null) {
                    long start = annotation.getBegin();
                    long end = annotation.getEnd();
                    retval.add(new TermAnnotation(start, end, term));
                }
            }
            return retval;
        } catch (CTakesWrapper.CTakesException e) {
            throw new AnnotationException(e.getMessage(), e);
        }
    }

    /**
     * Get the vocabulary term corresponding to the EntityMention given.
     * @param annotation the annotation.
     */
    private VocabularyTerm getVocabularyTerm(EntityMention annotation)
    {
        Entity entity = annotation.getEntity();
        OntologyConcept concept = entity.getOntologyConcept();
        String termId = concept.getCode();
        return vocabularies.resolveTerm(termId);
    }
}
