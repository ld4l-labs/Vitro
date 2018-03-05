/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import edu.cornell.mannlib.vitro.testing.AbstractTestClass;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.InvalidConfigFileException;

/**
 * TODO
 */
public class ConfigFileImplTest extends AbstractTestClass {
    
    @Test
    public void parseExample() throws IOException, InvalidConfigFileException {
        ConfigFileImpl configFile = ConfigFileImpl.parse(getContentsOfFile("example1.jsonld"));
        System.out.println(configFile);
        fail("parseExample not implemented");
    }
    
    // ----------------------------------------------------------------------
    // Helper methods
    // ----------------------------------------------------------------------

    private String getContentsOfFile(String filename) throws IOException {
        InputStream stream = this.getClass().getResourceAsStream(filename);
        if (stream == null) {
            throw new FileNotFoundException("Didn't find local resource: " + filename);
        }
        return IOUtils.toString(stream, Charset.defaultCharset());
    }
    
}
