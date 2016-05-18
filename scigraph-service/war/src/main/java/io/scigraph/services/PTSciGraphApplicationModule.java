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
package io.scigraph.services;

import io.scigraph.annotation.EntityModule;
import io.scigraph.lexical.LexicalLibModule;
import io.scigraph.neo4j.Graph;
import io.scigraph.neo4j.GraphTransactionalImpl;
import io.scigraph.neo4j.Neo4jModule;
import io.scigraph.opennlp.OpenNlpModule;
import io.scigraph.owlapi.curies.CurieModule;
import io.scigraph.services.configuration.ApplicationConfiguration;
import io.scigraph.services.jersey.dynamic.DynamicResourceModule;
import io.scigraph.services.refine.RefineModule;
import io.scigraph.services.swagger.beans.resource.Apis;
import io.scigraph.owlapi.loader.OwlLoadConfiguration;
import io.scigraph.owlapi.loader.PTSciGraphLoadModule;

import java.util.List;

import ru.vyarus.dropwizard.guice.module.support.ConfigurationAwareModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;



/**
 * A guice module for a scigraph instance running with PhenoTips.
 */
public class PTSciGraphApplicationModule extends SciGraphApplicationModule
{
    /**
     * The config for loading.
     */
    private OwlLoadConfiguration loadConfig;

    /**
     * Construct a new module.
     * @param config the load configuration to be used when loading.
     */
    public PTSciGraphApplicationModule(OwlLoadConfiguration config)
    {
        this.loadConfig = config;
    }

    @Override
    public void configure()
    {
        install(new Neo4jModule(configuration.getGraphConfiguration()));
        install(new EntityModule());
        install(new LexicalLibModule());
        install(new OpenNlpModule());
        install(new RefineModule(configuration.getServiceMetadata()));
        install(new DynamicResourceModule());
        install(new CurieModule());
        install(new PTSciGraphLoadModule(loadConfig));
        bind(Graph.class).to(GraphTransactionalImpl.class);
    }
}
