/* $This file is distributed under the terms of the license in /doc/license.txt$ */

var minimalconfigtemplate = {

    /* *** Initial page setup *** */
   
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

        //this.initItemData();
       
        this.bindEventListeners();
        
        this.generateFields();
                       
    },
    
    generateFields: function() {
    	//date time has specific handling, do separately
    	//fields in order
    	var fields = ["pubType", "title"];
    	var f;
    	var len = fields.length;
		var form = $("#addpublicationToPerson");
    	for(f = 0; f < len; f++) {
    		var fieldName = fields[f];
    		//Find this field within the configuration
    		var configComponent = minimalconfigtemplate.getConfigurationComponent(fieldName);
    		//Constant options field
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
    		else if(minimalconfigtemplate.componentHasType(configComponent, "forms:StringField")) {
    			var HTML =  '<p><label for="title">' + fieldName + '</label><input class="acSelector" size="60"  type="text" id="' + fieldName + '" name="' + fieldName + '" acGroupName="publication"  value="" /></p>';
    			form.append(HTML);
    		}
    		
    	}
    	
    	
    	
    } ,
    
    getConfigurationComponent:function(componentName) {
    	var graph = configjson["@graph"];
    	var numberComponents = graph.length;
    	var n;
    	var fieldNameProperty = "http://vitro.mannlib.cornell.edu/ns/vitro/CustomFormConfiguration#fieldName";
    	for(n = 0; n < numberComponents; n++) {
    		var component = graph[n];
    		var fieldNamesArray = new Array().concat(component[fieldNameProperty]);
    		
			var includedInArray = $.inArray(componentName, fieldNamesArray);
			if(includedInArray > -1) {
				return component;
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