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
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors.ModelChangePreprocessor;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelNames;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService.ResultFormat;
import edu.cornell.mannlib.vitro.webapp.utils.FrontEndEditingUtils.EditMode;
import static edu.cornell.mannlib.vitro.webapp.utils.sparqlrunner.SparqlQueryRunner.createSelectQueryContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.cornell.mannlib.vitro.webapp.utils.FrontEndEditingUtils;
import edu.cornell.mannlib.vitro.webapp.utils.FrontEndEditingUtils.EditMode;
public class MinimalEditConfigurationGenerator  implements EditConfigurationGenerator{
	
	private Log log = LogFactory.getLog(MinimalEditConfigurationGenerator.class);	
	private String subjectUri = null;
	private String predicateUri = null;
	private String objectUri = null;	
	private static String classURIParameter = "classURI";
	
	@Override	
	 public EditConfigurationVTwo getEditConfiguration(VitroRequest vreq, HttpSession session) throws Exception {
	    	
		
			EditConfigurationVTwo editConfiguration = new EditConfigurationVTwo();  
			
		
			
	    	
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
	    	
	    
	    	//includes getting the configuration json file associated with this property
			addFormSpecificData(editConfiguration, vreq);
	    	//Form title and submit label moved to template
	    	//Can also assign a custom template and not the minimal template if need be
	    	setTemplate(vreq, editConfiguration);
	    	
	    	
	    
	    	
	    	//Set edit key
	    	setEditKey(editConfiguration, vreq);
	    	    	       	    	
	    	
	    	//preprocessor
	    	editConfiguration.addEditSubmissionPreprocessor(new MinimalConfigurationPreprocessor(editConfiguration));
	    	//Add additional preprocessors, currently only model change preprocessors and only those that don't require additional work with constructors
	    	addAdditionalPreprocessors(editConfiguration, vreq);
	    	return editConfiguration;
	    }
	
	//EditSubmission preprocessors need to be added separately
	//TODO: How to handle constructor arguments, right now just doing
	//a constructor without arguments
	  private void addAdditionalPreprocessors(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
		//Check for model change preprocessors
		  List<String> preprocessorClassNames = getModelChangePreprocessorClassNames(vreq);
		  for(String p:preprocessorClassNames) {
			  //Create class from class name
			  ModelChangePreprocessor mcp = createModelChangePreprocessorFromClassName(p);
			  //Associate class with edit configuration
			  if(mcp != null) {
				  editConfiguration.addModelChangePreprocessor(mcp);
			  }
		  }
		  
		  
		
	}
	  
	  private ModelChangePreprocessor createModelChangePreprocessorFromClassName(String preprocessorName) {
			ModelChangePreprocessor mcp = null;
	    	
	        Object object = null;
	        try {
	            Class classDefinition = Class.forName(preprocessorName);
	            object = classDefinition.newInstance();
	            mcp = (ModelChangePreprocessor) object;
	        } catch (InstantiationException e) {
	            System.out.println(e);
	        } catch (IllegalAccessException e) {
	            System.out.println(e);
	        } catch (ClassNotFoundException e) {
	            System.out.println(e);
	        }    
	        return mcp;
	  }


	private List<String> getModelChangePreprocessorClassNames(VitroRequest vreq) {
		List<String> preprocessorFiles = getCustomModelChangePreprocessorForProperty(vreq);
		if(preprocessorFiles.size() == 0) {
			preprocessorFiles = getCustomModelChangePreprocessorForFauxProperty(vreq);
		}
		return preprocessorFiles;
		
	}


	private List<String> getCustomModelChangePreprocessorForFauxProperty(VitroRequest vreq) {
		List<String> configFiles = new ArrayList<String>();
		String configTemplatePredicate = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customModelChangePreprocessorAnnot";		
    	String predicateUri = EditConfigurationUtils.getPredicateUri(vreq);
    	//Check and see if domain/range exist, if so, then this may be a faux property
    	String rangeUri = EditConfigurationUtils.getRangeUri(vreq);
    	String domainUri = EditConfigurationUtils.getDomainUri(vreq);
		//Get everything really
		
		//Do we need BOTH for a faux property or just one?
		if(StringUtils.isNotEmpty(domainUri) && StringUtils.isNotEmpty(rangeUri)) {
			String query = "SELECT ?configTemplateFile WHERE ";
			query += "{ ?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#configContextFor> <" + predicateUri + "> ." + 
					" ?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#hasConfiguration> ?fauxConfig ." + 
					"?fauxConfig <" + configTemplatePredicate + "> ?configTemplateFile . " + 
					"?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#qualifiedByDomain> <" + domainUri + "> ." + 
					"?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#qualifiedBy> <" + rangeUri + "> . }" ;
	        try {
	        	configFiles.addAll(createSelectQueryContext(ModelAccess.on(vreq).getOntModel(ModelNames.DISPLAY),query).execute().toStringFields("configTemplateFile").flatten());	        	
	        } catch(Exception ex) {
	        	log.error("Error occurred in retrieving template file", ex);
	        }
	        
		}
		return configFiles;
	}

	private List<String> getCustomModelChangePreprocessorForProperty(VitroRequest vreq) {
		List<String> files = new ArrayList<String>();
	String configFilePredicate = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customModelChangePreprocessorAnnot";
		
		
    	String predicateUri = EditConfigurationUtils.getPredicateUri(vreq);
		
		//Get everything really
		String query = "SELECT ?file WHERE {<" + predicateUri + ">  <" + configFilePredicate + "> ?file .}";

		 
	        try {
	        	ResultSet rs = QueryUtils.getQueryResults(query, vreq);
	        	while(rs.hasNext()) {
	        		QuerySolution qs = rs.nextSolution();
	        		Literal configFileLiteral = qs.getLiteral("file");
	        		if(configFileLiteral != null && StringUtils.isNotEmpty(configFileLiteral.getString())) {
	        			files.add(configFileLiteral.getString());
	        		}
	        	}
	        	
	        } catch (Exception ex) {
	        	log.error("Exception occurred in query retrieving information for this field", ex);
	        }
		return files;
	}

	private void setTemplate(VitroRequest vreq, EditConfigurationVTwo editConfiguration) {
		  String customTemplate = getCustomTemplateFile(vreq);
		  if(customTemplate != null) {
			  editConfiguration.setTemplate(customTemplate);
		  } else {
			  editConfiguration.setTemplate("minimalconfigtemplate.ftl");
		  }
		
	}
	  
	  //if classURI is passed in
	  private boolean isClassSpecificNewForm(VitroRequest vreq) {
		  String classURI = vreq.getParameter(classURIParameter);
		  return StringUtils.isNotEmpty(classURI);
	  }

	//This gets custom template file using value of the template property
	private String getCustomTemplateFile(VitroRequest vreq) {
		if(isClassSpecificNewForm(vreq)) {
			return getClassSpecificCustomTemplateFile(vreq);
		}
		return getCustomTemplateFileResult(vreq);
	}
	//This needs to work for both faux properties as well as regular properties
	private String getCustomTemplateFileResult(VitroRequest vreq) {
		//TODO: The logic here should be fixed - from simply checking if one is null to checking
		//whether it's a faux property
		String templateFile = getCustomTemplateForProperty(vreq);
		if(StringUtils.isEmpty(templateFile)) {
			templateFile = getCustomTemplateForFauxProperty(vreq);
		}
		return templateFile;
	}
	
	private String getCustomTemplateForProperty(VitroRequest vreq) {
		String configFilePredicate = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot";
		
		
    	String predicateUri = EditConfigurationUtils.getPredicateUri(vreq);
		
		//Get everything really
		String query = "SELECT ?templateFile WHERE {<" + predicateUri + ">  <" + configFilePredicate + "> ?templateFile .}";

		 
	        try {
	        	ResultSet rs = QueryUtils.getQueryResults(query, vreq);
	        	while(rs.hasNext()) {
	        		QuerySolution qs = rs.nextSolution();
	        		Literal configFileLiteral = qs.getLiteral("templateFile");
	        		if(configFileLiteral != null && StringUtils.isNotEmpty(configFileLiteral.getString())) {
	        			return configFileLiteral.getString();
	        		}
	        	}
	        	
	        } catch (Exception ex) {
	        	log.error("Exception occurred in query retrieving information for this field", ex);
	        }
		return null;
	}
	
	private String getCustomTemplateForFauxProperty(VitroRequest vreq) {
		String configTemplatePredicate = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot";
		
		
    	String predicateUri = EditConfigurationUtils.getPredicateUri(vreq);
    	//Check and see if domain/range exist, if so, then this may be a faux property
    	String rangeUri = EditConfigurationUtils.getRangeUri(vreq);
    	String domainUri = EditConfigurationUtils.getDomainUri(vreq);
		//Get everything really
		
		//Do we need BOTH for a faux property or just one?
		if(StringUtils.isNotEmpty(domainUri) && StringUtils.isNotEmpty(rangeUri)) {
			String query = "SELECT ?configTemplateFile WHERE ";
			query += "{ ?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#configContextFor> <" + predicateUri + "> ." + 
					" ?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#hasConfiguration> ?fauxConfig ." + 
					"?fauxConfig <" + configTemplatePredicate + "> ?configTemplateFile . " + 
					"?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#qualifiedByDomain> <" + domainUri + "> ." + 
					"?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#qualifiedBy> <" + rangeUri + "> . }" ;
	        try {
	        	List<String> configFiles = createSelectQueryContext(ModelAccess.on(vreq).getOntModel(ModelNames.DISPLAY),query).execute().toStringFields("configTemplateFile").flatten();
	        	if(configFiles.size() > 0) {
	        		return configFiles.get(0);
	        	}
	        	
	        } catch(Exception ex) {
	        	log.error("Error occurred in retrieving template file", ex);
	        }
	        
		}
		return null;		
	}
	
	//This gets custom template file for a particular class - e.g. for a new individual form as opposed to property-related form
	public String getClassSpecificCustomTemplateFile(VitroRequest vreq) {
	
		String configFilePredicate = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customTemplateFileAnnot";
		String classURI = vreq.getParameter(classURIParameter);
		
		//Get everything really
		String query = "SELECT ?templateFile WHERE {<" + classURI + ">  <" + configFilePredicate + "> ?templateFile .}";

		 
	        try {
	        	ResultSet rs = QueryUtils.getQueryResults(query, vreq);
	        	while(rs.hasNext()) {
	        		QuerySolution qs = rs.nextSolution();
	        		Literal configFileLiteral = qs.getLiteral("templateFile");
	        		if(configFileLiteral != null && StringUtils.isNotEmpty(configFileLiteral.getString())) {
	        			return configFileLiteral.getString();
	        		}
	        	}
	        	
	        } catch (Exception ex) {
	        	log.error("Exception occurred in query retrieving information for this field", ex);
	        }
		return null;
	}
	
	public String getClassSpecificCustomConfigFile(VitroRequest vreq) {
		
		String configFilePredicate = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot";
		String classURI = vreq.getParameter(classURIParameter);
		
		//Get everything really
		String query = "SELECT ?configFile WHERE {<" + classURI + ">  <" + configFilePredicate + "> ?configFile .}";
	        try {
	        	ResultSet rs = QueryUtils.getQueryResults(query, vreq);
	        	while(rs.hasNext()) {
	        		QuerySolution qs = rs.nextSolution();
	        		Literal configFileLiteral = qs.getLiteral("configFile");
	        		if(configFileLiteral != null && StringUtils.isNotEmpty(configFileLiteral.getString())) {
	        			return configFileLiteral.getString();
	        		}
	        	}
	        	
	        } catch (Exception ex) {
	        	log.error("Exception occurred in query retrieving information for this field", ex);
	        }
		
	      // 
	        return null;
	}



	//Form specific data
    public void addFormSpecificData(EditConfigurationVTwo editConfiguration, VitroRequest vreq) {
    	 HashMap<String, Object> formSpecificData = new HashMap<String, Object>();
    	 String configFile = getConfigurationFile(vreq, editConfiguration);
         formSpecificData.put("editMode", getEditMode(vreq).name().toLowerCase());
         if(configFile != null && configFile.endsWith(".jsonld")) {
        	 formSpecificData.put("configFile", configFile);
        	 //TODO: override later    	
        	 //Just for now but this will probably be overridden later
        	 String configFileName = configFile.substring(0, configFile.indexOf(".jsonld"));
        	 String configDisplayFile = configFileName + "DisplayConfig.json";
        	 formSpecificData.put("configDisplayFile", configDisplayFile);
         } else {
        	 log.error("Config File either not found or does not have proper ending");
         }
         //Add URIs and literals in scope
         formSpecificData.put("urisInScope", editConfiguration.getUrisInScope());
         formSpecificData.put("literalsInScope", editConfiguration.getLiteralsInScope());
         //Do we have a default for configFile if none is returned?
         editConfiguration.setFormSpecificData(formSpecificData);
    }
	 
			
    private String getConfigurationFile(VitroRequest vreq, EditConfigurationVTwo editConfiguration) {
    	if(isClassSpecificNewForm(vreq)) {
    		return getClassSpecificCustomConfigFile(vreq);
    	} 
    	//if config file at base uri, return that, otherwise check for faux property
    	//TODO: revisit htis flow
    	String configFile =  getConfigurationFileResult(vreq);
    	if(StringUtils.isEmpty(configFile)) {
    		configFile = getConfigurationFileResultFromDisplayModel(vreq);
    	}
    	return configFile;
    }
    
    //Get the configuration file required
	private String getConfigurationFileResult(VitroRequest vreq) {
		//Custom form entry predicate: http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot
		String configFilePredicate = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot";
		
		
    	String predicateUri = EditConfigurationUtils.getPredicateUri(vreq);
    	
		//Get everything really
		String query = "SELECT ?configFile WHERE {";
		query += "<" + predicateUri + ">  <" + configFilePredicate + "> ?configFile .";
		query += "}";
	        try {
	        	ResultSet rs = QueryUtils.getQueryResults(query, vreq);
	        	while(rs.hasNext()) {
	        		QuerySolution qs = rs.nextSolution();
	        		Literal configFileLiteral = qs.getLiteral("configFile");
	        		if(configFileLiteral != null && StringUtils.isNotEmpty(configFileLiteral.getString())) {
	        			return configFileLiteral.getString();
	        		}
	        	}
	        } catch (Exception ex) {
	        	log.error("Exception occurred in query retrieving information for this field", ex);
	        }
		
	      // 
	        return null;
		
	}

	
	private String getConfigurationFileResultFromDisplayModel(VitroRequest vreq) {
		//Custom form entry predicate: http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customEntryFormAnnot
		String configFilePredicate = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#customConfigFileAnnot";
		
		
    	String predicateUri = EditConfigurationUtils.getPredicateUri(vreq);
    	//Check and see if domain/range exist, if so, then this may be a faux property
    	String rangeUri = EditConfigurationUtils.getRangeUri(vreq);
    	String domainUri = EditConfigurationUtils.getDomainUri(vreq);
		//Get everything really
		
		//Do we need BOTH for a faux property or just one?
		if(StringUtils.isNotEmpty(domainUri) && StringUtils.isNotEmpty(rangeUri)) {
			String query = "SELECT ?configFile WHERE ";
			query += "{ ?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#configContextFor> <" + predicateUri + "> ." + 
					" ?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#hasConfiguration> ?fauxConfig ." + 
					"?fauxConfig <" + configFilePredicate + "> ?configFile . " + 
					"?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#qualifiedByDomain> <" + domainUri + "> ." + 
					"?fauxProperty <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#qualifiedBy> <" + rangeUri + "> . }" ;
	        try {
	        	List<String> configFiles = createSelectQueryContext(ModelAccess.on(vreq).getOntModel(ModelNames.DISPLAY),query).execute().toStringFields("configFile").flatten();
	        	if(configFiles.size() > 0) {
	        		return configFiles.get(0);
	        	}
	        
	        

	        } catch (Exception ex) {
	        	log.error("Exception occurred in query retrieving information for this field", ex);
	        }
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
	    	editConfiguration.setVarNameForPredicate("predicate");
	    	editConfiguration.setPredicateUri(predicateUri);
	    	if(isClassSpecificNewForm(vreq)) {
	    		editConfiguration.setEntityToReturnTo("?subject"); //could also configure this in the json ld but default here
	    		//Setting it to variable name enables it to be substituted with the new resource URI set for this variable
	    	} else {
	    		editConfiguration.setEntityToReturnTo(subjectUri);
	    	}
	    	//Don't expect custom data property forms, so defaulting to object property for now
	    
	    	//"object"       : [ "objectVar" ,  "${objectUriJson}" , "URI"],
	    	if(EditConfigurationUtils.isObjectProperty(predicateUri, vreq)) {
	    		log.debug("This is an object property: " + predicateUri);
	    		this.initObjectParameters(vreq);
	    		this.processObjectPropForm(vreq, editConfiguration);
	    	} else {
	    		//Data property ONLY if not object uri and not a class specific custom form
	    		if(!isClassSpecificNewForm(vreq)) {
	    			log.debug("This is a data property: " + predicateUri);
	    			return;
	    		}
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
	    	//Use this if this isn't a new instance of a class form
	    	if(!isClassSpecificNewForm(vreq)) {
		    	String editString = "?subject ?predicate ";    	
		    	editString += "?objectVar";    	
		    	editString += " .";
		    	n3ForEdit.add(editString);
	    	}
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
	    	//If this is the creation of a new individual from a class URI, we don't need the regular preparation
			if(!isClassSpecificNewForm(vreq)) {
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
