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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;

import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;

/**
 * Visits OWL objects in the HPO, and arranges them as an Iterable that lucene can index.
 *
 * @version $Id$
 */
public class CTakesOWLVisitor extends OWLOntologyWalkerVisitor
{
    /**
     * How big the documents map should be at first.
     * Set this as close as possible to the estimated number of phenotypes,
     * to avoid growing the hash map.
     */
    private static final int INITIAL_MAP_SIZE = 65536;

    /**
     * The documents that will be indexed in the end.
     */
    private Map<String, Map<String, String>> documents;

    /**
     * CTOR.
     * @param walker the OWLOntologyWalker to traverse the ontology.
     */
    public CTakesOWLVisitor(OWLOntologyWalker walker)
    {
        super(walker);
        documents = new HashMap<>(INITIAL_MAP_SIZE);
    }

    @Override
    public void visit(OWLAnnotationAssertionAxiom axiom) {
        if(axiom.getSubject() instanceof IRI) {
            String id = ((IRI) axiom.getSubject()).toString();
            if (axiom.getValue() instanceof OWLLiteral) {
                String property = axiom.getProperty().toString();
                String value = ((OWLLiteral) axiom.getValue()).getLiteral();
                System.out.println("Subject " + id + " " + property + " :" + value);
            }
        }
    }

    /**
     * Get the documents that lucene should index.
     * @return the documents
     */
    public Iterable<? extends Iterable<? extends IndexableField>> getDocuments()
    {
        List<List<IndexableField>> retval = new ArrayList<>(documents.size());
        for (Map<String, String> document : documents.values()) {
            List<IndexableField> doc = new ArrayList<>(document.size());
            doc.add(new StringField("id", document.get("id"), Store.YES));
            doc.add(new TextField("definition", document.get("definition"), Store.YES));
            doc.add(new TextField("label", document.get("label"), Store.YES));
            doc.add(new TextField("synonym", document.get("synonym"), Store.YES));
            retval.add(doc);
        }
        return retval;
    }
}
