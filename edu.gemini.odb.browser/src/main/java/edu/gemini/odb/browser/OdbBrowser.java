package edu.gemini.odb.browser;

/**
 * Interface for a service that provides candidate programs, targets and observations usind the ODB browser servlet.
 */
public interface OdbBrowser {

    /**
     * Gets a query result from the ODB browser for the query parameters passed in as arguments.
     * @param query
     * @return
     */
    QueryResult query(String query);

}
