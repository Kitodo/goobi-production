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

package org.kitodo.production.services.data;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.kitodo.data.database.beans.Comment;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.persistence.CommentDAO;
import org.kitodo.production.services.data.base.SearchDatabaseService;
import org.primefaces.model.SortOrder;

public class CommentService extends SearchDatabaseService<Comment, CommentDAO> {

    private static CommentService instance = null;

    /**
     * Constructor.
     */
    private CommentService() {
        super(new CommentDAO());
    }

    /**
     * Return singleton variable of type TaskService.
     *
     * @return unique instance of TaskService
     */
    public static CommentService getInstance() {
        if (Objects.equals(instance, null)) {
            synchronized (CommentService.class) {
                if (Objects.equals(instance, null)) {
                    instance = new CommentService();
                }
            }
        }
        return instance;
    }

    @Override
    public List<Comment> getAllForSelectedClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List loadData(int first, int pageSize, String sortField, SortOrder sortOrder, Map filters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long countDatabaseRows() throws DAOException {
        return countDatabaseRows("SELECT COUNT(*) FROM Comment");
    }

    @Override
    public Long countResults(Map filters) throws DAOException {
        return countDatabaseRows();
    }

    public List<Comment> getAllCommentsByProcess(Process process) {
        return dao.getAllByProcess(process);
    }
}
