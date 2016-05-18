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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.IOException;

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
     * The object for API interaction with scigraph.
     */
    @Inject
    private SciGraphAPI api;

    /**
     * The object mapper in use.
     */
    private ObjectMapper mapper;

    @Override
    public void initialize() throws InitializationException
    {
        mapper = new ObjectMapper();
    }

    /**
     * A response to a load command as sent by scigraph.
     */
    public static final class LoadResponse
    {
        /**
         * Whether the load succeeded.
         */
        private boolean success;

        /**
         * The message returned by the load.
         */
        private String message;

        /**
         * Return whether the load succeeded.
         */
        public boolean getSuccess()
        {
            return success;
        }

        /**
         * Set whether the load succeeded.
         */
        public void setSuccess(boolean success)
        {
            this.success = success;
        }

        /**
         * Get the load message.
         */
        public String getMessage()
        {
            return message;
        }

        /**
         * Set the load message.
         */
        public void setMessage(String message)
        {
            this.message = message;
        }
    }

    @Override
    public void load() throws LoadException {
        try {
            LoadResponse response = mapper.readValue(api.postEmpty("loader/load"), LoadResponse.class);
            if (!response.getSuccess()) {
                throw new LoadException(response.getMessage());
            }
        } catch (SciGraphAPI.SciGraphException | IOException e) {
            throw new LoadException(e.getMessage(), e);
        }
    }

    /**
     * Return whether the load has happened.
     * Really just checks if the directory where the database is supposed to be exists.
     * @return boolean whether the neo4j database is loaded.
     */
    public boolean isLoaded() throws LoadException {
        LoadResponse response;
        try {
            response = mapper.readValue(api.getEmpty("loader/isLoaded"), LoadResponse.class);
        } catch (SciGraphAPI.SciGraphException | IOException e) {
            throw new LoadException(e.getMessage(), e);
        }
        return response.getSuccess();
    }
}
