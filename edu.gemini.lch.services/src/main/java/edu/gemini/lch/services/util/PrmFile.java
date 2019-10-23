package edu.gemini.lch.services.util;

import edu.gemini.lch.configuration.Configuration;
import edu.gemini.lch.model.AzElLaserTarget;
import edu.gemini.lch.model.LaserNight;
import edu.gemini.lch.model.LaserTarget;
import edu.gemini.lch.model.RaDecLaserTarget;
import edu.gemini.lch.services.impl.Factory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * PRM file creation helper class.
 */
public interface PrmFile {

    /**
     * Helper class for creation of RaDec PRM files.
     */
    @Component("prmFileRaDec")
    @Scope("prototype")
    class RaDec extends Creator {
        public RaDec(LaserNight night, Integer maxTargets) {
            super(night, maxTargets);
        }
        @PostConstruct
        private void init() {
            this.engine = factory.createTemplateEngine(night);
            this.engine.addReplacement("TARGET-TYPE", "RADEC");
            createFiles(night.getRaDecLaserTargets());
        }
        protected String fillLaserTargetTemplate(LaserTarget target) {
            this.engine.setLaserTarget((RaDecLaserTarget) target);
            return engine.fillTemplate(Configuration.Value.PRM_RADEC_TARGET_TEMPLATE);
        }
    }

    /**
     * Helper class for creation of AzEl PRM files.
     */
    @Component("prmFileAzEl")
    @Scope("prototype")
    class AzEl extends Creator{
        public AzEl(LaserNight night, Integer maxTargets) {
            super(night, maxTargets);
        }
        @PostConstruct
        private void init() {
            this.engine = factory.createTemplateEngine(night);
            this.engine.addReplacement("TARGET-TYPE", "AZEL");
            createFiles(night.getAzElLaserTargets());
        }
        protected String fillLaserTargetTemplate(LaserTarget target) {
            this.engine.setLaserTarget((AzElLaserTarget) target);
            return engine.fillTemplate(Configuration.Value.PRM_AZEL_TARGET_TEMPLATE);
        }
    }

    abstract class Creator {
        @Resource
        protected Factory factory;

        protected final LaserNight night;
        protected final Set<File> files;
        protected final Integer maxTargets;
        protected TemplateEngine engine;

        protected abstract String fillLaserTargetTemplate(LaserTarget target);

        protected Creator(LaserNight night, Integer maxTargets) {
            this.night = night;
            this.maxTargets = maxTargets;
            this.files = new HashSet<>();
        }

        public Set<File> getPrmFiles() {
            return files;
        }

        protected void createFiles(Collection<? extends LaserTarget> targets) {
            int targetCount = 0;
            int fileNumber = 1;
            StringBuffer file = null;
            String fileName = "";
            for (LaserTarget t : targets) {
                if (file == null) {
                    // -- no current file, start a new one
                    file = new StringBuffer();

                    // -- for files we have two special replacements the template engine has to know about
                    engine.addReplacement("FILE-NUMBER", targets.size() > maxTargets ? "."+fileNumber : "");
                    // filename uses file count, make sure file count is defined before constructing the file name
                    fileName = engine.fillTemplate(Configuration.Value.PRM_FILENAME_TEMPLATE);
                    engine.addReplacement("FILE-NAME", fileName);

                    String prmHeader = engine.fillTemplate(Configuration.Value.PRM_HEADER_TEMPLATE);
                    file.append(prmHeader);
                }

                // -- appending another laser target
                file.append(fillLaserTargetTemplate(t));
                targetCount++;

                if (targetCount % maxTargets == 0 || targetCount == targets.size()) {
                    // -- either the current file ends here ..
                    fileNumber++;
                    String prmFooter = engine.fillTemplate(Configuration.Value.PRM_FOOTER_TEMPLATE);
                    file.append(prmFooter);
                    files.add(new File(fileName, file.toString()));
                    file = null;
                } else {
                    // -- .. or we write the separator and are ready for another target
                    String targetSeparator = engine.fillTemplate(Configuration.Value.PRM_TARGET_SEPARATOR_TEMPLATE);
                    file.append(targetSeparator);
                }
            }
        }
    }

    /**
     * PRM file representation holding the file and the file name.
     */
    class File {
        private final String name;
        private final String file;

        public File(String name, String file)  {
            this.name = name;
            this.file = file;
        }

        public String getName() {
            return name;
        }

        public String getFile() {
            return file;
        }
    }
}
