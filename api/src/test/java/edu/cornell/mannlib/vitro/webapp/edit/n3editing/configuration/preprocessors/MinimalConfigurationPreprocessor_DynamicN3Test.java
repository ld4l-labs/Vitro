package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.cornell.mannlib.vitro.testing.AbstractTestClass;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditSubmissionVTwoPreprocessor.FormConfigurationException;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditSubmissionVTwoPreprocessor.FormSubmissionException;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.ConfigFileDynamicN3Pattern;
import net.sf.json.JSONArray;
import net.sf.json.JSONSerializer;

/*
 * getPrefixes:
 * test non-empty prefix string
 * test empty prefix string
 */

/**
 * Tests class MinimalConfigurationPreprocessor
 *
 */
public class MinimalConfigurationPreprocessor_DynamicN3Test extends AbstractTestClass {
			
	// Strings representing dynamic N3 components do not include the entire configuration, but only the
	// portion (the component) that gets sent to MinimalConfigurationPreprocessor.buildDynamicN3Pattern(); that 
	// is, one element of the @graph array.
	
	private final static DynamicN3 BASE_DYNAMIC_N3_COMPONENT = new DynamicN3();
	
    private final static DynamicN3 DYNAMIC_N3_COMPONENT_WITH_EMPTY_PATTERN = 
            new DynamicN3()
            .addVarNames("?var1", "?var2");

	private final static DynamicN3 DYNAMIC_N3_COMPONENT_WITH_EMPTY_DYNAMIC_VARIABLES = 
	        new DynamicN3()
	        .addPatterns(
	                "?subject ex:predicate1 var1 .",
                    "?var1 rdfs:label ?var2 .",
                    "?var1 rdf:type ex:Class1 .");
	
	private final static DynamicN3 INVALID_TRIPLE_WITH_TWO_TERMS =
	        new DynamicN3()
	        .addPatterns(
                    "?subject ex:predicate1 ?var1 .",
                    "?var1 rdfs:label ?var2 .",
                    "?var1 ex:Class1 .")
	        .addVarNames("?var1", "?var2");

	private final static DynamicN3 INVALID_TRIPLE_WITH_FOUR_TERMS = 
            new DynamicN3()
            .addPatterns(
                    "?subject ex:predicate1 ?var1 .",
                    "?var1 rdfs:label ?var2 .",
                    "?var1 rdf:type ex:Class1 bogus .")
            .addVarNames("?var1", "?var2");
	
	private final static DynamicN3 VALID_DYNAMIC_N3_COMPONENT = 
            new DynamicN3()
            .addPatterns(
                    "?subject ex:predicate1 ?var1 .",
                    "?var1 rdfs:label ?var2 .",
                    "?var1 rdf:type ex:Class1 .")
            .addVarNames("?var1", "?var2");
	
	private final static String N3_CONFIG = 
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
	
	private static final List<String> DYNAMIC_VARS = 
			new ArrayList<>(Arrays.asList(new String[] {
			    "?var1", "?var2", "?var3"
			}));
	
	private static final List<String> DYNAMIC_PATTERN = 
	        new ArrayList<>(Arrays.asList(new String[] {
				"?subject ex:predicate1 ?var1 . ",
				"?var1 rdfs:label ?var2 . " ,
				"?var1 rdf:type ex:Class1 . "	 ,
				"?var2 ex:predicate2 ?var3 . "
			}));
	
	private static final List<String> TRIPLES_MISSING_FINAL_PERIOD = 
	        new ArrayList<>(Arrays.asList(new String[] {
				"?subject ex:predicate1 ?var1 ",
				"?var1 rdfs:label ?var2 . " ,
				"?var1 rdf:type ex:Class1"	 ,
				"?var2 ex:predicate2 ?var3 . "
			}));
	
	private MinimalConfigurationPreprocessor preprocessor;
	
	@Before
    public void setUp() {     
        this.preprocessor = new MinimalConfigurationPreprocessor(new EditConfigurationVTwo());
    }


    // ---------------------------------------------------------------------
    // The tests
    // ---------------------------------------------------------------------
	
	@Test
	public void dynamicN3ComponentWithEmptyPattern_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Custom form pattern is empty");		
		validateDynamicN3Component(DYNAMIC_N3_COMPONENT_WITH_EMPTY_PATTERN);	
	}
	
	@Test 
	public void dynamicN3ComponentWithEmptyDynamicVariables_ThrowsException() throws Exception {
		expectException(FormConfigurationException.class, "Dynamic variables array is empty.");
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
		getParameterValueCount(DYNAMIC_VARS.get(0), params);
	}
	
	@Test
	public void testDynamicVariableValueCountOfOne() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value"});
		int count = getParameterValueCount(DYNAMIC_VARS.get(0), params);
		Assert.assertEquals(count, 1);
	}
	
	@Test
	public void testDynamicVariableCountGreaterThanOne() throws Exception {
		Map<String, String[]> params = new HashMap<>();
		params.put("var1", new String[] {"var1_value1", "var_value2", "var1_value3"});
		params.put("var2", new String[] {"var2_value1", "var2_value2", "var2_value3"});
		int count = getParameterValueCount(DYNAMIC_VARS.get(0), params);
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
		Assert.assertEquals(StringUtils.join(DYNAMIC_PATTERN, " "), pattern);
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

	private void validateDynamicN3Component(ConfigFileDynamicN3Pattern component) throws Exception {
		preprocessor.validateDynamicN3Component(component);
	}
		
	private int getParameterValueCount(String var, Map<String, String[]> params) 
			throws Exception {
		return preprocessor.getDynamicVarParameterValueCount(var, params);
	}
	
	private int getDynamicVariableValueCount(List<String> dynamicVars, Map<String, String[]> params) 
			throws Exception {
		return preprocessor.getDynamicVariableValueCount(dynamicVars, params);
	}
	
	private String buildDynamicN3Pattern(List<String> array, List<String> vars, String prefixes, int paramValueCount) 
			throws Exception {
		return preprocessor.buildDynamicN3Pattern(array, vars, prefixes, paramValueCount);
	}
	
	private String buildDynamicN3Pattern(ConfigFileDynamicN3Pattern component, Map<String, String[]> params) 
			throws Exception {
		return preprocessor.buildDynamicN3Pattern(component, params);
	}

    private static class DynamicN3 implements ConfigFileDynamicN3Pattern {
        private List<String> prefixes = new ArrayList<>();
        private List<String> patterns = new ArrayList<>();
        private List<String> varNames = new ArrayList<>();
        
        public DynamicN3 addPrefixes(String...strings) {
            for (String s: strings) {
                prefixes.add(s);
            }
            return this;
        }

        public DynamicN3 addPatterns(String...strings) {
            for (String s: strings) {
                patterns.add(s);
            }
            return this;
        }
        
        public DynamicN3 addVarNames(String...strings) {
            for (String s: strings) {
                varNames.add(s);
            }
            return this;
        }
        
        @Override
        public List<String> getPattern() {
            return patterns;
        }

        @Override
        public List<String> getPrefixes() {
            return prefixes;
        }

        @Override
        public List<String> getVariables() {
            return varNames;
        }
    }
}
