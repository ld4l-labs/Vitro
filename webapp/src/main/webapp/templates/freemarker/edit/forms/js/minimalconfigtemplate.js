/* $This file is distributed under the terms of the license in /doc/license.txt$ */

function MinimalConfigTemplate() {
    var minimalconfigtemplate = {

        /* *** Initial page setup *** */
        fieldNameProperty : "customform:varName",
        configJSON:null,
        onLoad: function() {
            minimalconfigtemplate.mixIn();
            //Do ajax request to get config and only then trigger the rest
            $.ajax({
                method: "GET",
                url: minimalconfigtemplate.configFileURL
            })
            .done(function( content ) {
                minimalconfigtemplate.configJSON = JSON.parse(content);
                minimalconfigtemplate.initPage();
                //Bind event listeners only when everything on the page has been populated
                //Putting in bind event listeners here - any autocomplete fields should already be setup
                //As far as fields generated using AJAX requests - the event listeners should be attached
                //in the done/success methods of the ajax requests
                minimalconfigtemplate.bindEventListeners();
            });

        },

        mixIn: function() {

            // Get the custom form data from the page
            $.extend(minimalconfigtemplate, customFormData);
            $.extend(minimalconfigtemplate, i18nStrings);
        },

        // Initial page setup. Called only at page load.
        initPage: function() {
            //hash to store form fields
            minimalconfigtemplate.formFields = {};
            minimalconfigtemplate.formFieldsToOptions = {};//Key is field name, but options saved as component
            minimalconfigtemplate.allConfigComponents = {}; //Hash where key is @id

            minimalconfigtemplate.processConfigJSON();
            minimalconfigtemplate.generateFields();
            //If this is an EDITING operation, need to get existing values
            if(minimalconfigtemplate.editMode == "edit") {
                //Retrieve existing values if edit operation
                minimalconfigtemplate.retrieveExistingValueRequests();
            }

        },
        //get existing values
        retrieveExistingValueRequests:function() {
            var fieldName;
            var existingValueRequests = [];
            for(fieldName in minimalconfigtemplate.formFields) {
                var configComponent = minimalconfigtemplate.formFields[fieldName];
                //See if existing URI
                if("customform:queryForExistingValue" in configComponent) {
                    existingValueRequests.push(configComponent);
                }
            }

            //Do an ajax request to get existing values
            $.ajax(
                {
                    method: "GET",
                    url: minimalconfigtemplate.customFormAJAXUrl,
                    data: { "configComponentsExistingValues": JSON.stringify(existingValueRequests),
                    "urisInScope":JSON.stringify(urisInScope),
                    "literalsInScope":JSON.stringify(literalsInScope),
                    "action":"existingValues"
                }
            })
            .done(function( content ) {
                //templateclone is from URI Field, so make this work better
                //Should retrieve a hash with var name to existing value
                //and those can be used to populate the form

            });

        },
        bindEventListeners:function() {
            //This relies on the custom form with autocomplete file
            //TODO: Find a better way to do this
            if(customForm) {
                customForm.onLoad();
            }
        },

        //process the json and create a hash by varname/fieldname
        processConfigJSON: function() {
            //Process the entire JSON to save all components by @id in hash
            minimalconfigtemplate.generateConfigHash();
            //Get fields from the displayconfig
            minimalconfigtemplate.fieldDisplayProperties = displayConfig.fieldDisplayProperties;
            //Are these ALL fields?
            minimalconfigtemplate.fieldOrder = displayConfig.fieldOrder;
            var len = minimalconfigtemplate.fieldOrder.length;
            for(f = 0; f < len; f++) {
                var fieldName = minimalconfigtemplate.fieldOrder[f];
                var configComponent = minimalconfigtemplate.getConfigurationComponent(fieldName);
                if(configComponent != null) {
                    minimalconfigtemplate.formFields[fieldName] = configComponent;
                    //if form field has associated field options, store within formFieldsToFieldOptions hash
                    var fieldOptions = minimalconfigtemplate.getFieldOptions(configComponent);
                    if(fieldOptions != null) {
                        minimalconfigtemplate.formFieldsToOptions[fieldName] = fieldOptions;
                    }
                }
            }
        },

        generateConfigHash : function() {
            //Load configjson
            var graph = minimalconfigtemplate.configJSON["@graph"];
            var numberComponents = graph.length;
            var n;
            var fieldNameProperty = "customform:varName";
            for(n = 0; n < numberComponents; n++) {
                var component = graph[n];
                var id = component["@id"];
                minimalconfigtemplate.allConfigComponents[id] = component;
            }
        },

        getFieldOptions: function(configComponent) {
            var fieldOptionsFieldName = "customform:fieldOptions";
            if(fieldOptionsFieldName in configComponent) {
                var configId = configComponent[fieldOptionsFieldName]["@id"];
                var configComponent = minimalconfigtemplate.allConfigComponents[configId];
                //TODO: Include check - whether this id even exists, etc.
                return configComponent;
            }
            return null;

        },
        generateFields: function() {
            //date time has specific handling, do separately
            //fields in order
            //TODO: Have fields come in from ?json? file specifying display configuration
            //Example: workHasActivityDisplayConfig
            //activityType, agentType

            var f;
            var len = minimalconfigtemplate.fieldOrder.length;
            //TODO: Generic form id/name required
            var form = $("#addpublicationToPerson");
            for(f = 0; f < len; f++) {
                var fieldName = minimalconfigtemplate.fieldOrder[f];
                if(fieldName in minimalconfigtemplate.formFields) {
                    var configComponent = minimalconfigtemplate.formFields[fieldName];
                    minimalconfigtemplate.displayConfigComponent(configComponent);
                }
            }



        } ,

        displayConfigComponent: function(configComponent) {
            //Get fieldName
            var fieldName = configComponent[minimalconfigtemplate.fieldNameProperty];
            //TODO: Check if this key exists
            var displayInfo = minimalconfigtemplate.fieldDisplayProperties[fieldName];
            var templateClone = "";
            if(minimalconfigtemplate.componentHasType(configComponent, "forms:LiteralField")) {
                //Either autocomplete or regular field
                templateClone = minimalconfigtemplate.createLiteralField(configComponent, displayInfo);



            } else if(minimalconfigtemplate.componentHasType(configComponent, "forms:UriField")) {
                //UriField may have different field types for drop downs
                //Generated drop-down options are signified by field options
                //} else if(minimalconfigtemplate.componentHasType(configComponent, "forms:FieldOptions")) {

                //dropdown needed - since field options are always drop-downs of some sort
                //if(fieldName in minimalconfigtemplate.formFieldsToOptions) {
                //  templateClone = minimalconfigtemplate.createURIField(configComponent);
                if(fieldName in minimalconfigtemplate.formFieldsToOptions) {
                    templateClone = minimalconfigtemplate.createDropdownFieldContainer(configComponent, displayInfo);
                }
            }

            if(templateClone != "" && templateClone != null) {
                templateClone.appendTo("#formcontent");
                templateClone.show();
            }
            //URI Field
            //Constant options field
            //How do you treate "generated" fields?

            //If options associated, then use an AJAX request to populate the drop-down
            //How to do this then? Create drop-down and THEN display?
            //First, just see if this even works
            if(fieldName in minimalconfigtemplate.formFieldsToOptions) {
                var fieldOptionComponent = minimalconfigtemplate.formFieldsToOptions[fieldName];
                //Just pass the entire JSON object to the servlet and let the servlet parse it
                $.ajax(
                    {
                        method: "GET",
                        url: minimalconfigtemplate.customFormAJAXUrl,
                        data: { "configComponent": JSON.stringify(fieldOptionComponent),
                        "fieldName": fieldName,
                        "action": "dropdown"
                    }
                })
                .done(function( content ) {
                    //templateclone is from URI Field, so make this work better
                    //templateClone = minimalconfigtemplate.createURIField(configComponent);
                    minimalconfigtemplate.createDropdownField(fieldName, content);

                });
            }



        },
        createLiteralField:function(configComponent, displayInfo) {
            var templateClone = "";
            var varName = minimalconfigtemplate.getVarName(configComponent);
            var label = varName;
            if("label" in displayInfo)
            label = displayInfo["label"];
            //Coding in a shortcut but autocomplete requires id and LABEL
            //how to encode that?
            if("autocomplete" in displayInfo) {
                var labelFieldFor = displayInfo["labelFieldFor"];
                //Copy autocomplete portion over
                //Just add label
                templateClone = $("[templateId='autocompleteLiteralTemplate']").clone();
                var selectorComponent = templateClone.find("[templateId='inputAcSelector']");
                var inputField = selectorComponent.find("input.acSelector");
                //TODO: Should id be the varname or something else?
                inputField.attr("id", varName);
                inputField.attr("name", varName);
                //acGroupName attribute
                var selectedComponent = templateClone.find("[templateId='literalSelection']");
                //e.g. agentName corresponds to agent
                selectedComponent.find("input.acUriReceiver").attr("id", labelFieldFor);
                selectedComponent.find("input.acUriReceiver").attr("name", labelFieldFor);
                //Label field
                var labelField = templateClone.find("p[templateId='inputAcSelector'] label");
                labelField.html(label);
                labelField.attr("for", varName);
                //remove the templateId attribute

            } else {
                //Otherwise copy regular literal over
                templateClone = $("[templateId='literalTemplate']").clone();
                //Set the label and id/field name of the template clone
                var textInput= templateClone.find("input[type='text']");
                var labelField = templateClone.find("label");
                textInput.attr("id", varName);
                textInput.attr("name", varName);
                labelField.html(label);
                labelField.attr("for", varName);

            }
            templateClone.removeAttr("templateId");

            return templateClone;


        },
        //this needs to be changed
        createURIField:function(configComponent) {
            var varName = minimalconfigtemplate.getVarName(configComponent);
            var templateClone = $("[templateId='selectDropdownTemplate']").clone();
            var selectInput = templateClone.find("select");
            selectInput.attr("id", varName);
            selectInput.attr("name", varName);
            templateClone.removeAttr("templateId");

            return templateClone;
        },
        createDropdownFieldContainer:function(configComponent, displayInfo) {
            var varName = minimalconfigtemplate.getVarName(configComponent);
            var templateClone = $("[templateId='selectDropdownTemplate']").clone();
            var selectInput = templateClone.find("select");
            selectInput.attr("id", varName);
            selectInput.attr("name", varName);
            var dropdownLabelElement = templateClone.find("label");
            var dropdownLabelValue = varName;
            if("label" in displayInfo) {
                dropdownLabelValue = displayInfo["label"];
            }
            dropdownLabelElement.attr("for", varName);
            dropdownLabelElement.html(dropdownLabelValue);
            templateClone.removeAttr("templateId");

            return templateClone;
        },
        createDropdownField:function(varName, content) {
            //populate drop downs
            var dropdownElement = $("select#" + varName);
            var htmlList = "";
            $.each(content, function(key, value) {
                htmlList += "<option value='" + key + "'>" + value + "</option>";

            });
            dropdownElement.empty().append(htmlList);
        },
        getVarName:function(configComponent) {
            var varNameFieldName = "customform:varName";
            if(varNameFieldName in configComponent)
            return configComponent[varNameFieldName];
            return null;
        },
        //field options need to be generated
        createFieldOptions:function(configComponent) {
            return null;
        },
        getConfigurationComponent:function(componentName) {
            var graph = minimalconfigtemplate.configJSON["@graph"];
            var numberComponents = graph.length;
            var n;
            var fieldNameProperty = "customform:varName";
            for(n = 0; n < numberComponents; n++) {
                var component = graph[n];
                //if field name property is contained within component
                //Does this fall under massaging everything into a common format instead?
                if(fieldNameProperty in component) {
                    var fieldNamesArray = new Array().concat(component[fieldNameProperty]);

                    var includedInArray = $.inArray(componentName, fieldNamesArray);
                    if(includedInArray > -1) {
                        return component;
                    }
                }

            }
            return null;

        },

        componentHasType: function(component, componentType) {
            //returns array of types
            var types = component["@type"];
            var len = types.length;
            var l;
            for(l = 0; l < len; l++) {
                var type = types[l];
                if(type == componentType)
                return true;
            }
            return false;
        }
    }

    return {
        onLoad: minimalconfigtemplate.onLoad
    }
};

$(document).ready(function() {
    new MinimalConfigTemplate().onLoad();
});
