package edu.gemini.lch.services.model;

import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class JAXBTest {

    @Test
    public void test() throws Exception {

        NightFull night = new NightFull();

        List<LaserTarget> laserTargets = new ArrayList<>();
        laserTargets.add(new LaserTarget(1L, new Coordinates(Coordinates.RADEC, 0.0, 0.0)));
        laserTargets.add(new LaserTarget(2L, new Coordinates(Coordinates.RADEC, 0.0, 0.0)));
        laserTargets.add(new LaserTarget(3L, new Coordinates(Coordinates.AZEL, 0.0, 0.0)));

        Observation observation = new Observation("obsId", new ArrayList<>());
        //observation.setLaserTarget(laserTargets.get(0));

        night.getObservations().add(observation);
        night.getLaserTargets().addAll(laserTargets);

        JAXBContext context = JAXBContext.newInstance(NightFull.class);
        Marshaller marshaller = context.createMarshaller();
        Unmarshaller unmarshaller = context.createUnmarshaller();

        ByteArrayOutputStream out = new ByteArrayOutputStream(10000);
        marshaller.marshal(night, out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        NightFull result = (NightFull)unmarshaller.unmarshal(in);
    }
}
