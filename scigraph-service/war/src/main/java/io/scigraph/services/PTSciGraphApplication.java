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
/* We need access to some protected stuff in the MainApplication, so there's no
 * choice but to place this in this package. No biggie, since we're already dumping
 * services in that package anyway. */
package io.scigraph.services;

import io.scigraph.services.MainApplication;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.views.ViewBundle;
import io.scigraph.services.configuration.ApplicationConfiguration;
import io.dropwizard.setup.Bootstrap;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import io.scigraph.owlapi.loader.OwlLoadConfiguration;
import io.scigraph.owlapi.loader.OwlLoadConfigurationLoader;
import io.scigraph.owlapi.loader.BatchOwlLoader;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.net.URL;

/**
 * A Scigraph application for use with phenotips.
 */
public class PTSciGraphApplication extends MainApplication
{
    /**
     * The load config object.
     */
    private OwlLoadConfiguration config;

    /**
     * The name used to verify whether the graph is there.
     * TODO MAKE THIS PORTABLE
     */
    private static String GRAPH_NAME = "index/lucene/node/node_auto_index";

    @Override
    public void initialize(Bootstrap<ApplicationConfiguration> bootstrap)
    {
        File graph = new File(config.getGraphConfiguration().getLocation(), GRAPH_NAME);
        if (!graph.exists()) {
            try {
                BatchOwlLoader.load(config);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        super.initialize(bootstrap);
    }

    /**
     * Construct a new PTSciGraphApplication.
     * @param loadConfig the location of the load configuration.
     */
    public PTSciGraphApplication(String loadConfig)
    {
        try {
            OwlLoadConfigurationLoader owlLoadConfigurationLoader =
                new OwlLoadConfigurationLoader(new File(loadConfig));
            config = owlLoadConfigurationLoader.loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
