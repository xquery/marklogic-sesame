/*
 * Copyright 2015 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * A library that enables access to a MarkLogic-backed triple-store via the
 * Sesame API.
 */
package com.marklogic.semantics.sesame;

import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.sesame.query.MarkLogicTupleQuery;
import info.aduna.iteration.ConvertingIteration;
import info.aduna.iteration.ExceptionConvertingIteration;
import info.aduna.iteration.Iterations;
import org.junit.*;
import org.openrdf.model.*;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.*;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author James Fuller
 */
public class MarkLogicRepositoryConnectionTest extends SesameTestBase {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected MarkLogicRepositoryConnection conn;
    protected ValueFactory f;

    @Before
    public void setUp()
            throws Exception {
        logger.debug("setting up test");
        rep.initialize();
        f = rep.getValueFactory();
        conn =(MarkLogicRepositoryConnection)rep.getConnection();
        logger.info("test setup complete.");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown()
            throws Exception {
        logger.debug("tearing down...");
        conn.close();
        conn = null;
        rep.shutDown();
        rep = null;
        logger.info("tearDown complete.");
    }

    @Test
    public void testMarkLogicRepositoryConnectionOpen()
            throws Exception {
        Assert.assertEquals(true, conn.isOpen());
    }

    @Test
    public void testMarkLogicRepositoryConnection()
            throws Exception {

        Assert.assertNotNull("Expected repository to exist.", rep);
        Assert.assertTrue("Expected repository to be initialized.", rep.isInitialized());
        rep.shutDown();
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("gradle.properties"));
        } catch (IOException e) {
            System.err.println("problem loading properties file.");
            System.exit(1);
        }
        String host = props.getProperty("mlHost");
        int port = Integer.parseInt(props.getProperty("mlRestPort"));
        String user = props.getProperty("mlUsername");
        String pass = props.getProperty("mlPassword");

        rep = new MarkLogicRepository(host, port, user, pass, "DIGEST");

        Assert.assertNotNull("Expected repository to exist.", rep);
        Assert.assertFalse("Expected repository to not be initialized.", rep.isInitialized());
        rep.initialize();
        Assert.assertTrue("Expected repository to be initialized.", rep.isInitialized());
        rep.shutDown();
        Assert.assertFalse("Expected repository to not be initialized.", rep.isInitialized());
        rep.initialize();
        Assert.assertNotNull("Expected repository to exist.", rep);
    }

    @Test
    public void testSPARQLQueryWithPrepareQuery()
            throws Exception {

        String queryString = "select ?s ?p ?o { ?s ?p ?o } limit 1 ";
        Query q = conn.prepareQuery(QueryLanguage.SPARQL, queryString);

        if (q instanceof TupleQuery) {
            TupleQueryResult result = ((TupleQuery) q).evaluate();
            while (result.hasNext()) {
                BindingSet tuple = result.next();
                Assert.assertEquals("s", tuple.getBinding("s").getName());
                Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#AlexandriaGeodata", tuple.getBinding("s").getValue().stringValue());
            }
        }
    }

    @Test
    public void testSPARQLQuery()
            throws Exception {

        String queryString = "select ?s ?p ?o { ?s ?p ?o } limit 2 ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult results = tupleQuery.evaluate();

        Assert.assertEquals(results.getBindingNames().get(0), "s");
        Assert.assertEquals(results.getBindingNames().get(1), "p");
        Assert.assertEquals(results.getBindingNames().get(2), "o");

        BindingSet bindingSet = results.next();

        Value sV = bindingSet.getValue("s");
        Value pV = bindingSet.getValue("p");
        Value oV = bindingSet.getValue("o");

        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#AlexandriaGeodata", sV.stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#altitude", pV.stringValue());
        Assert.assertEquals("0", oV.stringValue());

        BindingSet bindingSet1 = results.next();

        Value sV1 = bindingSet1.getValue("s");
        Value pV1 = bindingSet1.getValue("p");
        Value oV1 = bindingSet1.getValue("o");

        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#AmphipolisGeodata", sV1.stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#altitude", pV1.stringValue());
        Assert.assertEquals("0", oV1.stringValue());
    }

    @Test
    public void testPrepareTupleQueryQueryStringMethod() throws Exception{
        String queryString = "select ?s ?p ?o { ?s ?p ?o } limit 10 ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
        TupleQueryResult results = tupleQuery.evaluate();

        Assert.assertEquals(results.getBindingNames().get(0), "s");
        Assert.assertEquals(results.getBindingNames().get(1), "p");
        Assert.assertEquals(results.getBindingNames().get(2), "o");
    }

    @Test
    public void testSPARQLQueryWithDefaultInferred()
            throws Exception {

        String queryString = "select ?s ?p ?o { ?s ?p ?o } limit 2 ";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        tupleQuery.setIncludeInferred(true);
        TupleQueryResult results = tupleQuery.evaluate();
        Assert.assertEquals(results.getBindingNames().get(0), "s");
        Assert.assertEquals(results.getBindingNames().get(1), "p");
        Assert.assertEquals(results.getBindingNames().get(2), "o");

        BindingSet bindingSet = results.next();

        Value sV = bindingSet.getValue("s");
        Value pV = bindingSet.getValue("p");
        Value oV = bindingSet.getValue("o");

        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#AlexandriaGeodata", sV.stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#altitude", pV.stringValue());
        Assert.assertEquals("0", oV.stringValue());

        BindingSet bindingSet1 = results.next();

        Value sV1 = bindingSet1.getValue("s");
        Value pV1 = bindingSet1.getValue("p");
        Value oV1 = bindingSet1.getValue("o");

        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#AmphipolisGeodata", sV1.stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#altitude", pV1.stringValue());
        Assert.assertEquals("0", oV1.stringValue());
    }

    @Test
    public void testSPARQLQueryDistinct()
            throws Exception {

        try {
            String queryString = "SELECT DISTINCT ?_ WHERE { GRAPH ?_ { ?s ?p ?o } }";
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            TupleQueryResult result = tupleQuery.evaluate();
            RepositoryResult<Resource> rr =
                    new RepositoryResult<Resource>(
                            new ExceptionConvertingIteration<Resource, RepositoryException>(
                                    new ConvertingIteration<BindingSet, Resource, QueryEvaluationException>(result) {

                                        @Override
                                        protected Resource convert(BindingSet bindings)
                                                throws QueryEvaluationException {
                                            return (Resource) bindings.getValue("_");
                                        }
                                    }) {

                                @Override
                                protected RepositoryException convert(Exception e) {
                                    return new RepositoryException(e);
                                }
                            });

            Resource resource = rr.next();

            logger.debug(resource.stringValue());

        } catch (MalformedQueryException e) {
            throw new RepositoryException(e);
        } catch (QueryEvaluationException e) {
            throw new RepositoryException(e);
        }
    }

    @Test
    public void testSPARQLQueryWithPagination()
            throws Exception {
        String queryString = "select ?s ?p ?o { ?s ?p ?o } limit 100 ";
        MarkLogicTupleQuery tupleQuery = (MarkLogicTupleQuery) conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        TupleQueryResult results = tupleQuery.evaluate(3, 1);

        Assert.assertEquals(results.getBindingNames().get(0), "s");
        Assert.assertEquals(results.getBindingNames().get(1), "p");
        Assert.assertEquals(results.getBindingNames().get(2), "o");

        BindingSet bindingSet = results.next();

        Value sV = bindingSet.getValue("s");
        Value pV = bindingSet.getValue("p");
        Value oV = bindingSet.getValue("o");

        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#AntiochGeodata", sV.stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#altitude", pV.stringValue());
        Assert.assertEquals("0", oV.stringValue());

        Assert.assertFalse(results.hasNext());
    }

    @Test
    public void testSPARQLQueryWithRuleset()
            throws Exception {
        String queryString = "select ?s ?p ?o { ?s ?p ?o } limit 100 ";
        MarkLogicTupleQuery tupleQuery = (MarkLogicTupleQuery) conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

        tupleQuery.setRulesets(SPARQLRuleset.RDFS_FULL);
        TupleQueryResult results = tupleQuery.evaluate();

        Assert.assertEquals(results.getBindingNames().get(0), "s");
        Assert.assertEquals(results.getBindingNames().get(1), "p");
        Assert.assertEquals(results.getBindingNames().get(2), "o");

        BindingSet bindingSet = results.next();

        Value sV = bindingSet.getValue("s");
        Value pV = bindingSet.getValue("p");
        Value oV = bindingSet.getValue("o");

        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#AlexandriaGeodata", sV.stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#altitude", pV.stringValue());
        Assert.assertEquals("0", oV.stringValue());

    }

    @Test
    public void testSPARQLQueryWithResultsHandler()
            throws Exception {
        String queryString = "select ?s ?p ?o { ?s ?p ?o } limit 10";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

        tupleQuery.evaluate(new TupleQueryResultHandler() {
            @Override
            public void startQueryResult(List<String> bindingNames) {
                Assert.assertEquals(bindingNames.get(0), "s");
                Assert.assertEquals(bindingNames.get(1), "p");
                Assert.assertEquals(bindingNames.get(2), "o");
            }

            @Override
            public void handleSolution(BindingSet bindingSet) {
                Assert.assertEquals(bindingSet.getBinding("o").getValue().stringValue(), "0");
            }

            @Override
            public void endQueryResult() {
            }

            @Override
            public void handleBoolean(boolean arg0)
                    throws QueryResultHandlerException {
            }

            @Override
            public void handleLinks(List<String> arg0)
                    throws QueryResultHandlerException {
            }
        });
        tupleQuery.evaluate();
    }

    @Test
    public void testSPARQLQueryBindings()
            throws Exception {

        String queryString = "select ?s ?p ?o { ?s ?p ?o . filter (?s = ?b) filter (?p = ?c) }";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

        tupleQuery.setBinding("b", ValueFactoryImpl.getInstance().createURI("http://semanticbible.org/ns/2006/NTNames#Jim"));
        tupleQuery.setBinding("c", ValueFactoryImpl.getInstance().createURI("http://semanticbible.org/ns/2006/NTNames#parentOf"));

        tupleQuery.removeBinding("c");

        // TBD -  Assert. for confirmation of removal

        Assert.assertEquals(null, tupleQuery.getBindings().getBinding("c"));

        tupleQuery.clearBindings();

        Assert.assertEquals(null, tupleQuery.getBindings().getBinding("b"));

        tupleQuery.setBinding("b", ValueFactoryImpl.getInstance().createURI("http://semanticbible.org/ns/2006/NTNames#Jotham"));
        tupleQuery.setBinding("c", ValueFactoryImpl.getInstance().createURI("http://semanticbible.org/ns/2006/NTNames#parentOf"));

        TupleQueryResult results = tupleQuery.evaluate();

        Assert.assertEquals(results.getBindingNames().get(0), "s");
        Assert.assertEquals(results.getBindingNames().get(1), "p");
        Assert.assertEquals(results.getBindingNames().get(2), "o");

        logger.info(results.getBindingNames().toString());

        Assert.assertTrue(results.hasNext());
        BindingSet bindingSet = results.next();

        Value sV = bindingSet.getValue("s");
        Value pV = bindingSet.getValue("p");
        Value oV = bindingSet.getValue("o");

        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#Jotham", sV.stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#parentOf", pV.stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#Ahaz", oV.stringValue());
    }


    @Test
    public void testSPARQLWithWriter()
            throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SPARQLResultsXMLWriter sparqlWriter = new SPARQLResultsXMLWriter(out);

        String expected = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<sparql xmlns='http://www.w3.org/2005/sparql-results#'>\n" +
                "\t<head>\n" +
                "\t\t<variable name='s'/>\n" +
                "\t\t<variable name='p'/>\n" +
                "\t\t<variable name='o'/>\n" +
                "\t</head>\n" +
                "\t<results>\n" +
                "\t\t<result>\n" +
                "\t\t\t<binding name='s'>\n" +
                "\t\t\t\t<uri>http://semanticbible.org/ns/2006/NTNames#AlexandriaGeodata</uri>\n" +
                "\t\t\t</binding>\n" +
                "\t\t\t<binding name='p'>\n" +
                "\t\t\t\t<uri>http://semanticbible.org/ns/2006/NTNames#altitude</uri>\n" +
                "\t\t\t</binding>\n" +
                "\t\t\t<binding name='o'>\n" +
                "\t\t\t\t<literal datatype='http://www.w3.org/2001/XMLSchema#int'>0</literal>\n" +
                "\t\t\t</binding>\n" +
                "\t\t</result>\n" +
                "\t</results>\n" +
                "</sparql>\n";

        String queryString = "select * { ?s ?p ?o . } limit 1";
        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

        tupleQuery.evaluate(sparqlWriter);

        Assert.assertEquals(expected,out.toString());

    }

    @Ignore
    public void incrementallyBuildQueryTest() throws MalformedQueryException, RepositoryException {

        TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, "");

//        StringBuffer sb = new StringBuffer();
//        sb.append("SELECT ?g ?s ?p ?o where {GRAPH ?g { ?s ?p ?o }");
//        if (s != Node) {
//            tupleQuery.setBinding("a", s.getURI());
//            sb.append("FILTER (?s = ?a) ");
//        }
//        if (p != Node.ANY) {
//            tupleQuery.setBinding("b", p.getURI());
//            sb.append("FILTER (?p = ?b) ");
//        }
//        if (o != Node.ANY) {
//            tupleQuery.setBinding("c",o);
//            sb.append("FILTER (?o = ?c) ");
//        }
//        sb.append("}");
//
//        qdef.setSparql(sb.toString());

    }

    @Test
    public void testConstructQuery()
            throws Exception {
        String queryString = "PREFIX nn: <http://semanticbible.org/ns/2006/NTNames#>\n" +
                "PREFIX test: <http://marklogic.com#test>\n" +
                "\n" +
                "construct { ?s  test:test \"0\"} where  {GRAPH <http://marklogic.com/test/my-graph> {?s nn:childOf nn:Eve . }}";
        GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        GraphQueryResult results = graphQuery.evaluate();
        Statement st1 = results.next();
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#Abel", st1.getSubject().stringValue());
        Statement st2 = results.next();
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#Cain", st2.getSubject().stringValue());
    }


    @Ignore
    public void testConstructQueryWithWriter()
            throws Exception {
        RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, System.out);

        String queryString = "PREFIX nn: <http://semanticbible.org/ns/2006/NTNames#>\n" +
                "PREFIX test: <http://marklogic.com#test>\n" +
                "\n" +
                "construct { ?s  test:test \"0\"} where  {GRAPH <http://marklogic.com/test/my-graph> {?s nn:childOf nn:Eve . }}";
        conn.prepareGraphQuery(QueryLanguage.SPARQL,
                "CONSTRUCT {?s ?p ?o } WHERE {?s ?p ?o } ").evaluate(writer);
    }


    @Test
    public void testDescribeQuery()
            throws Exception {
        String queryString = "DESCRIBE <http://semanticbible.org/ns/2006/NTNames#Shelah>";
        GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
        GraphQueryResult results = graphQuery.evaluate();
        Statement st1 = results.next();
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#Shelah", st1.getSubject().stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#childOf", st1.getPredicate().stringValue());
        Assert.assertEquals("http://semanticbible.org/ns/2006/NTNames#CainanSonOfArphaxad", st1.getObject().stringValue());
    }

    @Test
    public void testBooleanQuery()
            throws Exception {
        String queryString = "ASK { GRAPH <http://marklogic.com/test/my-graph> {<http://semanticbible.org/ns/2006/NTNames#Shelah1> ?p ?o}}";
        BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
        boolean results = booleanQuery.evaluate();
        Assert.assertEquals(false, results);
        queryString = "ASK { GRAPH <http://marklogic.com/test/my-graph> { <http://semanticbible.org/ns/2006/NTNames#Shelah> ?p ?o}}";
        booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
        results = booleanQuery.evaluate();
        Assert.assertEquals(true, results);
    }

    @Test
    public void testUpdateQuery()
            throws Exception {
        String defGraphQuery = "INSERT DATA { GRAPH <http://marklogic.com/test/g27> { <http://marklogic.com/test> <pp1> <oo1> } }";
        String checkQuery = "ASK WHERE { <http://marklogic.com/test> <pp1> <oo1> }";
        Update updateQuery = conn.prepareUpdate(QueryLanguage.SPARQL, defGraphQuery);
        updateQuery.execute();
        BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, checkQuery);
        boolean results = booleanQuery.evaluate();
        Assert.assertEquals(true, results);
    }

    @Test
    public void testUpdateQueryWithBaseURI()
            throws Exception {
        String defGraphQuery = "INSERT DATA { GRAPH <http://marklogic.com/test/context10> {  <http://marklogic.com/test/subject> <pp1> <oo1> } }";
        String checkQuery = "ASK WHERE { <http://marklogic.com/test/subject> <pp1> <oo1> }";
        Update updateQuery = conn.prepareUpdate(QueryLanguage.SPARQL, defGraphQuery,"http://marklogic.com/test/baseuri");
        updateQuery.execute();
        BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, checkQuery);
        boolean results = booleanQuery.evaluate();
        Assert.assertEquals(true, results);
        conn.clear(conn.getValueFactory().createURI("http://marklogic.com/test/context10"));
    }

    @Test
    public void testClear()
            throws Exception {
        String defGraphQuery = "INSERT DATA { GRAPH <http://marklogic.com/test/ns/cleartest> { <http://marklogic.com/cleartest> <pp1> <oo1> } }";
        String checkQuery = "ASK WHERE { <http://marklogic.com/cleartest> <pp1> <oo1> }";
        Update updateQuery = conn.prepareUpdate(QueryLanguage.SPARQL, defGraphQuery);
        updateQuery.execute();
        BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, checkQuery);
        boolean results = booleanQuery.evaluate();
        Assert.assertEquals(true, results);

        conn.clear(conn.getValueFactory().createURI("http://marklogic.com/test/ns/cleartest"));
        booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, checkQuery);
        results = booleanQuery.evaluate();
        Assert.assertEquals(false, results);

    }

    @Test
    public void testAddTurtle() throws Exception {
        File inputFile = new File("src/test/resources/testdata/default-graph-1.ttl");
        String baseURI = "http://example.org/example1/";
        Resource context1 = conn.getValueFactory().createURI("http://marklogic.com/test/context1");
        Resource context2 = conn.getValueFactory().createURI("http://marklogic.com/test/context2");
        conn.add(inputFile, baseURI, RDFFormat.TURTLE, context1, context2);
        conn.clear(context1, context2);
    }

    // https://github.com/marklogic/marklogic-sesame/issues/19
    @Test
    public void testAddTurtleWithDefaultContext() throws Exception {
        File inputFile = new File("src/test/resources/testdata/default-graph-2.ttl");

        conn.add(inputFile, null, RDFFormat.TURTLE);

        String checkQuery = "PREFIX dc:<http://purl.org/dc/elements/1.1/> PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> ASK { <urn:x-local:graph1> dc:publisher ?o .}";
        BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, checkQuery);
        conn.setNamespace("dc", "http://purl.org/dc/elements/1.1/");
        Assert.assertTrue(booleanQuery.evaluate());
    }

    @Test
    public void testAddWithInputStream() throws Exception {
        File inputFile = new File("src/test/resources/testdata/default-graph-1.ttl");
        FileInputStream is = new FileInputStream(inputFile);
        String baseURI = "http://example.org/example1/";
        Resource context3 = conn.getValueFactory().createURI("http://marklogic.com/test/context3");
        Resource context4 = conn.getValueFactory().createURI("http://marklogic.com/test/context4");
        conn.add(is, baseURI, RDFFormat.TURTLE, context3, context4);
        conn.clear(context3, context4);
    }

    // this test requires access to https://raw.githubusercontent.com/marklogic/marklogic-sesame/develop/marklogic-sesame/src/test/resources/testdata/testData.trig
    @Test
    @Ignore
    public void testAddTrigWithURL() throws Exception {
        URL dataURL = new URL("https://raw.githubusercontent.com/marklogic/marklogic-sesame/develop/marklogic-sesame/src/test/resources/testdata/testData.trig?token=AApzyAXWDMZiXGGf9DFnhq534MpEP-tKks5VwxFswA%3D%3D");

        Resource context1 = conn.getValueFactory().createURI("http://[example.org/g1");
        Resource context2 = conn.getValueFactory().createURI("http://[example.org/g2");
        Resource context3 = conn.getValueFactory().createURI("http://[example.org/g3");
        Resource context4 = conn.getValueFactory().createURI("http://[example.org/g4");
        Resource context5 = conn.getValueFactory().createURI("http://[example.org/o1");
        Resource context6 = conn.getValueFactory().createURI("http://[example.org/o2");
        Resource context7 = conn.getValueFactory().createURI("http://[example.org/o3");
        Resource context8 = conn.getValueFactory().createURI("http://[example.org/o4");
        conn.add(dataURL, dataURL.toString(), RDFFormat.TRIG, context3, context4);

        String checkQuery = "ASK { <http://example.org/r1> <http://example.org/p1> \"string value 0\" .}";
        BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, checkQuery);
        Assert.assertTrue(booleanQuery.evaluate());

        conn.clear(context1,context2,context3,context4,context5,context6,context7,context8);
    }

    @Test
    public void testAddNQuads() throws Exception{
        File inputFile = new File("src/test/resources/testdata/nquads1.nq");
        String baseURI = "http://example.org/example1/";

        Resource graph1 = conn.getValueFactory().createURI("http://example.org/graph1");
        Resource graph2 = conn.getValueFactory().createURI("http://example.org/graph2");
        Resource graph3 = conn.getValueFactory().createURI("http://example.org/graph3");
        Resource graph4 = conn.getValueFactory().createURI("http://example.org/graph4");

        conn.add(inputFile,baseURI,RDFFormat.NQUADS);

        String checkQuery = "ASK {GRAPH <http://example.org/graph4> { <http://example.org/kennedy/person1> <http://example.org/kennedy/death-year> '1969' . } }";
        BooleanQuery booleanQuery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, checkQuery);
        Assert.assertTrue(booleanQuery.evaluate());

        conn.clear(graph1,graph2,graph3,graph4);
    }

    @Test
    public void testAddNquadWithInputStream() throws Exception {
        File inputFile = new File("src/test/resources/testdata/nquads1.nq");
        FileInputStream is = new FileInputStream(inputFile);
        String baseURI = "http://example.org/example1/";
        Resource graph1 = conn.getValueFactory().createURI("http://example.org/graph1");
        Resource graph2 = conn.getValueFactory().createURI("http://example.org/graph2");
        Resource graph3 = conn.getValueFactory().createURI("http://example.org/graph3");
        Resource graph4 = conn.getValueFactory().createURI("http://example.org/graph4");
        conn.add(is, baseURI, RDFFormat.NQUADS);
        conn.clear(graph1, graph2, graph3, graph4);
    }

    @Test
    public void testAddRemoveStatementWithMultipleContext() throws Exception {

        Resource context5 = conn.getValueFactory().createURI("http://marklogic.com/test/context5");
        Resource context6 = conn.getValueFactory().createURI("http://marklogic.com/test/context6");

        URI alice = conn.getValueFactory().createURI("http://example.org/people/alice");
        URI bob = conn.getValueFactory().createURI("http://example.org/people/bob");
        URI name = conn.getValueFactory().createURI("http://example.org/ontology/name");
        URI person = conn.getValueFactory().createURI("http://example.org/ontology/Person");
        Literal bobsName = conn.getValueFactory().createLiteral("Bob");
        Literal alicesName = conn.getValueFactory().createLiteral("Alice");

        conn.add(alice, RDF.TYPE, person, context5);
        conn.add(alice, name, alicesName,context5, context6);
        conn.add(bob, RDF.TYPE, person, context5);
        conn.add(bob, name, bobsName, context5, context6);

        conn.remove(alice, RDF.TYPE, person, context5);
        conn.remove(alice, name, alicesName, context5, context6);
        conn.remove(bob, RDF.TYPE, person, context5);
        conn.remove(bob, name, bobsName, context5, context6);
    }

    @Test
    public void testContextIDs()
            throws Exception {
        RepositoryResult<Resource> result = conn.getContextIDs();
        try {
            Assert.assertTrue("result should not be empty", result.hasNext());
            logger.debug("ContextIDs");
            Resource result1 = result.next();
            logger.debug(result1.stringValue());
            Assert.assertEquals("http://marklogic.com/test/my-graph", result1.stringValue());
        } finally {
            result.close();
        }
    }

    @Test
    public void testHasStatement() {

    }

    @Test
    public void testTransaction1() throws Exception {
        File inputFile = new File("src/test/resources/testdata/named-graph-1.ttl");
        String baseURI = "http://example.org/example1/";
        Resource context1 = conn.getValueFactory().createURI("http://marklogic.com/test/transactiontest");
        conn.begin();
        conn.add(inputFile, baseURI, RDFFormat.TURTLE, context1);
        conn.rollback();
    }

    @Test
    public void testTransaction2() throws Exception {
        File inputFile = new File("src/test/resources/testdata/named-graph-1.ttl");

        String baseURI = "http://example.org/example1/";

        Resource context1 = conn.getValueFactory().createURI("http://marklogic.com/test/transactiontest");
        conn.begin();
        conn.add(inputFile, baseURI, RDFFormat.TURTLE, context1);
        conn.commit();
        conn.clear(context1);
    }

    @Test
    public void testTransaction3() throws Exception {
        Resource context1 = conn.getValueFactory().createURI("http://marklogic.com/test/my-graph");
        conn.begin();
        conn.clear(context1);
        conn.rollback();
    }

    @Test
    public void testOpen() throws Exception {
        Assert.assertEquals(true, conn.isOpen());
        conn.close();
        Assert.assertEquals(false, conn.isOpen());
    }

    @Test
    public void testActive() throws Exception {
        Assert.assertEquals(false, conn.isActive());
        conn.begin();
        Assert.assertEquals(true, conn.isActive());
    }

    @Test
    @Ignore
    public void testSize() throws Exception {
        File inputFile = new File("src/test/resources/testdata/default-graph-1.ttl");
        String baseURI = "http://example.org/example1/";
        Resource context1 = conn.getValueFactory().createURI("http://marklogic.com/test/sizetest");
        conn.add(inputFile, baseURI, RDFFormat.TURTLE, context1);
        conn.clear(context1);
        Assert.assertEquals(10, conn.size(context1));
        conn.clear(context1);
    }

    @Test
    public void testModel() throws Exception{
        Resource context5 = conn.getValueFactory().createURI("http://marklogic.com/test/context5");
        Resource context6 = conn.getValueFactory().createURI("http://marklogic.com/test/context6");

        URI alice = conn.getValueFactory().createURI("http://example.org/people/alice");
        URI bob = conn.getValueFactory().createURI("http://example.org/people/bob");
        URI name = conn.getValueFactory().createURI("http://example.org/ontology/name");
        URI person = conn.getValueFactory().createURI("http://example.org/ontology/Person");
        Literal bobsName = conn.getValueFactory().createLiteral("Bob");
        Literal alicesName = conn.getValueFactory().createLiteral("Alice");

        conn.add(alice, RDF.TYPE, person, context5);
        conn.add(alice, name, alicesName,context5, context6);
        conn.add(bob, RDF.TYPE, person, context5);
        conn.add(bob, name, bobsName, context5, context6);

        //TBD- need to be able to set baseURI
        RepositoryResult<Statement> statements = conn.getStatements(alice, null, null, true,context5);

        Model aboutAlice = Iterations.addAll(statements, new LinkedHashModel());

        conn.remove(alice, RDF.TYPE, person, context5);
        conn.remove(alice, name, alicesName, context5, context6);
        conn.remove(bob, RDF.TYPE, person, context5);
        conn.remove(bob, name, bobsName, context5, context6);

        conn.clear(context5,context6);
    }


}
