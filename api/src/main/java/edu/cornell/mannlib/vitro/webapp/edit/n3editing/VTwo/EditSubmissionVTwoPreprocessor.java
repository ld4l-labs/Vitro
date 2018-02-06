/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo;

import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.MultiValueEditSubmission;

public interface EditSubmissionVTwoPreprocessor {
	
    /**
     * Signals a problem when preprocessing a form configuration 
     */
    public static class FormConfigurationException extends Exception {
        private static final long serialVersionUID = 1L;

        public FormConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }

        public FormConfigurationException(String message) {
            super(message);
        }

        public FormConfigurationException(Throwable cause) {
            super(cause);
        }
    } 
    
    /**
     * Signals a problem when preprocessing a form submission 
     */
    public static class FormSubmissionException extends Exception {
        private static final long serialVersionUID = 1L;

        public FormSubmissionException(String message, Throwable cause) {
            super(message, cause);
        }

        public FormSubmissionException(String message) {
            super(message);
        }

        public FormSubmissionException(Throwable cause) {
            super(cause);
        }
    } 
	
	//certain preprocessors might require the vreq - which should be passed at the time this method is executed
    public void preprocess(MultiValueEditSubmission editSubmission, VitroRequest vreq);
}
