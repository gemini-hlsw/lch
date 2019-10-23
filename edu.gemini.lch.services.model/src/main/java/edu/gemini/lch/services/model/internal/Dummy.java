package edu.gemini.lch.services.model.internal;

/**
 * This class' single purpose is to make some warning messages of the mvn-bundle-plugin go away.
 * (It does not like empty packages.) As soon as there are "real" classes in the internal package
 * this class can be safely deleted. I prefer this to manually change the export/private configuration
 * for different packages in case we have more OSGi bundles in the future - they should all follow the
 * same convention.
 */
public class Dummy {
}
