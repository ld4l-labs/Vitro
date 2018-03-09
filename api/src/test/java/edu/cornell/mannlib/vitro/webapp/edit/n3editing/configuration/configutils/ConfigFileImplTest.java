/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils;

import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.PROPERTY_DEPENDENCIES;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.PROPERTY_DYNAMIC_VARIABLES;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.PROPERTY_ID;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.PROPERTY_MAY_USE_NEW_RESOURCE;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.PROPERTY_PATTERN;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.PROPERTY_PREFIXES;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.PROPERTY_TYPE;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.PROPERTY_VAR_NAME;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_DEPENDENCIES;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_DYNAMIC_N3_PATTERN;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_LITERAL_FIELD;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_OPTIONAL_N3_PATTERN;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_REQUIRED_N3_PATTERN;
import static edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.TYPE_URI_FIELD;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.cornell.mannlib.vitro.testing.AbstractTestClass;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils.ConfigFile.InvalidConfigFileException;

/**
 * Create JSON config files and test them.
 * 
 * Use the helper classes, ConfigFileStructure and ConfigFileComponent, to
 * create the config files. Add some values to these classes, and Jackson will
 * turn them into the correct JSON.
 */
public class ConfigFileImplTest extends AbstractTestClass {
    /**
     * This is a basic configuration, which should parse successfully.
     * 
     * We will use the add(), remove(), and replace() methods to modify this to
     * test for edge cases.
     */
    private static final ConfigFileStructure BASIC_N3 = new ConfigFileStructure(
            new ConfigFileComponent("the_required_n3", TYPE_REQUIRED_N3_PATTERN)
                    .property(PROPERTY_PREFIXES, "@prefix req: <http://req#>")
                    .property(PROPERTY_PATTERN, "req:sub req:pred ?var1 .",
                            "req:sub req:pred ?var2 ."),
            new ConfigFileComponent("the_optional_n3", TYPE_OPTIONAL_N3_PATTERN)
                    .property(PROPERTY_PREFIXES,
                            "@prefix opt: <http://optional#>",
                            "@prefix opt2: <http://optional2#>")
                    .property(PROPERTY_PATTERN,
                            "opt:sub opt:pred ?var1 . opt:sub opt:pred ?var2 ."),
            new ConfigFileComponent("the_dynamic_n3", TYPE_DYNAMIC_N3_PATTERN)
                    .property(PROPERTY_PATTERN, "?var1 a ?var2 .")
                    .property(PROPERTY_DYNAMIC_VARIABLES, "?var1", "?var2"),
            new ConfigFileComponent("first_uri_fields", TYPE_URI_FIELD)
                    .property(PROPERTY_VAR_NAME, "uri1", "uri2"),
            new ConfigFileComponent("second_uri_fields", TYPE_URI_FIELD)
                    .property(PROPERTY_VAR_NAME, "uri3", "uri4")
                    .property(PROPERTY_MAY_USE_NEW_RESOURCE, true),
            new ConfigFileComponent("first_literal_fields", TYPE_LITERAL_FIELD)
                    .property(PROPERTY_VAR_NAME, "literal1", "literal2"),
            new ConfigFileComponent("dependencies", TYPE_DEPENDENCIES)
                    .property(PROPERTY_DEPENDENCIES, "uri1, uri2,uri3"));

    private ObjectMapper mapper = new ObjectMapper();

    private ConfigFileImpl cfi;

    // ----------------------------------------------------------------------
    // test required N3
    // ----------------------------------------------------------------------

    private ConfigFileStructure NO_REQUIRED_N3 = BASIC_N3
            .remove("the_required_n3");

    private ConfigFileStructure EMPTY_REQUIRED_N3 = BASIC_N3 //
            .replace(new ConfigFileComponent("the_required_n3",
                    TYPE_REQUIRED_N3_PATTERN) //
                            .property(PROPERTY_PREFIXES)
                            .property(PROPERTY_PATTERN));

    private ConfigFileStructure MULTIPLE_REQUIRED_N3 = BASIC_N3 //
            .add(new ConfigFileComponent("another_required_n3",
                    TYPE_REQUIRED_N3_PATTERN)
                            .property(PROPERTY_PREFIXES,
                                    "@prefix req2 <http://req2#>")
                            .property(PROPERTY_PATTERN,
                                    "req2:sub req2:pred ?var1 ."));

    @Test
    public void emptyRequiredN3_NoProblem() {
        parse(EMPTY_REQUIRED_N3);
        assertEquals(emptyList(), cfi.getRequiredN3().getPrefixes());
        assertEquals(emptyList(), cfi.getRequiredN3().getPattern());
        assertEquals("", cfi.getRequiredN3().getJoinedPrefixes());
        assertEquals("", cfi.getRequiredN3().getJoinedPattern());
        assertEquals("", cfi.getRequiredN3().getJoined());
    }

    @Test
    public void noRequiredN3_SameAsEmpty() {
        parse(NO_REQUIRED_N3);
        assertEquals("", cfi.getRequiredN3().getJoined());
    }

    @Test
    public void basicRequiredN3_NoProblem() {
        parse(BASIC_N3);
        assertEquals(list("@prefix req: <http://req#>"),
                cfi.getRequiredN3().getPrefixes());
        assertEquals(
                list("req:sub req:pred ?var1 .", "req:sub req:pred ?var2 ."),
                cfi.getRequiredN3().getPattern());
        assertEquals("@prefix req: <http://req#>",
                cfi.getRequiredN3().getJoinedPrefixes());
        assertEquals("req:sub req:pred ?var1 . req:sub req:pred ?var2 .",
                cfi.getRequiredN3().getJoinedPattern());
        assertEquals(
                "@prefix req: <http://req#> req:sub req:pred ?var1 . req:sub req:pred ?var2 .",
                cfi.getRequiredN3().getJoined());
    }

    @Test
    public void multipleRequiredN3_AreMerged() {
        parse(MULTIPLE_REQUIRED_N3);
        assertEquals(
                list("@prefix req: <http://req#>",
                        "@prefix req2 <http://req2#>"),
                cfi.getRequiredN3().getPrefixes());
        assertEquals(
                list("req:sub req:pred ?var1 .", "req:sub req:pred ?var2 .",
                        "req2:sub req2:pred ?var1 ."),
                cfi.getRequiredN3().getPattern());
    }

    // ----------------------------------------------------------------------
    // test optional N3
    // ----------------------------------------------------------------------

    private ConfigFileStructure NO_OPTIONAL_N3 = BASIC_N3
            .remove("the_optional_n3");

    private ConfigFileStructure EMPTY_OPTIONAL_N3 = BASIC_N3 //
            .replace(new ConfigFileComponent("the_optional_n3",
                    TYPE_OPTIONAL_N3_PATTERN) //
                            .property(PROPERTY_PREFIXES)
                            .property(PROPERTY_PATTERN));

    private ConfigFileStructure MULTIPLE_OPTIONAL_N3 = BASIC_N3 //
            .add(new ConfigFileComponent("another_optional_n3",
                    TYPE_OPTIONAL_N3_PATTERN)
                            .property(PROPERTY_PREFIXES,
                                    "@prefix opt3 <http://opt3#>")
                            .property(PROPERTY_PATTERN,
                                    "req2:sub opt3:pred ?var1 ."));

    @Test
    public void emptyOptionalN3_NoProblem() {
        parse(EMPTY_OPTIONAL_N3);
        assertEquals(false, cfi.hasOptionalN3());
        assertEquals(emptyList(), cfi.getOptionalN3().getPrefixes());
        assertEquals(emptyList(), cfi.getOptionalN3().getPattern());
        assertEquals("", cfi.getOptionalN3().getJoinedPrefixes());
        assertEquals("", cfi.getOptionalN3().getJoinedPattern());
        assertEquals("", cfi.getOptionalN3().getJoined());
    }

    @Test
    public void noOptionalN3_SameAsEmpty() {
        parse(NO_OPTIONAL_N3);
        assertEquals(false, cfi.hasOptionalN3());
        assertEquals("", cfi.getOptionalN3().getJoined());
    }

    @Test
    public void basicOptionalN3_NoProblem() {
        parse(BASIC_N3);
        assertEquals(true, cfi.hasOptionalN3());
        assertEquals(
                list("@prefix opt: <http://optional#>",
                        "@prefix opt2: <http://optional2#>"),
                cfi.getOptionalN3().getPrefixes());
        assertEquals(list("opt:sub opt:pred ?var1 . opt:sub opt:pred ?var2 ."),
                cfi.getOptionalN3().getPattern());
        assertEquals(
                "@prefix opt: <http://optional#> @prefix opt2: <http://optional2#>",
                cfi.getOptionalN3().getJoinedPrefixes());
        assertEquals("opt:sub opt:pred ?var1 . opt:sub opt:pred ?var2 .",
                cfi.getOptionalN3().getJoinedPattern());
        assertEquals(
                "@prefix opt: <http://optional#> @prefix opt2: <http://optional2#> "
                        + "opt:sub opt:pred ?var1 . opt:sub opt:pred ?var2 .",
                cfi.getOptionalN3().getJoined());
    }

    @Test
    public void multipleOptionalN3_AreMerged() {
        parse(MULTIPLE_OPTIONAL_N3);
        assertEquals(true, cfi.hasOptionalN3());
        assertEquals(
                list("@prefix opt: <http://optional#>",
                        "@prefix opt2: <http://optional2#>",
                        "@prefix opt3 <http://opt3#>"),
                cfi.getOptionalN3().getPrefixes());
        assertEquals(
                list("opt:sub opt:pred ?var1 . opt:sub opt:pred ?var2 .",
                        "req2:sub opt3:pred ?var1 ."),
                cfi.getOptionalN3().getPattern());
    }

    // ----------------------------------------------------------------------
    // test dynamic N3
    // ----------------------------------------------------------------------

    private ConfigFileStructure NO_DYNAMIC_N3 = BASIC_N3
            .remove("the_dynamic_n3");
    private ConfigFileStructure MULTIPLE_DYNAMIC_N3 = BASIC_N3
            .add(new ConfigFileComponent("another_dynamic_n3",
                    TYPE_DYNAMIC_N3_PATTERN));

    @Test
    public void emptyDynamicN3_NoProblem() {
        parse(NO_DYNAMIC_N3);
        assertEquals(false, cfi.hasDynamicN3());
        assertEquals(list(), cfi.getDynamicN3().getVariables());
    }

    @Test
    public void basicDynamicN3_NoProblem() {
        parse(BASIC_N3);
        assertEquals(true, cfi.hasDynamicN3());
        assertEquals(list("?var1", "?var2"), cfi.getDynamicN3().getVariables());
    }

    @Test
    public void multipleDynamicN3_ThrowsException() throws Exception {
        try {
            parse(MULTIPLE_DYNAMIC_N3);
            fail("Expected an exception.");
        } catch (Exception e) {
            assertInExceptionChain(e, InvalidConfigFileException.class,
                    "more than one");
        }
    }

    // ----------------------------------------------------------------------
    // test URI Fields and Literal fields
    // ----------------------------------------------------------------------

    private ConfigFileStructure NO_FIELDS = BASIC_N3 //
            .remove("first_uri_fields").remove("second_uri_fields")
            .remove("first_literal_fields");

    @Test
    public void noFields_NoProblem() {
        parse(NO_FIELDS);
        assertEquals(set(), cfi.getAllowedVarNames());
        assertEquals(set(), cfi.getNewResourceVarNames());
    }

    @Test
    public void basicFields_NoProblem() {
        parse(BASIC_N3);
        assertEquals(true, cfi.hasFieldComponent("uri1"));
        assertEquals(true, cfi.hasFieldComponent("uri2"));
        assertEquals(true, cfi.hasFieldComponent("uri3"));
        assertEquals(true, cfi.hasFieldComponent("uri4"));
        assertEquals(true, cfi.hasFieldComponent("literal1"));
        assertEquals(true, cfi.hasFieldComponent("literal2"));
        assertEquals(
                set("uri1", "uri2", "uri3", "uri4", "literal1", "literal2"),
                cfi.getAllowedVarNames());
        assertEquals(set("uri3", "uri4"), cfi.getNewResourceVarNames());
    }

    // ----------------------------------------------------------------------
    // test dependencies
    // ----------------------------------------------------------------------

    private ConfigFileStructure MORE_DEPENDENCIES = BASIC_N3
            .add(new ConfigFileComponent("more_dependencies", TYPE_DEPENDENCIES)
                    .property(PROPERTY_DEPENDENCIES, "uri1,literal1"));

    @Test
    public void basicDependencies_NoProblem() {
        parse(BASIC_N3);
        assertEquals(true, cfi.hasDependenciesFor("uri1"));
        assertEquals(true, cfi.hasDependenciesFor("uri2"));
        assertEquals(true, cfi.hasDependenciesFor("uri3"));
        assertEquals(false, cfi.hasDependenciesFor("uri4"));
        assertEquals(false, cfi.hasDependenciesFor("literal1"));
        assertEquals(false, cfi.hasDependenciesFor("literal2"));
        assertEquals(set("uri1", "uri2", "uri3"),
                cfi.getDependenciesFor("uri1"));
        assertEquals(set("uri1", "uri2", "uri3"),
                cfi.getDependenciesFor("uri2"));
        assertEquals(set("uri1", "uri2", "uri3"),
                cfi.getDependenciesFor("uri3"));
        assertEquals(set(), cfi.getDependenciesFor("literal1"));
    }

    @Test
    public void moreDependencies_GetMerged() {
        parse(MORE_DEPENDENCIES);
        assertEquals(true, cfi.hasDependenciesFor("uri1"));
        assertEquals(true, cfi.hasDependenciesFor("uri2"));
        assertEquals(true, cfi.hasDependenciesFor("uri3"));
        assertEquals(false, cfi.hasDependenciesFor("uri4"));
        assertEquals(true, cfi.hasDependenciesFor("literal1"));
        assertEquals(false, cfi.hasDependenciesFor("literal2"));
        assertEquals(set("uri1", "uri2", "uri3", "literal1"),
                cfi.getDependenciesFor("uri1"));
        assertEquals(set("uri1", "uri2", "uri3"),
                cfi.getDependenciesFor("uri2"));
        assertEquals(set("uri1", "uri2", "uri3"),
                cfi.getDependenciesFor("uri3"));
        assertEquals(set("uri1", "literal1"),
                cfi.getDependenciesFor("literal1"));
    }

    // ----------------------------------------------------------------------
    // Helper methods
    // ----------------------------------------------------------------------

    private void parse(ConfigFileStructure n3) {
        try {
            cfi = ConfigFileImpl.parse(mapper.writeValueAsString(n3));
        } catch (JsonProcessingException | InvalidConfigFileException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }

    @SuppressWarnings("unchecked")
    private <T> Set<T> set(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    private void assertInExceptionChain(Throwable e,
            Class<? extends Throwable> expectedCauseClass,
            String expectedMessageSubstring) {
        Throwable cause = e;
        while (cause != null) {
            if (expectedCauseClass.isInstance(cause)) {
                if (cause.getMessage() != null && cause.getMessage()
                        .contains(expectedMessageSubstring)) {
                    return;
                }
            }
            cause = cause.getCause();
        }
        throw new RuntimeException("Expected an exception of type '"
                + expectedCauseClass.getSimpleName()
                + "', with message containing '" + expectedMessageSubstring
                + "'.", e);
    }

    // ----------------------------------------------------------------------
    // Helper classes
    // ----------------------------------------------------------------------

    /**
     * Make sure that this is immutable, so calling add(), remove(), or
     * replace() creates a new structure without changing the existing one.
     */
    public static class ConfigFileStructure {
        @JsonProperty(value = "@context")
        private final Map<String, String> context;

        @JsonProperty(value = "@graph")
        private final List<ConfigFileComponent> graph;

        public ConfigFileStructure(ConfigFileComponent... components) {
            this.context = new HashMap<>();
            this.graph = new ArrayList<>(Arrays.asList(components));
        }

        public ConfigFileStructure(ConfigFileStructure other) {
            this.context = new HashMap<>(other.context);
            this.graph = new ArrayList<>(other.graph);
        }

        public ConfigFileStructure copy() {
            return new ConfigFileStructure(this);
        }

        public ConfigFileStructure add(ConfigFileComponent component) {
            ConfigFileStructure copy = copy();
            copy.graph.add(component);
            return copy;
        }

        public ConfigFileStructure remove(String id) {
            ConfigFileStructure copy = copy();
            Iterator<ConfigFileComponent> graphIt = copy.graph.iterator();
            while (graphIt.hasNext()) {
                String componentId = (String) graphIt.next().get(PROPERTY_ID);
                if (id.equals(componentId)) {
                    graphIt.remove();
                }
            }
            return copy;
        }

        public ConfigFileStructure replace(ConfigFileComponent component) {
            return remove(component.id()).add(component);
        }

        @Override
        public String toString() {
            return String.format("N3Structure[context=%s, graph=%s]", context,
                    graph);
        }
    }

    public static class ConfigFileComponent extends HashMap<String, Object> {
        public ConfigFileComponent(String id, String... types) {
            put(PROPERTY_ID, id);
            put(PROPERTY_TYPE, types);
        }

        public ConfigFileComponent(ConfigFileComponent other) {
            putAll(other);
        }

        public ConfigFileComponent copy() {
            return new ConfigFileComponent(this);
        }

        public ConfigFileComponent property(String propName, String value) {
            ConfigFileComponent copy = copy();
            copy.put(propName, value);
            return copy;
        }

        public ConfigFileComponent property(String propName, String... values) {
            ConfigFileComponent copy = copy();
            copy.put(propName, values);
            return copy;
        }

        public ConfigFileComponent property(String propName, boolean value) {
            ConfigFileComponent copy = copy();
            copy.put(propName, value);
            return copy;
        }

        public String id() {
            return (String) get(PROPERTY_ID);
        }

    }

}
