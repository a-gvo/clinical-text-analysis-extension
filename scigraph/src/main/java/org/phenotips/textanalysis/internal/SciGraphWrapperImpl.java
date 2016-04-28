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

import java.io.File;
import java.io.IOException;
import java.util.List;

/* This is the same parser used by Scigraph itself, which is why we're
 * using it here.
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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
public class SciGraphWrapperImpl implements SciGraphWrapper, Initializable
{

    /**
     * The root directory for all scigraph files.
     */
    public static final String ROOT_DIRECTORY = "resources/Scigraph/";

    /**
     * The configuration file for scigraph.
     */
    public static final String CONFIG_FILE = "annotations.yaml";

    /**
     * The entity processor to use for annotations.
     */
    private EntityProcessor processor;

    @Override
    public void initialize() throws InitializationException {
        /* Scigraph uses Google Guice for dependency injection, so we'll have to
         * set up a guice injector to start up. */
        try {
            Neo4jConfiguration config = getConfig();
            Injector injector = Guice.createInjector(new Neo4jModule(config), new EntityModule());
            processor = injector.getInstance(EntityProcessor.class);
        } catch (IOException e) {
            throw new InitializationException(e.getMessage());
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
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String configFile = new File(ROOT_DIRECTORY, CONFIG_FILE).toString();
        Neo4jConfiguration config = mapper.readValue(configFile, Neo4jConfiguration.class);
        /* Gotta qualify the location with the scigraph root. */
        config.setLocation(new File(ROOT_DIRECTORY, config.getLocation()).toString());
        return config;
    }
}
