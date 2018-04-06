# Configurable Custom Forms

# Overview
Custom entry forms are a very powerful mechanism for customizing Vitro. Instead of entering data one triple at a time, the user may enter a page filled with related data, and Vitro will organize it into the appropriate triples. The forms can be used to create new data, or to edit existing data.

Designing custom forms can be complex. Form designers must answer questions like these:

* What type of data goes into each field?
* Which fields are required and which are optional?
* What is the relationship among the fields?
* Does the user enter new data, or choose from existing data, or both?

Also, a layout must be created to display the form on the screen.

As originally implemented, each custom form was built from a Java class (the form "generator") and a Freemarker template. Configurable Custom Forms provides another way to create custom forms, using 

* a JSON-LD file to describe the fields and their relationships,
* an optional JSON file to describe the layout of the form on the screen.

These tools do not replace the original implementation of custom entry forms. Rather, the intent is to make the development of **most** custom forms easier.


# The form definition file

The file describes the fields on the form, and the RDF triples that the form will create. The file uses JSON-LD syntax.

The description of each field may include its data type, validation methods, and a recipe for finding existing data that will pre-populate the field.

The RDF triples may be designated as required or optional, depending on whether they are derived from required fields or optional fields on the form.

## How the definition works

The basic purpose of the form definition is to provide a recipe for translating values from the HTML form into RDF. 

The RDF must be defined, in terms of patterns. N3 syntax is used. When an HTML form is submitted, variables in the RDF are bound to values in the form. Each triple in the RDF pattern may be treated as required or optional. 

* Required N3 - If any variables in a required triple are not bound by the form, the submission fails. 
* Optional N3 - If any variables in an optional triple are not bound by the form, then that triple is ignored, but the submission does not fail.

Each field in the HTML form must be defined in the file. We need to state whether the value in the field will be used as a URI or a Literal in the RDF triples that are created. Other information may be provided also, as needed.

Sometimes the choice of _required N3_ versus _optional N3_ is not sufficient to express the relationship among fields on the form. For more expressive power, you can specify groups of fields that _depend_ on each other. If any field in the group has no value, then the other fields in the group are treated as if they have no values also. This dependency can cause optional N3 triples to be omitted, or can cause the entire submission to fail if the variables appear in the required N3.


## The structure of the file

The JSON-LD format is just another notation for specifying an RDF graph. In this case, the graph describes the custom form.

Any file in JSON-LD format has the same overall structure. The file contains a single JSON object, with:

* a `@context` element which specifies prefixes on URLs in the file.
* a `@graph` element, which specifies the actual contents.

Here is the overall "boiler plate" used in the form definition file:

```
{
  "@context": {
    "foaf": "http://xmlns.com/foaf/0.1/",
    "forms": "java:edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.forms#",
    "bib": "http://bibliotek-o.org/ontology/",
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    "vivo": "http://vivoweb.org/ontology/core#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "bibframe": "http://id.loc.gov/ontologies/bibframe/",
    "customform": "http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#"
  },
  "@graph": [
     ... THE FORM DESCRIPTION GOES HERE ...
  ]
}
```

#### Restrictions on JSON-LD

The form definition file is interpreted by code that does not realize all of the possibilities of the JSON-LD syntax. For example, JSON-LD will permit the use of any prefixes for namespaces, but the form definition file requires the prefixes as shown in the examples.

## The form components

As shown above, the `@graph` section of the file contains an array of JavaScript objects. Each of these objects describes a component of the form.

### Required N3

Describes the triples that are required for the form submission to be successful. Values from the HTML form are bound to variables in the N3 pattern. If any of the variables are not bound by the form submission, then the submission fails.

If there is more than one Required N3 component, the prefixes of the components will be concatenated, as will the patterns.

#### Properties

| name | description |
|----|----|
| `@id` | Required. Must be unique within the configuration.
| `@type` | Required. Must be `forms:RequiredN3Pattern`|
| `customform:prefixes` | Optional. Specifies namespace prefixes for the triples in the pattern, by one or more N3 `@prefix` statements. May be a single string, or an array of strings to improve readability. |
| `customform:pattern` | Required. Specifies the triples pattern. May be a single string, or an array of strings to improve readability. |

#### Examples

```
{
  "@id": "customform:whcor_requiredN3",
  "@type": [
    "forms:RequiredN3Pattern"
  ],
  "customform:pattern": [
    "?objectVar bib:hasAgent ?agent . ?agent bib:isAgentOf ?objectVar .    ",
    "?objectVar a bib:Activity . ?objectVar a ?activityType . ?objectVar rdfs:label ?activityLabel .     ",
    "?subject bib:hasActivity ?objectVar . ?objectVar bib:isActivityOf ?subject .    "
  ],
  "customform:prefixes": "@prefix bib: <http://bibliotek-o.org/ontology/> . @prefix bibframe: <http://id.loc.gov/ontologies/bibframe/> .   @prefix  rdfs: <http://www.w3.org/2000/01/rdf-schema#>  .     "
},
```

```
{
  "@id": "customform:ap2p_requiredN3",
  "@type": "forms:RequiredN3Pattern",
  "customform:prefixes": "PREFIX core: <http://vivoweb.org/ontology/core#>\n",
  "customform:pattern": "?objectVar \n   a core:Authorship ;\n.  core:relates ?subject .\n"
},
```

### Optional N3

Describes the triples that will be included if (and only if) their variables are bound to values on the HTML form. Any triples with unbound variables will not be included, but will not cause an error.

If there is more than one Optional N3 component, the prefixes of the components will be concatenated, as will the patterns.

#### Properties

| name | description |
|----|----|
| `@id` | Required. Must be unique within the configuration.
| `@type` | Required. Must be `forms:OptionalN3Pattern`|
| `customform:prefixes` | Optional. Specifies namespace prefixes for the triples in the pattern, by one or more N3 `@prefix` statements. May be a single string, or an array of strings to improve readability. |
| `customform:pattern` | Required. Specifies the triples pattern. May be a single string, or an array of strings to improve readability. |

#### Examples

```
{
  "@id": "customform:whcor_optionalN3",
  "@type": [
    "forms:OptionalN3Pattern"
  ],
  "customform:prefixes": [
    "@prefix  bib: <http://bibliotek-o.org/ontology/> . ", 
    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>  . ", 
    "@prefix  foaf: <http://xmlns.com/foaf/0.1/> .  "
  ],
  "customform:pattern": ["?agent a ?agentType.  ?agent rdfs:label ?agentName.  ?agent foaf:name ?agentName .  "]
},
```

### URI fields

Describes a field on the HTML form whose value will be bound as a URI in the N3 patterns. The `varName` is both the name of the field on the form, and also the name of the N3 variable to which the field value will be bound.

Field descriptions may include a `queryForExistingValue` property. This is a SPARQL SELECT query which is used to populate the field when the form is presented. 

URI field descriptions may include a `mayUseNewResource` flag. If a field has this flag, and the submitted form does not include a value for the field, then a new URI will be minted and used as the value of the field.

#### Properties

| name | description |
|----|----|
| `@id` | Required. Must be unique within the configuration.
| `@type` | Required. Must be `forms:UriField` |
| `customform:varName` | Required. Must be a string or an array of strings. A description with more than one `varName` is equivalent to multiple descriptions with the same optional properties. |
| `customform:queryForExistingValue` | Optional. If present, must be a single string. this query will be executed, and the returned value will become the initial value of the field. If the SPARQL query contains the variable `?objectVar`, it will be bound to the URI of the individual being edited. |
| `customform:mayUseNewResource` | Optional. If `true`, and the form submission does not include a value for this field, then a new URI will be minted and used as the value. |

#### Examples

```
{
  "@id": "customform:whcor_fieldAgent",
  "@type": [
    "forms:UriField"
  ],
  "customform:mayUseNewResource": true,
  "customform:queryForExistingValue": "PREFIX bib: <http://bibliotek-o.org/ontology/>   SELECT ?existingAgent WHERE {  ?objectVar bib:hasAgent ?existingAgent . }   ",
  "customform:varName": "agent"
},
```
```
{
  "@id": "customform:noteValue",
  "@type": "forms:LiteralField",
  "customform:varName": "objectValue",
  "customform:queryForExistingValue": "PREFIX bib: <http://bibliotek-o.org/ontology/>  PREFIX vitro: <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#>   PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  SELECT ?existingObjectValue   WHERE {?objectVar rdf:value ?existingObjectValue .} "
}
```

### Literal fields

Describes a field on the HTML form whose value will be bound as a Literal in the N3 patterns. The `varName` is both the name of the field on the form, and also the name of the N3 variable to which the field value will be bound.

Field descriptions may include a `queryForExistingValue` property. This is a SPARQL SELECT query which is used to populate the field when the form is presented. 

#### Properties

| name | description |
|----|----|
| `@id` | Required. Must be unique within the configuration.
| `@type` | Required. Must be `forms:LiteralField` |
| `customform:varName` | _Same as for URI field._ |
| `customform:queryForExistingValue` | _Same as for URI field._ |

#### Examples

```
{
  "@id": "customform:noteValue",
  "@type": [
    "forms:LiteralField"
  ],
  "customform:varName": "objectValue",
  "customform:queryForExistingValue": "PREFIX bib: <http://bibliotek-o.org/ontology/>  PREFIX vitro: <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#>   PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  SELECT ?existingObjectValue   WHERE {?objectVar rdf:value ?existingObjectValue .} "
}
```

### Inter-field dependencies

Describes one or more co-dependent groups of fields on the HTML form. If any field in a group has no value, then all fields in that group are considered to have no values.

This may mean that variables in the required N3 pattern go unbound, causing the submission to fail. Or it may mean that variables in the optional N3 pattern go unbound, causing triples that are otherwise valid to be ignored.

#### Properties

| name | description |
|----|----|
| `@id` | Required. Must be unique within the configuration.
| `@type` | Required. Must be `forms: FieldDependencies ` |
| `customform: dependencies ` | Required. Must be a string or an array of strings. Each string contains a comma-separated list of field names which make up a co-dependent group. |

#### Examples

```
{
  "@id": "customform:field_dependencies",
  "@type": "forms:FieldDependencies",
  "customform:dependencies": [
    "newEditor,editor,vcardEditor,vcardName",
    "newBook,book",
    "newEvent,event",
    "dateTimeNode,dateTime-value"
  ]
}

```

# The display configuration file

#### TBD Purpose and description of the JSON 

# Apply the configuration files

#### TBD How to make the connection. Can we do it through the GUI? Here is an example of a connection in N3.

```
@prefix : <http://vitro.mannlib.cornell.edu/ns/vitro/ApplicationConfiguration#> .
@prefix vitro: <http://vitro.mannlib.cornell.edu/ns/vitro/0.7#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix local: <http://vitro.mannlib.cornell.edu/ns/vitro/siteConfig/> .

local:fpgenconfig289 
    :listViewConfigFile 
        "listViewConfig-workHasActivity.xml"^^xsd:string ;  
    vitro:customEntryFormAnnot 
        "edu.cornell.mannlib.vitro.webapp.edit.n3editing.configuration.generators.MinimalEditConfigurationGenerator"^^xsd:string ;
    vitro:customConfigFileAnnot "audioInstanceHasActivity.jsonld" . 
```

# Developer notes

## Source code

* Freemarker templates:
	* `[vitro]/webapp/src/main/webapp/templates/freemarker/edit/forms/minimalconfigtemplate.ftl`
* JavaScript:
	* `[vitro]/webapp/src/main/webapp/templates/freemarker/edit/forms/js/minimalconfigtemplate.js`
* Java:
	* `[vitro]/api/src/main/java/edu/cornell/mannlib/vitro/webapp/edit/n3editing/configuration/preprocessors/MinimalConfigurationPreprocessor.java`
	* `[vitro]/api/src/main/java/edu/cornell/mannlib/vitro/webapp/controller/ajax/CustomFormAJAXController.java`

## Configuration

* Assign the custom form to a faux property:
	* [vitrolib]/home/src/main/resources/rdf/display/everytime/infoForFauxProperty.n3
