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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;

import io.dropwizard.setup.Bootstrap;
import io.scigraph.owlapi.loader.BatchOwlLoader;
import io.scigraph.owlapi.loader.OwlLoadConfiguration;
import io.scigraph.owlapi.loader.OwlLoadConfigurationLoader;
import io.scigraph.services.configuration.ApplicationConfiguration;

/**
 * A Scigraph application for use with phenotips.
 *
 * @version $Id$
 */
public class PTSciGraphApplication extends MainApplication
{
    /**
     * The load config object.
     */
    private OwlLoadConfiguration config;

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

    @Override
    public void initialize(Bootstrap<ApplicationConfiguration> bootstrap)
    {
        File graph = new File(config.getGraphConfiguration().getLocation());
        /* There's no other way to reindex, so unfortunately we will have to delete everything every time */
        FileUtils.deleteQuietly(graph);
        try {
            BatchOwlLoader.load(config);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        super.initialize(bootstrap);
    }
}
