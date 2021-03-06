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

import java.io.File;

import javax.inject.Inject;

import org.apache.metamodel.util.FileResource;
import org.eobjects.analyzer.connection.JsonDatastore;
import org.eobjects.datacleaner.bootstrap.WindowContext;
import org.eobjects.datacleaner.guice.Nullable;
import org.eobjects.datacleaner.user.MutableDatastoreCatalog;
import org.eobjects.datacleaner.user.UserPreferences;
import org.eobjects.datacleaner.util.FileFilters;
import org.eobjects.datacleaner.util.IconUtils;
import org.eobjects.datacleaner.widgets.FilenameTextField;

/**
 * Dialog for {@link JsonDatastore}s
 */
public final class JsonDatastoreDialog extends AbstractFileBasedDatastoreDialog<JsonDatastore> {

    private static final long serialVersionUID = 1L;

    @Inject
    protected JsonDatastoreDialog(@Nullable JsonDatastore originalDatastore,
            MutableDatastoreCatalog mutableDatastoreCatalog, WindowContext windowContext,
            UserPreferences userPreferences) {
        super(originalDatastore, mutableDatastoreCatalog, windowContext, userPreferences);
    }

    @Override
    protected String getBannerTitle() {
        return "JSON file";
    }

    @Override
    public String getWindowTitle() {
        return "JSON file | Datastore";
    }

    @Override
    protected JsonDatastore createDatastore(String name, String filename) {
        final File file = new File(filename);
        return new JsonDatastore(name, new FileResource(file));
    }

    @Override
    protected String getDatastoreIconPath() {
        return IconUtils.JSON_IMAGEPATH;
    }

    @Override
    protected void setFileFilters(FilenameTextField filenameField) {
        filenameField.addChoosableFileFilter(FileFilters.JSON);
        filenameField.addChoosableFileFilter(FileFilters.ALL);
        filenameField.setSelectedFileFilter(FileFilters.JSON);
    }
}
