/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.services.data;

import java.util.List;

import org.kitodo.data.database.beans.SubfolderType;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.persistence.FolderDAO;
import org.kitodo.services.data.base.SearchDatabaseService;

public class FolderService extends SearchDatabaseService<SubfolderType, FolderDAO> {

    /**
     * Creates a new folder service.
     */
    public FolderService() {
        super(new FolderDAO());
    }

    @Override
    public Long countDatabaseRows() throws DAOException {
        return countDatabaseRows("SELECT COUNT(*) FROM Process");
    }

    @Override
    public List<SubfolderType> getAllForSelectedClient() {
        throw new UnsupportedOperationException();
    }
}
