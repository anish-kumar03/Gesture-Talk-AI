package com.gesturetalk.dao;

import com.gesturetalk.model.GestureHistory;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for gesture recognition audit records.
 */
public interface HistoryDAO {

    /**
     * Inserts a new gesture recognition record.
     *
     * @param history record to insert
     * @return generated ID
     * @throws SQLException if a database access error occurs
     */
    long insert(GestureHistory history) throws SQLException;

    /**
     * Retrieves recent recognition records up to specified limit.
     *
     * @param limit maximum records
     * @return list of matching records
     * @throws SQLException if a database access error occurs
     */
    List<GestureHistory> findRecent(int limit) throws SQLException;

    /**
     * Retrieves recent records filtered by gesture code.
     *
     * @param gestureCode gesture code or "ALL"
     * @param limit maximum records
     * @return list of matching records
     * @throws SQLException if a database access error occurs
     */
    List<GestureHistory> findByGesture(String gestureCode, int limit) throws SQLException;

    /**
     * Retrieves a record by ID.
     *
     * @param id record primary key
     * @return Optional containing the record if found
     * @throws SQLException if a database access error occurs
     */
    Optional<GestureHistory> findById(long id) throws SQLException;

    /**
     * Deletes all history records.
     *
     * @return number of deleted records
     * @throws SQLException if a database access error occurs
     */
    int deleteAll() throws SQLException;

    /**
     * Counts the total number of history records.
     *
     * @return count
     * @throws SQLException if a database access error occurs
     */
    int count() throws SQLException;
}
