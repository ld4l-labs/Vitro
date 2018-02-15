package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.cornell.mannlib.vitro.testing.AbstractTestClass;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditSubmissionVTwoPreprocessor.FormConfigurationException;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditSubmissionVTwoPreprocessor.FormSubmissionException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;


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
			        "'?subject ex:predicate1 var1 .'," +
				    "'?var1 rdfs:label ?var2 .'," +
				    "'?var1 rdf:type ex:Class1 .'," +
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
			        "'?subject ex:predicate1 ?var1 .'," +
				    "'?var1 rdfs:label ?var2 .'," +
				    "'?var1 rdf:type ex:Class1 .'," +
    				"]," +
    				"'customform:dynamic_variables': [" +
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
			        "'?subject ex:predicate1 ?var1 .'," +
				    "'?var1 rdfs:label ?var2 .'," +
				    "'?var1 ex:Class1 .'," +
    				"]," +
    				"'customform:dynamic_variables': [" +
    					"'?var1'," +
    					"'?var2'" +
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
			        "'?subject ex:predicate1 ?var1 .'," +
				    "'?var1 rdfs:label ?var2 .'," +
				    "'?var1 ex:Class1 .'," +
    				"]," +
    				"'customform:dynamic_variables': [" +
    					"'?var1'," +
    					"'?var2'" +
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
			        "'?subject ex:predicate1 ?var1 .'," +
				    "'?var1 rdfs:label ?var2 .'," +
				    "'?var1 rdf:type ex:Class1 .'," +
    				"]," +
    				"'customform:dynamic_variables': [" +
    					"'?var1'," +
    					"'?var2'" +
    				"]" + 
		    "}";
	
	private final static String N3_CONFIG_REQUIRED = 
			"{" +
				"'@context': {" +
		    			"'owl': 'http://www.w3.org/2002/07/owl#'" +
	    			"}," + 
	    			"'@graph': [" + 
	    				"{" +
    						"'@id': 'customform:addWork_requiredN3'," +
    						"'@type': [" +
    							"'forms:DynamicN3Pattern'," +
    							"'forms:FormComponent'" +
    						"]," +
    						"'customform:pattern': [" +
    							"'?subject ex:predicate1 ?var1 .'" +
						"]" +
    					"}" +
    				"]" +
			"}";
	
	private final static String N3_CONFIG_NO_REQUIRED = 
			"{" +
				"'@context': {" +
		    			"'owl': 'http://www.w3.org/2002/07/owl#'" +
	    			"}," + 
	    			"'@graph': [" + 
	    				"{" +
    						"'@id': 'customform:addWork_dynamicN3'," +
    						"'@type': [" +
    							"'forms:DynamicN3Pattern'," +
    							"'forms:FormComponent'" +
    						"]," +
    	    			    		"'customform:pattern': [" +
    	    			    			"'?subject ex:predicate1 ?var1 .'," +
	    			    			"'?var1 rdfs:label ?var2 .'," +
	    			    			"'?var1 rdf:type ex:Class1 .'," +
	    			    		"]," +
	    			    		"'customform:dynamic_variables': [" +
	    			    			"'?var1'," +
	    			    			"'?var2'" +
	    			    		"]" + 
    					"}" +
    				"]" +
			"}";
	
	private final static String N3_CONFIG_NO_REQUIRED_OR_DYNAMIC = 
			"{" +
				"'@context': {" +
		    			"'owl': 'http://www.w3.org/2002/07/owl#'" +
	    			"}," + 
	    			"'@graph': [" + 
	    				"{" +
    						"'@id': 'customform:addWork_optionalN3'," +
    						"'@type': [" +
    							"'forms:DynamicN3Pattern'," +
    							"'forms:FormComponent'" +
    						"]," +
    						"'customform:pattern': [" +
    							"'?subject ex:predicate1 ?var1 .'" +
						"]" +
    					"}" +
    				"]" +
			"}";
	
	private static final JSONArray DYNAMIC_VARS = 
			(JSONArray) JSONSerializer.toJSON(new String[] {"?var1", "?var2", "?var3"});
	
	private static final JSONArray DYNAMIC_PATTERN = 
			(JSONArray) JSONSerializer.toJSON(new String[] {
				"?subject ex:predicate1 ?var1 . ",
				"?var1 rdfs:label ?var2 . " ,
				"?var1 rdf:type ex:Class1 . "	 ,
				"?var2 ex:predicate2 ?var3 . "
			});
	
	private static final JSONArray TRIPLES_MISSING_FINAL_PERIOD = 
			(JSONArray) JSONSerializer.toJSON(new String[] {
				"?subject ex:predicate1 ?var1 ",
				"?var1 rdfs:label ?var2 . " ,
				"?var1 rdf:type ex:Class1"	 ,
				"?var2 ex:predicate2 ?var3 . "
			});
	
	private MinimalConfigurationPreprocessor preprocessor;
	
	@Before
    public void setUp() {     
        this.preprocessor = new MinimalConfigurationPreprocessor(new EditConfigurationVTwo());
    }


    // ---------------------------------------------------------------------
    // The tests
    // ---------------------------------------------------------------------
	
	@Test
	public void formWithNoDynamicN3Component_Succeeds() throws Exception {
		JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_REQUIRED);
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"value"});
		preprocessor.requiredN3Component = config.getJSONArray("@graph").getJSONObject(0);
		preprocessor.updateConfiguration(params, config);
	}
	
	@Test
	public void formWithDynamicButNoRequiredN3Component_Succeeds() throws Exception {
		JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_NO_REQUIRED);
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"value1"});
		params.put("var2", new String[] {"value2"});
		preprocessor.dynamicN3Component = config.getJSONArray("@graph").getJSONObject(0);
		preprocessor.updateConfiguration(params, config);
	}
	
	@Test (expected = FormConfigurationException.class)
	public void formWithDynamicButNoRequiredN3Component_ThrowException() throws Exception {
		JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_NO_REQUIRED_OR_DYNAMIC);
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"value1"});
		params.put("var2", new String[] {"value2"});
		preprocessor.optionalN3Component = config.getJSONArray("@graph").getJSONObject(0);
		preprocessor.updateConfiguration(params, config);
	}
	
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

	@Test
	public void dynamicVariableWithNoValue_ThrowsException() throws Exception {
		expectException(FormSubmissionException.class, "Dynamic variable requires at least one value");
		Map<String, String[]> params = new HashMap<>();
		params.put("var2", new String[] {"value2"});
		params.put("var3", new String[] {"value3"});
		getParameterValueCount(0, DYNAMIC_VARS, params);
	}
	
	@Test
	public void testDynamicVariableValueCountOfOne() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value"});
		int count = getParameterValueCount(0, DYNAMIC_VARS, params);
		Assert.assertEquals(count, 1);
	}
	
	@Test
	public void testDynamicVariableCountGreaterThanOne() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1", "var_value2", "var1_value3"});
		params.put("var2", new String[] {"var2_value1", "var2_value2", "var2_value3"});
		int count = getParameterValueCount(0, DYNAMIC_VARS, params);
		Assert.assertEquals(count, 3);
	}
	
	@Test
	public void nonMatchingDynamicVariableValueCounts_ThrowsException() throws Exception {
		expectException(FormSubmissionException.class, "Dynamic variables must have the same number of values");
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1", "var1_value2"});
		params.put("var2", new String[] {"var2_value1", "var2_value2", "var2_value3"});
		getDynamicVariableValueCount(DYNAMIC_VARS, params);
	}
	
	@Test
	public void testDynamicPatternWithOneValue() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1"});
		params.put("var2", new String[] {"var2_value1"});
		params.put("var3", new String[] {"var3_value1"});
		String pattern = buildDynamicN3Pattern(DYNAMIC_PATTERN, DYNAMIC_VARS, "", 1);
		Assert.assertEquals(DYNAMIC_PATTERN.join(" "), pattern);
	}
	
	@Test 
	public void testDynamicPatternWithMultipleValues() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1", "var1_value2", "var1_value3"});
		params.put("var2", new String[] {"var2_value1", "var2_value2", "var2_value3"});
		params.put("var3", new String[] {"var3_value1", "var3_value1", "var3_value3"});
		String expected = "?subject ex:predicate1 ?var10 . "
				+ "?subject ex:predicate1 ?var11 . "
				+ "?subject ex:predicate1 ?var12 . "
				+ "?var10 rdfs:label ?var20 . "
				+ "?var11 rdfs:label ?var21 . "
				+ "?var12 rdfs:label ?var22 . "
				+ "?var10 rdf:type ex:Class1 . "
				+ "?var11 rdf:type ex:Class1 . "
				+ "?var12 rdf:type ex:Class1 . "
				+ "?var20 ex:predicate2 ?var30 . "
				+ "?var21 ex:predicate2 ?var31 . "
				+ "?var22 ex:predicate2 ?var32 . ";				
		String pattern = buildDynamicN3Pattern(DYNAMIC_PATTERN, DYNAMIC_VARS, "", 3);
		Assert.assertEquals(expected, pattern);
	}
	
	@Test
	public void testDynamicPatternWithNonDynamicVariable() throws Exception {
		JSONArray dynamicVars = (JSONArray) JSONSerializer.toJSON(new String[] {"?var1", "?var2"});
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1", "var1_value2", "var1_value3"});
		params.put("var2", new String[] {"var2_value1", "var2_value2", "var2_value3"});
		String expected = "?subject ex:predicate1 ?var10 . "
				+ "?subject ex:predicate1 ?var11 . "
				+ "?subject ex:predicate1 ?var12 . "
				+ "?var10 rdfs:label ?var20 . "
				+ "?var11 rdfs:label ?var21 . "
				+ "?var12 rdfs:label ?var22 . "
				+ "?var10 rdf:type ex:Class1 . "
				+ "?var11 rdf:type ex:Class1 . "
				+ "?var12 rdf:type ex:Class1 . "
				+ "?var20 ex:predicate2 ?var3 . "
				+ "?var21 ex:predicate2 ?var3 . "
				+ "?var22 ex:predicate2 ?var3 . ";				
		String pattern = buildDynamicN3Pattern(DYNAMIC_PATTERN, dynamicVars, "", 3);
		Assert.assertEquals(expected, pattern);		
	}
	
	@Test
	public void testDynamicN3TripleWithNoFinalPeriod() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1", "var1_value2"});
		params.put("var2", new String[] {"var2_value1", "var2_value2"});
		params.put("var3", new String[] {"var3_value1", "var3_value1", "var3_value3"});
		String expected = "?subject ex:predicate1 ?var10 . "
				+ "?subject ex:predicate1 ?var11 . "
				+ "?subject ex:predicate1 ?var12 . "
				+ "?var10 rdfs:label ?var20 . "
				+ "?var11 rdfs:label ?var21 . "
				+ "?var12 rdfs:label ?var22 . "
				+ "?var10 rdf:type ex:Class1 . "
				+ "?var11 rdf:type ex:Class1 . "
				+ "?var12 rdf:type ex:Class1 . "
				+ "?var20 ex:predicate2 ?var30 . "
				+ "?var21 ex:predicate2 ?var31 . "
				+ "?var22 ex:predicate2 ?var32 . ";
		String pattern = buildDynamicN3Pattern(TRIPLES_MISSING_FINAL_PERIOD, DYNAMIC_VARS, "", 3);
		Assert.assertEquals(expected, pattern);
	}

	@Test
	public void testDynamicN3Component() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1", "var1_value2", "var1_value3"});
		params.put("var2", new String[] {"var2_value1", "var2_value2", "var2_value3"});
		String pattern = buildDynamicN3Pattern(VALID_DYNAMIC_N3_COMPONENT, params);
		String expected = "?subject ex:predicate1 ?var10 . "
				+ "?subject ex:predicate1 ?var11 . "
				+ "?subject ex:predicate1 ?var12 . "
				+ "?var10 rdfs:label ?var20 . "
				+ "?var11 rdfs:label ?var21 . "
				+ "?var12 rdfs:label ?var22 . "
				+ "?var10 rdf:type ex:Class1 . "
				+ "?var11 rdf:type ex:Class1 . "
				+ "?var12 rdf:type ex:Class1 . ";
		Assert.assertEquals(expected, pattern);
	}

	public void testDynamicN3ComponentWithPrefixes() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1", "var1_value2", "var1_value3"});
		params.put("var2", new String[] {"var2_value1", "var2_value2", "var2_value3"});
		String pattern = buildDynamicN3Pattern(VALID_DYNAMIC_N3_COMPONENT, params);
		String expected = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " 
				+ "@prefix ex: <http://example.org/> . "
				+ "?subject ex:predicate1 ?var10 . "
				+ "?subject ex:predicate1 ?var11 . "
				+ "?subject ex:predicate1 ?var12 . "
				+ "?var10 rdfs:label ?var20 . "
				+ "?var11 rdfs:label ?var21 . "
				+ "?var12 rdfs:label ?var22 . "
				+ "?var10 rdf:type ex:Class1 . "
				+ "?var11 rdf:type ex:Class1 . "
				+ "?var12 rdf:type ex:Class1 . ";
		Assert.assertEquals(expected, pattern);
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
		
	private int getParameterValueCount(int index, JSONArray dynamicVars, Map<String, String[]> params) 
			throws Exception {
		return preprocessor.getDynamicVarParameterValueCount(index,  dynamicVars, params);
	}
	
	private int getDynamicVariableValueCount(JSONArray dynamicVars, Map<String, String[]> params) 
			throws Exception {
		return preprocessor.getDynamicVariableValueCount(dynamicVars, params);
	}
	
	private String buildDynamicN3Pattern(JSONArray array, JSONArray vars, String prefixes, int paramValueCount) 
			throws Exception {
		return preprocessor.buildDynamicN3Pattern(array, vars, prefixes, paramValueCount);
	}
	
	private String buildDynamicN3Pattern(String jsonString, Map<String, String[]> params) 
			throws Exception {
		JSONObject dynamicN3Component = getComponent(jsonString);
		return preprocessor.buildDynamicN3Pattern(dynamicN3Component, params);
	}

	private JSONObject getComponent(String jsonString) {
		return (JSONObject) JSONSerializer.toJSON(jsonString);
	}


}
