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
package org.phenotips.scigraphwar;

import javax.servlet.annotation.WebListener;

import be.fluid_it.tools.dropwizard.box.WebApplication;
import io.scigraph.services.MainApplication;
import io.scigraph.services.configuration.ApplicationConfiguration;
import io.scigraph.services.PTSciGraphApplication;


/**
 * The webapp.
 *
 * @version $Id$
 */
@WebListener
public class ScigraphWebApp extends WebApplication<ApplicationConfiguration>
{
    private static String LOAD_CONFIG;

    static {
        LOAD_CONFIG = ScigraphWebApp.class.getClassLoader().getResource("load.yaml").getPath();
    }
    /**
     * CTOR.
     */
    public ScigraphWebApp()
    {
        super(new PTSciGraphApplication(LOAD_CONFIG), "server.yaml");
    }
}
