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
package io.scigraph.vocabulary;

import javax.inject.Inject;
import org.neo4j.graphdb.GraphDatabaseService;
import javax.annotation.Nullable;
import io.scigraph.neo4j.bindings.IndicatesNeo4jGraphLocation;
import io.scigraph.owlapi.curies.CurieUtil;
import io.scigraph.neo4j.NodeTransformer;
import io.scigraph.frames.Concept;

import java.io.IOException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.FuzzyQuery;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.IndexHits;
import io.scigraph.lucene.LuceneUtils;
import io.scigraph.frames.NodeProperties;

import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;


public class PTVocabularyImpl extends VocabularyNeo4jImpl
{
    /**
     * The similarity score used when searching.
     */
    public static final float SIMILARITY = 0.7f;

    private static final Logger logger = Logger.getLogger(PTVocabularyImpl.class.getName());

    private GraphDatabaseService graph;

    private NodeTransformer transformer;

    @Inject
    public PTVocabularyImpl(GraphDatabaseService graph, @Nullable @IndicatesNeo4jGraphLocation String neo4jLocation,
        CurieUtil curieUtil, NodeTransformer transformer) throws IOException {
        super(graph, neo4jLocation, curieUtil, transformer);
        this.graph = graph;
        this.transformer = transformer;
    }

    private SpanNearQuery createSpanQuery(String text, String field, float fuzzy, float boost)
    {
        String[] parts = ("^ " + text + " $").split(" ");
        SpanQuery[] clauses = new SpanQuery[parts.length];
        for (int i = 0; i < parts.length; i++) {
            /* There's a limit of 5 terms, which we set just to speed everything along. */
            clauses[i] = new SpanMultiTermQueryWrapper<FuzzyQuery>(
                    new FuzzyQuery(new Term(field, parts[i]), fuzzy, 0, 5));
        }
        /* Slop of 0 and inOrder of true basically turns this into a lucene phrase query */
        SpanNearQuery q = new SpanNearQuery(clauses, 0, true);
        q.setBoost(boost);
        return q;
    }

    @Override
    public List<Concept> getConceptsFromTerm(Query query)
    {
        /* This stuff is a translated-ish version of the field boosting you can find in 
         * org.phenotips.solr.internal.HumanPhenotypeOntology
         * Because we only use scigraph for text annotation, we ignore loads and loads of parameters
         * from the query passed in, such as isIncludeSynonyms/Abbreviations/Acronyms, depcreation of
         * concepts, and the limit. */
        BooleanQuery finalQuery = new BooleanQuery();
        try {
            String text = query.getInput().toLowerCase();
            QueryParser parser = getQueryParser();
            String exactQuery = String.format("\"\\^ %s $\"", text);
            /* The boost for full-text (i.e. non-phrase, non-exact) matching will depend on the length
             * of the phrase fed in. This way, we try to limit irrelevant matches (for instance so that
             * "slow" in "slow growth" doesn't match something like "slow onset"). Similarly, we'll
             * significantly punish the boost if we should be dealing with a single word, simply because
             * we could be dealing with descriptive words such as "abnormality" or "syndrome" that we
             * don't want popping up. */
            float wordCountScore = text.split(" ").length == 1 ? 0.5f : 1.4f;
            float textBoost;

            textBoost = Math.min((text.length() - 1) * wordCountScore, 20.0f);
            finalQuery.add(LuceneUtils.getBoostedQuery(parser, exactQuery, 100.0f), Occur.SHOULD);
            finalQuery.add(createSpanQuery(text, NodeProperties.LABEL, SIMILARITY, 36.0f), Occur.SHOULD);
            finalQuery.add(LuceneUtils.getBoostedQuery(parser, text, textBoost), Occur.SHOULD);

            textBoost = Math.min((text.length() - 2) * (wordCountScore - 0.2f), 15.0f);
            finalQuery.add(LuceneUtils.getBoostedQuery(parser, Concept.SYNONYM + ":" + exactQuery, 70.0f),
                    Occur.SHOULD);
            finalQuery.add(createSpanQuery(text, Concept.SYNONYM, SIMILARITY, 25.0f), Occur.SHOULD);
            finalQuery.add(LuceneUtils.getBoostedQuery(parser, Concept.SYNONYM + ":" + text, textBoost),
                    Occur.SHOULD);

            finalQuery.add(LuceneUtils.getBoostedQuery(parser, "comment:" + text, 3.0f), Occur.SHOULD);
            finalQuery.add(createSpanQuery(text, "comment", SIMILARITY, 5.0f), Occur.SHOULD);
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Failed to parse query", e);
        }
        System.out.println("\n" + finalQuery);
        try (Transaction tx = graph.beginTx()) {
            IndexHits<Node> hits = graph.index().getNodeAutoIndexer().getAutoIndex().query(finalQuery);
            /* We're gonna increase this as we go along, so if a phrase returns very few results, it'll return
             * what it has, but if there's tons and tons of results we'll only return good stuff.
             * This really only works because the hits come back sorted by order of relevance.
             */
            float threshold = 0.15f;
            int count = 2;
            List<Concept> result = new ArrayList<Concept>();
            for(Node n : hits) {
                float score = hits.currentScore();
                Concept c = transformer.apply(n);
                System.out.println(score + " " + c.toString());
                if (score >= threshold) {
                    result.add(c);
                    threshold += (0.8 / (count));
                    count *= 2;
                }
            }
            tx.success();
            return result;
        }
    }
}
