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
package org.eobjects.datacleaner.util;

import java.util.Arrays;
import java.util.List;

import javax.swing.SwingWorker;

import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;
import org.eobjects.analyzer.data.InputColumn;
import org.eobjects.analyzer.data.InputRow;
import org.eobjects.analyzer.job.AnalysisJob;
import org.eobjects.analyzer.job.AnalyzerJob;
import org.eobjects.analyzer.job.FilterJob;
import org.eobjects.analyzer.job.TransformerJob;
import org.eobjects.analyzer.job.concurrent.PreviousErrorsExistException;
import org.eobjects.analyzer.job.runner.AnalysisJobCancellation;
import org.eobjects.analyzer.job.runner.AnalysisJobMetrics;
import org.eobjects.analyzer.job.runner.AnalysisListener;
import org.eobjects.analyzer.job.runner.AnalysisResultFuture;
import org.eobjects.analyzer.job.runner.AnalysisRunner;
import org.eobjects.analyzer.job.runner.AnalysisRunnerImpl;
import org.eobjects.analyzer.job.runner.AnalyzerMetrics;
import org.eobjects.analyzer.job.runner.RowProcessingMetrics;
import org.eobjects.analyzer.result.AnalyzerResult;
import org.eobjects.analyzer.util.LabelUtils;
import org.eobjects.analyzer.util.SourceColumnFinder;
import org.eobjects.datacleaner.panels.ProgressInformationPanel;
import org.eobjects.datacleaner.windows.ResultWindow;
import org.apache.metamodel.schema.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnalysisRunnerSwingWorker extends SwingWorker<AnalysisResultFuture, Void> implements
        AnalysisListener {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisRunnerSwingWorker.class);
    private final AnalysisRunner _analysisRunner;
    private final AnalysisJob _job;
    private final ResultWindow _resultWindow;
    private final ProgressInformationPanel _progressInformationPanel;
    private AnalysisResultFuture _resultFuture;

    public AnalysisRunnerSwingWorker(AnalyzerBeansConfiguration configuration, AnalysisJob job,
            ResultWindow resultWindow, ProgressInformationPanel progressInformationPanel) {
        _analysisRunner = new AnalysisRunnerImpl(configuration, this);
        _job = job;
        _resultWindow = resultWindow;
        _progressInformationPanel = progressInformationPanel;
    }

    @Override
    protected AnalysisResultFuture doInBackground() throws Exception {
        try {
            _resultFuture = _analysisRunner.run(_job);
            return _resultFuture;
        } catch (final Exception e) {
            logger.error("Unexpected error occurred when invoking run(...) on AnalysisRunner", e);
            errorUknown(_job, e);
            throw e;
        }
    }

    @Override
    public void jobBegin(AnalysisJob job, AnalysisJobMetrics metrics) {
        _progressInformationPanel.addUserLog("Job begin");
    }

    @Override
    public void jobSuccess(AnalysisJob job, AnalysisJobMetrics metrics) {
        _progressInformationPanel.addUserLog("Job success!");
        _progressInformationPanel.onSuccess();
        _resultWindow.setResult(_resultFuture);
    }

    @Override
    public void rowProcessingBegin(final AnalysisJob job, final RowProcessingMetrics metrics) {
        final int expectedRows = metrics.getExpectedRows();
        final Table table = metrics.getTable();
        if (expectedRows == -1) {
            _progressInformationPanel.addUserLog("Starting processing of " + table.getName());
        } else {
            _progressInformationPanel.addUserLog("Starting processing of " + table.getName()
                    + " (approx. " + expectedRows + " rows)");
            _progressInformationPanel.setExpectedRows(table, expectedRows);
        }
    }

    @Override
    public void rowProcessingProgress(AnalysisJob job, final RowProcessingMetrics metrics, final int currentRow) {
//        if (currentRow % 100 != 0) {
//            // quickly discard too chatty updates
//            return;
//        }
        _progressInformationPanel.updateProgress(metrics.getTable(), currentRow);
    }

    @Override
    public void rowProcessingSuccess(AnalysisJob job, final RowProcessingMetrics metrics) {
        _progressInformationPanel.updateProgressFinished(metrics.getTable());
        _progressInformationPanel.addUserLog("Processing of " + metrics.getTable().getName()
                + " finished. Generating results ...");
    }

    @Override
    public void analyzerBegin(AnalysisJob job, final AnalyzerJob analyzerJob, AnalyzerMetrics metrics) {
        _progressInformationPanel.addUserLog("Starting analyzer '" + LabelUtils.getLabel(analyzerJob) + "'");
    }

    @Override
    public void analyzerSuccess(AnalysisJob job, final AnalyzerJob analyzerJob, final AnalyzerResult result) {
        final List<InputColumn<?>> inputColumns = Arrays.asList(analyzerJob.getInput());
        final SourceColumnFinder sourceColumnFinder = new SourceColumnFinder();
        sourceColumnFinder.addSources(job);

        Table table = null;
        String tableName = null;
        for (InputColumn<?> inputColumn : inputColumns) {
            table = sourceColumnFinder.findOriginatingTable(inputColumn);
            if (table != null) {
                tableName = table.getName();
                break;
            }
        }

        assert table != null;

        _progressInformationPanel.addUserLog("Analyzer '" + LabelUtils.getLabel(analyzerJob) + "' finished, adding result to tab of " + tableName);
        _resultWindow.addResult(table, analyzerJob, result);
    }

    @Override
    public void errorInFilter(AnalysisJob job, final FilterJob filterJob, InputRow row, final Throwable throwable) {
        _progressInformationPanel.addUserLog("An error occurred in the filter: " + LabelUtils.getLabel(filterJob),
                throwable, true);
    }

    @Override
    public void errorInTransformer(AnalysisJob job, final TransformerJob transformerJob, InputRow row,
            final Throwable throwable) {
        _progressInformationPanel.addUserLog(
                "An error occurred in the transformer: " + LabelUtils.getLabel(transformerJob), throwable, true);
    }

    @Override
    public void errorInAnalyzer(AnalysisJob job, final AnalyzerJob analyzerJob, InputRow row, final Throwable throwable) {
        _progressInformationPanel.addUserLog("An error occurred in the analyzer: " + LabelUtils.getLabel(analyzerJob),
                throwable, true);
    }

    @Override
    public void errorUknown(AnalysisJob job, final Throwable throwable) {
        if (throwable instanceof AnalysisJobCancellation) {
            _progressInformationPanel.updateProgressCancelled();
            return;
        } else if (throwable instanceof PreviousErrorsExistException) {
            // do nothing
            return;
        }
        _progressInformationPanel.addUserLog("An error occurred in the analysis job!", throwable, true);
    }

    public void cancelIfRunning() {
        if (_resultFuture != null) {
            if (!_resultFuture.isDone()) {
                _resultFuture.cancel();
            }
        }
    }
}
