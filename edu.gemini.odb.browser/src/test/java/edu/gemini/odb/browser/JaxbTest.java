package edu.gemini.odb.browser;

import org.junit.Assert;
import org.junit.Test;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

/**
 * NOTE: IF THIS DOES NOT COMPILE RUN "mvn compile" FIRST FOR THIS MODULE IN ORDER TO CREATE OBJECTS FROM SCHEMA.
 * Simple JAXB test, mostly meant to be used as documentation on how to use JAXB.
 */
public class JaxbTest {

    /**
     * JAXB Usage example
     * @throws Exception
     */
    @Test
    public void canMarshallAndUnmarshall() throws Exception {

        HmsDms hmsDms = new HmsDms();
        hmsDms.setRa("00:00:00");
        hmsDms.setDec("00:00:00");

        Sidereal sideral = new Sidereal();
        sideral.setName("My Name");
        sideral.setHmsDms(hmsDms);

        NonSidereal nonSidereal = new NonSidereal();
        nonSidereal.setName("Jupiter");
        nonSidereal.setHorizonsObjectId("MajorBody_599");

        TargetsNode targets = new TargetsNode();
        targets.getTargets().add(sideral);
        targets.getTargets().add(nonSidereal);

        Conditions c = new Conditions();
        TimingWindowsNode tw = new TimingWindowsNode();
        tw.getTimingWindows().add(new TimingWindow());
        c.setTimingWindowsNode(tw);

        Observation observation = new Observation();
        observation.setName("My Name");
        observation.setStatus("Ready");
        // etc..
        observation.setTargetsNode(targets);
        observation.setConditions(c);

        ObservationsNode observations = new ObservationsNode();
        observations.getObservations().add(observation);

        Program program = new Program();
        program.setSemester("2012B");
        program.setTitle("My Title");
        program.setActive("Yes");
        // etc..
        program.setObservationsNode(observations);

        ProgramsNode programs = new ProgramsNode();
        programs.getPrograms().add(program);

        QueryResult query = new QueryResult();
        query.setProgramsNode(programs);


        // ====================================
        // HOW TO READ AND WRITE XML USING JAXB
        // ====================================
        JAXBContext context = JAXBContext.newInstance(QueryResult.class);
        Marshaller marshaller = context.createMarshaller();
        Unmarshaller unmarshaller = context.createUnmarshaller();

        ByteArrayOutputStream out = new ByteArrayOutputStream(10000);
        marshaller.marshal(query, out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        QueryResult result = (QueryResult)unmarshaller.unmarshal(in);
        // ====================================

    }

    /**
     * Validates that the query example is valid.
     * @throws Exception
     */
    @Test
    public void validateQueryExample() throws Exception {
        Query query = (Query) validateExample(Query.class, "/xsd/query.xsd", "/queryExample.xml");
        Assert.assertEquals(query.getProgram().getSemester(), "2012B");
    }

    /**
     * Validates that the result example is valid.
     * @throws Exception
     */
    @Test
    public void validateResultExample() throws Exception {
        QueryResult result = (QueryResult) validateExample(QueryResult.class, "/xsd/queryResult.xsd", "/queryResultExample.xml");
        Assert.assertEquals(result.getProgramsNode().getPrograms().get(0).getSemester(), "2012B");

        // check some access
        ProgramsNode programs = result.getProgramsNode();
        Program program = programs.getPrograms().get(0);
        ObservationsNode observationsNode = program.getObservationsNode();
        Observation observation = observationsNode.getObservations().get(0);
        TargetsNode targets = observation.getTargetsNode();
        List<Serializable> targetsList = targets.getTargets();
        for (Object t : targetsList) {
            Assert.assertTrue(t instanceof Sidereal || t instanceof NonSidereal);
        }

    }

    // -- helpers

    private Object validateExample(Class clazz, String schema, String example) throws Exception {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        unmarshaller.setSchema(getSchema(schema));

        try (InputStream in = getClass().getResourceAsStream(example)) {
            return unmarshaller.unmarshal(in);
        }
    }


    private Schema getSchema(String schemaName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(schemaName)) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            //factory.setResourceResolver(createResourceResolver());
            return factory.newSchema(new StreamSource(in));
        }
    }
}
