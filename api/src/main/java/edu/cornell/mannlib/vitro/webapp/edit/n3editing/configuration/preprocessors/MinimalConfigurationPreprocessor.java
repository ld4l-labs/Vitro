/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_CONSTANT_OPTIONS_FIELD;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_LITERAL_FIELD;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_URI_FIELD;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cornell.mannlib.vitro.webapp.application.ApplicationUtils;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactory;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.BaseEditSubmissionPreprocessorVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.MultiValueEditSubmission;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.ConfigFileField;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.ConfigFileN3Pattern;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFileImpl;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;

public class MinimalConfigurationPreprocessor extends
		BaseEditSubmissionPreprocessorVTwo {

	protected static final Log log = LogFactory
			.getLog(MinimalConfigurationPreprocessor.class.getName());
	protected OntModel ontModel = null;
	protected WebappDaoFactory wdf = null;

	private MultiValueEditSubmission submission = null;

    ConfigFile configFile;
    
	public MinimalConfigurationPreprocessor(EditConfigurationVTwo editConfig) {
		super(editConfig);
	}

    @Override
    public void preprocess(MultiValueEditSubmission inputSubmission, VitroRequest vreq) {
        submission = inputSubmission;
        this.wdf = vreq.getWebappDaoFactory();
        this.ontModel = ModelAccess.on(vreq).getOntModel();
        
        String configjsonString = vreq.getParameter("configFile");
        //This needs to be based on the VIVO app itself and deployment, not installation directory
        configjsonString = ApplicationUtils.instance().getServletContext().getRealPath("/templates/freemarker/edit/forms/js/jsonconfig/" + configjsonString);
        //Read in config file, interpret and store as json object
        try {
            String contents = new String(Files.readAllBytes(Paths.get(configjsonString)));
            configFile = ConfigFileImpl.parse(contents);
            updateConfiguration(vreq.getParameterMap());
            handleExistingValues(vreq);
            
        } catch (Exception ex) {
            log.error("Exception occurred reading in configuration file", ex);
        }
    }
	
	private void handleExistingValues(VitroRequest vreq) throws IOException {
		String existingValues = vreq.getParameter("existingValuesRetrieved");
		if(StringUtils.isNotEmpty(existingValues)) {
			//Convert to JSON object - each key has an array of string values
            @SuppressWarnings("unchecked")
            Map<String, List<String>> existingValuesObject = new ObjectMapper().readValue(existingValues, HashMap.class);
			for(String key: existingValuesObject.keySet()) {
                if(configFile.hasFieldComponent(key)) {
                    ConfigFileField configurationComponent = configFile.getFieldComponent(key);
                    List<String> values = existingValuesObject.get(key);
                    if(configurationComponent.hasType(TYPE_URI_FIELD)) {
						this.editConfiguration.addUrisInScope(key, new ArrayList<String>(values));
                    } else if(configurationComponent.hasType(TYPE_LITERAL_FIELD)) {
						for(String value: values) {
							Literal valueLiteral = ResourceFactory.createPlainLiteral(value);
							this.editConfiguration.addLiteralInScope(key,valueLiteral);
						}
					}
				}
			}
		}
	}

	// Add fields, etc. for what we see
	private void updateConfiguration(Map<String, String[]> parameterMap) 
			throws FormConfigurationException, FormSubmissionException {
		//Normally, would get fields from json? or just see everything within vreq param and check from json config
		//The latter parallels the javascript approach
		/*
		for(String k: parameterMap.keySet()) {
			//Check if field exists within configuration
			JSONObject component = getConfigurationComponent(k, json);
			if(component != null) {
				//Get information - add field object, add n3 required at this point 
				addConfigurationComponent(component);
			}
		}*/
		
		HashSet<String> satisfiedVarNames = getSatisfiedVarNames(parameterMap);
		String fakeNS = "http://www.uriize.com/fake-ns#";
		String uriizedAllN3 = createN3WithFakeNS(fakeNS);
		//Add to a model
		Model allowedN3Model = createAllowedModel(satisfiedVarNames, fakeNS, uriizedAllN3);
		//Breaking up into individual statements as opposed to one big string that 
		//can be accepted or rejected together if any part of it is wrong/incorrect
		List<String> allowedN3 = unURIize(fakeNS, allowedN3Model);
		//System.out.println(allowedN3);
		
        // Config always has required N3
        ConfigFileN3Pattern requiredN3 = configFile.getRequiredN3();
        String requiredN3String = requiredN3.getJoined();
        this.editConfiguration.addN3Required(requiredN3String);
		
		// Attach allowedN3 as n3 optional (AllowedN3 is generated in part by optional N3 - retractions are based on required/optional 
		//and a situation can occur when optional N3 was not defined initially but if we add everything to required, the retractions
		//will require values where there weren't any to begin with due to those being optional the first time the info was added.
		//Additionally, try to split into an array instead of a single line that must be evaluated or discarded
		//Technically at this point, we only have the RDF for submission but useful to split in case of errors so at least some of the RDF is submitted
		//This way is a little hacky but we're trying it
		this.editConfiguration.addN3Optional(allowedN3);
		
		//For each satisfiedVarName: get component and check if URI field, string field, or new resource and add accordingly
		for(String s: satisfiedVarNames) {
			//reserved names subject, predicate, objectVar do not need to be processed
			//that said, we may need to override certain properties, so do process if element is present
			if(!isReservedVarName(s)) {
				//Get component 
                ConfigFileField component = configFile.getFieldComponent(s);
                if(configFile.getNewResourceVarNames().contains(s)) {
					//Add new resource field
					this.editConfiguration.addNewResource(s, null);
				}
				boolean addField = false;
				boolean isURI = false;
				boolean isLiteral = false;
				//constant options fields - values are always URIs as far as I know
                if(component.hasType(TYPE_URI_FIELD) || component.hasType(TYPE_CONSTANT_OPTIONS_FIELD)) {
					//create URI field component
					this.editConfiguration.addUrisOnForm(s);
					addField = true;
					isURI = true;
				} else if(component.hasType(TYPE_LITERAL_FIELD)){
					this.editConfiguration.addLiteralsOnForm(s);
					addField = true;
					isLiteral = true;
				}
				if(addField) {
					this.editConfiguration.addField(new FieldVTwo().setName(s));
					//Not dealing with validators or validation or anything else here 
					//Just adding the field with a name for now
				}
				
				//Add URI - add Literal
				if(isURI) {
					String[] uriVals = parameterMap.get(s);
					submission.addUriToForm(this.editConfiguration, s, uriVals);
				} else if(isLiteral) {
					String[] literalVals = parameterMap.get(s);
					FieldVTwo literalField = this.editConfiguration.getField(s);
					submission.addLiteralToForm(this.editConfiguration, literalField, s, literalVals);
				}
				//Need a way to deal with Date Time separately - this will require separate implementation?
				//Do we have date-time in VitroLib?
            } else if(isReservedVarName(s) && configFile.hasFieldComponent(s) ) {
				//We may need to add some information even for reserved name, such as in the case of
				//a create new individual form where the subject needs to be a new resource
                if(configFile.getNewResourceVarNames().contains(s)) {
					//Add new resource field
					this.editConfiguration.addNewResource(s, null);
				}
			}
		}		
	}
	
	private boolean isReservedVarName(String s) {
		return (s.equals("subject") || s.equals("predicate") || s.equals("objectVar"));
	}

	private List<String> unURIize(String fakeNS, Model allowedN3Model) {
		//Un uriize
		StringWriter sw = new StringWriter();
		//Turtle is a subset of N3 and should use short prefix format
		allowedN3Model.write(sw, "ttl");
		String allowedN3 = sw.toString().trim();
		allowedN3 = allowedN3.replaceAll("@prefix\\s*v:.*fake-ns#>\\s*\\.", "");
		allowedN3 = allowedN3.replaceAll("v:", "?");
		log.debug("As a whole, allowed N3 is " + allowedN3);
		
		List<String> optionalStrings = new ArrayList<String>();
		List<Statement> stmts = allowedN3Model.listStatements().toList();
		for(Statement stmt: stmts) {
			//For each statement, add to Model and then output as ttl string
			Model stmtModel = ModelFactory.createDefaultModel();
			stmtModel.setNsPrefix("v", fakeNS);
			stmtModel.add(stmt);
			StringWriter stmtWriter = new StringWriter();
			stmtModel.write(stmtWriter, "ttl");
			String optString = stmtWriter.toString();
			//Do the same replacements at this level that you would do above
			optString = optString.replaceAll("@prefix\\s*v:.*fake-ns#>\\s*\\.", "");
			optString = optString.replaceAll("v:", "?");
			optionalStrings.add(optString);
		}
		
		return optionalStrings;
	}

	private String createN3WithFakeNS(String fakeNS) {
		String uriizedAllN3 = null;
		//Take the N3 strings, and then URI-ize them
		//Need to check if empty or not
        if (configFile.hasOptionalN3()) {
            String n3Prefixes = configFile.getOptionalN3().getJoinedPrefixes();
			
			String fakeNSPrefix = "@prefix v: <" + fakeNS + "> .";
			//For now we are going to pretend there are no ?s in the strings for now - 
			List<String> uriizedN3 = new ArrayList<String>();
			String allPrefixes = fakeNSPrefix + n3Prefixes;
			uriizedN3.add(fakeNSPrefix);
			uriizedN3.add(n3Prefixes);
			Model testModel = ModelFactory.createDefaultModel();
            for(String n3String: configFile.getOptionalN3().getPattern()) {
				String substitutedN3String = n3String.replaceAll("[?]", "v:");
				uriizedN3.add(substitutedN3String);
				//one at a time so we can see which N3 statement might be a problem
				try {
					log.debug(allPrefixes + substitutedN3String);
					 StringReader reader = new StringReader(allPrefixes + substitutedN3String.replaceAll("\n", "").replaceAll("\r",""));
					testModel.read(reader, "", "N3");
				} catch(Exception ex) {
					ex.printStackTrace();
				}
				
			}
			uriizedAllN3 = StringUtils.join(uriizedN3, " ");
			//remove newline/carriage return characters
			uriizedAllN3 = uriizedAllN3.replaceAll("\n", "").replaceAll("\r","");
			log.debug("N3 after newline removal");
			log.debug(uriizedAllN3);
		}
		return uriizedAllN3;
	}

	//Given the values for the parameters, which varnames are satisfifed
	private HashSet<String> getSatisfiedVarNames(Map<String, String[]> parameterMap) {
		HashSet<String> satisfiedVarNames = new HashSet<String>();
		satisfiedVarNames.add("subject");
		satisfiedVarNames.add("predicate");
		satisfiedVarNames.add("objectVar");
		//Go through list of variables, for each check if variable has matching parameter which is populated
		//Also check for dependencies - create list or set of valid satisfied var names
        for(String varName: configFile.getAllowedVarNames()) {
			//Is there a matching parameter and value and we haven't already encountered this
            if(configFile.getNewResourceVarNames().contains(varName)) {
				satisfiedVarNames.add(varName);
			}
			else if(!satisfiedVarNames.contains(varName) && parameterMap.containsKey(varName)) {
				String[] paramValues = parameterMap.get(varName);
				if(paramValues.length > 0 && StringUtils.isNotEmpty(paramValues[0])) {
					satisfiedVarNames.add(varName);
				}
			} 
		}
		//Check against dependencies - if var name in dependency list doesn't also have the other varnames on that list, then remove
		//from satisfied varnames
		HashSet<String> removeSatisfiedVarName = new HashSet<String>();
		for(String satisfiedVarName: satisfiedVarNames) {
			//if the varname isn't already in the removal list - and if dependencies contains the var name
            if(!removeSatisfiedVarName.contains(satisfiedVarName) && configFile.hasDependenciesFor(satisfiedVarName)) {
                Set<String> set = configFile.getDependenciesFor(satisfiedVarName);
				//Are all the other items in the set satisfied - if not, then add all items to the removal set
				for(String dependency: set) {
					if(!satisfiedVarNames.contains(dependency)) {
						removeSatisfiedVarName.addAll(set);
						break;
					}
				}
			}
		}
		
		satisfiedVarNames.removeAll(removeSatisfiedVarName);
		return satisfiedVarNames;
	}

	private Model createAllowedModel(HashSet<String> satisfiedVarNames, String fakeNS, String uriizedAllN3) {
		Model allowedN3Model = ModelFactory.createDefaultModel();
		//this string may be null or empty if there are no optional N3 defined
		if(StringUtils.isNotEmpty(uriizedAllN3)) {
			Model uriizedModel = ModelFactory.createDefaultModel();
			
			StringReader modelReader = new StringReader(uriizedAllN3);
			uriizedModel.read(modelReader, "", "N3");
			//The "allowable" n3 bucket
			allowedN3Model.setNsPrefix("v", fakeNS);
	
			//iterate through the model, see if each variable is part of the satisifed varnames bucket
			StmtIterator stmtIterator = uriizedModel.listStatements();
			
			while(stmtIterator.hasNext()) {
				Statement stmt = stmtIterator.nextStatement();
				//Get all the variables
				//Subject, predicate, resource or literal
				Resource subject = stmt.getSubject();
				Property predicate = stmt.getPredicate();
				RDFNode object = stmt.getObject();
				boolean disallowedStatement = false;
				//if fakeNS variable but varname not in allowed set, disallow this statement
				if(subject.getNameSpace().equals(fakeNS)) {
					disallowedStatement = !(satisfiedVarNames.contains(subject.getLocalName()));
				}
				if(!disallowedStatement) {
					if(predicate.getNameSpace().equals(fakeNS)) {
						disallowedStatement = !(satisfiedVarNames.contains(predicate.getLocalName()));
					}
				}
				if(!disallowedStatement) {
					if(object.isResource() 
						&& object.asResource().getNameSpace().equals(fakeNS)) {
						disallowedStatement = !(satisfiedVarNames.contains(object.asResource().getLocalName()));
					}
					//if actual literal value, then pass along
				}
				if(!disallowedStatement) {
					allowedN3Model.add(stmt);
				}
				
			}
			//Write out allowedN3Model
			allowedN3Model.write(System.out, "ttl");
		}
		return allowedN3Model;
	}
}
