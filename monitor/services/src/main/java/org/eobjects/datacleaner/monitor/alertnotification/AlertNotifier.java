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
package org.eobjects.datacleaner.monitor.alertnotification;

import java.util.Map;

import org.eobjects.datacleaner.monitor.configuration.ResultContext;
import org.eobjects.datacleaner.monitor.scheduling.model.AlertDefinition;
import org.eobjects.datacleaner.monitor.scheduling.model.ExecutionLog;
import org.apache.metamodel.util.Ref;

/**
 * Interface for components that listen for executions and notifies in case any
 * alerts was raised..
 */
public interface AlertNotifier {

    public void onExecutionFinished(ExecutionLog execution, Ref<Map<AlertDefinition, Number>> activeAlerts,
            ResultContext result);
}
