package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import edu.cornell.mannlib.vitro.testing.AbstractTestClass;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditSubmissionVTwoPreprocessor.FormConfigurationException;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditSubmissionVTwoPreprocessor.FormSubmissionException;

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
			
	// Strings representing dynamic N3 components do not include the entire configuration, but only the
	// portion (the component) that gets sent to MinimalConfigurationPreprocessor.buildDynamicN3Pattern(); that 
	// is, one element of the @graph array.
	
	private final static String BASE_DYNAMIC_N3_COMPONENT = 
		    "{" +
    				"'@id': 'customform:addWork_dynamicN3'," +
    				"'@type': [" +
    					"'forms:DynamicN3Pattern'," +
    					"'forms:FormComponent'" +
					"]," +
		    "}";
	
	private final static String DYNAMIC_N3_COMPONENT_WITH_EMPTY_PATTERN = 
		    "{" +
    				"'@id': 'customform:addWork_dynamicN3'," +
    				"'@type': [" +
    					"'forms:DynamicN3Pattern'," +
    					"'forms:FormComponent'" +
					"]," +
    			    "'customform:pattern': [" +
    				"]" +
		    "}";
	
	private final static String DYNAMIC_N3_COMPONENT_WITHOUT_DYNAMIC_VARIABLES = 
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
		    "}";

	
	private final static String DYNAMIC_N3_COMPONENT_WITH_EMPTY_DYNAMIC_VARIABLES = 
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
		    "}";
	

	
	private final static String INVALID_TRIPLE_MISSING_FINAL_PERIOD = 
		    "{" +
    				"'@id': 'customform:addWork_dynamicN3'," +
    				"'@type': [" +
    					"'forms:DynamicN3Pattern'," +
    					"'forms:FormComponent'" +
					"]," +
    			    "'customform:pattern': [" +
			        "'?subject bibframe:genreForm ?lcsh .'," +
				    "'?lcsh rdfs:label ?lcshTerm'," +
				    "'?lcsh rdf:type owl:Thing .'," +
    				"]," +
    				"'customform:dynamic_variables': [" +
    					"'?lcsh'," +
    					"'?lcsh'" +
    				"]" + 
		    "}";
	
	private final static String INVALID_TRIPLE_WITH_TWO_TERMS = 
		    "{" +
    				"'@id': 'customform:addWork_dynamicN3'," +
    				"'@type': [" +
    					"'forms:DynamicN3Pattern'," +
    					"'forms:FormComponent'" +
					"]," +
    			    "'customform:pattern': [" +
			        "'?subject bibframe:genreForm ?lcsh .'," +
				    "'?lcsh rdfs:label ?lcshTerm .'," +
				    "'?lcsh owl:Thing .'," +
    				"]," +
    				"'customform:dynamic_variables': [" +
    					"'?lcsh'," +
    					"'?lcsh'" +
    				"]" + 
		    "}";

	private final static String INVALID_TRIPLE_WITH_FOUR_TERMS = 
		    "{" +
    				"'@id': 'customform:addWork_dynamicN3'," +
    				"'@type': [" +
    					"'forms:DynamicN3Pattern'," +
    					"'forms:FormComponent'" +
					"]," +
    			    "'customform:pattern': [" +
			        "'?subject bibframe:genreForm ?lcsh .'," +
				    "'?lcsh rdfs:label ?lcshTerm .'," +
				    "'?lcsh owl:Thing .'," +
    				"]," +
    				"'customform:dynamic_variables': [" +
    					"'?lcsh'," +
    					"'?lcsh'" +
    				"]" + 
		    "}";
	
	private final static String VALID_DYNAMIC_N3_COMPONENT = 
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
    					"'?lcsh'," +
    					"'?lcshTerm'" +
    				"]" + 
		    "}";
	
	private static final JSONArray DYNAMIC_VARS = 
			(JSONArray) JSONSerializer.toJSON(new String[] {"?var1", "?var2", "?var3"});
	
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
		validateDynamicN3Component(BASE_DYNAMIC_N3_COMPONENT);
	}
	
	@Test
	public void dynamicN3ComponentWithEmptyPattern_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Custom form pattern is empty");		
		validateDynamicN3Component(DYNAMIC_N3_COMPONENT_WITH_EMPTY_PATTERN);	
	}
	
	@Test 
	public void dynamicN3ComponentWithoutDynamicVariables_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Dynamic variables not defined or not a JSON array");
		validateDynamicN3Component(DYNAMIC_N3_COMPONENT_WITHOUT_DYNAMIC_VARIABLES);
	}
	
	@Test 
	public void dynamicN3ComponentWithEmptyDynamicVariables_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Dynamic variables array is empty");
		validateDynamicN3Component(DYNAMIC_N3_COMPONENT_WITH_EMPTY_DYNAMIC_VARIABLES);
	}
	
	@Test
	public void dynamicN3TripleWithNoFinalPeriod_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Triple in pattern is missing final period");
		validateDynamicN3Component(INVALID_TRIPLE_MISSING_FINAL_PERIOD);
	}
	
	@Test
	public void dynamicN3TripleWithTwoTerms_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Triple in pattern does not have exactly three terms");
		validateDynamicN3Component(INVALID_TRIPLE_WITH_TWO_TERMS);
	}
	
	@Test
	public void dynamicN3TripleWithFourTerms_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Triple in pattern does not have exactly three terms");
		validateDynamicN3Component(INVALID_TRIPLE_WITH_FOUR_TERMS);
	}
	
	@Test 
	public void validDynamicN3Component_Succeeds() throws Exception {
		validateDynamicN3Component(VALID_DYNAMIC_N3_COMPONENT);		
	}
	
	// TODO - 1
	// 2 - one value for first var, 2 values for second var
	// 3 - 2 value for first var, 3 values for second var
	// 4 - 0 value for first var
	// 5 - 0 value for second var
	// 6 - 2 values for each var - succeeds
	@Test
	public void dynamicVariableWithNoValue_ThrowsException() throws Exception {
		expectException(FormSubmissionException.class, "Dynamic variable requires at least one value");
		Map<String, String[]> params = new HashMap<>();
		params.put("var2", new String[] {"value2"});
		params.put("var3", new String[] {"value3"});
		getParameterValueCount(0, DYNAMIC_VARS, params);
	}
	
	@Test
	public void dynamicVariableCountOfOne_Succeeds() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1", "var1_value2"});
		int count = getParameterValueCount(0, DYNAMIC_VARS, params);
		Assert.assertEquals(count, 2);
	}
	
	@Test
	@Ignore
	public void validDynamicVariableCountWithMultipleValues_Succeeds() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("lcsh", new String[] {"lcsh_v1", "lcsh_v2", "lcsh_v3"});
		params.put("lcshTerm", new String[] {"lcshTerm_v1", "lcshTerm_v2", "lcshTerm_v3"});
		buildDynamicN3Component(VALID_DYNAMIC_N3_COMPONENT, params);
	}
	
	@Test
	@Ignore
	public void nonMatchingDynamicVariableValueCounts_ThrowsException() {
		HashMap<String, String[]> params = new HashMap();
		//** Check vitrolib forms to see whether incoming param has "?" prefixed or not
		for (int i = 1; i <= 3; i++) {
			String index = String.valueOf(i);
			//sparams.put("var" + index, "value" + index);
		}
		fail("nonMatchingDynamicVariableValueCounts_ThrowsException not implemented");
	}
	

	
	
    // ---------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------

	private void validateDynamicN3Component(String jsonString) throws Exception {
		JSONObject component = getComponent(jsonString);
		validateDynamicN3Component(component);
	}
	
	private void validateDynamicN3Component(JSONObject component) throws Exception {
		preprocessor.validateDynamicN3Component(component);
	}
	
	private void buildDynamicN3Component(String jsonString, Map<String, String[]> parameters) throws Exception {
		preprocessor.dynamicN3Component = getComponent(jsonString);
		preprocessor.buildDynamicN3Pattern(parameters);
	}

	private JSONObject getComponent(String jsonString) {
		return (JSONObject) JSONSerializer.toJSON(jsonString);
	}
	
	private int getParameterValueCount(int index, JSONArray dynamicVars, Map<String, String[]> params) 
			throws FormSubmissionException {
		return preprocessor.getParameterValueCount(index,  dynamicVars, params);
	}


}
