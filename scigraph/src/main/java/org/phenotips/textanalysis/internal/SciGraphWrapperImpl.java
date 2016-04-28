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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import edu.sdsc.scigraph.annotation.EntityProcessor;
import edu.sdsc.scigraph.annotation.EntityAnnotation;
import edu.sdsc.scigraph.annotation.Entity;
import edu.sdsc.scigraph.annotation.EntityFormatConfiguration;
import edu.sdsc.scigraph.annotation.EntityModule;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import edu.sdsc.scigraph.neo4j.Neo4jConfiguration;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Wrapper component for the SciGraph annotation service.
 */
@Component
public class SciGraphWrapperImpl implements SciGraphWrapper, Initializable {

    /**
     * The entity processor to use for annotations.
     */
    private EntityProcessor processor;

    @Override
    public void initialize() throws InitializationException {
        /* Scigraph uses Google Guice for dependency injection, so we'll have to
         * set up a guice injector to start up. */
        Neo4jConfiguration config = getConfig();
        Injector injector = Guice.createInjector(new Neo4jModule(config), new EntityModule());
        processor = injector.getInstance(EntityProcessor.class);
    }

    @Override
    public List<EntityAnnotation> annotate(EntityFormatConfiguration config) throws IOException {
        return processor.annotateEntities(config);
    }

    /**
     * Get the configuration for the Neo4j database that scigraph uses.
     * @return Neo4jConfiguration the configuration
     */
    private Neo4jConfiguration getConfig() {
        /* TODO Get rid of hardcoded settings. */
        Neo4jConfiguration config = new Neo4jConfiguration();
        Map<String, String> curies = config.getCuries();
        curies.put("hpo", "http://purl.obolibrary.org/obo/");
        curies.put("oboInOwl", "http://www.geneontology.org/formats/oboInOwl#");
        config.getIndexedNodeProperties().addAll(Arrays.asList("category", "label", "fragment"));
        config.getExactNodeProperties().addAll(Arrays.asList("label", "synonym"));
        config.setLocation("resources/hpo_neo4j/");
        return config;
    }
}
