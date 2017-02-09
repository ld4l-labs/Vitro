/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactory;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.BaseEditSubmissionPreprocessorVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationUtils;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.MultiValueEditSubmission;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields.FieldVTwo;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelAccess;

public class MinimalConfigurationPreprocessor extends
		BaseEditSubmissionPreprocessorVTwo {

	protected static final Log log = LogFactory
			.getLog(AddAssociatedConceptsPreprocessor.class.getName());
	protected OntModel ontModel = null;
	protected WebappDaoFactory wdf = null;

	private static MultiValueEditSubmission submission = null;
	Map<String, List<Literal>> copyLiteralsFromForm = new HashMap<String, List<Literal>>();
	Map<String, List<String>> copyUrisFromForm = new HashMap<String, List<String>>();
	//Save components in a hash by field name 
	Map<String, JSONObject> fieldNameToConfigurationComponent = new HashMap<String, JSONObject>();
	//These are variable names within the N3 - retrieved from field names as well as whatever is in new resources
	List<String> allowedVarNames = new ArrayList<String>();
	JSONObject optionalN3Component = null;
	JSONObject requiredN3Component = null;
	JSONObject newResourcesComponent = null;
	HashSet<String> newResourcesSet = new HashSet<String>();
	HashMap<String, HashSet<String>> dependencies = new HashMap<String, HashSet<String>>();
	//N3 optional
	
	// String datatype

	// Will be editing the edit configuration as well as edit submission here

	public MinimalConfigurationPreprocessor(EditConfigurationVTwo editConfig) {
		super(editConfig);
		
	}

	public void preprocess(MultiValueEditSubmission inputSubmission, VitroRequest vreq) {
		submission = inputSubmission;
		this.wdf = vreq.getWebappDaoFactory();
		this.ontModel = ModelAccess.on(vreq).getOntModel();
		//Need to keep independent 
		copySubmissionValues();
		
		String configjsonString = vreq.getParameter("configFile");
		configjsonString = "C:\\Users\\hjk54\\workspace\\vivocode\\VIVO\\webapp\\src\\main\\webapp\\templates\\freemarker\\edit\\forms\\js\\jsonconfig\\" + configjsonString;
		//Read in config file, interpret and store as json object
		try {
			String contents = new String(Files.readAllBytes(Paths.get(configjsonString)));
			//Remove these characters from the beginning of the string
			contents = contents.replaceFirst("var configjson =", "");
			JSONObject contentsJSON = (JSONObject) JSONSerializer.toJSON(contents);
			processConfigurationJSONFields(contentsJSON);
			updateConfiguration(vreq, contentsJSON);
			
		}catch (Exception ex) {
			log.error("Exception occurred reading in file", ex);
		}
		
	

	}
	
	
	private void processConfigurationJSONFields(JSONObject contentsJSON) {
		String fieldNameProperty =  "http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#fieldName";
		JSONArray graph = contentsJSON.getJSONArray("@graph");
		
		int len = graph.size();
		int i;
		for(i = 0; i < len; i++) {
			JSONObject component = graph.getJSONObject(i);
			JSONArray types = component.getJSONArray("@type");
			//Get field name info
			Object fieldInfo = component.get(fieldNameProperty);
			if(fieldInfo != null) {
				JSONArray jsonArray = new JSONArray();
				if(fieldInfo instanceof String)
					jsonArray.add(fieldInfo);
				else if(((JSON)fieldInfo).isArray())
					jsonArray.addAll((JSONArray)fieldInfo);
				else
					log.error("This is neither string nor array but probably a json object instead");
				
				
				int fieldNumber = jsonArray.size();
				int f;
				for(f = 0; f < fieldNumber; f++) {
					String fieldName = jsonArray.getString(f);
					fieldNameToConfigurationComponent.put(fieldName, component);
				}
			}
			//required n3 pattern
			if(types.contains("forms:RequiredN3Pattern")) {
				this.requiredN3Component= component;
			}
			//optional n3 pattern - assuming only one optional n3 component
			if(types.contains("forms:OptionalN3Pattern")) {
				this.optionalN3Component= component;
			}
			//new resources
			if(types.contains("forms:NewResource")) {
				this.newResourcesComponent = component;
				JSONArray newResourceFieldNames = component.getJSONArray("http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#fieldName");
				String[] newResourcesArray = new String[newResourceFieldNames.size()];
				newResourcesArray = (String []) newResourceFieldNames.toArray(newResourcesArray);
				newResourcesSet.addAll(new ArrayList<String>(Arrays.asList(newResourcesArray)));
			}
			//Check for dependencies components
			if(types.contains( "forms:FieldDependencies")) {
				JSONArray dependenciesArray = component.getJSONArray("http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#dependencies");
				int n, numberDependencies = dependenciesArray.size();
				for(n = 0; n < numberDependencies; n++ ) {
					String dependencyDelimited = dependenciesArray.getString(n);
					//Replace all empty spaces
					dependencyDelimited = dependencyDelimited.replaceAll("\\s+","");
					String[] dependenciesStringArray = StringUtils.split(dependencyDelimited, ",");
					List<String> dependenciesList = Arrays.asList(dependenciesStringArray);
					for(String s: dependenciesList) {
						HashSet<String> set = new HashSet<String>();
						set.addAll(dependenciesList);
						dependencies.put(s, set);
					}
				}
				
			}
					
		}
		
		//Add all fieldNameToConfigurationComponent keys to allowed names
		//new resources also has a field name property so it is also being added
		this.allowedVarNames.addAll(fieldNameToConfigurationComponent.keySet());
		
	}

	//Add fields, etc. for what we see
	private void updateConfiguration(VitroRequest vreq, JSONObject json) {
		//Normally, would get fields from json? or just see everything within vreq param and check from json config
		//The latter parallels the javascript approach
		Map<String, String[]> parameterMap = vreq.getParameterMap();
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
		System.out.println(allowedN3);
		//Hardcoding here - will do the rest above
		//N3 required
		//how did this even work before?
		
		String requiredN3String = this.requiredN3Component.getString("http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#pattern");
		if(StringUtils.isNotEmpty(requiredN3String)) {
			this.editConfiguration.addN3Required(requiredN3String);
		}
		//Attach allowedN3 as n3 required
		this.editConfiguration.addN3Required(allowedN3);
		//For each satisfiedVarName: get commponent and check if URI field, string field, or new resource and add accordingly
		for(String s: satisfiedVarNames) {
			//reserved names subject, predicate, objectVar do not need to be processed
			if(!isReservedVarName(s)) {
				//Get component 
				JSONObject component = this.fieldNameToConfigurationComponent.get(s);
				if(this.newResourcesSet.contains(s)) {
					//Add new resource field
					this.editConfiguration.addNewResource(s, null);
				}
				JSONArray types = component.getJSONArray("@type");
				boolean addField = false;
				boolean isURI = false;
				boolean isLiteral = false;
				//constant options fields - values are always URIs as far as I know
				if(types.contains("forms:UriField") || types.contains("forms:ConstantOptionsField")) {
					//create URI field component
					this.editConfiguration.addUrisOnForm(s);
					addField = true;
					isURI = true;
				} else if(types.contains("forms:StringField")){
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
					String uriValue = vreq.getParameter(s);
					String[] uriVals = new String[1];
					uriVals[0] = uriValue;
					this.submission.addUriToForm(this.editConfiguration, s, uriVals);
				} else if(isLiteral) {
					String literalValue = vreq.getParameter(s);
					String[] literalVals = new String[1];
					literalVals[0] = literalValue;
					FieldVTwo literalField = this.editConfiguration.getField(s);
					this.submission.addLiteralToForm(this.editConfiguration, literalField, s, literalVals);
				}
				//Need a way to deal with Date Time separately - this will require separate implementation?
				//Do we have date-time in VitroLib?
			}
		}
		
	/*	
	    String prefixes = "@prefix afn: <http://jena.hpl.hp.com/ARQ/function#> .        @prefix bibo: <http://purl.org/ontology/bibo/> .        @prefix core: <http://vivoweb.org/ontology/core#> .        @prefix foaf: <http://xmlns.com/foaf/0.1/> .      @prefix obo: <http://purl.obolibrary.org/obo/> .        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .       @prefix vcard: <http://www.w3.org/2006/vcard/ns#> .    ";

		List<String> additionalN3 = new ArrayList<String>();
		//subject = person, objectVar = authorship
		additionalN3.add(prefixes + "?objectVar a core:Authorship ;" + 
        "core:relates ?subject .");
		additionalN3.add(prefixes + "?objectVar core:relates ?newPublication . " + 
				"?newPublication core:relatedBy ?objectVar . " + 
				"?newPublication a ?pubType ;      rdfs:label ?title.");
		this.editConfiguration.addN3Required(additionalN3);
		*/
		//New resource
		/*
		this.editConfiguration.addNewResource("newPublication", null);
		//uris and literals on form
		List<String> urisOnForm = new ArrayList<String>();
		urisOnForm.add("pubType");
		this.editConfiguration.addUrisOnForm(urisOnForm);
		List<String> literalsOnForm = new ArrayList<String>();
		literalsOnForm.add("title");
		this.editConfiguration.addLiteralsOnForm(literalsOnForm);
		//fields: newPublication, pubType, title
		this.editConfiguration.addField(new FieldVTwo().setName("newPublication"));
		this.editConfiguration.addField(new FieldVTwo().setName("pubType"));
		this.editConfiguration.addField(new FieldVTwo().setName("title"));
		*/
		//Add inputs to submission
		//pubtype and title should both come from parameter
		/*
		String pubType = vreq.getParameter("pubType");
		String title = vreq.getParameter("title");
		String[] pTypeVals = new String[1];
		pTypeVals[0] = pubType;
		String[] titleVals = new String[1];
		titleVals[0] = title;
		
		this.submission.addUriToForm(this.editConfiguration, "pubType", pTypeVals);
		FieldVTwo titleField = this.editConfiguration.getField("title");
		this.submission.addLiteralToForm(this.editConfiguration, titleField, "title", titleVals);
		*/
		
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
		String fakeNSPrefix = "@prefix v: <" + fakeNS + "> .";
		
		allowedN3 = allowedN3.replaceAll("@prefix\\s*v:.*fake-ns#>\\s*\\.", "");
		allowedN3 = allowedN3.replaceAll("v:", "?");
		System.out.println("Resubstituting");
		return allowedN3;
	}

	private String createN3WithFakeNS(String fakeNS) {
		//Take the N3 strings, and then URI-ize them
		//Need to check if empty or not
		String n3Prefixes = optionalN3Component.getString("http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#prefixes");
		JSONArray optionalN3Array = optionalN3Component.getJSONArray("http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#pattern");		//Do we need period at end?
		
		String fakeNSPrefix = "@prefix v: <" + fakeNS + "> .";
		//For now we are going to pretend there are no ?s in the strings for now - 
		List<String> uriizedN3 = new ArrayList<String>();
		String allPrefixes = fakeNSPrefix + n3Prefixes;
		uriizedN3.add(fakeNSPrefix);
		uriizedN3.add(n3Prefixes);
		int optArrayLength = optionalN3Array.size();
		int on;
		Model testModel = ModelFactory.createDefaultModel();
		for(on = 0; on < optArrayLength; on++) {
			String n3String = optionalN3Array.getString(on);
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
		String uriizedAllN3 = StringUtils.join(uriizedN3, " ");
		System.out.println("Pre carriage return removal");
		System.out.println(uriizedAllN3);
		//remove newline/carriage return characters
		uriizedAllN3 = uriizedAllN3.replaceAll("\n", "").replaceAll("\r","");
		System.out.println("N3 after newline removal");
		System.out.println(uriizedAllN3);
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
		for(String varName: allowedVarNames) {
			//Is there a matching parameter and value and we haven't already encountered this
			if(newResourcesSet.contains(varName)) {
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
			if(!removeSatisfiedVarName.contains(satisfiedVarName) && dependencies.containsKey(satisfiedVarName)) {
				HashSet<String> set = dependencies.get(satisfiedVarName);
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
		Model uriizedModel = ModelFactory.createDefaultModel();
		StringReader modelReader = new StringReader(uriizedAllN3);
		uriizedModel.read(modelReader, "", "N3");
		//The "allowable" n3 bucket
		Model allowedN3Model = ModelFactory.createDefaultModel();
		allowedN3Model.setNsPrefix("v", fakeNS);

		//iterate through the model, see if each variable is part of the satisifed varnames bucket
		StmtIterator stmtIterator = uriizedModel.listStatements();
		
		while(stmtIterator.hasNext()) {
			Statement stmt = stmtIterator.nextStatement();
			//Get all the variables
			//Subject, predicate, resource
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
				if(object.asResource().getNameSpace().equals(fakeNS)) {
					disallowedStatement = !(satisfiedVarNames.contains(object.asResource().getLocalName()));
				}
			}
			if(!disallowedStatement) {
				allowedN3Model.add(stmt);
			}
			
		}
		//Write out allowedN3Model
		allowedN3Model.write(System.out, "ttl");
		return allowedN3Model;
	}
	
	private void addConfigurationComponent(JSONObject component) {
		//Get the N3 
		//Create field
		
	}

	private JSONObject getConfigurationComponent(String fieldName, JSONObject json) {
		if(this.fieldNameToConfigurationComponent.containsKey(fieldName)) {
			return this.fieldNameToConfigurationComponent.get(fieldName);
		}
		
		return null;
	}

	//Since we will change the uris and literals from form, we should make copies
	//of the original values and store them, this will also make iterations
	//and updates to the submission independent from accessing the values
	private void copySubmissionValues() {
		Map<String, List<String>> urisFromForm = submission.getUrisFromForm();
		Map<String, List<Literal>> literalsFromForm = submission.getLiteralsFromForm();
		//Copy
		copyUrisFromForm.putAll(urisFromForm);
		copyLiteralsFromForm.putAll(literalsFromForm);
	}
	
	

	

}
