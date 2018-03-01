package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.preprocessors;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
            
    // Strings representing dynamic N3 components do not include the entire configuration, but only the
    // portion (the component) that gets sent to MinimalConfigurationPreprocessor.buildDynamicN3Pattern(); that 
    // is, one element of the @graph array.
    
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
                    "'?subject ex:predicate1 ?uri1 . '" +
                    "]," +
                    "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                        "@prefix  rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
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
                    "'?subject ex:predicate1 ?uri1 . '," +
                    "'?uri1 rdfs:label ?lit1 . '," +
                    "'?uri1 rdf:type ex:Class1 . '" +
                    "]," +
                    "'customform:dynamic_variables': [" +
                    "]," + 
                    "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                        "@prefix  rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                        "@prefix ex: <http://example.org> . '" +
            "}";
    
    private final static String INVALID_TRIPLE_WITH_TWO_TERMS = 
            "{" +
                    "'@id': 'customform:sampleForm_dynamicN3'," +
                    "'@type': [" +
                        "'forms:DynamicN3Pattern'," +
                        "'forms:FormComponent'" +
                "]," +
                    "'customform:pattern': [" +
                    "'?subject ex:predicate1 ?uri1 . '," +
                    "'?uri1 rdfs:label ?lit1 . '," +
                    "'?uri1 ex:Class1 . '" +
                    "]," +
                    "'customform:dynamic_variables': [" +
                        "'?uri1'," +
                        "'?lit1'" +
                    "]," + 
                    "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                        "@prefix  rdfs: <http://www.w3.org/2000/01/rdf-schema#> . '" +
            "}";

    private final static String INVALID_TRIPLE_WITH_FOUR_TERMS = 
            "{" +
                    "'@id': 'customform:sampleForm_dynamicN3'," +
                    "'@type': [" +
                        "'forms:DynamicN3Pattern'," +
                        "'forms:FormComponent'" +
                "]," +
                    "'customform:pattern': [" +
                    "'?subject ex:predicate1 ?uri1 . '," +
                    "'?uri1 rdfs:label ?lit1 ?uri1 . '," +
                    "'?uri1 rdf:type ex:Class1 . '" +
                    "]," +
                    "'customform:dynamic_variables': [" +
                        "'?uri1'," +
                        "'?lit1'" +
                    "]," + 
                    "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                        "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                        "@prefix ex: <http://example.org> . '" +
            "}";
    
    private final static String VALID_DYNAMIC_N3_COMPONENT = 
            "{" +
                    "'@id': 'customform:sampleForm_dynamicN3'," +
                    "'@type': [" +
                        "'forms:DynamicN3Pattern'," +
                        "'forms:FormComponent'" +
                "]," +
                    "'customform:pattern': [" +
                    "'?subject ex:predicate1 ?uri1 . '," +
                    "'?uri1 rdfs:label ?lit1 . '," +
                    "'?uri1 rdf:type ex:Class1 . '" +
                    "]," +
                    "'customform:dynamic_variables': [" +
                        "'?uri1'," +
                        "'?lit1'" +
                    "]," +
                    "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                            "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                            "@prefix ex: <http://example.org> . '" +
            "}";
    
    private final static String VALID_DYNAMIC_N3_COMPONENT_NO_PREFIXES = 
            "{" +
                    "'@id': 'customform:sampleForm_dynamicN3'," +
                    "'@type': [" +
                        "'forms:DynamicN3Pattern'," +
                        "'forms:FormComponent'" +
                "]," +
                    "'customform:pattern': [" +
                    "'?subject <http://example.org/predicate1> ?uri1 . '," +
                    "'?uri1 <http://www.w3.org/2000/01/rdf-schema#label> ?lit1 . '," +
                    "'?uri1 <http://www.w3.org/2000/01/rdf-schema#type> <http://example.org/Class1> . '" +
                    "]," +
                    "'customform:dynamic_variables': [" +
                        "'?uri1'," +
                        "'?lit1'" +
                    "]" +
            "}";
    
    private final static String VALID_DYNAMIC_N3_COMPONENT_NO_PREFIXES_DEFINED = 
            "{" +
                    "'@id': 'customform:sampleForm_dynamicN3'," +
                    "'@type': [" +
                        "'forms:DynamicN3Pattern'," +
                        "'forms:FormComponent'" +
                "]," +
                    "'customform:pattern': [" +
                    "'?subject ex:predicate1 ?uri1 . '," +
                    "'?uri1 rdfs:label ?lit1 . '," +
                    "'?uri1 rdf:type ex:Class1 . '" +
                    "]," +
                    "'customform:dynamic_variables': [" +
                        "'?uri1'," +
                        "'?lit1'" +
                    "]" +
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
                                "'?subject ex:predicate1 ?uri1 . '" +
                        "]," +
                        "'customform:prefixes': '@prefix ex: <http://example.org> . '" +
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
                                        "'?subject ex:predicate1 ?uri1 . '," +
                                    "'?uri1 rdfs:label ?lit1 . '," +
                                    "'?uri1 rdf:type ex:Class1 . '" +
                                "]," +
                                "'customform:dynamic_variables': [" +
                                    "'?uri1'," +
                                    "'?lit1'" +
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
                                "'?subject ex:predicate1 ?uri1 . '" +
                        "]," +
                            "'customform:prefixes': '@prefix ex: <http://example.org> . '" +
                        "}" +
                    "]" +
            "}";
    
    private static final JSONArray DYNAMIC_VARS = 
            (JSONArray) JSONSerializer.toJSON(new String[] {"?uri1", "?lit1", "?uri2"});
    
    private static final JSONArray DYNAMIC_PATTERN = 
            (JSONArray) JSONSerializer.toJSON(new String[] {
                "?subject ex:predicate1 ?uri1 . ",
                "?uri1 rdfs:label ?lit1 . " ,
                "?uri1 rdf:type ex:Class1 . ",
                "?uri1 ex:predicate2 ?uri2 . "
            });
    
    private static final JSONArray TRIPLES_MISSING_FINAL_PERIOD = 
            (JSONArray) JSONSerializer.toJSON(new String[] {
                "?subject ex:predicate1 ?uri1 ",
                "?uri1 rdfs:label ?lit1 . "
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
        JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_REQUIRED_COMPONENT);
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value>"});
        preprocessor.requiredN3Component = config.getJSONArray("@graph").getJSONObject(0);
        preprocessor.updateConfiguration(params, config);
    }
    
    @Test
    public void formWithDynamicButNoRequiredN3Component_Succeeds() throws Exception {
        JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_NO_REQUIRED_COMPONENT);
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>"});
        params.put("lit1", new String[] {"lit1_value1"});
        preprocessor.dynamicN3Component = config.getJSONArray("@graph").getJSONObject(0);
        preprocessor.updateConfiguration(params, config);
    }
    
    @Test
    public void formWithDynamicButNoRequiredN3Component_ThrowException() throws Exception {
        expectException(FormConfigurationException.class, 
                "Configuration must include either a required or dynamic component");
        JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_NO_REQUIRED_OR_DYNAMIC_COMPONENT);
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>"});
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
        validateDynamicN3Component(DYNAMIC_N3_COMPONENT_EMPTY_PATTERN);    
    }
    
    @Test 
    public void dynamicN3ComponentWithoutDynamicVariables_ThrowsException() throws Exception {
        expectException(FormConfigurationException.class, "Dynamic variables not defined or not a JSON array");
        validateDynamicN3Component(DYNAMIC_N3_COMPONENT_NO_DYNAMIC_VARIABLES);
    }
    
    @Test 
    public void dynamicN3ComponentWithEmptyDynamicVariables_ThrowsException() throws Exception {
        expectException(FormConfigurationException.class, "Dynamic variables array is empty");
        validateDynamicN3Component(DYNAMIC_N3_COMPONENT_EMPTY_DYNAMIC_VARIABLES);
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
    public void validDynamicN3ComponentNoPrefixes_Succeeds() throws Exception {
        validateDynamicN3Component(VALID_DYNAMIC_N3_COMPONENT_NO_PREFIXES);        
    }
    
    @Test 
    // This should fail elsewhere but not in the MinimalConfigurationPreprocessor
    public void validDynamicN3ComponentNoPrefixesDefined_Succeeds() throws Exception {
        validateDynamicN3Component(VALID_DYNAMIC_N3_COMPONENT_NO_PREFIXES_DEFINED);        
    }

    @Test
    public void dynamicVariableWithNoValue_ThrowsException() throws Exception {
        expectException(FormSubmissionException.class, "Dynamic variable requires at least one value");
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("lit1", new String[] {"lit1_value1"});
        params.put("uri2", new String[] {"<http://example.org/uri2_value1>"});
        getParameterValueCount(0, DYNAMIC_VARS, params);
    }
    
    @Test
    public void testDynamicVariableValueCountOfOne() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>"});
        int count = getParameterValueCount(0, DYNAMIC_VARS, params);
        Assert.assertEquals(count, 1);
    }
    
    @Test
    public void testDynamicVariableCountGreaterThanOne() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});        
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>", "<http://example.org/uri1_value2>", 
                "<http://example.org/uri1_value3>"});
        params.put("lit1", new String[] {"lit1_value1", "lit1_value2", "lit1_value3"});
        int count = getParameterValueCount(0, DYNAMIC_VARS, params);
        Assert.assertEquals(count, 3);
    }
    
    @Test
    @Ignore
    public void nonMatchingDynamicVariableValueCounts_ThrowsException() throws Exception {
        expectException(FormSubmissionException.class, "Dynamic variables must have the same number of values");
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>", "<http://example.org/uri1_value2>"});
        params.put("lit1", new String[] {"lit1_value1", "lit1_value2", "lit1_value3"});
        getDynamicVariableValueCount(DYNAMIC_VARS, params);
    }
    
    @Test
    public void testDynamicPatternWithOneValue() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>"});
        params.put("lit1", new String[] {"lit1_value1"});
        params.put("uri2", new String[] {"<http://example.org/uri2_value1>"});
        String pattern = buildDynamicN3Pattern(DYNAMIC_PATTERN, DYNAMIC_VARS, "", 1);
        Assert.assertEquals(DYNAMIC_PATTERN.join(" ", true), pattern);
    }
    
    @Test
    @Ignore
    public void testJSONArrayJoin() {
        JSONArray ja = new JSONArray();
        StringBuilder sb = new StringBuilder();
        ja.add("a");
        ja.add("b");
        ja.add("c");
        sb.append(ja.join(" ", true)); // true needed to remove strings around array items of JSONArray
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
    
    @Test 
    public void testDynamicPatternWithMultipleValues() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>", "<http://example.org/uri1_value2>", 
                "<http://example.org/uri1_value3>"});
        params.put("lit1", new String[] {"lit1_value1", "lit1_value2", "lit1_value3"});
        params.put("uri2", new String[] {"<http://example.org/uri2_value1>", "<http://example.org/uri2_value2>", 
                "<http://example.org/uri2_value3>"});
        String expected = 
                "?subject ex:predicate1 ?uri10 . " +
                "?subject ex:predicate1 ?uri11 . " +
                "?subject ex:predicate1 ?uri12 . " +
                "?uri10 rdfs:label ?lit10 . " +
                "?uri11 rdfs:label ?lit11 . " +
                "?uri12 rdfs:label ?lit12 . " +
                "?uri10 rdf:type ex:Class1 . " +
                "?uri11 rdf:type ex:Class1 . " +
                "?uri12 rdf:type ex:Class1 . " +
                "?uri10 ex:predicate2 ?uri20 . " +
                "?uri11 ex:predicate2 ?uri21 . " +
                "?uri12 ex:predicate2 ?uri22 . ";                
        String pattern = buildDynamicN3Pattern(DYNAMIC_PATTERN, DYNAMIC_VARS, "", 3);
        Assert.assertEquals(expected, pattern);
    }
    
    @Test
    public void testDynamicPatternWithNonDynamicVariable() throws Exception {
        JSONArray dynamicVars = (JSONArray) JSONSerializer.toJSON(new String[] {"?uri1", "?lit1"});
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>", "<http://example.org/uri1_value2>", 
                "<http://example.org/uri1_value3>"});
        params.put("lit1", new String[] {"lit1_value1", "lit1_value2", "lit1_value3"});
        String expected = 
                "?subject ex:predicate1 ?uri10 . " +
                "?subject ex:predicate1 ?uri11 . " +
                "?subject ex:predicate1 ?uri12 . " +
                "?uri10 rdfs:label ?lit10 . " +
                "?uri11 rdfs:label ?lit11 . " +
                "?uri12 rdfs:label ?lit12 . " +
                "?uri10 rdf:type ex:Class1 . " +
                "?uri11 rdf:type ex:Class1 . " +
                "?uri12 rdf:type ex:Class1 . " +
                "?uri10 ex:predicate2 ?uri2 . " +
                "?uri11 ex:predicate2 ?uri2 . " +
                "?uri12 ex:predicate2 ?uri2 . ";                
        String pattern = buildDynamicN3Pattern(DYNAMIC_PATTERN, dynamicVars, "", 3);
        Assert.assertEquals(expected, pattern);        
    }
    
    @Test
    public void dynamicN3TripleMissingFinalPeriod_Succeeds() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>", "<http://example.org/uri1_value2>"});
        params.put("lit1", new String[] {"lit1_value1", "lit1_value2"});
        String expected = 
                "?subject ex:predicate1 ?uri10 . " +
                "?subject ex:predicate1 ?uri11 . " +
                "?subject ex:predicate1 ?uri12 . " +
                "?uri10 rdfs:label ?lit10 . " +
                "?uri11 rdfs:label ?lit11 . " +
                "?uri12 rdfs:label ?lit12 . ";
        String pattern = buildDynamicN3Pattern(TRIPLES_MISSING_FINAL_PERIOD, DYNAMIC_VARS, "", 3);
        Assert.assertEquals(expected, pattern);
    }

    @Test
    public void testDynamicN3Component() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>", "<http://example.org/uri1_value2>", 
                "<http://example.org/uri1_value3>"});
        params.put("lit1", new String[] {"lit1_value1", "lit1_value2", "lit1_value3"});
        String pattern = buildDynamicN3Pattern(VALID_DYNAMIC_N3_COMPONENT, params);
        String expected = 
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                    "@prefix ex: <http://example.org> . " +    
                "?subject ex:predicate1 ?uri10 . " +
                "?subject ex:predicate1 ?uri11 . " +
                "?subject ex:predicate1 ?uri12 . " +
                "?uri10 rdfs:label ?lit10 . " +
                "?uri11 rdfs:label ?lit11 . " +
                "?uri12 rdfs:label ?lit12 . " +
                "?uri10 rdf:type ex:Class1 . " +
                "?uri11 rdf:type ex:Class1 . " +
                "?uri12 rdf:type ex:Class1 . ";
        Assert.assertEquals(expected, pattern);
    }

    public void testDynamicN3ComponentNoPrefixes() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("uri1", new String[] {"<http://example.org/uri1_value1>", "<http://example.org/uri1_value2>", 
                "<http://example.org/uri1_value3>"});
        params.put("lit1", new String[] {"lit1_value1", "lit1_value2", "lit1_value3"});
        String pattern = buildDynamicN3Pattern(VALID_DYNAMIC_N3_COMPONENT_NO_PREFIXES, params);
        String expected = 
                "?subject ex:predicate1 ?uri10 . " +
                "?subject ex:predicate1 ?uri11 . " +
                "?subject ex:predicate1 ?uri12 . " +
                "?uri10 rdfs:label ?lit10 . " +
                "?uri11 rdfs:label ?lit11 . " +
                "?uri12 rdfs:label ?lit12 . " +
                "?uri10 rdf:type ex:Class1 . " +
                "?uri11 rdf:type ex:Class1 . " +
                "?uri12 rdf:type ex:Class1 . ";
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
