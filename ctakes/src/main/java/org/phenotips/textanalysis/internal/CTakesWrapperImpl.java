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
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

@Component
public class CTakesWrapperImpl implements CTakesWrapper, Initializable
{

    /**
     * The uima analysis engine in use.
     */
    private AnalysisEngine ae;

    /**
     * The JCas instance used for analysis.
     */
    private JCas jcas;

    @Override
    public void initialize() throws InitializationException
    {
        URL engineXML = CTakesWrapperImpl.class.getClassLoader().getResource("pipeline/AnalysisEngine.xml");
        try {
            XMLInputSource in = new XMLInputSource(engineXML);
            ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
            ae = UIMAFramework.produceAnalysisEngine(specifier);
            jcas = ae.newJCas();
        } catch (IOException | InvalidXMLException | ResourceInitializationException e) {
            throw new InitializationException(e.getMessage(), e);
        }
    }

    @Override
    public List<EntityMention> annotate(String text) throws CTakesException
    {
        try {
            jcas.setDocumentText(text);
            ae.process(jcas);
            jcas.reset();
        } catch (AnalysisEngineProcessException e) {
            throw new CTakesException(e.getMessage(), e);
        }
        List<EntityMention> mentions = new LinkedList<>();
        Iterator<Annotation> iter = jcas.getAnnotationIndex(EntityMention.type).iterator();
        while (iter.hasNext()) {
            mentions.add((EntityMention) iter.next());
        }
        return mentions;
    }
}
