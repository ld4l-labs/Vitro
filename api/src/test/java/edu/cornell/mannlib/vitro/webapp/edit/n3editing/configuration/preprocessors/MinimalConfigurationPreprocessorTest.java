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
                    "'?subject ex:predicate0 ?entity0 . '" +
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
                    "'?subject ex:predicate0 ?entity0 . '," +
                    "'?entity0 rdfs:label ?label0 . '," +
                    "'?entity0 rdf:type ex:Class0 . '" +
                "]," +
                "'customform:dynamic_variables': [" +
                "]," + 
                "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
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
                    "'?subject ex:predicate0 ?entity0 . '," +
                    "'?entity0 rdfs:label ?label0 . '," +
                    "'?entity0 ex:Class0 . '" +
                "]," +
                "'customform:dynamic_variables': [" +
                    "'?entity0'," +
                    "'?label0'" +
                "]," + 
                "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                    "@prefix ex: <http://example.org> . '" +
            "}";

    private final static String INVALID_TRIPLE_WITH_FOUR_TERMS = 
            "{" +
                "'@id': 'customform:sampleForm_dynamicN3'," +
                "'@type': [" +
                    "'forms:DynamicN3Pattern'," +
                    "'forms:FormComponent'" +
                "]," +
                "'customform:pattern': [" +
                    "'?subject ex:predicate0 ?entity0 . '," +
                    "'?entity0 rdfs:label ?label0 ?entity0 . '," +
                    "'?entity0 rdf:type ex:Class0 . '" +
                "]," +
                "'customform:dynamic_variables': [" +
                    "'?entity0'," +
                    "'?label0'" +
                "]," + 
                "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                    "@prefix ex: <http://example.org> . '" +
            "}";
    
    private final static String VALID_DYNAMIC_N3_COMPONENT_1 = 
            "{" +
                "'@id': 'customform:sampleForm_dynamicN3'," +
                "'@type': [" +
                    "'forms:DynamicN3Pattern'," +
                    "'forms:FormComponent'" +
                "]," +
                "'customform:pattern': [" +
                    "'?subject ex:predicate0 ?entity0 . '," +
                    "'?entity0 rdfs:label ?label0 . '," +
                    "'?entity0 rdf:type ex:Class0 . '" +
                "]," +
                "'customform:dynamic_variables': [" +
                    "'?entity0'," +
                    "'?label0'" +
                "]," +
                "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                    "@prefix ex: <http://example.org> . '" +
            "}";
    
    private final static String VALID_DYNAMIC_N3_COMPONENT_2 = 
            "{" +
                "'@id': 'customform:sampleForm_dynamicN3'," +
                "'@type': [" +
                    "'forms:DynamicN3Pattern'," +
                    "'forms:FormComponent'" +
                "]," +
                "'customform:pattern': [" +
                    "'?subject ex:predicate0 ?entity0 . '," +
                    "'?entity0 rdfs:label ?label0 . '," +
                "]," +
                "'customform:dynamic_variables': [" +
                    "'?entity0'," +
                    "'?label0'" +
                "]," +
                "'customform:prefixes': '@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " + 
                    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                    "@prefix ex: <http://example.org> . '" +
            "}";
    
    private final static String VALID_DYNAMIC_N3_COMPONENT_NO_PREFIXES_USED = 
            "{" +
                "'@id': 'customform:sampleForm_dynamicN3'," +
                "'@type': [" +
                    "'forms:DynamicN3Pattern'," +
                    "'forms:FormComponent'" +
                "]," +
                "'customform:pattern': [" +
                    "'?subject ex:predicate0 ?entity0 . '," +
                    "'?entity0 <http://www.w3.org/2000/01/rdf-schema#label> ?label0 . '," +
                    "'?entity0 <http://www.w3.org/2000/01/rdf-schema#type> <http://example.org/Class0> . '" +
                "]," +
                "'customform:dynamic_variables': [" +
                    "'?entity0'," +
                    "'?label0'" +
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
                    "'?subject ex:predicate0 ?entity0 . '," +
                    "'?entity0 rdfs:label ?label0 . '," +
                    "'?entity0 rdf:type ex:Class0 . '" +
                "]," +
                "'customform:dynamic_variables': [" +
                    "'?entity0'," +
                    "'?label0'" +
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
                            "'?subject ex:predicate0 ?entity0 . '" +
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
                            "'?subject ex:predicate0 ?entity0 . '," +
                            "'?entity0 rdfs:label ?label0 . '," +
                            "'?entity0 rdf:type ex:Class0 . '" +
                        "]," +
                        "'customform:dynamic_variables': [" +
                            "'?entity0'," +
                            "'?label0'" +
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
                            "'?subject ex:predicate0 ?entity0 . '" +
                        "]," +
                    "}" +
                "]" +
            "}";
    
    private static final JSONArray DYNAMIC_VARS = 
            (JSONArray) JSONSerializer.toJSON(new String[] {"?entity0", "?label0", "?entity1"});
    
    private static final JSONArray DYNAMIC_PATTERN = 
            (JSONArray) JSONSerializer.toJSON(new String[] {
                "?subject ex:predicate0 ?entity0 . ",
                "?entity0 rdfs:label ?label0 . " ,
                "?entity0 rdf:type ex:Class0 . ",
                "?entity0 ex:predicate1 ?entity1 . "
            });
    
    private static final JSONArray TRIPLES_MISSING_FINAL_PERIOD = 
            (JSONArray) JSONSerializer.toJSON(new String[] {
                "?subject ex:predicate0 ?entity0 ",
                "?entity0 rdfs:label ?label0 . "
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
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>"});
        preprocessor.requiredN3Component = config.getJSONArray("@graph").getJSONObject(0);
        preprocessor.updateConfiguration(params, config);
    }
    
    @Test
    public void formWithDynamicButNoRequiredN3Component_Succeeds() throws Exception {
        JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_NO_REQUIRED_COMPONENT);
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>"});
        params.put("label0", new String[] {"label0_value0"});
        preprocessor.dynamicN3Component = config.getJSONArray("@graph").getJSONObject(0);
        preprocessor.updateConfiguration(params, config);
    }
    
    @Test
    public void formWithDynamicButNoRequiredN3Component_ThrowException() throws Exception {
        expectException(FormConfigurationException.class, 
                "Configuration must include either a required or dynamic component");
        JSONObject config = (JSONObject) JSONSerializer.toJSON(N3_CONFIG_NO_REQUIRED_OR_DYNAMIC_COMPONENT);
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>"});
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
        validateDynamicN3Component(VALID_DYNAMIC_N3_COMPONENT_1);        
    }
    
    @Test 
    public void validDynamicN3ComponentNoPrefixes_Succeeds() throws Exception {
        validateDynamicN3Component(VALID_DYNAMIC_N3_COMPONENT_NO_PREFIXES_USED);        
    }
    
    @Test 
    // This should fail elsewhere but not in the MinimalConfigurationPreprocessor
    public void validDynamicN3ComponentNoPrefixesDefined_Succeeds() throws Exception {
        validateDynamicN3Component(VALID_DYNAMIC_N3_COMPONENT_NO_PREFIXES_DEFINED);        
    }

    @Test
    public void entityWithNoValue_Succeeds() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>"});
        params.put("label0", new String[] {"label0_value0"});
        params.put("entity1", new String[] {"<http://example.org/entity1_local_name0>"});
        getParameterValueCount(0, DYNAMIC_VARS, params);
    }
    
    @Test
    public void testDynamicVariableValueCountOfOne() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>"});
        params.put("entity1", new String[] {"<http://example.org/entity1_local_name0>"});
        int count = getParameterValueCount(0, DYNAMIC_VARS, params);
        Assert.assertEquals(count, 1);
    }
    
    @Test
    public void testDynamicVariableCountGreaterThanOne() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});  
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>",  
        		"<http://example.org/entity0_local_name1>", "<http://example.org/entity0_local_name2>"});
        params.put("label0", new String[] {"label0_value0", "label0_value1", "label0_value2"});
        params.put("entity1", new String[] {"<http://example.org/entity1_local_name0>", 
        		"<http://example.org/entity1_local_name1>", "<http://example.org/entity1_local_name2>"});
        int count = getParameterValueCount(0, DYNAMIC_VARS, params);
        Assert.assertEquals(count, 3);
    }
    
    @Test
    public void testDynamicPatternWithOneValue() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>"});
        params.put("label0", new String[] {"label0_value0"});
        params.put("entity1", new String[] {"<http://example.org/entity1_local_name0>"});
        String pattern = buildDynamicN3Pattern(DYNAMIC_PATTERN, DYNAMIC_VARS, "", 1);
        Assert.assertEquals(DYNAMIC_PATTERN.join(" ", true), pattern);
    }
    
    
    @Test 
    public void testDynamicPatternWithMultipleValues() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>", "<http://example.org/entity0_local_name1>", 
                "<http://example.org/entity0_local_name2>"});
        params.put("label0", new String[] {"label0_value0", "label0_value1", "label0_value2"});
        params.put("entity1", new String[] {"<http://example.org/entity1_local_name0>", "<http://example.org/entity1_local_name1>", 
                "<http://example.org/entity1_local_name2>"});
        String expected = 
                "?subject ex:predicate0 ?entity00 . " +
                "?subject ex:predicate0 ?entity01 . " +
                "?subject ex:predicate0 ?entity02 . " +
                "?entity00 rdfs:label ?label00 . " +
                "?entity01 rdfs:label ?label01 . " +
                "?entity02 rdfs:label ?label02 . " +
                "?entity00 rdf:type ex:Class0 . " +
                "?entity01 rdf:type ex:Class0 . " +
                "?entity02 rdf:type ex:Class0 . " +
                "?entity00 ex:predicate1 ?entity10 . " +
                "?entity01 ex:predicate1 ?entity11 . " +
                "?entity02 ex:predicate1 ?entity12 . ";                
        String pattern = buildDynamicN3Pattern(DYNAMIC_PATTERN, DYNAMIC_VARS, "", 3);
        Assert.assertEquals(expected, pattern);
    }
    
    @Test
    public void testDynamicPatternWithNonDynamicVariable() throws Exception {
        JSONArray dynamicVars = (JSONArray) JSONSerializer.toJSON(new String[] {"?entity0", "?label0"});
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>", "<http://example.org/entity0_local_name1>", 
                "<http://example.org/entity0_local_name2>"});
        params.put("label0", new String[] {"label0_value0", "label0_value1", "label0_value2"});
        params.put("entity1", new String[] {"<http://example.org/entity1_local_name0>"});
        String expected = 
                "?subject ex:predicate0 ?entity00 . " +
                "?subject ex:predicate0 ?entity01 . " +
                "?subject ex:predicate0 ?entity02 . " +
                "?entity00 rdfs:label ?label00 . " +
                "?entity01 rdfs:label ?label01 . " +
                "?entity02 rdfs:label ?label02 . " +
                "?entity00 rdf:type ex:Class0 . " +
                "?entity01 rdf:type ex:Class0 . " +
                "?entity02 rdf:type ex:Class0 . " +
                "?entity00 ex:predicate1 ?entity1 . " +
                "?entity01 ex:predicate1 ?entity1 . " +
                "?entity02 ex:predicate1 ?entity1 . ";                
        String pattern = buildDynamicN3Pattern(DYNAMIC_PATTERN, dynamicVars, "", 3);
        Assert.assertEquals(expected, pattern);        
    }
    
    @Test
    public void dynamicN3TripleMissingFinalPeriod_Succeeds() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] 
        		{"<http://example.org/entity0_local_name0>", "<http://example.org/entity0_local_name1>"});
        params.put("label0", new String[] {"label0_value0", "label0_value1"});
        JSONArray dynamicVars =  
        		(JSONArray) JSONSerializer.toJSON(new String[] {"?entity0", "?label0"});
        String expected = 
                "?subject ex:predicate0 ?entity00 . " +
                "?subject ex:predicate0 ?entity01 . " +
                "?entity00 rdfs:label ?label00 . " +
                "?entity01 rdfs:label ?label01 . ";
        String pattern = buildDynamicN3Pattern(TRIPLES_MISSING_FINAL_PERIOD, dynamicVars, "", 2);
        Assert.assertEquals(expected, pattern);
    }

    @Test
    public void testDynamicN3Component() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>", "<http://example.org/entity0_local_name1>", 
                "<http://example.org/entity0_local_name2>"});
        params.put("label0", new String[] {"label0_value0", "label0_value1", "label0_value2"});
        String pattern = buildDynamicN3Pattern(VALID_DYNAMIC_N3_COMPONENT_1, params);
        String expected = 
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                "@prefix ex: <http://example.org> . " +    
                "?subject ex:predicate0 ?entity00 . " +
                "?subject ex:predicate0 ?entity01 . " +
                "?subject ex:predicate0 ?entity02 . " +
                "?entity00 rdfs:label ?label00 . " +
                "?entity01 rdfs:label ?label01 . " +
                "?entity02 rdfs:label ?label02 . " +
                "?entity00 rdf:type ex:Class0 . " +
                "?entity01 rdf:type ex:Class0 . " +
                "?entity02 rdf:type ex:Class0 . ";
        Assert.assertEquals(expected, pattern);
    }

    @Test
    public void testDynamicN3ComponentNoPrefixesUsed() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>", "<http://example.org/entity0_local_name1>", 
                "<http://example.org/entity0_local_name2>"});
        params.put("label0", new String[] {"label0_value0", "label0_value1", "label0_value2"});
        String pattern = buildDynamicN3Pattern(VALID_DYNAMIC_N3_COMPONENT_NO_PREFIXES_USED, params);
        String expected = 
                "?subject ex:predicate0 ?entity00 . " +
                "?subject ex:predicate0 ?entity01 . " +
                "?subject ex:predicate0 ?entity02 . " +
                "?entity00 <http://www.w3.org/2000/01/rdf-schema#label> ?label00 . " +
                "?entity01 <http://www.w3.org/2000/01/rdf-schema#label> ?label01 . " +
                "?entity02 <http://www.w3.org/2000/01/rdf-schema#label> ?label02 . " +
                "?entity00 <http://www.w3.org/2000/01/rdf-schema#type> <http://example.org/Class0> . " +
                "?entity01 <http://www.w3.org/2000/01/rdf-schema#type> <http://example.org/Class0> . " +
                "?entity02 <http://www.w3.org/2000/01/rdf-schema#type> <http://example.org/Class0> . ";
        Assert.assertEquals(expected, pattern);
    }
    
    @Test
    public void testLargestDynamicVarParameterValueCount() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>", "<http://example.org/entity0_local_name1>"});
        params.put("label0", new String[] {"label0_value0"});
        params.put("entity1", new String[] {"<http://example.org/entity1_local_name0>", "<http://example.org/entity1_local_name1>", 
    				"<http://example.org/entity1_local_name2>"});
        int largest = getLargestDynamicVariableValueCount(DYNAMIC_VARS, params);
        Assert.assertEquals(3, largest);    	
    }
    
    @Test
    @Ignore
    // Not yet sure what should happen in this case - need to test with a form that can send multiple values.
    public void testUnequalDynamicVarParameterValueCountUses() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("subject", new String[] {"<http://example.org/subject_value>"});
        params.put("entity0", new String[] {"<http://example.org/entity0_local_name0>", "<http://example.org/entity0_local_name1>"});
        params.put("label0", new String[] {"label0_value0", "label0_value1", "label0_value2"});
        String pattern = buildDynamicN3Pattern(VALID_DYNAMIC_N3_COMPONENT_2, params);
        String expected = 
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . " +
                "@prefix ex: <http://example.org> . " +    
                "?subject ex:predicate0 ?entity00 . " +
                "?subject ex:predicate0 ?entity01 . " +
                "?entity00 rdfs:label ?label00 . " +
                "?entity01 rdfs:label ?label01 . " +
                "?entity00 rdf:type ex:Class0 . " +
                "?entity01 rdf:type ex:Class0 . ";
        Assert.assertEquals(expected, pattern);	
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
    
    private int getLargestDynamicVariableValueCount(JSONArray dynamicVars, Map<String, String[]> params) 
            throws Exception {
        return preprocessor.getLargestDynamicVariableValueCount(dynamicVars, params);
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
