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
import io.scigraph.neo4j.RelationshipMap;
import io.scigraph.neo4j.GraphTransactionalImpl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentMap;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import javax.inject.Inject;
import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class PTLoader extends BatchOwlLoader
{
    /**
     * The indexer for this load operation.
     */
    @Inject
    private PTIndexer indexer;

    /**
     * Decorates graphs to prevent them from shutting down.
     */
    private static class GraphDecorator implements Graph
    {
        /**
         * The decorated graph.
         */
        private Graph inner;

        /**
         * The ids for created nodes.
         */
        private Set<Long> ids;

        /**
         * Construct a new graph decorator.
         * @param inner the graph to decorate.
         */
        public GraphDecorator(Graph inner)
        {
            this.inner = inner;
            this.ids = new HashSet<>();
        }

        @Override
        public void shutdown()
        {

        }

        @Override
        public long createNode(String id)
        {
            long result = inner.createNode(id);
            ids.add(result);
            return result;
        }

        @Override
        public Optional<Long> getNode(String id)
        {
            return inner.getNode(id);
        }

        @Override
        public long createRelationship(long start, long end, RelationshipType type)
        {
            return inner.createRelationship(start, end, type);
        }

        @Override
        public Optional<Long> getRelationship(long start, long end, RelationshipType type)
        {
            return inner.getRelationship(start, end, type);
        }

        @Override
        public Collection<Long> createRelationshipsPairwise(Collection<Long> nodeIds, RelationshipType type)
        {
            return inner.createRelationshipsPairwise(nodeIds, type);
        }

        @Override
        public void setNodeProperty(long node, String property, Object value)
        {
            inner.setNodeProperty(node, property, value);
        }

        @Override
        public void addNodeProperty(long node, String property, Object value)
        {
            inner.addNodeProperty(node, property, value);
        }

        @Override
        public <T> Optional<T> getNodeProperty(long node, String property, Class<T> type)
        {
            return inner.getNodeProperty(node, property, type);
        }

        @Override
        public <T> Collection<T> getNodeProperties(long node, String property, Class<T> type)
        {
            return inner.getNodeProperties(node, property, type);
        }

        @Override
        public void setRelationshipProperty(long relationship, String property, Object value)
        {
            inner.setRelationshipProperty(relationship, property, value);
        }

        @Override
        public void addRelationshipProperty(long relationship, String property, Object value)
        {
            inner.addRelationshipProperty(relationship, property, value);
        }

        @Override
        public <T> Optional<T> getRelationshipProperty(long relationship, String property, Class<T> type)
        {
            return inner.getRelationshipProperty(relationship, property, type);
        }

        @Override
        public <T> Collection<T> getRelationshipProperties(long relationship, String property, Class<T> type)
        {
            return inner.getRelationshipProperties(relationship, property, type);
        }

        @Override
        public void setLabel(long node, Label label)
        {
            inner.setLabel(node, label);
        }

        @Override
        public void addLabel(long node, Label label)
        {
            inner.addLabel(node, label);
        }

        @Override
        public Collection<Label> getLabels(long node)
        {
            return inner.getLabels(node);
        }

        /**
         * Get the ids that have been inserted into this graph.
         */
        public List<Long> getIds()
        {
            return new ArrayList<Long>(ids);
        }
    }

    @Override
    public void loadOntology() throws InterruptedException, ExecutionException
    {
        GraphDecorator decorator = new GraphDecorator(graph);
        graph = decorator;
        super.loadOntology();
        indexer.index(decorator.getIds());
    }
}
