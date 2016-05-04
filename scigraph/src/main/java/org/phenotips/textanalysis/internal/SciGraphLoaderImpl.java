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
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.validator.routines.UrlValidator;

import org.coode.owlapi.obo12.parser.OBO12ParserFactory;
import org.coode.owlapi.oboformat.OBOFormatParserFactory;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.io.OWLParserFactoryRegistry;

import org.slf4j.Logger;

import edu.sdsc.scigraph.neo4j.Neo4jConfiguration;
import edu.sdsc.scigraph.owlapi.OwlApiUtils;
import edu.sdsc.scigraph.owlapi.loader.BatchOwlLoader;
import edu.sdsc.scigraph.owlapi.loader.OwlLoadConfiguration;
import edu.sdsc.scigraph.neo4j.Neo4jModule;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.graphdb.index.IndexManager;
import edu.sdsc.scigraph.lucene.VocabularyIndexAnalyzer;


/**
 * Loads the hpo into a scigraph.
 *
 * @version $Id$
 */
@Component
@Singleton
public class SciGraphLoaderImpl implements Initializable, SciGraphLoader
{
    /**
     * The environment in use.
     */
    @Inject
    private Environment environment;

    /**
     * The config for this loader.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * A logger.
     */
    @Inject
    private Logger logger;

    /**
     * The location of the configuration file for the SciGraph load.
     */
    private String configFile;

    /**
     * The location of the graph.
     */
    private String location;

    /**
     * The config object for the load.
     */
    private OwlLoadConfiguration config;

    @Override
    public void initialize() throws InitializationException
    {
        location = new File(environment.getPermanentDirectory(), SciGraphWrapperImpl.GRAPH_LOCATION).getAbsolutePath();
        config = new OwlLoadConfiguration();

        Neo4jConfiguration graph = new Neo4jConfiguration();
        graph.setLocation(location);
        graph.getIndexedNodeProperties().addAll(SciGraphWrapperImpl.PROPERTIES);
        graph.getExactNodeProperties().addAll(SciGraphWrapperImpl.PROPERTIES);

        graph.getCuries().putAll(SciGraphWrapperImpl.CURIES);

        config.setGraphConfiguration(graph);

        config.getCategories().put("hpo:HP_0000001", "phenotype");

        OwlLoadConfiguration.MappedProperty label = new OwlLoadConfiguration.MappedProperty("label");
        label.setProperties(Arrays.asList("rdfs:label", "http://www.w3.org/2004/02/skos/core#prefLabel"));
        OwlLoadConfiguration.MappedProperty comment = new OwlLoadConfiguration.MappedProperty("comment");
        comment.setProperties(Arrays.asList("rdfs:comment"));
        OwlLoadConfiguration.MappedProperty synonym = new OwlLoadConfiguration.MappedProperty("synonym");
        synonym.setProperties(Arrays.asList("oboInOwl:hasExactSynonym", "oboInOwl:hasBroadSynonym"));
        config.getMappedProperties().addAll(Arrays.asList(label, comment, synonym));

        OwlLoadConfiguration.OntologySetup ontology = new OwlLoadConfiguration.OntologySetup();
        String url = configuration.getProperty("phenotips.textanalysis.scigraph.hpoURL",
                                               "http://purl.obolibrary.org/obo/hp.owl");
        ontology.setUrl(url);

        config.setProducerThreadCount(configuration.
                                      getProperty("phenotips.textanalysis.scigraph.producerThreadCount", 2));
        config.setConsumerThreadCount(configuration.
                                      getProperty("phenotips.textanalysis.scigraph.consumerThreadCount", 2));
    }

    /**
     * Load the HPO into a scigraph.
     * @throws LoadException if the load goes wrong
     */
    public void load() throws LoadException {
        /* The load will fail rather noisily if the directory already exists */
        logger.info("Starting Scigraph load on " + location);
        FileUtils.deleteQuietly(new File(location));
        try {
            /* It's hack hack hack time. It transpires that the BatchOwlLoader has a static initializer that calls
             * a static method in the OWLAPIUtils class. What this static method does is run through
             * OWLParserFactories to make sure it doesn't see any that it doesn't like (the instanceof check below).
             * Unfortunately, it doesn't do this properly. The OWLParserFactoryRegistry's ParserFactory list is an
             * immutable copy of the Registry's internal list, so it cannot be have elements removed while it is being
             * iterated over lest it throw ConcurrentModificatonException (even if using unregisterParserFactory()
             * as a proxy for deletion). This results in the OWLAPIUtils static method throwing, which results in
             * the BatchOwlLoader static initializer failing, so the class can't be loaded and we get a cryptic
             * class not found error.
             * So, manually run over the registry, properly deleting the factories (by copying the original list) and
             * then keep going. Because the instanceof check will always be false in OWLAPIUtils now, we won't have a
             * problem.
             */
            /*OWLManager.createOWLOntologyManager();
            OWLParserFactoryRegistry registry = OWLParserFactoryRegistry.getInstance();
            List<OWLParserFactory> factories = registry.getParserFactories();
            List<OWLParserFactory> factoriesCp = new ArrayList<>(factories);
            for (OWLParserFactory factory : factoriesCp) {
                if (factory instanceof OBOFormatParserFactory
                    || factory instanceof OBO12ParserFactory) {
                    registry.unregisterParserFactory(factory);
                }
            }*/
            MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "analyzer",
                    VocabularyIndexAnalyzer.class.getName());
            BatchOwlLoader.load(config);
        } catch (InterruptedException e) {
            throw new LoadException(e.getMessage(), e);
        }
    }

    /**
     * Return whether the load has happened.
     * Really just checks if the directory where the database is supposed to be exists.
     * @return boolean whether the neo4j database is loaded.
     */
    public boolean isLoaded() {
        File f = new File(location);
        return f.exists() && f.isDirectory();
    }
}
