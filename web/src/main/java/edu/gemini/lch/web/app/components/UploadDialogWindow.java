package edu.gemini.lch.web.app.components;

import com.vaadin.ui.*;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.services.LaserNightService;
import edu.gemini.lch.web.app.windows.night.NightWindow;
import edu.gemini.odb.browser.QueryResult;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 */
public class UploadDialogWindow extends Window implements Upload.SucceededListener, Upload.FailedListener, Upload.Receiver{

    private final UI parent;
    private final Upload upload;
    private ByteArrayOutputStream out;
    private final LaserNight laserNight;
    private final LaserNightService laserNightService;
    private final NightWindow nightWindow;

    public UploadDialogWindow(String caption, NightWindow nightWindow, LaserNight night, LaserNightService laserNightService) {
        this.parent = nightWindow.getUI();
        this.nightWindow = nightWindow;
        this.laserNight = night;
        this.laserNightService = laserNightService;
        this.upload = new Upload(caption, this);
        this.upload.addSucceededListener(this);
        this.upload.addFailedListener(this);
        this.upload.setReceiver(this);

        setCaption(caption);
        setWidth("500px");
        setContent(upload);

        parent.addWindow(this);
        setModal(true);
    }

    @Override
    public void uploadFailed(Upload.FailedEvent event) {
        parent.removeWindow(this);
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        parent.removeWindow(this);
        out = new ByteArrayOutputStream(100000);
        return out;
    }

    @Override
    public void uploadSucceeded(Upload.SucceededEvent event) {
        parent.removeWindow(this);

        try {
            JAXBContext context = JAXBContext.newInstance(QueryResult.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            QueryResult result = (QueryResult)unmarshaller.unmarshal(in);
            laserNightService.updateLaserNight(laserNight, result, new QueryResult());
            // reload and update
            nightWindow.setNight(laserNight.getId());

        }  catch (JAXBException e) {
            throw new RuntimeException("could not connect with server", e);
        }
    }

}
