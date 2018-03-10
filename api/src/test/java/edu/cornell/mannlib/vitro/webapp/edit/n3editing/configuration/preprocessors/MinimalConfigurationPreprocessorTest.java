package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
    
    private final static String BASE_DYNAMIC_N3_COMPONENT = 
            "{" +
                "'@id': 'customform:sampleForm_dynamicN3'," +
                "'@type': [" +
                    "'forms:DynamicN3Pattern'," +
                    "'forms:FormComponent'" +
                "]," +
            "}";
    
    private final static String DYNAMIC_N3_COMPONENT_EMPTY_PATTERN = 
            "{" +
                "'@id': 'customform:sampleForm_dynamicN3'," +
                "'@type': [" +
                    "'forms:DynamicN3Pattern'," +
                    "'forms:FormComponent'" +
                "]," +
                "'customform:pattern': [" +
                "]" +
            "}";
    
    private final static String DYNAMIC_N3_COMPONENT_NO_DYNAMIC_VARIABLES = 
            "{" +
                "'@id': 'customform:sampleForm_dynamicN3'," +
                "'@type': [" +
                    "'forms:DynamicN3Pattern'," +
                    "'forms:FormComponent'" +
                "]," +
                "'customform:pattern': [" +
                    "'?subject ex:predicate0 ?objectVar . '" +
                "]," +
                "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
            			"@prefix ex: <http://example.org> . '" +
            "}";

    
    private final static String DYNAMIC_N3_COMPONENT_EMPTY_DYNAMIC_VARIABLES = 
            "{" +
                "'@id': 'customform:sampleForm_dynamicN3'," +
                "'@type': [" +
                    "'forms:DynamicN3Pattern'," +
                    "'forms:FormComponent'" +
                "]," +
                "'customform:pattern': [" +
                    "'?subject ex:predicate0 ?objectVar . '," +
                    "'?objectVar rdfs:label ?objectLabel . '," +
                    "'?objectVar rdf:type ex:Class0 . '" +
                "]," +
                "'customform:dynamic_variables': [" +
                "]," + 
                "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                    "@prefix ex: <http://example.org> . '" +
            "}";
    
    private final static String[] DYNAMIC_VARS = {
    			"?objectVar",
    			"?objectLabel"
    		};
    
    private final static String[] DYNAMIC_VARS_WITH_NO_OBJECT_VAR = {
    			"?objectLabel"
    		};

    
    private final static String[] INVALID_TRIPLE_WITH_TWO_TERMS = {
            "?subject ex:predicate0 ?objectVar . ",
            "?objectVar rdfs:label ?objectLabel . ",
            "?objectVar ex:Class0 . " 
    		};


    private final static String[] INVALID_TRIPLE_WITH_FOUR_TERMS = {
            "?subject ex:predicate0 ?objectVar . ",
            "?objectVar rdfs:label ?objectLabel ?objectVar . ",
            "?objectVar rdf:type ex:Class0 . "
    	    };
    
    private static final String[] TRIPLE_MISSING_FINAL_PERIOD = {
            "?subject ex:predicate0 ?objectVar ",
            "?objectVar rdfs:label ?objectLabel . "
        };
               
    
    private final static String VALID_DYNAMIC_N3_COMPONENT = 
            "{" +
                "'@id': 'customform:sampleForm_dynamicN3'," +
                "'@type': [" +
                    "'forms:DynamicN3Pattern'," +
                    "'forms:FormComponent'" +
                "]," +
                "'customform:pattern': [" +
                    "'?subject ex:predicate0 ?objectVar . '," +
                    "'?objectVar rdfs:label ?objectLabel . '," +
                    "'?objectVar rdf:type ex:Class0 . '" +
                "]," +
                "'customform:dynamic_variables': [" +
                    "'?objectVar'," +
                    "'?objectLabel'" +
                "]," +
                "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                    "@prefix ex: <http://example.org> . '" +
            "}";
    

    private final static String N3_CONFIG_REQUIRED_COMPONENT = 
            "{" +
                "'@context': {" +
                    "'owl': 'http://www.w3.org/2002/07/owl#'" +
                "}," + 
                "'@graph': [" + 
                    "{" +
                        "'@id': 'customform:sampleForm_requiredN3'," +
                        "'@type': [" +
                            "'forms:RequiredN3Pattern'," +
                            "'forms:FormComponent'" +
                        "]," +
                        "'customform:pattern': [" +
                            "'?subject ex:predicate0 ?objectVar . '" +
                        "]," +
                    "}" +
                "]" +
            "}";
    
    private final static String N3_CONFIG_NO_REQUIRED_COMPONENT = 
            "{" +
                "'@context': {" +
                    "'owl': 'http://www.w3.org/2002/07/owl#'" +
                "}," + 
                "'@graph': [" + 
                    "{" +
                        "'@id': 'customform:sampleForm_dynamicN3'," +
                        "'@type': [" +
                            "'forms:DynamicN3Pattern'," +
                            "'forms:FormComponent'" +
                        "]," +
                        "'customform:pattern': [" +
                            "'?subject ex:predicate0 ?objectVar . '," +
                            "'?objectVar rdfs:label ?objectLabel . '," +
                            "'?objectVar rdf:type ex:Class0 . '" +
                        "]," +
                        "'customform:dynamic_variables': [" +
                            "'?objectVar'," +
                            "'?objectLabel'" +
                        "]," + 
                        "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                                "@prefix  rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                                "@prefix ex: <http://example.org> . '" +
                        "}" +
                    "]" +
            "}";
    
    private final static String N3_CONFIG_NO_REQUIRED_OR_DYNAMIC_COMPONENT = 
            "{" +
                "'@context': {" +
                    "'owl': 'http://www.w3.org/2002/07/owl#'" +
                "}," + 
                "'@graph': [" + 
                    "{" +
                        "'@id': 'customform:sampleForm_optionalN3'," +
                        "'@type': [" +
                            "'forms:OptionalN3Pattern'," +
                            "'forms:FormComponent'" +
                        "]," +
                        "'customform:pattern': [" +
                            "'?subject ex:predicate0 ?objectVar . '" +
                        "]," +
                    "}" +
                "]" +
            "}";

    private static final String[] DYNAMIC_PATTERN = {
            "?subject ex:predicate0 ?objectVar . ",
            "?objectVar rdfs:label ?objectLabel . " 
        };
 
    private static final String[] DYNAMIC_PATTERN_WITH_NO_OBJECT_VAR = {
            "?subject ex:predicate0 ?object . ",
            "?object rdfs:label ?objectLabel . "
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
    public void formWithNoDynamicN3Component_Succeeds() throws Exception {
        JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_REQUIRED_COMPONENT);
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_localName>"});
        params.put("objectVar", new String[] {"<http://example.org/objectVar_localName0>"});
        preprocessor.requiredN3Component = config.getJSONArray("@graph").getJSONObject(0);
        preprocessor.updateConfiguration(params, config);
    }
    
    @Test
    public void formWithDynamicButNoRequiredN3Component_Succeeds() throws Exception {
        JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_NO_REQUIRED_COMPONENT);
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_localName>"});
        params.put("objectVar", new String[] {"<http://example.org/objectVar_localName0>"});
        params.put("objectLabel", new String[] {"objectLabel_value0"});
        preprocessor.dynamicN3Component = config.getJSONArray("@graph").getJSONObject(0);
        preprocessor.requiredN3Component = null;
        preprocessor.updateConfiguration(params, config);
    }
    
    @Test
    public void formWithNoRequiredOrDynamicN3Component_ThrowsException() throws Exception {
        expectException(FormConfigurationException.class, 
                "Configuration must include either a required or dynamic component");
        JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_NO_REQUIRED_OR_DYNAMIC_COMPONENT);
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_localName>"});
        params.put("objectVar", new String[] {"<http://example.org/objectVar_localName0>"});
        preprocessor.optionalN3Component = config.getJSONArray("@graph").getJSONObject(0);
        preprocessor.updateConfiguration(params, config);
    }
    
    @Test
    public void dynamicN3ComponentWithNoPattern_ThrowsException() throws Exception {
        expectException(FormConfigurationException.class, "Custom form pattern not defined or not a JSON array");
        preprocessor.getN3Pattern(getJSONObject(BASE_DYNAMIC_N3_COMPONENT));
    }
    
    @Test
    public void dynamicN3ComponentWithEmptyPattern_ThrowsException() throws Exception {
        expectException(FormConfigurationException.class, "Custom form pattern is empty");   
        preprocessor.getN3Pattern(getJSONObject(DYNAMIC_N3_COMPONENT_EMPTY_PATTERN));   
    }
    
    @Test 
    public void dynamicN3ComponentWithNoDynamicVariables_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Dynamic variables not defined or not a JSON array");
		preprocessor.getDynamicVars(getJSONObject(DYNAMIC_N3_COMPONENT_NO_DYNAMIC_VARIABLES));
    }
    
    @Test 
    public void dynamicN3ComponentWithEmptyDynamicVariables_ThrowsException() throws Exception {
    		expectException(FormConfigurationException.class, "Dynamic variables cannot be empty");
        preprocessor.getDynamicVars(getJSONObject(DYNAMIC_N3_COMPONENT_EMPTY_DYNAMIC_VARIABLES));
    }    
    
    @Test
    public void dynamicVarsWithNoObjectVar_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Dynamic vars must include \"?objectVar\"");
        preprocessor.validateDynamicVars(getJSONArray(DYNAMIC_VARS_WITH_NO_OBJECT_VAR));
    }    
    
    @Test
    public void dynamicN3TripleWithTwoTerms_ThrowsException() throws Exception {
        expectException(FormConfigurationException.class, "Triple in pattern does not have exactly three terms");
        preprocessor.validateDynamicN3Pattern(getJSONArray(INVALID_TRIPLE_WITH_TWO_TERMS));
    }
    
    @Test
    public void dynamicN3TripleWithFourTerms_ThrowsException() throws Exception {
        expectException(FormConfigurationException.class, "Triple in pattern does not have exactly three terms");
        preprocessor.validateDynamicN3Pattern(getJSONArray(INVALID_TRIPLE_WITH_FOUR_TERMS));
    }
    
    @Test
    public void dynamicN3TripleMissingFinalPeriod_ThrowsException() throws Exception {
    		expectException(FormConfigurationException.class, "Triple must end in a period");
    		preprocessor.checkAllTriplesWellFormed(getJSONArray(TRIPLE_MISSING_FINAL_PERIOD));
    }
    
    @Test
    public void dynamicPatternWithNoObjectVar_ThrowsException() throws Exception { 
        expectException(FormConfigurationException.class, "Dynamic pattern must contain dynamic object");
    		preprocessor.validateDynamicN3Pattern(getJSONArray(DYNAMIC_PATTERN_WITH_NO_OBJECT_VAR));
    }

    @Test
    public void noDynamicVarInFormParams_ThrowsException() throws Exception {
    		expectException(FormSubmissionException.class, "Form parameters must contain at least one value for");
		Map<String, String[]> params = new HashMap<>();
		params.put("subject", new String[] {"<http://example.org/subject_localName>"});  		
    		preprocessor.preprocessDynamicN3(getJSONObject(VALID_DYNAMIC_N3_COMPONENT), params);
    }
 
    @Test
    public void testDynamicVarParameterValueCount() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("subject", new String[] {"<http://example.org/subject_localName>"});  
		params.put("objectVar", new String[] {"<http://example.org/objectVar_localName0>", 
				"<http://example.org/objectVar_localName1>", "<http://example.org/objectVar_localName2>"}); 
		Assert.assertEquals(3, preprocessor.getDynamicVarParameterValueCount("?objectVar", params));
    }
    
    @Test
    public void invalidDynamicVarParameterValueCounts_ThrowsException() throws Exception {
		expectException(FormSubmissionException.class, "Dynamic variables must have the same number of values");
		JSONArray dynamicVars = getJSONArray(new String[] { "?objectVar", "?objectLabel" }); 
		Map<String, String[]> params = new HashMap<>();
		params.put("subject", new String[] {"<http://example.org/subject_localName>"});  
		params.put("objectVar", new String[] {"<http://example.org/objectVar_localName0>", 
				"<http://example.org/objectVar_localName1>"});
		params.put("objectLabel", new String[] {"objectLabel_value0"}); 
		preprocessor.validateDynamicParameterValues(dynamicVars, 2, params);
    }

	@Test
	public void testDynamicPatternWithOneValue() throws Exception {
		JSONArray pattern = getJSONArray(DYNAMIC_PATTERN);
		JSONArray dynamicVars = getJSONArray(new String[] {"?objectVar", "?objectLabel"});
		Set<String> actual = preprocessor.buildDynamicN3Triples(pattern, dynamicVars, 1);
		Set<String> expected = new HashSet<>(Arrays.asList(DYNAMIC_PATTERN));
		Assert.assertEquals(expected, actual);
	  }
	
	@Test
	public void testDynamicPatternWithMultipleValues() throws Exception {
		JSONArray pattern = getJSONArray(DYNAMIC_PATTERN);
		JSONArray dynamicVars = getJSONArray(new String[] {"?objectVar", "?objectLabel"});
		Set<String> actual = preprocessor.buildDynamicN3Triples(pattern, dynamicVars, 2);
		Set<String> expected = new HashSet<>();
		expected.add("?subject ex:predicate0 ?objectVar0 . ");
        expected.add("?objectVar0 rdfs:label ?objectLabel0 . ");
		expected.add("?subject ex:predicate0 ?objectVar1 . ");
        expected.add("?objectVar1 rdfs:label ?objectLabel1 . ");
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testDynamicPatternWithNonDynamicVariable() throws Exception {
		JSONArray pattern = getJSONArray(DYNAMIC_PATTERN);
		pattern.add("?objectVar ex:predicate1 ?entity0 . ");
		JSONArray dynamicVars = getJSONArray(new String[] {"?objectVar", "?objectLabel"});
		Set<String> actual = preprocessor.buildDynamicN3Triples(pattern, dynamicVars, 2);
		Set<String> expected = new HashSet<>();
		expected.add("?subject ex:predicate0 ?objectVar0 . ");
        expected.add("?objectVar0 rdfs:label ?objectLabel0 . ");
        expected.add("?objectVar0 ex:predicate1 ?entity0 . ");
		expected.add("?subject ex:predicate0 ?objectVar1 . ");
        expected.add("?objectVar1 rdfs:label ?objectLabel1 . ");
        expected.add("?objectVar1 ex:predicate1 ?entity0 . ");
		Assert.assertEquals(expected, actual);		
	}
	
	@Test
	public void testRewriteParameterMap() {
		JSONArray dynamicVars = getJSONArray(DYNAMIC_VARS);
		Map<String, String[]> params = new HashMap<>();
		params.put("subject", new String[] {"<http://example.org/subject_localName>"});  
		params.put("objectVar", new String[] {"<http://example.org/objectVar_localName0>", 
				"<http://example.org/objectVar_localName1>"});
		params.put("objectLabel", new String[] {"objectLabel_value0", "objectLabel_value1"}); 
		Map<String, String[]> actual = preprocessor.rewriteParameterMap(dynamicVars, params);
		Map<String, String[]> expected = new HashMap<>();
		expected.put("subject", new String[] {"<http://example.org/subject_localName>"});
		expected.put("objectVar0", new String[] { "<http://example.org/objectVar_localName0>" });
		expected.put("objectVar1", new String[] { "<http://example.org/objectVar_localName1>" });
		expected.put("objectLabel0", new String[] { "objectLabel_value0" });
		expected.put("objectLabel1", new String[] { "objectLabel_value1" });
		Assert.assertTrue(mapDeepEquals(expected, actual));
	}


    // ---------------------------------------------------------------------
    // Tests to ignore (illustrative only)
    //
    // These tests illustrate the difference between JSONArray.join() method 
    // and StringUtils.join(). The former retains quotes around each element
    // by default.
    // ---------------------------------------------------------------------

    /*
    @Test
    @Ignore
    public void testJSONArrayJoin() {
        JSONArray ja = new JSONArray();
        StringBuilder sb = new StringBuilder();
        ja.add("a");
        ja.add("b");
        ja.add("c");
        sb.append(ja.join(" ")); 
        String expected = "\"a\" \"b\" \"c\"";
        Assert.assertEquals(expected, sb.toString()); 
    }
        
    @Test
    @Ignore
    public void testJSONArrayJoinRemoveQuotes() {
        JSONArray ja = new JSONArray();
        StringBuilder sb = new StringBuilder();
        ja.add("a");
        ja.add("b");
        ja.add("c");
        sb.append(ja.join(" ", true)); // true needed to remove quotes around array items of JSONArray
        String expected = "a b c";
        Assert.assertEquals(expected, sb.toString());        
    }
    
    @Test
    @Ignore
    public void testArrayJoin() {
        String[] strings = new String[] {"a", "b", "c"};
        String actual = StringUtils.join(strings, " ");
        String expected = "a b c";
        Assert.assertEquals(expected, actual);
    }
    */
    
    
    // ---------------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------------


    private JSONObject getJSONObject(String jsonString) {
        return (JSONObject) JSONSerializer.toJSON(jsonString);
    }

    private JSONArray getJSONArray(String[] strings) {
    		return (JSONArray) JSONSerializer.toJSON(strings);    	
    }
    
    private boolean mapDeepEquals(Map<String, String[]> map1, Map<String, String[]> map2) {
    		if (! map1.keySet().equals(map2.keySet())) {
    			return false;
    		}
    		for (String key1 : map1.keySet()) {
    			String[] value1 = map1.get(key1);
    			String[] value2 = map2.get(key1);
    			if (! Arrays.deepEquals(value1, value2)) {
    				return false;
    			}
    		}
    		return true;
    }

}
