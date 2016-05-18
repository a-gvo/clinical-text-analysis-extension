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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;


/**
 * Implements the scigraph api.
 *
 * @version $Id$
 */
@Component
public class SciGraphAPIImpl implements SciGraphAPI
{

    /**
     * The url where scigraph is being hosted.
     */
    private static final String BASE_URL = "http://localhost:8080/scigraph/scigraph/";

    @Override
    public InputStream postJson(String method, InputStream content) throws SciGraphException
    {
        try {
            URI uri = getAbsoluteURI(method);
            return Request.Post(uri).
                bodyStream(content, ContentType.APPLICATION_JSON).
                execute().returnContent().asStream();
        } catch (IOException | URISyntaxException e) {
            throw new SciGraphException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream postForm(String method, Map<String, String> params) throws SciGraphException
    {
        try {
            URI uri = getAbsoluteURI(method);
            List<NameValuePair> list = new ArrayList<>(params.size());
            for (Map.Entry<String, String> entry : params.entrySet()) {
                NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry.getValue());
                list.add(pair);
            }
            return Request.Post(uri).
                bodyForm(list).
                execute().returnContent().asStream();
        } catch (IOException | URISyntaxException e) {
            throw new SciGraphException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream postEmpty(String method) throws SciGraphException
    {
        try {
            URI uri = getAbsoluteURI(method);
            return Request.Post(uri).execute().returnContent().asStream();
        } catch (IOException | URISyntaxException e) {
            throw new SciGraphException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream getEmpty(String method) throws SciGraphException
    {
        try {
            URI uri = getAbsoluteURI(method);
            return Request.Get(uri).execute().returnContent().asStream();
        } catch (IOException | URISyntaxException e) {
            throw new SciGraphException(e.getMessage(), e);
        }
    }

    /**
     * Get the uri to access a method.
     * @param method the name of the method
     * @return the corresponding uri.
     */
    private URI getAbsoluteURI(String method) throws URISyntaxException, MalformedURLException {
        URL base = new URL(BASE_URL);
        URL absolute = new URL(base, method);
        return absolute.toURI();
    }
}
