/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.controller; 

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import freemarker.template.*;

import edu.cornell.mannlib.vitro.webapp.template.freemarker.FreeMarkerHttpServlet;

public class AboutControllerFreeMarker extends FreeMarkerHttpServlet {
	
	private static final Log log = LogFactory.getLog(AboutControllerFreeMarker.class.getName());
    
    protected String getTitle() {
    	return "About " + portal.getAppName();
    }
    
    protected String getBody() {
        
    	Map body = new HashMap();
    
        // *** RY Velocity works like StringTemplate here: if the value is an empty string,
        // an if test on the variable will succeed, unlike the EL empty operator.
        // Since these methods return nulls rather than empty strings, this is ok here,
        // but in other cases, we might need a utility method that won't put the value
        // in the context if it's an empty string.
        body.put("aboutText", portal.getAboutText());
        body.put("acknowledgeText", portal.getAcknowledgeText()); 
 
        String templateName = "about.ftl";       
        return mergeBodyToTemplate(templateName, body);

    }


}
