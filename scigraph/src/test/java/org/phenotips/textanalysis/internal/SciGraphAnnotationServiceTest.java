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
import org.phenotips.textanalysis.TermAnnotationService.AnnotationException;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.ArgumentMatcher;

import edu.sdsc.scigraph.annotation.EntityProcessor;
import edu.sdsc.scigraph.annotation.Entity;
import edu.sdsc.scigraph.annotation.EntityAnnotation;
import edu.sdsc.scigraph.annotation.EntityFormatConfiguration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SciGraphAnnotationServiceTest
{
    private TermAnnotationService client;

    /**
     * Mocker for SciGraphAnnotationService component.
     */
    @Rule
    public final MockitoComponentMockingRule<TermAnnotationService> mocker =
        new MockitoComponentMockingRule<TermAnnotationService>(SciGraphAnnotationService.class);

    /**
     * A mockito matcher to match EntityFormatConfiguration instances that contain certain pieces of
     * text.
     */
    private static class EntityFormatConfigurationMatcher extends ArgumentMatcher<EntityFormatConfiguration>
    {
        /**
         * The text we expect to find in the FormatConfiguration
         */
        private String text;

        /**
         * Construct a new EntityFormatConfigurationMatcher to match EntityFormatConfiguration instances
         * with the given text.
         */
        public EntityFormatConfigurationMatcher(String text)
        {
            this.text = text;
        }

        @Override
        public boolean matches(Object o)
        {
            EntityFormatConfiguration config = (EntityFormatConfiguration) o;
            CharBuffer buffer = CharBuffer.allocate(256);
            Reader reader = config.getReader();
            try {
                /* Reset the reader after the read() so that if anything else happens to it it stays
                 * as it was */
                reader.mark(256);
                reader.read(buffer);
                reader.reset();
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            return buffer.rewind().toString().trim().equals(text);
        }
    }

    /**
     * Test for cases where there's only one annotation in the text.
     *
     * @throws ComponentLookupException if the mocked component doesn't exist
     * @throws AnnotationException if the annotation process failed
     * @throws IOException if annotateEntities throws (hopefully never)
     */
    @Test
    public void testSingleAnnotation() throws AnnotationException, ComponentLookupException, IOException
    {
        client = this.mocker.getComponentUnderTest();
        String term = "blue eyes";
        String text = "The lady has ";
        int start = text.length();
        text += term;
        int end = start + term.length();
        String termId = "test id";

        List<EntityAnnotation> result = new LinkedList<EntityAnnotation>();
        Entity entity = new Entity(term, termId);
        EntityAnnotation blueEyesAnnotation = new EntityAnnotation(entity, start, end);
        result.add(blueEyesAnnotation);

        /* Mock SciGraph wrapper */
        SciGraphWrapper wrapper = this.mocker.getInstance(SciGraphWrapper.class);
        when(wrapper.annotate(argThat(new EntityFormatConfigurationMatcher(text)))).thenReturn(result);

        // Mock Ontology Manager
        VocabularyManager vocabularyManager = this.mocker.getInstance(VocabularyManager.class);
        VocabularyTerm t = mock(VocabularyTerm.class);
        when(t.getId()).thenReturn(termId);
        when(vocabularyManager.resolveTerm(termId)).thenReturn(t);

        List<TermAnnotation> expected = new LinkedList<TermAnnotation>();
        expected.add(new TermAnnotation(start, end, t));

        List<TermAnnotation> actual = client.annotate(text);

        assertEquals(expected, actual);
    }

    /**
     * Test for cases where two terms overlap in the text.
     *
     * @throws ComponentLookupException if the mocked component doesn't exist
     * @throws AnnotationException if the annotation process failed
     * @throws IOException if annotateEntities throws (hopefully never)
     */
    @Test
    public void testOverlappingAnnotations() throws AnnotationException, ComponentLookupException, IOException
    {
        client = this.mocker.getComponentUnderTest();
        String term1 = "blue eyes";
        String term2 = "eyes";
        String text = "The layd has ";
        int start1 = text.length();
        int start2 = text.length() + "blue ".length();
        int end1 = start1 + term1.length();
        int end2 = start2 + term2.length();
        String termId1 = "id1";
        String termId2 = "id2";
        text += term1;

        List<EntityAnnotation> result = new LinkedList<EntityAnnotation>();
        Entity entity1 = new Entity(term1, termId1);
        EntityAnnotation annotation1 = new EntityAnnotation(entity1, start1, end1);
        Entity entity2 = new Entity(term2, termId2);
        EntityAnnotation annotation2 = new EntityAnnotation(entity2, start2, end2);
        result.add(annotation1);
        result.add(annotation2);

        /* Mock SciGraph wrapper */
        SciGraphWrapper wrapper = this.mocker.getInstance(SciGraphWrapper.class);
        when(wrapper.annotate(argThat(new EntityFormatConfigurationMatcher(text)))).thenReturn(result);

        /* Mock Ontology wrapper */
        VocabularyManager vocabularyManager = this.mocker.getInstance(VocabularyManager.class);
        VocabularyTerm t1 = mock(VocabularyTerm.class);
        when(t1.getId()).thenReturn(termId1);
        when(vocabularyManager.resolveTerm(termId1)).thenReturn(t1);
        VocabularyTerm t2 = mock(VocabularyTerm.class);
        when(t2.getId()).thenReturn(termId2);
        when(vocabularyManager.resolveTerm(termId2)).thenReturn(t2);

        List<TermAnnotation> expected = new LinkedList<TermAnnotation>();
        expected.add(new TermAnnotation(start1, end1, t1));
        expected.add(new TermAnnotation(start2, end2, t2));

        List<TermAnnotation> actual = client.annotate(text);
        assertEquals(expected, actual);
    }

    /**
     * Test for cases where we're annotating empty text.
     *
     * @throws ComponentLookupException if the mocked component doesn't exist
     * @throws AnnotationException if the annotation process failed
     * @throws IOException if annotateEntities throws (hopefully never)
     */
    @Test
    public void testAnnotateEmpty() throws AnnotationException, ComponentLookupException, IOException
    {
        client = this.mocker.getComponentUnderTest();
        String text = "";
        List<EntityAnnotation> result = new LinkedList<EntityAnnotation>();

        SciGraphWrapper wrapper = this.mocker.getInstance(SciGraphWrapper.class);
        when(wrapper.annotate(argThat(new EntityFormatConfigurationMatcher(text)))).thenReturn(result);

        List<TermAnnotation> expected = new LinkedList<TermAnnotation>();
        assertEquals(expected, client.annotate(text));
    }
}
