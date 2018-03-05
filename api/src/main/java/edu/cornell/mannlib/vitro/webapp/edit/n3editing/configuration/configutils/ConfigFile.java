/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.configutils;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * When processing a configurable custom form, this describes the information
 * that is available from the parsed JSON-LD.
 */
public interface ConfigFile {
    String TYPE_DEPENDENCIES = "forms:FieldDependencies";
    String TYPE_OPTIONAL_N3_PATTERN = "forms:OptionalN3Pattern";
    String TYPE_REQUIRED_N3_PATTERN = "forms:RequiredN3Pattern";
    String TYPE_DYNAMIC_N3_PATTERN = "forms:DynamicN3Pattern";
    String TYPE_LITERAL_FIELD = "forms:LiteralField";
    String TYPE_URI_FIELD = "forms:UriField";
    String PROPERTY_ID = "@id";
    String PROPERTY_TYPE = "@type";
    String PROPERTY_DEPENDENCIES = "customform:dependencies";
    String PROPERTY_PATTERN = "customform:pattern";
    String PROPERTY_PREFIXES = "customform:prefixes";
    String PROPERTY_VAR_NAME = "customform:varName";
    String PROPERTY_DYNAMIC_VARIABLES = "customform:dynamic_variables";
    String PROPERTY_MAY_USE_NEW_RESOURCE = "customform:mayUseNewResource";

    boolean hasFieldComponent(String varName);

    ConfigFileField getFieldComponent(String varName);

    boolean hasDependenciesFor(String varName);

    Set<String> getDependenciesFor(String varName);

    ConfigFileN3Pattern getRequiredN3();

    boolean hasOptionalN3();

    ConfigFileN3Pattern getOptionalN3();

    boolean hasDynamicN3();

    ConfigFileDynamicN3Pattern getDynamicN3();

    Set<String> getNewResourceVarNames();

    Set<String> getAllowedVarNames();

    /**
     * Describes a form field in the configuration.
     */
    interface ConfigFileField {

        boolean hasType(String string);

    }

    /**
     * Describes the Required N3 and Optional N3 components of the config file.
     */
    interface ConfigFileN3Pattern {

        /** The pattern may be written as an array of Strings. */
        List<String> getPattern();

        /** Join the pattern array into a single String. */
        default String getJoinedPattern() {
            return StringUtils.join(getPattern(), " ");
        }

        /** The prefixes may be written as an array of Strings. */
        List<String> getPrefixes();

        /** Join the prefixes array into a single String. */
        default String getJoinedPrefixes() {
            return StringUtils.join(getPrefixes(), " ");
        }

        /** Combine the prefixes and the patterns into a single String. */
        default String getJoined() {
            return getJoinedPrefixes() + " " + getJoinedPattern();
        }

    }

    /**
     * Describes the Dynamic N3 component of the config file.
     */
    interface ConfigFileDynamicN3Pattern extends ConfigFileN3Pattern {

        List<String> getVariables();

    }

    /**
     * Problem with the config file!
     */
    public static class InvalidConfigFileException extends Exception {

        public InvalidConfigFileException(String message) {
            super(message);
        }

        public InvalidConfigFileException(Throwable cause) {
            super(cause);
        }

        public InvalidConfigFileException(String message, Throwable cause) {
            super(message, cause);
        }

    }
}
