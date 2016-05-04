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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Guice;
import com.google.inject.Injector;

import edu.sdsc.scigraph.annotation.EntityAnnotation;
import edu.sdsc.scigraph.annotation.EntityFormatConfiguration;
import edu.sdsc.scigraph.annotation.EntityModule;
import edu.sdsc.scigraph.annotation.EntityProcessor;
import edu.sdsc.scigraph.neo4j.Neo4jConfiguration;
import edu.sdsc.scigraph.neo4j.Neo4jModule;

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
     * The name of the directory containing the graph.
     */
    public static final String GRAPH_LOCATION = "hpo_graph";

    /**
     * The CURIES used to access the HPO.
     * These are primarily for convenience of configuration and mapping, though an hpo: curie has to
     * be defined at all times.
     */
    public static final Map<String, String> CURIES;

    /**
     * The properties that will be indexed on every HPO element.
     */
    public static final Set<String> PROPERTIES;

    /**
     * The configuration file for scigraph.
     */
    public static final String CONFIG_FILE = "annotations.yaml";

    /**
     * The load configuration file for scigraph.
     */
    public static final String LOAD_FILE = "load.yaml";

    /**
     * The entity processor to use for annotations.
     */
    private EntityProcessor processor;

    /**
     * The environment in use.
     */
    @Inject
    private Environment environment;

    /**
     * The Scigraph loader.
     */
    @Inject
    private SciGraphLoader loader;

    static {
        CURIES = new HashMap<>(3);
        CURIES.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        CURIES.put("hpo", "http://purl.obolibrary.org/obo/");
        CURIES.put("oboInOwl", "http://www.geneontology.org/formats/oboInOwl#");
        PROPERTIES = new HashSet<>(Arrays.asList("category", "label", "fragment", "synonym"));
    }

    @Override
    public void initialize() throws InitializationException {
        try {
            if (!loader.isLoaded()) {
                loader.load();
            }
            Neo4jConfiguration config = getConfig();
            /* Scigraph uses Google Guice for dependency injection, so we'll have to
             * set up a guice injector to start up. */
            Injector injector = Guice.createInjector(new Neo4jModule(config), new EntityModule());
            processor = injector.getInstance(EntityProcessor.class);
        } catch (IOException | SciGraphLoader.LoadException e) {
            throw new InitializationException(e.getMessage(), e);
        }
    }

    @Override
    public List<EntityAnnotation> annotate(EntityFormatConfiguration config) throws IOException {
        return processor.annotateEntities(config);
    }

    /**
     * Get the configuration for the Neo4j database that scigraph uses.
     * @return Neo4jConfiguration the configuration
     */
    private Neo4jConfiguration getConfig() throws IOException {
        String location = new File(environment.getPermanentDirectory(), GRAPH_LOCATION).getAbsolutePath();
        Neo4jConfiguration config = new Neo4jConfiguration();
        config.getCuries().putAll(CURIES);
        config.setLocation(location);
        config.getIndexedNodeProperties().addAll(PROPERTIES);
        config.getIndexedNodeProperties().addAll(PROPERTIES);
        return config;
    }
}
