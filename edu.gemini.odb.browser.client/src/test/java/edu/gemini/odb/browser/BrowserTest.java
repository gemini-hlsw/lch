package edu.gemini.odb.browser;

import org.junit.Ignore;
import org.junit.Test;

/**
 */
public class BrowserTest {

    @Ignore
    @Test
    public void canDoQuery() throws Exception {
        OdbBrowser browser = new OdbBrowserImpl();
        Query query = new Query();
        QueryResult result = browser.query("xyz");
    }

}
