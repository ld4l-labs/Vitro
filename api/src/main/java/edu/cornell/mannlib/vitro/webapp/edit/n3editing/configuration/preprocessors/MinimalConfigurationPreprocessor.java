/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_CONSTANT_OPTIONS_FIELD;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_LITERAL_FIELD;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_URI_FIELD;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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

import edu.cornell.mannlib.vitro.webapp.application.ApplicationUtils;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactory;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.BaseEditSubmissionPreprocessorVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.MultiValueEditSubmission;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.ConfigFileDynamicN3Pattern;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.ConfigFileField;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.ConfigFileN3Pattern;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFileImpl;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

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
	
	private void handleExistingValues(VitroRequest vreq) {
		String existingValues = vreq.getParameter("existingValuesRetrieved");
		if(StringUtils.isNotEmpty(existingValues)) {
			//Convert to JSON object
			JSONObject existingValuesObject = (JSONObject) JSONSerializer.toJSON(existingValues);
			@SuppressWarnings("unchecked")
			Set<String> keys = existingValuesObject.keySet();
			for(String key: keys) {
                if(configFile.hasFieldComponent(key)) {
                    ConfigFileField configurationComponent = configFile.getFieldComponent(key);
					JSONArray values = existingValuesObject.getJSONArray(key);
					int valuesLength = values.size();
					int v;
                    if(configurationComponent.hasType(TYPE_URI_FIELD)) {
						List<String> urisInScope = new ArrayList<String>();
						for(v = 0; v < valuesLength; v++) {
							urisInScope.add(values.getString(v));
						}
						this.editConfiguration.addUrisInScope(key, urisInScope);
                    } else if(configurationComponent.hasType(TYPE_LITERAL_FIELD)) {
						for(v = 0; v < valuesLength; v++) {
							String value = values.getString(v);
							Literal valueLiteral = ResourceFactory.createPlainLiteral(value);
							this.editConfiguration.addLiteralInScope(key,valueLiteral);
						}
						
					}
				}
			}
		}
	}

	// Add fields, etc. for what we see
	void updateConfiguration(Map<String, String[]> parameterMap) 
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
		
		String allowedN3 = unURIize(fakeNS, allowedN3Model);
		//System.out.println(allowedN3);
		
        // Config always has required N3
        ConfigFileN3Pattern requiredN3 = configFile.getRequiredN3();
        String requiredN3String = requiredN3.getJoined();
        this.editConfiguration.addN3Required(requiredN3String);
		
		// Attach allowedN3 as n3 optional (AllowedN3 is generated in part by optional N3 - retractions are based on required/optional 
		//and a situation can occur when optional N3 was not defined initially but if we add everything to required, the retractions
		//will require values where there weren't any to begin with due to those being optional the first time the info was added.
		
		this.editConfiguration.addN3Optional(allowedN3);
		
		// Add dynamic N3 pattern to the edit configuration's required N3
        if (configFile.hasDynamicN3()) {
            String dynamicN3Pattern = buildDynamicN3Pattern(configFile.getDynamicN3(), parameterMap);
			this.editConfiguration.addN3Required(dynamicN3Pattern);
		}

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
	
    String buildDynamicN3Pattern(ConfigFileDynamicN3Pattern dynamicComponent, Map<String, String[]> parameterMap) 
            throws FormConfigurationException, FormSubmissionException {
    
        validateDynamicN3Component(dynamicComponent);

        // Get the custom form configuration pattern
        List<String> dynamicN3Array = dynamicComponent.getPattern();

        // Get the dynamic variables
        List<String> dynamicVars = dynamicComponent.getVariables();
        
        // Get the count of the dynamic variable values in the form submission
        // TODO - maybe don't define dynamic variables, just get all the params that have multiple values
        int valueCount = getDynamicVariableValueCount(dynamicVars, parameterMap);
        
        String prefixes = dynamicComponent.getJoinedPrefixes();

        return buildDynamicN3Pattern(dynamicN3Array, dynamicVars, prefixes, valueCount);
    }

    String buildDynamicN3Pattern(List<String> dynamicN3Array, List<String> dynamicVars, String prefixes, 
            int paramValueCount) {
		
	    StringBuilder stringBuilder = new StringBuilder();
	    stringBuilder.append(prefixes);
	    
	    if (paramValueCount == 1) {
            stringBuilder.append(StringUtils.join(dynamicN3Array, " "));
    			return stringBuilder.toString();
	    }

	    // For each triple in the dynamic pattern
        for (String triple: dynamicN3Array) {
 		
	    		triple = triple.trim();
	    		if (triple.endsWith(".")) {
	    			// Peel off final period
	    			triple = triple.substring(0, triple.length() - 1).trim(); // triple.lastIndexOf(".");
	    		}
	    		
	    		// Split the triple into terms
	    		String[] terms = triple.trim().split("\\s+");
	    		
	    		// For each set of values in the input
	    		for (int valueIndex = 0; valueIndex < paramValueCount; valueIndex++) {
	    			// For each term in the triple
    				String[] newTerms = new String[3];
    				for (int termIndex = 0; termIndex < 3; termIndex++) {
    					String term = terms[termIndex];
    				    newTerms[termIndex] = dynamicVars.contains(term) ? term + valueIndex : term;
    				}
	    		    // Join the new terms into a triple, appending the final punctuation
		    		stringBuilder.append(StringUtils.join(newTerms, " ")).append(" . ");
	    		}
	    }
	    
	    return stringBuilder.toString();
	}
	
	/**
	 * Validates the dynamic N3 component. Throws an error if the component is invalid.
	 * @throws FormConfigurationException 
	 */
    void validateDynamicN3Component(ConfigFileDynamicN3Pattern dynamicN3Component) throws FormConfigurationException {

        validateDynamicN3Pattern(dynamicN3Component);	
		validateDynamicN3Variables(dynamicN3Component);
	}
	
	/**
	 * Validates the dynamic N3 component pattern. Throws an error if the pattern is invalid.
	 * @throws FormConfigurationException
	 */
    private void validateDynamicN3Pattern(ConfigFileDynamicN3Pattern dynamicN3Component) throws FormConfigurationException {   
		
		// Check that the first element of the graph defines a non-empty pattern array.
		
        List<String> pattern = dynamicN3Component.getPattern();
		if (pattern.size() == 0) {
			throw new FormConfigurationException("Custom form pattern is empty.");
		}
		
		// Check that each element of the pattern is a well-formed triple: 3 terms plus final period.
        for (String triple : pattern) {
			triple = triple.trim();
			
			// Peel off final period (in case preceded by spaces) 
			triple = triple.substring(0, triple.length() - 1).trim(); // triple.lastIndexOf(".");
		
			String[] terms = triple.split("\\s+");
			if (terms.length != 3) {
				throw new FormConfigurationException("Triple in pattern does not have exactly three terms.");
			}			
		}	
	}
	
	/**
	 * Validates the dynamic N3 dynamic variables array. Throws an error if the array is invalid.
	 * @throws FormConfigurationException
	 */
    private void validateDynamicN3Variables(ConfigFileDynamicN3Pattern dynamicN3Component) throws FormConfigurationException {
		
		// Check that the first element of the graph defines a non-empty dynamic variables array. 
        List<String> dynamicVars = dynamicN3Component.getVariables();
		if (dynamicVars.size() == 0) {
			throw new FormConfigurationException("Dynamic variables array is empty.");
		}
	}	
	
	/**
	 * Returns true iff the count of values in the form submission is the same for each dynamic variable. 
	 * @throws FormSubmissionException 
	 */
    int getDynamicVariableValueCount(List<String> dynamicVars, Map<String, String[]> params) 
			throws FormSubmissionException  {

	    // Get the first dynamic variable to compare to the others.
        int firstValueCount = getDynamicVarParameterValueCount(dynamicVars.get(0), params);

	    // Match the dynamic variables to the input parameter values and make sure all variables have the same 
	    // number of inputs.	 
        for (String var: dynamicVars.subList(1, dynamicVars.size())) {
            int valueCount = getDynamicVarParameterValueCount(var, params);
	    		if (valueCount != firstValueCount) {
	    			throw new FormSubmissionException("Dynamic variables must have the same number of values.");
	    		}   		
	    }
	    
	    return firstValueCount;
	}
	
	/** 
	 * Return the number of values in the parameter map for the specified variable
	 * @throws FormSubmissionException 
	 */
    int getDynamicVarParameterValueCount(String var, Map<String, String[]> params) 
    		throws FormSubmissionException {
    	
    		// Remove initial "?" from the variable for the comparison with the params
        String varName = var.substring(1);
        if (! params.containsKey(varName)) {
			throw new FormSubmissionException("Dynamic variable requires at least one value.");
		}
        return params.get(varName).length;
    }

	private boolean isReservedVarName(String s) {
		return (s.equals("subject") || s.equals("predicate") || s.equals("objectVar"));
	}

	private String unURIize(String fakeNS, Model allowedN3Model) {
		//Un uriize
		StringWriter sw = new StringWriter();
		//Turtle is a subset of N3 and should use short prefix format
		allowedN3Model.write(sw, "ttl");
		String allowedN3 = sw.toString().trim();
		//Substitute v: with 
		//Remove fakeNS line
		// String fakeNSPrefix = "@prefix v: <" + fakeNS + "> .";
		
		allowedN3 = allowedN3.replaceAll("@prefix\\s*v:.*fake-ns#>\\s*\\.", "");
		allowedN3 = allowedN3.replaceAll("v:", "?");
		System.out.println("Resubstituting");
		return allowedN3;
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
					System.out.println(allPrefixes + substitutedN3String);
					 StringReader reader = new StringReader(allPrefixes + substitutedN3String.replaceAll("\n", "").replaceAll("\r",""));
					testModel.read(reader, "", "N3");
				} catch(Exception ex) {
					ex.printStackTrace();
				}
				
			}
			uriizedAllN3 = StringUtils.join(uriizedN3, " ");
			System.out.println("Pre carriage return removal");
			System.out.println(uriizedAllN3);
			//remove newline/carriage return characters
			uriizedAllN3 = uriizedAllN3.replaceAll("\n", "").replaceAll("\r","");
			System.out.println("N3 after newline removal");
			System.out.println(uriizedAllN3);
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
