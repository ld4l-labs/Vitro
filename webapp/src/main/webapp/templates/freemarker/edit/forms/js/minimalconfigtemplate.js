/* $This file is distributed under the terms of the license in /doc/license.txt$ */

var minimalconfigtemplate = {

    /* *** Initial page setup *** */
   fieldNameProperty : "http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#varName",
    onLoad: function() {
    		
            this.mixIn();               
            this.initPage();       
        },

    mixIn: function() {

        // Get the custom form data from the page
        $.extend(this, customFormData);
        $.extend(this, i18nStrings);
    },

    // Initial page setup. Called only at page load.
    initPage: function() {
    	//hash to store form fields
		this.formFields = {};
		
        //this.initItemData();
       
        this.bindEventListeners();
        
        this.processConfigJSON();
        
        this.generateFields();
                       
    },
    
    //process the json and create a hash by varname/fieldname
    processConfigJSON: function() {
    	//Get fields from the displayconfig
    	this.fieldDisplayProperties = displayConfig.fieldDisplayProperties;
    	//Are these ALL fields?
		this.fieldOrder = displayConfig.fieldOrder;
		var len = this.fieldOrder.length;
		for(f = 0; f < len; f++) {
    		var fieldName = this.fieldOrder[f];
    		var configComponent = minimalconfigtemplate.getConfigurationComponent(fieldName);
    		if(configComponent != null) {
    			this.formFields[fieldName] = configComponent;
    		}
		}
    },
   
    generateFields: function() {
    	//date time has specific handling, do separately
    	//fields in order
    	//TODO: Have fields come in from ?json? file specifying display configuration
    	//Example: workHasActivityDisplayConfig
    	//activityType, agentType
    	
    	var f;
    	var len = this.fieldOrder.length;
    	//TODO: Generic form id/name required
		var form = $("#addpublicationToPerson");
    	for(f = 0; f < len; f++) {
    		var fieldName = this.fieldOrder[f];
    		if(fieldName in this.formFields) {
    			var configComponent = this.formFields[fieldName];
    			displayConfigComponent(configComponent);
    		}
    		
    		/*
    		//TODO: Move these portions into HTML instead that will be copied from the template
    		if(minimalconfigtemplate.componentHasType(configComponent, "forms:ConstantOptionsField")) {	
    			var options = configComponent["http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#options"];
    			options = options.replace("\n", "");
    			//array of arrays
    			options = "[" + options + "]";
    			var optionsArray = jQuery.parseJSON(options);
    			var  optionLength = optionsArray.length;
    			var o;
    			//This is a drop down that should be generated with the options available
    			var selectHTML= '<p class="inline"><label for="typeSelector">Publication Type</label><select id="typeSelector" name="' + fieldName + '" acGroupName="publication" ><option value="">Select One</option>';
    			for(o = 0; o < optionLength; o++) {
    				//also an array
    				var option = optionsArray[o];
    				selectHTML += "<option value='" + option[0] + "'>" + option[1] + "</option>";
    			}
    			selectHTML += '</select>';
    			form.append(selectHTML);
             
    		}     		//Input field
    		else if(minimalconfigtemplate.componentHasType(configComponent, "forms:LiteralField")) {
    			var HTML =  '<p><label for="title">' + fieldName + '</label><input class="acSelector" size="60"  type="text" id="' + fieldName + '" name="' + fieldName + '" acGroupName="publication"  value="" /></p>';
    			form.append(HTML);
    		}*/
    		
    	}
    	
    	
    	
    } ,
    
    displayConfigComponent: function(configComponent) {
    	//Get fieldName
    	var fieldName = configComponent[minimalconfigtemplate.fieldNameProperty];
    	//TODO: Check if this key exists
    	var displayInfo = minimalconfigtemplate.fieldDisplayProperties[fieldName];
    	if(minimalconfigtemplate.componentHasType(configComponent, "forms:LiteralField")) {
    		//Either autocomplete or regular field
    		var templateClone = "";
    		if("autocomplete" in displayInfo) {
    			//Copy autocomplete portion over
    			templateClone = $("#autocompleteLiteralTemplate").clone();
    			
    		} else {
    			//Otherwise copy regular literal over
    			templateClone = $("#literalTemplate").clone();
    		}
    		templateClone.appendTo("#formcontent");
    	}
    	//URI Field
    	//Constant options field
    	//How do you treate "generated" fields?
    },
    
    getConfigurationComponent:function(componentName) {
    	var graph = configjson["@graph"];
    	var numberComponents = graph.length;
    	var n;
    	var fieldNameProperty = "http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#varName";
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
    },
    
    bindEventListeners: function() {

               
    }
                      
   

};

$(document).ready(function() {   
    minimalconfigtemplate.onLoad();
}); 