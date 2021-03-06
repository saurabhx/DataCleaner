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
package org.eobjects.datacleaner.windows;

import javax.inject.Inject;
import javax.swing.filechooser.FileFilter;

import org.eobjects.analyzer.connection.ExcelDatastore;
import org.eobjects.datacleaner.bootstrap.WindowContext;
import org.eobjects.datacleaner.guice.Nullable;
import org.eobjects.datacleaner.user.MutableDatastoreCatalog;
import org.eobjects.datacleaner.user.UserPreferences;
import org.eobjects.datacleaner.util.FileFilters;
import org.eobjects.datacleaner.util.IconUtils;
import org.eobjects.datacleaner.widgets.FilenameTextField;
import org.apache.metamodel.util.FileResource;

public final class ExcelDatastoreDialog extends AbstractFileBasedDatastoreDialog<ExcelDatastore> {

	private static final long serialVersionUID = 1L;

	@Inject
	protected ExcelDatastoreDialog(@Nullable ExcelDatastore originalDatastore,
			MutableDatastoreCatalog mutableDatastoreCatalog, WindowContext windowContext, UserPreferences userPreferences) {
		super(originalDatastore, mutableDatastoreCatalog, windowContext, userPreferences);
	}

	@Override
	protected void setFileFilters(FilenameTextField filenameField) {
		FileFilter combinedFilter = FileFilters.combined("Any Excel Spreadsheet (.xls, .xlsx)", FileFilters.XLS,
				FileFilters.XLSX);
		filenameField.addChoosableFileFilter(combinedFilter);
		filenameField.addChoosableFileFilter(FileFilters.XLS);
		filenameField.addChoosableFileFilter(FileFilters.XLSX);
		filenameField.addChoosableFileFilter(FileFilters.ALL);
		filenameField.setSelectedFileFilter(combinedFilter);
	}

	@Override
	protected String getBannerTitle() {
		return "MS Excel spreadsheet";
	}

	@Override
	public String getWindowTitle() {
		return "Excel spreadsheet | Datastore";
	}

	@Override
	protected ExcelDatastore createDatastore(String name, String filename) {
		return new ExcelDatastore(name, new FileResource(filename), filename);
	}

	@Override
	protected String getDatastoreIconPath() {
		return IconUtils.EXCEL_IMAGEPATH;
	}
}
