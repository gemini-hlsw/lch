package edu.gemini.odb.browser;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.net.ConnectException;
import java.net.UnknownHostException;

/**
 * Implementation of OdbBrowser interface which accesses an actual ODB using the OdbQuery servlet (lchquery).
 */
public class OdbBrowserImpl implements OdbBrowser {

    /** {@inheritDoc} */
    @Override public QueryResult query(String query) {

        try {
            Client client = Client.create();

            // TODO: pass down url and list of parameters form above, don's split up stuff here
            String[] mainParts = query.split("\\?");
            WebResource webResource = client.resource(mainParts[0]);
            for (String parameter : mainParts[1].split(",")) {
                String[] parts = parameter.split("=");
                webResource = webResource.queryParam(parts[0], parts[1]);
            }

            ClientResponse response = webResource.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
            }

            QueryResult result = webResource.accept(MediaType.APPLICATION_XML_TYPE).get(QueryResult.class);
            return result;

        } catch (RuntimeException e) {
            // make error message a bit more user friendly for some common cases and re-throw
            // (the error message of the exception thrown here will be presented to the user)
            // NOTE: do NOT swallow exception because this will interfere with transaction handling!
            if (e.getCause() instanceof UnknownHostException) {
                throw new RuntimeException("The configured ODB host name is invalid: " + e.getCause().getMessage(), e);
            } else if (e.getCause() instanceof ConnectException) {
                throw new RuntimeException("Could not connect to ODB (check port number): " + e.getCause().getMessage(), e);
            // ok, don't know what happened, pass this on and let the user sort it out
            } else {
                throw e;
            }
        }

    }
}
