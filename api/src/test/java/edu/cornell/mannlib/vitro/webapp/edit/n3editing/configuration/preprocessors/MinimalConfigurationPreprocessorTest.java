package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

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
	
	private final static String BASE_DYNAMIC_N3_COMPONENT = 
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
	
	private final static String[] N3_PATTERN = {
		"?subject bibframe:genreForm ?lcsh .",
		"?lcsh rdfs:label ?lcshTerm .",
		"?lcsh rdf:type owl:Thing ."
	};
			
	
	private MinimalConfigurationPreprocessor preprocessor;
	
	@Before
    public void setUp() {     
        this.preprocessor = new MinimalConfigurationPreprocessor(new EditConfigurationVTwo());
    }

    // ---------------------------------------------------------------------
    // The tests
    // ---------------------------------------------------------------------
	
	
	@Test
	public void invalidDynamicN3Component_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "No custom form pattern");
		validateDynamicN3Component(BASE_DYNAMIC_N3_COMPONENT);
	}
	
	@Test 
	@Ignore
	public void noDynamicVariablesInDynamicN3Component_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "No dynamic variables");
		validateDynamicN3Component(BASE_DYNAMIC_N3_COMPONENT, "customform:pattern", N3_PATTERN);
	}
	
    // ---------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------

	private void validateDynamicN3Component(String jsonString) throws Exception {
		validateDynamicN3Component(jsonString, null, null);		
	}
	
	private void validateDynamicN3Component(String jsonBase, String key, String[] array) throws Exception {
		JSONObject component = getComponent(jsonBase, key, array);
		validateDynamicN3Component(component);
	}
	
	private void validateDynamicN3Component(JSONObject component) throws Exception {
		preprocessor.validateDynamicN3Component(component);		
	}
	
	private JSONObject getComponent(String base, String key, String[] value) {
		JSONObject json = addArrayToJsonObject(base, key, value);
		JSONArray graph = json.getJSONArray("@graph");
		return graph.getJSONObject(0);		
	}
	
	private JSONObject addArrayToJsonObject(String base, String key, String[] value) {
		JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(base);
		if (key != null && value != null) {
			jsonObject.put(key, value);
		}
		return jsonObject;
	}

}
