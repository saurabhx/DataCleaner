/**
 * DataCleaner (community edition)
 * Copyright (C) 2013 Human Inference
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.datacleaner.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfigurationImpl;
import org.eobjects.analyzer.connection.Datastore;
import org.eobjects.analyzer.connection.DatastoreCatalogImpl;
import org.eobjects.analyzer.connection.JdbcDatastore;
import org.eobjects.analyzer.connection.PojoDatastore;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.builder.AnalysisJobBuilder;
import org.eobjects.analyzer.job.builder.AnalyzerJobBuilder;
import org.eobjects.analyzer.job.builder.TransformerJobBuilder;
import org.eobjects.analyzer.job.runner.AnalysisResultFuture;
import org.eobjects.analyzer.job.runner.AnalysisRunnerImpl;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.datacleaner.util.PreviewTransformedDataAnalyzer;
import org.apache.metamodel.pojo.MapTableDataProvider;
import org.apache.metamodel.pojo.TableDataProvider;
import org.apache.metamodel.util.SimpleTableDef;

public class LuceneIntegrationTest extends TestCase {

    private static final String[] AVAILABLE_FIELD_NAMES = new String[] { "name", "country", "phone" };

    public void testScenario() throws Exception {
        final Datastore orderdb = new JdbcDatastore("orderdb", "jdbc:hsqldb:res:orderdb;readonly=true",
                "org.hsqldb.jdbcDriver");

        AnalyzerBeansConfigurationImpl conf = new AnalyzerBeansConfigurationImpl().replace(new DatastoreCatalogImpl(
                orderdb));

        final SearchIndex searchIndex = runWriteSearchIndexJob(conf);
        runSearchIndexAssertions(searchIndex);

        final SimpleTableDef tableDef = new SimpleTableDef("inputtable", new String[] { "searchinput" });
        final Collection<Map<String, ?>> searchInput = new ArrayList<Map<String, ?>>();
        searchInput.add(createSearchInput("Atelier"));
        searchInput.add(createSearchInput("Foobar"));
        searchInput.add(createSearchInput("Rochelle France"));
        searchInput.add(createSearchInput("Land of Toys Inc."));
        searchInput.add(createSearchInput("Signal Gift Stores"));
        searchInput.add(createSearchInput("Signal"));
        searchInput.add(createSearchInput("Gift"));
        searchInput.add(createSearchInput("Atelier Graphick"));
        searchInput.add(createSearchInput("Ateler Graphick"));
        
        final TableDataProvider<?> tableDataProvider = new MapTableDataProvider(tableDef, searchInput);
        final Datastore searches = new PojoDatastore("searches",
                Arrays.<TableDataProvider<?>> asList(tableDataProvider));
        conf = conf.replace(new DatastoreCatalogImpl(searches));

        runSearchJob(conf, searchIndex);
        
        assertEquals("[name, country, phone]", searchIndex.getFieldNames().toString());
    }

    private void runSearchIndexAssertions(SearchIndex searchIndex) throws CorruptIndexException, IOException {
        final AbstractSearchIndex abstractSearchIndex = (AbstractSearchIndex) searchIndex;
        final DirectoryReader reader = abstractSearchIndex.getIndexReader();

        final Document document = reader.document(0);
        assertEquals("[Atelier graphique]", Arrays.toString(document.getValues("name")));
        assertEquals("{name=Atelier graphique, country=France, phone=40.32.2555}", SearchToMapTransformer.toMap(document)
                .toString());
        assertEquals("{name=Signal Gift Stores, country=USA, phone=7025551838}",
                SearchToMapTransformer.toMap(reader.document(1)).toString());
        assertEquals("{name=Australian Collectors, Co., country=Australia, phone=03 9520 4555}", SearchToMapTransformer
                .toMap(reader.document(2)).toString());
        assertEquals("{name=La Rochelle Gifts, country=France, phone=40.67.8555}",
                SearchToMapTransformer.toMap(reader.document(3)).toString());
    }

    private Map<String, ?> createSearchInput(String text) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("searchinput", text);
        return map;
    }

    private void runSearchJob(AnalyzerBeansConfiguration conf, SearchIndex searchIndex) {
        final AnalysisJobBuilder analysisJobBuilder = new AnalysisJobBuilder(conf);
        analysisJobBuilder.setDatastore("searches");
        analysisJobBuilder.addSourceColumns("searchinput");

        final TransformerJobBuilder<SearchToMapTransformer> transformer = analysisJobBuilder
                .addTransformer(SearchToMapTransformer.class);
        transformer.getConfigurableBean().searchIndex = searchIndex;
        transformer.addInputColumns(analysisJobBuilder.getSourceColumns());
        transformer.getOutputColumns().get(0).setName("out1");
        transformer.getOutputColumns().get(1).setName("out2");

        final AnalyzerJobBuilder<PreviewTransformedDataAnalyzer> analyzer = analysisJobBuilder
                .addAnalyzer(PreviewTransformedDataAnalyzer.class);
        analyzer.addInputColumns(analysisJobBuilder.getSourceColumns());
        analyzer.addInputColumns(transformer.getOutputColumns());

        final AnalysisJob job = analysisJobBuilder.toAnalysisJob();
        analysisJobBuilder.close();

        final AnalysisResultFuture future = new AnalysisRunnerImpl(conf).run(job);
        if (future.isErrornous()) {
            Throwable error = future.getErrors().get(0);
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new IllegalStateException(error);
        }

        final List<AnalyzerResult> results = future.getResults();
        assertEquals(1, results.size());

        final PreviewTransformedDataAnalyzer result = (PreviewTransformedDataAnalyzer) results.get(0);
        final List<Object[]> list = result.getList();

        assertEquals(9, list.size());
        assertEquals("[Atelier, {name=Atelier graphique, country=France, phone=40.32.2555}, 2.6278064]",
                Arrays.toString(list.get(0)));
        assertEquals("[Foobar, null, 0]", Arrays.toString(list.get(1)));
        assertStartsWith("[Rochelle France, {name=La Rochelle Gifts, country=France, phone=40.67.8555}, 3.",
                Arrays.toString(list.get(2)));
        assertStartsWith("[Land of Toys Inc., {name=Land of Toys Inc., country=USA, phone=2125557818}, 3.",
                Arrays.toString(list.get(3)));
        assertStartsWith("[Signal Gift Stores, {name=Signal Gift Stores, country=USA, phone=7025551838}, 3.",
                Arrays.toString(list.get(4)));
        assertStartsWith("[Signal, {name=Signal Gift Stores, country=USA, phone=7025551838}, 2.",
                Arrays.toString(list.get(5)));
        assertStartsWith("[Gift, {name=Signal Gift Stores, country=USA, phone=7025551838}, 1.7",
                Arrays.toString(list.get(6)));
        assertStartsWith("[Atelier Graphick, {name=Atelier graphique, country=France, phone=40.32.2555}, 0.8",
                Arrays.toString(list.get(7)));

        // TODO: This could be improved
        assertEquals("[Ateler Graphick, null, 0]", Arrays.toString(list.get(8)));
    }

    private void assertStartsWith(String expectedStartsWith, String actualString) {
        if (!actualString.startsWith(expectedStartsWith)) {
            assertEquals(expectedStartsWith, actualString);
        }
    }

    private SearchIndex runWriteSearchIndexJob(AnalyzerBeansConfiguration conf) {
        final AnalysisJobBuilder analysisJobBuilder = new AnalysisJobBuilder(conf);
        analysisJobBuilder.setDatastore("orderdb");
        analysisJobBuilder.addSourceColumns("CUSTOMERS.CUSTOMERNAME", "CUSTOMERS.COUNTRY", "CUSTOMERS.PHONE");

        final AnalyzerJobBuilder<WriteSearchIndexAnalyzer> analyzer = analysisJobBuilder
                .addAnalyzer(WriteSearchIndexAnalyzer.class);

        final InMemorySearchIndex searchIndex = new InMemorySearchIndex("my index");

        analyzer.getConfigurableBean().searchFields = AVAILABLE_FIELD_NAMES;
        analyzer.getConfigurableBean().searchIndex = searchIndex;
        analyzer.addInputColumns(analysisJobBuilder.getSourceColumns());

        final AnalysisJob job = analysisJobBuilder.toAnalysisJob();
        analysisJobBuilder.close();

        final AnalysisResultFuture future = new AnalysisRunnerImpl(conf).run(job);
        if (future.isErrornous()) {
            Throwable error = future.getErrors().get(0);
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new IllegalStateException(error);
        }

        return searchIndex;
        
    }
}
