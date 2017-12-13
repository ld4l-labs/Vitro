package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.cornell.mannlib.vitro.testing.AbstractTestClass;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditSubmissionVTwoPreprocessor.FormConfigurationException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;



/*
 * Test plan:
 * 
 * 
 * buildDynamicN3String:
 * invalid component - throws exception
 * value count == 0 - throws exception
 * builds good string
 * 
 * 
 * buildN3Pattern:
 * final punct with preceding space
 * final punct no preceding space
 * terms.length == 3
 * terms.length != 3 - throws exception
 * single value term 
 * multi-value term
 * test final triple
 * test final triple w/prefixes
 * 
 * getPrefixes:
 * test non-empty prefix string
 * test empty prefix string
 * 
 * getParameterValueCount - values count 0, values count 1, values count > 1
 * 
 * isValidDynamicN3Component:
 * no customform:pattern
 * dynamicArraySize = 0
 * no customform:dynamic_variables
 * dynamic variables 0
 * All satisfied, return true
 * 
 * getParameterValueCount - test a couple of different sizes
 * 
 * getDynamicVariableValueCount
 * no values for first dynamic var
 * dynamic vars not all same counts
 * dynamic vars all same counts
 * 
 * 
 * 
 */

/**
 * Tests class MinimalConfigurationPreprocessor
 *
 */
public class MinimalConfigurationPreprocessorTest extends AbstractTestClass {
			
	private final static String BASE_DYNAMIC_N3_CONFIG = 
	    "{" +
	    		"'@context': { " +
	    			"'forms': 'java:edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.forms#'," +
	    			"'customform': 'http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#'" +
	    		"}," +
	    		"'@graph': [" +
	    			"{" +
	    				"'@id': 'customform:addWork_dynamicN3'," +
	    				"'@type': [" +
	    					"'forms:DynamicN3Pattern'," +
	    					"'forms:FormComponent'" +
    					"]," +
	      		"}" +
	      	"]" +
	    "}";
	
	private final static String DYNAMIC_N3_CONFIG_WITH_EMPTY_PATTERN = 
		    "{" +
		    		"'@context': { " +
		    			"'forms': 'java:edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.forms#'," +
		    			"'customform': 'http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#'" +
		    		"}," +
		    		"'@graph': [" +
		    			"{" +
		    				"'@id': 'customform:addWork_dynamicN3'," +
		    				"'@type': [" +
		    					"'forms:DynamicN3Pattern'," +
		    					"'forms:FormComponent'" +
	    					"]," +
		    			    "'customform:pattern': [" +
		    				"]" +
		      		"}" +
		      	"]" +
		    "}";
	
	private final static String DYNAMIC_N3_CONFIG_WITHOUT_DYNAMIC_VARIABLES = 
		    "{" +
		    		"'@context': { " +
		    			"'forms': 'java:edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.forms#'," +
		    			"'customform': 'http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#'" +
		    		"}," +
		    		"'@graph': [" +
		    			"{" +
		    				"'@id': 'customform:addWork_dynamicN3'," +
		    				"'@type': [" +
		    					"'forms:DynamicN3Pattern'," +
		    					"'forms:FormComponent'" +
	    					"]," +
		    			    "'customform:pattern': [" +
		    			        "'?subject bibframe:genreForm ?lcsh .'," +
		    				    "'?lcsh rdfs:label ?lcshTerm .'," +
		    				    "'?lcsh rdf:type owl:Thing .'," +
		    				"]" +
		      		"}" +
		      	"]" +
		    "}";
	
	private final static String DYNAMIC_N3_CONFIG_WITH_EMPTY_DYNAMIC_VARIABLES = 
		    "{" +
		    		"'@context': { " +
		    			"'forms': 'java:edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.forms#'," +
		    			"'customform': 'http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#'" +
		    		"}," +
		    		"'@graph': [" +
		    			"{" +
		    				"'@id': 'customform:addWork_dynamicN3'," +
		    				"'@type': [" +
		    					"'forms:DynamicN3Pattern'," +
		    					"'forms:FormComponent'" +
	    					"]," +
		    			    "'customform:pattern': [" +
		    			        "'?subject bibframe:genreForm ?lcsh .'," +
		    				    "'?lcsh rdfs:label ?lcshTerm .'," +
		    				    "'?lcsh rdf:type owl:Thing .'," +
		    				"]," +
		    				"'customform:dynamic_variables': [" +
		    				"]" + 
		      		"}" +
		      	"]" +
		    "}";
	
	private final static String VALID_DYNAMIC_N3_CONFIG = 
		    "{" +
		    		"'@context': { " +
		    			"'forms': 'java:edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.forms#'," +
		    			"'customform': 'http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#'" +
		    		"}," +
		    		"'@graph': [" +
		    			"{" +
		    				"'@id': 'customform:addWork_dynamicN3'," +
		    				"'@type': [" +
		    					"'forms:DynamicN3Pattern'," +
		    					"'forms:FormComponent'" +
	    					"]," +
		    			    "'customform:pattern': [" +
		    			        "'?subject bibframe:genreForm ?lcsh .'," +
		    				    "'?lcsh rdfs:label ?lcshLabel .'," +
		    				    "'?lcsh rdf:type owl:Thing .'," +
		    				"]," +
		    				"'customform:dynamic_variables': [" +
		    				    "'?lcsh'," +
		    				    "'?lcshLabel'," +		    				
		    				"]" + 
		      		"}" +
		      	"]" +
		    "}"; 
	
//	private final static String[] N3_PATTERN = {
//		"?subject bibframe:genreForm ?lcsh .",
//		"?lcsh rdfs:label ?lcshTerm .",
//		"?lcsh rdf:type owl:Thing ."
//	};
			
	
	private MinimalConfigurationPreprocessor preprocessor;
	
	@Before
    public void setUp() {     
        this.preprocessor = new MinimalConfigurationPreprocessor(new EditConfigurationVTwo());
    }

    // ---------------------------------------------------------------------
    // The tests
    // ---------------------------------------------------------------------
	
	@Test
	public void dynamicN3ComponentWithoutPattern_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Custom form pattern not defined or not a JSON array");
		validateDynamicN3Component(BASE_DYNAMIC_N3_CONFIG);
	}
	
	@Test
	public void dynamicN3ComponentWithEmptyPattern_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Custom form pattern is empty");		
//		JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(BASE_DYNAMIC_N3_COMPONENT);
//		JSONArray graph = jsonObject.getJSONArray("@graph");
//		JSONObject first = graph.getJSONObject(0);	
//		first.put("customform:pattern", ArrayUtils.EMPTY_STRING_ARRAY);
//		graph.remove(0);
//		graph.add(0, first);
//		validateDynamicN3Component(jsonObject);	
		validateDynamicN3Component(DYNAMIC_N3_CONFIG_WITH_EMPTY_PATTERN);
	
	}
	
	@Test 
	public void dynamicN3ComponentWithoutDynamicVariables_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Dynamic variables not defined or not a JSON array");
		validateDynamicN3Component(DYNAMIC_N3_CONFIG_WITHOUT_DYNAMIC_VARIABLES);
	}
	
	@Test 
	public void dynamicN3ComponentWithEmptyDynamicVariables_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Dynamic variables array is empty");
		validateDynamicN3Component(DYNAMIC_N3_CONFIG_WITH_EMPTY_DYNAMIC_VARIABLES);
	}
	
	@Test 
	public void validDynamicN3Component_Succeeds() throws Exception {
		validateDynamicN3Component(VALID_DYNAMIC_N3_CONFIG);		
	}
	
    // ---------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------

	private void validateDynamicN3Component(String jsonString) throws Exception {
		validateDynamicN3Component(getComponent(jsonString));		
	}
	
	private void validateDynamicN3Component(JSONObject component) throws Exception {
		preprocessor.validateDynamicN3Component(component);
	}

	private JSONObject getComponent(String jsonString) {
		JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
		return getComponent(json);		
	}
	
	private JSONObject getComponent(JSONObject json) {
		JSONArray graph = json.getJSONArray("@graph");
		return graph.getJSONObject(0);				
	}
	
	

}
