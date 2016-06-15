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

import java.io.IOException;

import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.lucene.index.IndexWriter;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Annotates free text using apache ctakes.
 *
 * @version $Id$
 */
public class CTakesAnnotationService extends ServerResource
{
    /**
     * The uima analysis engine in use.
     */
    private AnalysisEngine ae;

    /**
     * The JCas instance used for analysis.
     */
    private JCas jcas;

    /**
     * The object mapper to use for json serialization.
     */
    private ObjectMapper om;

    /**
     * A lucene index writer to use.
     */
    private IndexWriter writer;

    /**
     * CTOR.
     */
    public CTakesAnnotationService()
    {
        super();
        writer = null;
        om = new ObjectMapper();
        URL engineXML = CTakesAnnotationService.class.getClassLoader().getResource("pipeline/AnalysisEngine.xml");
        ae = null;
        jcas = null;
        /*try {
            XMLInputSource in = new XMLInputSource(engineXML);
            ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
            ae = UIMAFramework.produceAnalysisEngine(specifier);
            jcas = ae.newJCas();
        } catch (IOException | InvalidXMLException | ResourceInitializationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }*/
    }

    /**
     * Annotate the text given.
     * @param text the text to annotate
     * @return the http response object
     */
    @Post
    @Produces("application/json")
    @Consumes("application/x-www-form-urlencoded")
    public Response annotate(String text)
    {
        try {
            List<EntityMention> annotations = annotateText(text);
            List<Map<String, Object>> transformed = new ArrayList<>(annotations.size());
            for (EntityMention annotation : annotations) {
                Map<String, Object> map = new HashMap<>(3);
                map.put("start", annotation.getBegin());
                map.put("end", annotation.getEnd());
                Map<String, Object> token = new HashMap<>(1);
                token.put("id", annotation.getEntity().getOntologyConcept().getCode());
                map.put("token", token);
                transformed.add(map);
            }
            byte[] retval = om.writeValueAsBytes(transformed);
            return Response.ok(retval, MediaType.APPLICATION_JSON).build();
        } catch (AnalysisEngineProcessException | JsonProcessingException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Get
    public String test()
    {
        return "Hello world";
    }

    /**
     * Fetch the HPO and reindex it.
     * @return whether it worked
     */
    @Post
    public Response reindex()
    {
        try {
            URL url = new URL("https://compbio.charite.de/jenkins/job/hpo/lastStableBuild/artifact/hp/hp.owl");
            Path temp = Files.createTempFile("hpoLoad", "owl");
            FileUtils.copyURLToFile(url, temp.toFile());
            CTakesLoader loader = new CTakesLoader(temp.toFile().toString(), writer);
            loader.load();
        } catch (IOException e) {
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    /**
     * Actually interact with CTakes to have the text annotated.
     * @param text the text to annotate
     * @return a list of CTakes EntityMentions
     * @throws AnalysisEngineProcessException if ctakes throws.
     */
    private List<EntityMention> annotateText(String text) throws AnalysisEngineProcessException
    {
        jcas.setDocumentText(text);
        ae.process(jcas);
        jcas.reset();
        List<EntityMention> mentions = new LinkedList<>();
        Iterator<Annotation> iter = jcas.getAnnotationIndex(EntityMention.type).iterator();
        while (iter.hasNext()) {
            mentions.add((EntityMention) iter.next());
        }
        return mentions;
    }
}
