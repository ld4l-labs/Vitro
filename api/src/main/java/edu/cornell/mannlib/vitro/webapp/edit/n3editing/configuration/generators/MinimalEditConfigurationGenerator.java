/* $This file is distributed under the terms of the license in /doc/license.txt$ */
package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;


import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.jena.QueryUtils;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationUtils;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.IdModelSelector;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.StandardModelSelector;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors.MinimalConfigurationPreprocessor;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService.ResultFormat;
import edu.cornell.mannlib.vitro.webapp.utils.FrontEndEditingUtils.EditMode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.cornell.mannlib.vitro.webapp.utils.FrontEndEditingUtils;
import edu.cornell.mannlib.vitro.webapp.utils.FrontEndEditingUtils.EditMode;
public class MinimalEditConfigurationGenerator  implements EditConfigurationGenerator{
	
	private Log log = LogFactory.getLog(MinimalEditConfigurationGenerator.class);	
	private String subjectUri = null;
	private String predicateUri = null;
	private String objectUri = null;	
	
	@Override	
	 public EditConfigurationVTwo getEditConfiguration(VitroRequest vreq, HttpSession session) throws Exception {
	    	
		
			EditConfigurationVTwo editConfiguration = new EditConfigurationVTwo();  
			//includes getting the configuration json file associated with this property
			addFormSpecificData(editConfiguration, vreq);
		
			
	    	
	    	//process subject, predicate, object parameters
	    	initProcessParameters(vreq, session, editConfiguration);
	    	
	      	//The most basic configuration for required n3: subject, predicate, object
	    	editConfiguration.setN3Required(this.generateN3Required(vreq));
	    	    	
	    	//No optional N3 as will be specified through the config file and/or interface as required
	    	
	    	//Todo: what do new resources depend on here?
	    	//In original form, these variables start off empty
	    	editConfiguration.setNewResources(new HashMap<String, String>());
	    	//if object doesn't exist, create new one
	    	editConfiguration.addNewResource("objectVar", null);
	    	//In scope
	    	this.setUrisAndLiteralsInScope(editConfiguration);
	    	
	    	//on Form
	    	this.setUrisAndLiteralsOnForm(editConfiguration, vreq);
	    	
	    	editConfiguration.setFilesOnForm(new ArrayList<String>());
	    	
	    	//Sparql queries
	    	this.setSparqlQueries(editConfiguration);
	    	
	    	//set fields
	    	setFields(editConfiguration, vreq, EditConfigurationUtils.getPredicateUri(vreq));
	    	
	    //	No need to put in session here b/c put in session within edit request dispatch controller instead
	    	//placing in session depends on having edit key which is handled in edit request dispatch controller
	    //	editConfiguration.putConfigInSession(editConfiguration, session);

	    	prepareForUpdate(vreq, session, editConfiguration);
	    	
	    
	    	
	    	//Form title and submit label moved to template
	    	editConfiguration.setTemplate("minimalconfigtemplate.ftl");
	    	
	    
	    	
	    	//Set edit key
	    	setEditKey(editConfiguration, vreq);
	    	    	       	    	
	    	
	    	//preprocessor
	    	editConfiguration.addEditSubmissionPreprocessor(new MinimalConfigurationPreprocessor(editConfiguration));
	    	

	    	
	    	return editConfiguration;
	    }
	
	
	  //Form specific data
    public void addFormSpecificData(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
    	 HashMap<String, Object> formSpecificData = new HashMap<String, Object>();
    	 String configFile = getConfigurationFile(vreq, editConfiguration);
         formSpecificData.put("editMode", getEditMode(vreq).name().toLowerCase());
         if(configFile != null) {
        	 formSpecificData.put("configfile", configFile);
         } 
         //Do we have a default for configFile if none is returned?
         editConfiguration.setFormSpecificData(formSpecificData);
    }
	 
			
    //Get the configuration file required
	private String getConfigurationFile(VitroRequest vreq, EditConfigurationVTwo editConfiguration) {
		//Custom form entry predicate: http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot
		String configFilePredicate = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot";
		
		
    	String predicateUri = EditConfigurationUtils.getPredicateUri(vreq);
		
		//Get everything really
		String query = "SELECT ?configFile WHERE {<" + predicateUri + ">  <" + configFilePredicate + "> ?configFile .}";
		 RDFService rdfService = vreq.getRDFService();

		 
	        try {
	        	ResultSet rs = QueryUtils.getQueryResults(query, vreq);
	        	while(rs.hasNext()) {
	        		QuerySolution qs = rs.nextSolution();
	        		Literal configFileLiteral = qs.getLiteral("configFile");
	        		if(configFileLiteral != null && StringUtils.isNotEmpty(configFileLiteral.getString())) {
	        			return configFileLiteral.getString();
	        		}
	        	}
	        	editConfiguration.addFormSpecificData("queryresult", rs);
	        } catch (Exception ex) {
	        	log.error("Exception occurred in query retrieving information for this field", ex);
	        }
		
	      // 
	        return null;
		
	}


		//We only need enough for the error message to show up
		private EditConfigurationVTwo getCustomErrorEditConfiguration(VitroRequest vreq, HttpSession session) {
			EditConfigurationVTwo editConfiguration = new EditConfigurationVTwo();    	
	    	
	    	//process subject, predicate, object parameters
	    	this.initProcessParameters(vreq, session, editConfiguration);
	    	
	    	//uris: subject, predicate. literals = emptyu
	    	this.setUrisAndLiteralsInScope(editConfiguration);
	    	
	    	//Sparql queries - empty lists
	    	this.setSparqlQueries(editConfiguration);
	    	
	    
	    	prepareForUpdate(vreq, session, editConfiguration);
	    	
	    	editConfiguration.setTemplate("customErrorMessages.ftl");
	    	
	    	//Set edit key
	    	setEditKey(editConfiguration, vreq);
	    	
	    	
	    	return editConfiguration;
		}
	    
	    private void setEditKey(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
	    	String editKey = EditConfigurationUtils.getEditKey(vreq);	
	    	editConfiguration.setEditKey(editKey);
	    }
	    
		
		//Initialize setup: process parameters
	    private void initProcessParameters(VitroRequest vreq, HttpSession session, EditConfigurationVTwo editConfiguration) {
	    	String formUrl = EditConfigurationUtils.getFormUrlWithoutContext(vreq);

	    	String subjectUri = EditConfigurationUtils.getSubjectUri(vreq);
	    	String predicateUri = EditConfigurationUtils.getPredicateUri(vreq);
	    
	    	editConfiguration.setFormUrl(formUrl);
	    	
	    	editConfiguration.setUrlPatternToReturnTo("/individual");
	    	
	    	editConfiguration.setVarNameForSubject("subject");
	    	editConfiguration.setSubjectUri(subjectUri);
	    	editConfiguration.setEntityToReturnTo(subjectUri);
	    	editConfiguration.setVarNameForPredicate("predicate");
	    	editConfiguration.setPredicateUri(predicateUri);
	    	
	    	//Don't expect custom data property forms, so defaulting to object property for now
	    
	    	//"object"       : [ "objectVar" ,  "${objectUriJson}" , "URI"],
	    	if(EditConfigurationUtils.isObjectProperty(predicateUri, vreq)) {
	    		log.debug("This is an object property: " + predicateUri);
	    		this.initObjectParameters(vreq);
	    		this.processObjectPropForm(vreq, editConfiguration);
	    	} else {
	    		log.debug("This is a data property: " + predicateUri);
	    		return;
	    	}
	    }    

	    
		private void initObjectParameters(VitroRequest vreq) {
			//in case of object property
	    	objectUri = EditConfigurationUtils.getObjectUri(vreq);
		}

		private void processObjectPropForm(VitroRequest vreq, EditConfigurationVTwo editConfiguration) {
	    	editConfiguration.setVarNameForObject("objectVar");    	
	    	editConfiguration.setObject(objectUri);
	    	//this needs to be set for the editing to be triggered properly, otherwise the 'prepare' method
	    	//pretends this is a data property editing statement and throws an error
	    	//TODO: Check if null in case no object uri exists but this is still an object property
	    }
	       
	    //Get N3 required 
	    //Handles both object and data property    
	    private List<String> generateN3Required(VitroRequest vreq) {
	    	List<String> n3ForEdit = new ArrayList<String>();
	    	String editString = "?subject ?predicate ";    	
	    	editString += "?objectVar";    	
	    	editString += " .";
	    	n3ForEdit.add(editString);
	    	return n3ForEdit;
	    }

	    
	    //Set queries
	    private String retrieveQueryForInverse () {
	    	String queryForInverse =  "PREFIX owl:  <http://www.w3.org/2002/07/owl#>"
				+ " SELECT ?inverse_property "
				+ "    WHERE { ?inverse_property owl:inverseOf ?predicate } ";
	    	return queryForInverse;
	    }
	    
	    //Sets the bare minimum in scope: subject and predicate
	    private void setUrisAndLiteralsInScope(EditConfigurationVTwo editConfiguration) {
	    	HashMap<String, List<String>> urisInScope = new HashMap<String, List<String>>();
	    	//note that at this point the subject, predicate, and object var parameters have already been processed
	    	urisInScope.put(editConfiguration.getVarNameForSubject(), 
	    			Arrays.asList(new String[]{editConfiguration.getSubjectUri()}));
	    	urisInScope.put(editConfiguration.getVarNameForPredicate(), 
	    			Arrays.asList(new String[]{editConfiguration.getPredicateUri()}));
	    
	    	editConfiguration.setUrisInScope(urisInScope);
	    	//Uris in scope include subject, predicate, and object var
	    	//This is an empty array
	    	editConfiguration.setLiteralsInScope(new HashMap<String, List<Literal>>());
	    }
	    
	    //n3 should look as follows
	    //?subject ?predicate ?objectVar 
	    
	    private void setUrisAndLiteralsOnForm(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
	    	List<String> urisOnForm = new ArrayList<String>();
	    	List<String> literalsOnForm = new ArrayList<String>();
	    	
	    	//uris on form should be empty if data property
	    	urisOnForm.add("objectVar");
	    	
	    	editConfiguration.setUrisOnform(urisOnForm);
	    	editConfiguration.setLiteralsOnForm(literalsOnForm);
	    }
	        
	    //This is for various items
	    private void setSparqlQueries(EditConfigurationVTwo editConfiguration) {
	    	//Sparql queries defining retrieval of literals etc.
	    	editConfiguration.setSparqlForAdditionalLiteralsInScope(new HashMap<String, String>());
	    	
	    	Map<String, String> urisInScope = new HashMap<String, String>();
	    	urisInScope.put("inverseProp", this.retrieveQueryForInverse());
	    	editConfiguration.setSparqlForAdditionalUrisInScope(urisInScope);
	    	
	    	editConfiguration.setSparqlForExistingLiterals(generateSparqlForExistingLiterals());
	    	editConfiguration.setSparqlForExistingUris(generateSparqlForExistingUris());
	    }
	    
	    
	    //Get page uri for object
	    private HashMap<String, String> generateSparqlForExistingUris() {
	    	HashMap<String, String> map = new HashMap<String, String>();
	    	return map;
	    }
	    
	    private HashMap<String, String> generateSparqlForExistingLiterals() {
	    	HashMap<String, String> map = new HashMap<String, String>();
	    	return map;
	    }
	    
	    //
	    protected void setFields(EditConfigurationVTwo editConfiguration, VitroRequest vreq, String predicateUri) throws Exception {
	      
			FieldVTwo field = new FieldVTwo();
	    	field.setName("objectVar");    	
	    
	    	
	    	Map<String, FieldVTwo> fields = new HashMap<String, FieldVTwo>();
	    	fields.put(field.getName(), field);    	
	    	    	    	
	    	editConfiguration.setFields(fields);
	    }       

		private void prepareForUpdate(VitroRequest vreq, HttpSession session, EditConfigurationVTwo editConfiguration) {
	    	//Here, retrieve model from 
			OntModel model = ModelAccess.on(session.getServletContext()).getOntModel();
	    	//if object property
	    	if(EditConfigurationUtils.isObjectProperty(EditConfigurationUtils.getPredicateUri(vreq), vreq)){
		    	Individual objectIndividual = EditConfigurationUtils.getObjectIndividual(vreq);
		    	if(objectIndividual != null) {
		    		//update existing object
		    		editConfiguration.prepareForObjPropUpdate(model);
		    	}  else {
		    		//new object to be created
		            editConfiguration.prepareForNonUpdate( model );
		        }
	    	} else {
	    	    throw new Error("DefaultObjectPropertyForm does not handle data properties.");
	    	}
	    }
	      

	    
	
	        			
	

		public String getSubjectUri() {
			return subjectUri;
		}
		
		public String getPredicateUri() {
			return predicateUri;
		}
		
		public String getObjectUri() {
			return objectUri;
		}
		

		/** get the auto complete edit mode */
		public EditMode getEditMode(VitroRequest vreq) {
			//In this case, the original jsp didn't rely on FrontEndEditingUtils
			//but instead relied on whether or not the object Uri existed
			String objectUri = EditConfigurationUtils.getObjectUri(vreq);
			EditMode editMode = FrontEndEditingUtils.EditMode.ADD;
			if(objectUri != null && !objectUri.isEmpty()) {
				editMode = FrontEndEditingUtils.EditMode.EDIT;
				
			}
			return editMode;
		}
	    
		public String getSparqlForAcFilter(VitroRequest vreq) {
			String subject = EditConfigurationUtils.getSubjectUri(vreq);			
			String predicate = EditConfigurationUtils.getPredicateUri(vreq);
			//Get all objects for existing predicate, filters out results from addition and edit
			String query =  "SELECT ?objectVar WHERE { " + 
				"<" + subject + "> <" + predicate + "> ?objectVar .} ";
			return query;
		}
			
  
}
