/**
 * Copyright (C) 2014 The SciGraph authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.scigraph.owlapi.loader;

import io.scigraph.frames.CommonProperties;
import io.scigraph.neo4j.Graph;
import io.scigraph.neo4j.GraphBatchImpl;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.MappedProperty;
import io.scigraph.owlapi.loader.OwlLoadConfiguration.OntologySetup;
import io.scigraph.owlapi.loader.bindings.IndicatesExactIndexedProperties;
import io.scigraph.owlapi.loader.bindings.IndicatesIndexedProperties;
import io.scigraph.owlapi.loader.bindings.IndicatesMappedCategories;
import io.scigraph.owlapi.loader.bindings.IndicatesMappedProperties;
import io.scigraph.owlapi.loader.bindings.IndicatesNumberOfConsumerThreads;
import io.scigraph.owlapi.loader.bindings.IndicatesNumberOfProducerThreads;
import io.scigraph.owlapi.loader.bindings.IndicatesNumberOfShutdownProducers;
import io.scigraph.owlapi.loader.bindings.IndicatesCliqueConfiguration;
import io.scigraph.owlapi.loader.bindings.IndicatesUniqueProperty;
import io.scigraph.owlapi.postprocessors.CliqueConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.inject.Singleton;

import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

/**
 * A module that provides binding for data loading in a phenotips scigraph deployment.
 * This is needed to prevent a binding to the Graph class.
 */
public class PTSciGraphLoadModule extends OwlLoaderModule {

    private static final Logger logger = Logger.getLogger(OwlLoaderModule.class.getName());

    public PTSciGraphLoadModule(OwlLoadConfiguration config) {
        super(config);
        assert config != null;
    }

    @Override
    protected void configure() {
        bind(OwlLoadConfiguration.class).toInstance(config);
        bindConstant().annotatedWith(IndicatesUniqueProperty.class).to(CommonProperties.IRI);
        bind(new TypeLiteral<Set<String>>() {}).annotatedWith(IndicatesIndexedProperties.class).toInstance(config.getGraphConfiguration().getIndexedNodeProperties());
        bind(new TypeLiteral<Set<String>>() {}).annotatedWith(IndicatesExactIndexedProperties.class).toInstance(config.getGraphConfiguration().getExactNodeProperties());
        bind(new TypeLiteral<Map<String, String>>() {}).annotatedWith(IndicatesMappedCategories.class).toInstance(config.getCategories());
        bind(new TypeLiteral<List<MappedProperty>>() {}).annotatedWith(IndicatesMappedProperties.class).toInstance(config.getMappedProperties());
        bind(new TypeLiteral<List<OntologySetup>>() {}).toInstance(config.getOntologies());

        bind(new TypeLiteral<BlockingQueue<OWLCompositeObject>>(){}).to(new TypeLiteral<LinkedBlockingQueue<OWLCompositeObject>>(){}).in(Scopes.SINGLETON);
        bind(new TypeLiteral<BlockingQueue<OntologySetup>>(){}).to(new TypeLiteral<LinkedBlockingQueue<OntologySetup>>(){}).in(Scopes.SINGLETON);

        bind(Integer.class).annotatedWith(IndicatesNumberOfConsumerThreads.class).toInstance(config.getConsumerThreadCount());
        bind(Integer.class).annotatedWith(IndicatesNumberOfProducerThreads.class).toInstance(config.getProducerThreadCount());

        bind(AtomicInteger.class).annotatedWith(IndicatesNumberOfShutdownProducers.class).to(AtomicInteger.class).in(Scopes.SINGLETON);

        bind(new TypeLiteral<Optional<CliqueConfiguration>>() {}).annotatedWith(IndicatesCliqueConfiguration.class).toInstance(config.getCliqueConfiguration());
        bind(BatchOwlLoader.class).to(PTLoader.class);
    }
}

