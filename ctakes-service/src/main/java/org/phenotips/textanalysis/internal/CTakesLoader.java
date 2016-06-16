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

import java.io.File;
import java.io.IOException;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.IndexWriter;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.model.OWLObject;


/**
 * Loads the HPO into a lucene index for use by ctakes.
 *
 * @version $Id$
 */
public class CTakesLoader
{
    private CTakesOWLVisitor visitor;

    private OWLOntologyWalker walker;

    private OWLOntologyManager manager;

    private String location;

    private OWLOntology ontology;

    private IndexWriter indexWriter;

    public CTakesLoader(String ontologyLocation, IndexWriter writer)
    {
        manager = OWLManager.createOWLOntologyManager();
        location = ontologyLocation;
        ontology = getOntology();
        Set<OWLOntology> set = new HashSet<>(1);
        set.add(ontology);
        walker = new OWLOntologyWalker(set);
        visitor = new CTakesOWLVisitor(walker);
        /* Ontology doesn't look like a word anymore... */
        indexWriter = writer;
    }

    public void load()
    {
        try {
            ontology.accept(visitor);
            for (OWLObject object : ontology.getNestedClassExpressions()) {
                object.accept(visitor);
            }
            for (OWLObject object : ontology.getClassesInSignature()) {
                object.accept(visitor);
            }
            for (OWLObject object : ontology.getAxioms()) {
                object.accept(visitor);
            }
            indexWriter.addDocuments(visitor.getDocuments());
        } catch (IOException e) {
            /* XXX */
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private OWLOntology getOntology()
    {
        try {
            return manager.loadOntologyFromOntologyDocument(new File(location));
        } catch(OWLOntologyCreationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
