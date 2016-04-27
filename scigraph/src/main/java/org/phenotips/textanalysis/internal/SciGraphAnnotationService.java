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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Implementation of {@link TermAnnotationService} using SciGraph.
 *
 * @version $Id$
 */
@Component
@Named("scigraph")
@Singleton
public class SciGraphAnnotationService implements TermAnnotationService
{
    /**
     * The categories to which annotations must belong to.
     * In this case it's just "phenotype"
     */
    private static final Set<String> CATEGORIES = new HashSet<String>(Arrays.asList("phenotype"));

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

    @Override
    public List<TermAnnotation> annotate(String text) throws AnnotationException
    {
        List<SciGraphWrapper.SciGraphAnnotation> annotations = wrapper.annotate(text);
        List<TermAnnotation> retval = new ArrayList<>(annotations.size());
        for (SciGraphWrapper.SciGraphAnnotation annotation : annotations) {
            String termId = annotation.getToken().getId().replace("hpo:", "").replace("_", ":");
            VocabularyTerm term = this.vocabularies.resolveTerm(termId);
            if (term != null) {
                long start = annotation.getStart();
                long end = annotation.getEnd();
                retval.add(new TermAnnotation(start, end, term));
            }
        }
        return retval;
    }
}
