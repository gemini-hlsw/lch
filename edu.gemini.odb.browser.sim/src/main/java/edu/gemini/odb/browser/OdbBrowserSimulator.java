package edu.gemini.odb.browser;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

/**
 * Simple simulator for development and testing.
 */
public class OdbBrowserSimulator implements OdbBrowser {

    @Override
    public QueryResult query(String query) {

        // create a simple manual result
        if (query == null || query.isEmpty()) {
            return createSillyDefaultResult();
        }

        // create a more elaborate return which is read from a data file
        InputStream in = getClass().getResourceAsStream("/" + query);
        try {
            JAXBContext context = JAXBContext.newInstance(QueryResult.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            QueryResult result = (QueryResult)unmarshaller.unmarshal(in);
            in.close();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private QueryResult createSillyDefaultResult() {
        HmsDms dd = new HmsDms();
        dd.setRa("5:00:00");
        dd.setDec("+19:49:26");
        Sidereal s = new Sidereal();
        s.setName("Sidereal Target A");
        s.setHmsDms(dd);

        NonSidereal nonS = new NonSidereal();
        nonS.setHorizonsObjectId("MajorBody_599");
        nonS.setName("Jupiter");

        TargetsNode tl = new TargetsNode();
        tl.getTargets().add(s);
        tl.getTargets().add(nonS);

        Observation observation = new Observation();
        observation.setTargetsNode(tl);

        ObservationsNode observationsNode = new ObservationsNode();
        observationsNode.getObservations().add(observation);

        Program p = new Program();
        p.setReference("Reference");
        p.setObservationsNode(observationsNode);

        ProgramsNode pl = new ProgramsNode();
        pl.getPrograms().add(p);

        QueryResult result = new QueryResult();
        result.setProgramsNode(pl);

        return result;
    }
}
