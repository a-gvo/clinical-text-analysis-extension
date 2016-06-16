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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

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
     * The set of property names that contain definitions.
     */
    private static final Set<String> DEFINITION_FIELDS;

    /**
     * The set of property names that contain synonyms.
     */
    private static final Set<String> SYNONYM_FIELDS;

    /**
     * A set of property names that contain phenotype labels.
     */
    private static final Set<String> LABEL_FIELDS;

    private static final String NAMESPACE_FIELD = "oboInOwl#hasOBONamespace";

    /**
     * A validator for urls.
     */
    private static final UrlValidator URL_VALIDATOR = UrlValidator.getInstance();

    /**
     * The name of the label field in the lucene index.
     */
    private static final String LABEL_IDX_NAME = "label";

    /**
     * The name of the id field in the lucene index.
     */
    private static final String ID_IDX_NAME = "id";

    /**
     * The name of the definition field in the lucene index.
     */
    private static final String DEFINITION_IDX_NAME = "definition";

    /**
     * The name of the synonym field in the lucene index.
     */
    private static final String SYNONYM_IDX_NAME = "synonym";

    private static final String NAMESPACE = "human_phenotype";

    /**
     * The documents that will be indexed in the end.
     */
    private Map<String, Map<String, String>> documents;

    static
    {
        DEFINITION_FIELDS = new HashSet<>(3);
        DEFINITION_FIELDS.add("HP_0040005");
        DEFINITION_FIELDS.add("IAO_0000115");
        DEFINITION_FIELDS.add("rdfs:comment");
        SYNONYM_FIELDS = new HashSet<>(4);
        SYNONYM_FIELDS.add("oboInOwl#hasExactSynonym");
        SYNONYM_FIELDS.add("oboInOwl#hasBroadSynonym");
        SYNONYM_FIELDS.add("oboInOwl#hasRelatedSynonym");
        SYNONYM_FIELDS.add("oboInOwl#hasNarrowSynonym");
        LABEL_FIELDS = new HashSet<>(1);
        LABEL_FIELDS.add("rdfs:label");
    }
    
    /**
     * CTOR.
     * @param walker the OWLOntologyWalker to traverse the ontology.
     */
    public CTakesOWLVisitor(OWLOntologyWalker walker)
    {
        super(walker);
        documents = new HashMap<>(INITIAL_MAP_SIZE);
    }

    /**
     * Parse the iri given, and return the last component thereof.
     */
    private String getIRIName(String iri)
    {
        if(URL_VALIDATOR.isValid(iri)) {
            iri = iri.substring(iri.lastIndexOf('/') + 1);
        }
        return iri;
    }

    private void addTo(Map<String, String> propertyMap, String name, String value)
    {
        String there = propertyMap.get(name) + "\n" + value;
        propertyMap.put(name, there);
    }

    /**
     * Add the property with the IRI given to the dictionary for the id given.
     */
    private void addProperty(String id, String iri, String value)
    {
        id = getIRIName(id);
        if (iri.matches("^<.*>$")) {
            iri = iri.substring(1, iri.length() - 1);
        }
        iri = getIRIName(iri);
        Map<String, String> propertyMap = documents.get(id);
        if (propertyMap == null) {
            propertyMap = new HashMap<>();
            documents.put(id, propertyMap);
            propertyMap.put(ID_IDX_NAME, id);
            propertyMap.put(DEFINITION_IDX_NAME, "");
            propertyMap.put(SYNONYM_IDX_NAME, "");
            propertyMap.put(LABEL_IDX_NAME, "");
        }
        if (DEFINITION_FIELDS.contains(iri)) {
            addTo(propertyMap, DEFINITION_IDX_NAME, value);
        }
        if (SYNONYM_FIELDS.contains(iri)) {
            addTo(propertyMap, SYNONYM_IDX_NAME, value);
        }
        if (LABEL_FIELDS.contains(iri)) {
            addTo(propertyMap, LABEL_IDX_NAME, value);
        }
        if (NAMESPACE_FIELD.equals(iri)) {
            propertyMap.put(iri, value);
        }
    }

    @Override
    public void visit(OWLAnnotationAssertionAxiom axiom)
    {
        if(axiom.getSubject() instanceof IRI) {
            String id = ((IRI) axiom.getSubject()).toString();
            if (axiom.getValue() instanceof OWLLiteral) {
                String property = axiom.getProperty().toString();
                String value = ((OWLLiteral) axiom.getValue()).getLiteral();
                addProperty(id, property, value);
            }
        }
    }

    @Override
    public void visit(OWLDataPropertyAssertionAxiom axiom)
    {
        String id = axiom.getSubject().toString();
        OWLDataProperty property = axiom.getProperty().asOWLDataProperty();
        String propertyName = property.getIRI().toString();
        String value = axiom.getObject().getLiteral();
        addProperty(id, propertyName, value);
    }

    /**
     * Get the documents that lucene should index.
     * @return the documents
     */
    public Iterable<? extends Iterable<? extends IndexableField>> getDocuments()
    {
        List<List<IndexableField>> retval = new ArrayList<>(documents.size());
        for (Map<String, String> document : documents.values()) {
            if (!NAMESPACE.equals(document.get(NAMESPACE_FIELD))) {
                continue;
            }
            List<IndexableField> doc = new ArrayList<>(document.size());
            doc.add(new StringField(ID_IDX_NAME, document.get(ID_IDX_NAME).trim(), Store.YES));
            doc.add(new TextField(DEFINITION_IDX_NAME, document.get(DEFINITION_IDX_NAME).trim(), Store.YES));
            doc.add(new TextField(LABEL_IDX_NAME, document.get(LABEL_IDX_NAME).trim(), Store.YES));
            doc.add(new TextField(SYNONYM_IDX_NAME, document.get(SYNONYM_IDX_NAME).trim(), Store.YES));
            retval.add(doc);
        }
        return retval;
    }
}
