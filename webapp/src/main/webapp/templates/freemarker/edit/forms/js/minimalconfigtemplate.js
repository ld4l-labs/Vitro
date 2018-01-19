/* $This file is distributed under the terms of the license in /doc/license.txt$ */

function MinimalConfigTemplate(formData, displayData) {
    var fieldNameProperty = "customform:varName";

    var configJSON;
    var formFields;
    var formFieldsToOptions;
    var allConfigComponents;
    
    return {
        onLoad: onLoad
    }

    function onLoad() {
        //Do ajax request to get config and only then trigger the rest
        $.ajax({
            method: "GET",
            url: formData.configFileURL
        })
        .done(function( content ) {
            configJSON = JSON.parse(content);
            initPage();
        });

    }

    // Initial page setup. Called only at page load.
    function initPage() {
        //hash to store form fields
        formFields = {};
        formFieldsToOptions = {};//Key is field name, but options saved as component
        allConfigComponents = {}; //Hash where key is @id

        processConfigJSON();
        generateFields();
        //If this is an EDITING operation, need to get existing values
        if(formData.editMode == "edit") {
            //Retrieve existing values if edit operation
            retrieveExistingValueRequests();
        } else {
            //Bind event listeners only when everything on the page has been populated
            //Putting in bind event listeners here - any autocomplete fields should already be setup
            //As far as fields generated using AJAX requests - the event listeners should be attached
            //in the done/success methods of the ajax requests
            bindEventListeners();
        }
    }
    
    // get existing values
    function retrieveExistingValueRequests() {
        var id;
        var existingValueRequests = [];
        //Form fields populated with the fields in display config, may not include
        //for instance, receiver URI for autocomplete - and perhaps other fields that are not
        //displayed directly
        //so checking all config components instead
        for(id in allConfigComponents) {
            var configComponent = allConfigComponents[id];
            //See if existing URI
            if("customform:queryForExistingValue" in configComponent) {
                existingValueRequests.push(configComponent);
            }
        }

        //Do an ajax request to get existing values
        $.ajax(
            {
                method: "GET",
                url: formData.customFormAJAXUrl,
                data: { "configComponentsExistingValues": JSON.stringify(existingValueRequests),
                "urisInScope":JSON.stringify(formData.urisInScope),
                "literalsInScope":JSON.stringify(formData.literalsInScope),
                "action":"existingValues"
            }
        })
        .done(function( content ) {
            //templateclone is from URI Field, so make this work better
            //Should retrieve a hash with var name to existing value
            //and those can be used to populate the form
            populateExistingContent(content);
            //Existing value requests comprise another ajax request that will affect autocomplete selection
            bindEventListeners();
        });
    }

    //Hash with variable name to array of existing content values
    function populateExistingContent(content) {
        //Does this have to parsed into JSON?
        
        var fieldName;
        for(fieldName in content) {
            updateFieldWithExistingContent(fieldName, content[fieldName]);
        }
        //Create a hidden input with Stringified version 
        $("form").append("<input type='hidden' name='existingValuesRetrieved' id='existingValuesRetrieved' value='" + JSON.stringify(content)  + "'>");
    }
    
    function updateFieldWithExistingContent(fieldName, existingValue) {
        //Get form field
        //TODO: review how info is hashed and how we need to retrieve it
        var configComponent = getConfigurationComponent(fieldName);
        if(configComponent != null && configComponent != undefined) {
            //Doesn't appear to matter whether URI or literal field, select or input, as val() will set all of them
            var fieldName = configComponent["customform:varName"];
            var fieldElement = $("[name='" + fieldName + "']");
            //This really should be just ONE value
            //TODO: Check if we would ever want this to be more than one value
            if(fieldElement != null & fieldElement != undefined && existingValue.length > 0) {
                fieldElement.val(existingValue[0]);
            }
        }
    }

    //process the json and create a hash by varname/fieldname
    function processConfigJSON() {
        //Process the entire JSON to save all components by @id in hash
        generateConfigHash();
        //Get fields from the displayData
        //Are these ALL fields?
        var len = displayData.fieldOrder.length;
        for(f = 0; f < len; f++) {
            var fieldName = displayData.fieldOrder[f];
            var configComponent = getConfigurationComponent(fieldName);
            if(configComponent != null) {
                formFields[fieldName] = configComponent;
                //if form field has associated field options, store within formFieldsToFieldOptions hash
                var fieldOptions = getFieldOptions(configComponent);
                if(fieldOptions != null) {
                    formFieldsToOptions[fieldName] = fieldOptions;
                }
            }
        }
    }

    function bindEventListeners() {
        //This relies on the custom form with autocomplete file
        //TODO: Find a better way to do this
        if(customForm) {
            customForm.onLoad();
        }
    }

    function generateConfigHash() {
        //Load configjson
        var graph = configJSON["@graph"];
        var numberComponents = graph.length;
        var n;
        for(n = 0; n < numberComponents; n++) {
            var component = graph[n];
            var id = component["@id"];
            allConfigComponents[id] = component;
        }
    }

    function getFieldOptions(configComponent) {
        var fieldOptionsFieldName = "customform:fieldOptions";
        if(fieldOptionsFieldName in configComponent) {
            var configId = configComponent[fieldOptionsFieldName]["@id"];
            var configComponent = allConfigComponents[configId];
            //TODO: Include check - whether this id even exists, etc.
            return configComponent;
        }
        return null;

    }

    function generateFields() {
        //date time has specific handling, do separately
        //fields in order
        //TODO: Have fields come in from ?json? file specifying display configuration
        //Example: workHasActivityDisplayConfig
        //activityType, agentType

        var f;
        var len = displayData.fieldOrder.length;
        var form = $("#minimalForm");
        for(f = 0; f < len; f++) {
            var fieldName = displayData.fieldOrder[f];
            if(fieldName in formFields) {
                var configComponent = formFields[fieldName];
                displayConfigComponent(configComponent);
            }
        }
    } 

    function displayConfigComponent(configComponent) {
        //Get fieldName
        var fieldName = configComponent[fieldNameProperty];
        //TODO: Check if this key exists
        var displayInfo = displayData.fieldDisplayProperties[fieldName];
        var templateClone = "";
        if(componentHasType(configComponent, "forms:LiteralField")) {
            //Either autocomplete or regular field
            templateClone = createLiteralField(configComponent, displayInfo);

        } else if(componentHasType(configComponent, "forms:UriField")) {
            //UriField may have different field types for drop downs
            //Generated drop-down options are signified by field options
            //} else if(componentHasType(configComponent, "forms:FieldOptions")) {

            //dropdown needed - since field options are always drop-downs of some sort
            //if(fieldName in formFieldsToOptions) {
            //  templateClone = createURIField(configComponent);
            if(fieldName in formFieldsToOptions) {
                templateClone = createDropdownFieldContainer(configComponent, displayInfo);
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
        if(fieldName in formFieldsToOptions) {
            var fieldOptionComponent = formFieldsToOptions[fieldName];
            // "@type": [ "forms:ConstantList",
           
            if(componentHasType(fieldOptionComponent, "forms:ConstantList")) {
            	if("constantList" in displayInfo) {
            		var staticContent = displayInfo["constantList"];
            		createStaticDropdownField(fieldName, staticContent);
            	}
            } else { 
	            //If a constant field option, use the display data
	            //Otherwise do an ajax request
	            //Just pass the entire JSON object to the servlet and let the servlet parse it
	            $.ajax(
	                {
	                    method: "GET",
	                    url: formData.customFormAJAXUrl,
	                    data: { "configComponent": JSON.stringify(fieldOptionComponent),
	                    "fieldName": fieldName,
	                    "action": "dropdown"
	                }
	            })
	            .done(function( content ) {
	                //templateclone is from URI Field, so make this work better
	                //templateClone = createURIField(configComponent);
	                createDropdownField(fieldName, content);
	
	            });
            }
        }
    }

    function createLiteralField(configComponent, displayInfo) {
        var templateClone = "";
        var varName = getVarName(configComponent);
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
    }
    
    //this needs to be changed
    function createURIField(configComponent) {
        var varName = getVarName(configComponent);
        var templateClone = $("[templateId='selectDropdownTemplate']").clone();
        var selectInput = templateClone.find("select");
        selectInput.attr("id", varName);
        selectInput.attr("name", varName);
        templateClone.removeAttr("templateId");

        return templateClone;
    }
    
    function createDropdownFieldContainer(configComponent, displayInfo) {
        var varName = getVarName(configComponent);
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
    }

    function createDropdownField(varName, content) {
        //populate drop downs
        var dropdownElement = $("select#" + varName);
        var htmlList = "";
        //TODO: Make this sort occur at the data level, not here
        var keysSortedByValue = Object.keys(content).sort(function(a, b) {
        	
        	return content[a].localeCompare(content[b]);
        });
        $.each(keysSortedByValue, function(index, keyvalue) {
            htmlList += "<option value='" + keyvalue + "'>" + content[keyvalue] + "</option>";

        });
        /*
        $.each(content, function(key, value) {
            htmlList += "<option value='" + key + "'>" + value + "</option>";

        });*/
        dropdownElement.empty().append(htmlList);
    }
    
    //TODO: refactor above to use the same basis, one with sorting and one without
    //And or have sort at data level
    //Also this uses label and value - right now label and uri
    function createStaticDropdownField(varName, content) {
        //populate drop downs
        var dropdownElement = $("select#" + varName);
        var htmlList = "";
       
        $.each(content, function(index, contentObj) {
        	var uri = contentObj["uri"];
        	var label = contentObj["label"];
            htmlList += "<option value='" + uri + "'>" + label + "</option>";

        });
      
        dropdownElement.empty().append(htmlList);
    }
    
    function getVarName(configComponent) {
        var varNameFieldName = "customform:varName";
        if(varNameFieldName in configComponent) {
            return configComponent[varNameFieldName];
        }
        return null;
    }
    
    //field options need to be generated
    function createFieldOptions(configComponent) {
        return null;
    }
    
    function getConfigurationComponent(componentName) {
        var graph = configJSON["@graph"];
        var numberComponents = graph.length;
        var n;
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
    }

    function componentHasType(component, componentType) {
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
};
