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
package io.scigraph.owlapi.loader;

import io.scigraph.neo4j.Graph;
import org.neo4j.graphdb.GraphDatabaseService;
import javax.inject.Inject;
import java.util.Set;
import io.scigraph.owlapi.loader.bindings.IndicatesExactIndexedProperties;
import io.scigraph.owlapi.loader.bindings.IndicatesIndexedProperties;
import io.scigraph.owlapi.loader.bindings.IndicatesUniqueProperty;

import java.util.List;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import io.scigraph.lucene.VocabularyIndexAnalyzer;
import java.util.Map;
import java.util.HashMap;
import org.neo4j.graphdb.Node;
import io.scigraph.lucene.LuceneUtils;
import org.neo4j.graphdb.index.Index;
import java.util.Collections;

public class PTIndexer
{
    private String uniqueProperty;

    private Set<String> indexedProperties;

    private GraphDatabaseService graphDb;

    private Set<String> exactIndexedProperties;

    private static final Map<String, String> INDEX_CONFIG = MapUtil.stringMap(IndexManager.PROVIDER,
        "lucene", "analyzer", VocabularyIndexAnalyzer.class.getName());

    @Inject
    public PTIndexer(@IndicatesUniqueProperty String uniqueProperty,
      @IndicatesIndexedProperties Set<String> indexedProperties,
      @IndicatesExactIndexedProperties Set<String> exactIndexedProperties,
      GraphDatabaseService graphDb)
    {
        this.graphDb = graphDb;
        this.uniqueProperty = uniqueProperty;
        this.indexedProperties = indexedProperties;
        this.exactIndexedProperties = exactIndexedProperties;
    }

    public void index(List<Long> ids)
    {
        Index<Node> index = graphDb.index().forNodes("node_auto_index", INDEX_CONFIG);
        Collections.sort(ids);
        for (long id : ids) {
            Node node = graphDb.getNodeById(id);
            Map <String, Object> properties = new HashMap<>();
            for (String indexed : indexedProperties) {
                if (node.hasProperty(indexed) && !properties.containsKey(indexed)) {
                    properties.put(indexed, node.getProperty(indexed));
                }
            }
            for (String indexed : exactIndexedProperties) {
                String key = indexed + LuceneUtils.EXACT_SUFFIX;
                if (node.hasProperty(indexed) && !properties.containsKey(key)) {
                    properties.put(key, node.getProperty(indexed));
                }
            }
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                index.add(node, entry.getKey(), entry.getValue());
            }
        }
    }
}
