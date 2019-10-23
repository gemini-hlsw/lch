package edu.gemini.lch.services.simulators;

import edu.gemini.lch.services.impl.LtcsServiceImpl;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Simulator for LTCS service.
 */
@Service
public class LtcsServiceSimulator extends LtcsServiceImpl {

    private static final Logger LOGGER = Logger.getLogger(LtcsServiceSimulator.class);

    @Value("${lch.simulators.input}")
    private String inputFilePath;

    public LtcsServiceSimulator() {
        LOGGER.info("Input file for simulated LTCS values: " + inputFilePath);
    }

    @PostConstruct
    private void init() {
    }

    @PreDestroy
    private void destroy() {
    }

    /**
     * Updates LTCS status every few seconds asynchronously, see @Scheduled annotation on
     * {@link edu.gemini.lch.services.impl.LtcsServiceImpl} for scheduling. The method here overrides
     * the call to the LTCS and instead gets the pending collisions from a text file.
     */
    @Override
    public void update() {
        try {
            File f = new File(inputFilePath + File.separator + "ltcs.txt");
            FileInputStream fin = new FileInputStream(f);
            byte[] buffer = new byte[(int) f.length()];
            new DataInputStream(fin).readFully(buffer);
            fin.close();
            parseResponse(new String(buffer));
        } catch (Exception e) {
            LOGGER.error("could not read input file", e);
        }

    }

}
