/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The standard implementation of ConfigFile
 */
public class ConfigFileImpl implements ConfigFile {

    /**
     * The factory method.
     */
    public static ConfigFileImpl parse(String jsonFileContents)
            throws InvalidConfigFileException {
        try {
            return new ObjectMapper().readValue(jsonFileContents,
                    ConfigFileImpl.class);
        } catch (IOException e) {
            throw new InvalidConfigFileException(
                    "Failed to parse the config file", e);
        }
    }

    /**
     * Jackson will call this method when asked to parse a JacksonConfigFile.
     */
    @JsonCreator
    @SuppressWarnings("unchecked")
    public static ConfigFileImpl createFromFile(
            Map<String, Object> fileBindings)
            throws InvalidConfigFileException {
        return new ConfigFileImpl(
                (List<Map<String, Object>>) fileBindings.get("@graph"));
    }

    /**
     * Some fields (for example, @type) may be a single string or an array of
     * strings (deserialized as a List).
     * 
     * @return an unmodifiable list. Never null.
     */
    @SuppressWarnings("unchecked")
    private static List<String> toListOfStrings(Object binding) {
        if (binding == null) {
            return Collections.emptyList();
        } else if (binding instanceof List) {
            return Collections.unmodifiableList((List<String>) binding);
        } else {
            return Collections.singletonList(String.valueOf(binding));
        }
    }

    /**
     * Parse a string of names, delimited by commas with optional blanks.
     */
    private static Set<String> parseCommaSeparatedSet(String names) {
        String[] namesArray = names.replaceAll("\\s+", "").split(",");
        return new HashSet<>(Arrays.asList(namesArray));
    }

    /**
     * Check to see whether the binding is a true Boolean.
     */
    private static boolean isTrueBoolean(Object binding) {
        return (binding instanceof Boolean) && (Boolean) binding;
    }

    private UriFieldMap uris = new UriFieldMap();
    private FieldMap<LiteralField> literals = new FieldMap<>();
    private N3Patterns required = new N3Patterns();
    private N3Patterns optional = new N3Patterns();
    private DynamicN3Patterns dynamic = new DynamicN3Patterns();
    private Dependencies dependencies = new Dependencies();
    private List<Unknown> unknowns = new ArrayList<>();

    /**
     * Build the config file structures, starting from the bindings from
     * the @graph section of the JSON-LD.
     */
    public ConfigFileImpl(List<Map<String, Object>> graphBindings)
            throws InvalidConfigFileException {
        for (Map<String, Object> component : graphBindings) {
            List<String> types = toListOfStrings(component.get("@type"));

            if (types.contains(TYPE_REQUIRED_N3_PATTERN)) {
                required.addComponent(component);
            } else if (types.contains(TYPE_OPTIONAL_N3_PATTERN)) {
                optional.addComponent(component);
            } else if (types.contains(TYPE_DYNAMIC_N3_PATTERN)) {
                dynamic.addComponent(component);
            } else if (types.contains(TYPE_URI_FIELD)) {
                uris.add(new UriField(component));
            } else if (types.contains(TYPE_LITERAL_FIELD)) {
                literals.add(new LiteralField(component));
            } else if (types.contains(TYPE_DEPENDENCIES)) {
                dependencies.addComponent(component);
            } else {
                unknowns.add(new Unknown(component));
            }
        }
    }

    @Override
    public boolean hasFieldComponent(String varName) {
        return getFieldComponent(varName) == null;
    }

    @Override
    public ConfigFileField getFieldComponent(String varName) {
        if (uris.hasField(varName)) {
            return uris.getField(varName);
        } else {
            return literals.getField(varName);
        }
    }

    @Override
    public boolean hasDependenciesFor(String varName) {
        return getDependenciesFor(varName).isEmpty();
    }

    @Override
    public Set<String> getDependenciesFor(String varName) {
        return dependencies.getFor(varName);
    }

    @Override
    public ConfigFileN3Pattern getRequiredN3() {
        return required;
    }

    @Override
    public boolean hasOptionalN3() {
        return !optional.getPattern().isEmpty();
    }

    @Override
    public ConfigFileN3Pattern getOptionalN3() {
        return optional;
    }

    @Override
    public boolean hasDynamicN3() {
        return dynamic.getVariables().isEmpty();
    }

    @Override
    public ConfigFileDynamicN3Pattern getDynamicN3() {
        return dynamic;
    }

    @Override
    public Set<String> getNewResourceVarNames() {
        return uris.getNewResourceNames();
    }

    @Override
    public Set<String> getAllowedVarNames() {
        Set<String> varNames = new HashSet<>();
        varNames.addAll(uris.getVarNames());
        varNames.addAll(literals.getVarNames());
        return varNames;
    }

    private static abstract class Component {
        private final String id;
        private final List<String> types;
        private final Map<String, Object> properties;

        @SuppressWarnings("unused")
        Component(Map<String, Object> component)
                throws InvalidConfigFileException {
            Map<String, Object> props = new HashMap<>(component);
            this.id = (String) props.remove(PROPERTY_ID);
            this.types = toListOfStrings(props.remove(PROPERTY_TYPE));
            this.properties = Collections.unmodifiableMap(props);
        }

        public boolean hasType(String type) {
            return types.contains(type);
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "[id='" + id + "', types="
                    + types + ", properties=" + properties + "]";
        }
    }

    private static abstract class Field extends Component {
        private final List<String> varNames;

        Field(Map<String, Object> component) throws InvalidConfigFileException {
            super(component);
            this.varNames = toListOfStrings(component.get(PROPERTY_VAR_NAME));
        }

        public List<String> getVarNames() {
            return new ArrayList<>(varNames);
        }
    }

    private static class UriField extends Field
            implements ConfigFile.ConfigFileField {
        private final boolean eligibleForNewResource;

        public UriField(Map<String, Object> component)
                throws InvalidConfigFileException {
            super(component);
            eligibleForNewResource = isTrueBoolean(
                    component.get(PROPERTY_MAY_USE_NEW_RESOURCE));
        }

        public boolean mayUseNewResource() {
            return eligibleForNewResource;
        }
    }

    private static class LiteralField extends Field
            implements ConfigFile.ConfigFileField {

        public LiteralField(Map<String, Object> component)
                throws InvalidConfigFileException {
            super(component);
        }

    }

    private static class FieldMap<T extends Field> {
        protected Map<String, T> fields = new HashMap<>();

        public void add(T field) {
            for (String varName : field.getVarNames()) {
                fields.put(varName, field);
            }
        }

        public boolean hasField(String varName) {
            return fields.containsKey(varName);
        }

        public T getField(String varName) {
            return fields.get(varName);
        }

        public Set<String> getVarNames() {
            return new HashSet<>(fields.keySet());
        }

        @Override
        public String toString() {
            return "FieldMap[fields=" + fields + "]";
        }
    }

    private static class UriFieldMap extends FieldMap<UriField> {
        public Set<String> getNewResourceNames() {
            Set<String> names = new HashSet<>();
            for (UriField uriField : fields.values()) {
                if (uriField.mayUseNewResource()) {
                    names.addAll(uriField.getVarNames());
                }
            }
            return names;
        }

    }

    private static class N3Patterns implements ConfigFile.ConfigFileN3Pattern {
        private final List<String> prefixList = new ArrayList<>();
        private final List<String> patternList = new ArrayList<>();

        @SuppressWarnings("unused")
        public void addComponent(Map<String, Object> component)
                throws InvalidConfigFileException {
            this.patternList
                    .addAll(toListOfStrings(component.get(PROPERTY_PATTERN)));
            this.prefixList
                    .addAll(toListOfStrings(component.get(PROPERTY_PREFIXES)));
        }

        @Override
        public List<String> getPattern() {
            return new ArrayList<String>(patternList);
        }

        @Override
        public List<String> getPrefixes() {
            return new ArrayList<String>(prefixList);
        }

        @Override
        public String toString() {
            return "N3Patterns[prefixList=" + prefixList + ", patternList="
                    + patternList + "]";
        }

    }

    private static class DynamicN3Patterns extends N3Patterns
            implements ConfigFile.ConfigFileDynamicN3Pattern {
        private final List<String> variables = new ArrayList<>();

        @Override
        public void addComponent(Map<String, Object> component)
                throws InvalidConfigFileException {
            super.addComponent(component);
            if (this.variables.isEmpty()) {
                this.variables.addAll(toListOfStrings(
                        component.get(PROPERTY_DYNAMIC_VARIABLES)));
            } else {
                throw new InvalidConfigFileException(
                        "Config file contains more than one '"
                                + TYPE_DYNAMIC_N3_PATTERN + "' component.");
            }
        }

        @Override
        public List<String> getVariables() {
            return new ArrayList<>(variables);
        }

        @Override
        public String toString() {
            return "DynamicN3Patterns[variables=" + variables
                    + ", getPrefixes()=" + getPrefixes() + ", getPattern()="
                    + getPattern() + "]";
        }

    }

    private static class Dependencies {
        private Map<String, Set<String>> map = new HashMap<>();

        public void addComponent(Map<String, Object> component) {
            List<String> groups = toListOfStrings(
                    component.get(PROPERTY_DEPENDENCIES));
            for (String group : groups) {
                Set<String> varNames = parseCommaSeparatedSet(group);
                for (String varName : varNames) {
                    getNonNull(varName).addAll(varNames);
                }
            }
        }

        private Set<String> getNonNull(String varName) {
            if (!map.containsKey(varName)) {
                map.put(varName, new HashSet<>());
            }
            return map.get(varName);
        }

        public Set<String> getFor(String varName) {
            return new HashSet<>(getNonNull(varName));
        }

    }

    private static class Unknown extends Component {
        public Unknown(Map<String, Object> component)
                throws InvalidConfigFileException {
            super(component);
        }

    }

    @Override
    public String toString() {
        return "JacksonConfigFile[uris=" + uris + ", literals=" + literals
                + ", required=" + required + ", optional=" + optional
                + ", dynamic=" + dynamic + ", dependencies=" + dependencies
                + ", unknowns=" + unknowns + "]";
    }

}
