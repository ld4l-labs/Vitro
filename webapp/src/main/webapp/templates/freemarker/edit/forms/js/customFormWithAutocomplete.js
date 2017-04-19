/* $This file is distributed under the terms of the license in /doc/license.txt$ */

function CustomFormWithAutocomplete(formData, i18n) {
    var customForm = {
        onLoad: onLoad
    };
    return customForm;
    
    var acFilterForIndividuals = formData.acFilterForIndividuals;
    var acMultipleTypes = formData.acMultipleTypes;
    var acSelectOnly = formData.acSelectOnly;
    var acTypes = formData.acTypes;
    var acUrl = formData.acUrl;
    var baseHref = formData.baseHref;
    var blankSentinel = formData.blankSentinel;
    var defaultTypeName = formData.defaultTypeName;
    var editMode = formData.editMode;
    var flagClearLabelForExisting = formData.flagClearLabelForExisting;
    var formSteps = formData.formSteps;
    var multipleTypeNames = formData.multipleTypeNames;
    var sparqlForAcFilter = formData.sparqlForAcFilter;
    var sparqlQueryUrl = formData.sparqlQueryUrl;
    var typeName = formData.typeName;
    
    ver placeholderText;
    var labelsWithPlaceholders;
    
    //Setting the default Concept class here
    //This would need to change if we update the ontology, etc.
    var conceptClassURI: "http://www.w3.org/2004/02/skos/core#Concept",

    /* *** Initial page setup *** */
    function onLoad() {
        if (disableFormInUnsupportedBrowsers()) {
            return;
        }
        mixIn();
        initObjects();
        initPage();
    }

    function disableFormInUnsupportedBrowsers() {
        var disableWrapper = $('#ie67DisableWrapper');

        // Check for unsupported browsers only if the element exists on the page
        if (disableWrapper.length) {
            if (vitro.browserUtils.isIELessThan8()) {
                disableWrapper.show();
                $('.noIE67').hide();
                return true;
            }
        }
        return false;
    }
    
    function mixIn() {
        // Mix in the custom form utility methods
        $.extend(this, vitro.customFormUtils);
    }
    
    var form;
    var fullViewOnly;
    var button;
    var requiredLegend;
    var typeSelector;
    var typeSelectorInput;
    var typeSelectorSpan;
    var or;
    var cancel;
    var acHelpTextClass;
    var verifyMatch;
    var defaultAcType;
    var templateDefinedAcTypes;
    var acSelectors;
    var acSelections;
    var hasMultipleTypeNames;
    var clearAcSelections;
    
    // On page load, create references for easy access to form elements.
    // NB These must be assigned after the elements have been loaded onto the page.
    function initObjects() {
        form = $('form.customForm');
        this.form = form // --- For compatibility with customFormUtils.
        fullViewOnly = $('.fullViewOnly');
        button = $('#submit');
        requiredLegend = $('#requiredLegend');
        typeSelector = form.find('select#typeSelector');
        typeSelectorInput = form.find('input#typeSelectorInput');
        typeSelectorSpan = form.find('span#typeSelectorSpan');
        or = $('span.or');
        cancel = form.find('.cancel');
        acHelpTextClass = 'acSelectorWithHelpText';
        // verifyMatch is referenced in bindEventListeners to size and open
        // the verify popup window. Although there could be multiple verifyMatch objects
        // selecting one and binding the event works for all of them
        verifyMatch = form.find('.verifyMatch');
        defaultAcType = ""; // will be set in setType() first time through
        templateDefinedAcTypes = false;
        if ( acTypes != undefined ) {
            templateDefinedAcTypes = true;
        }

        // find all the acSelector input elements
        acSelectors = [] ;

        form.find('.acSelector').each(function() {
            acSelectors.push($(this));
        });

        // find all the acSelection div elements
        acSelections = new Object();
        form.find('.acSelection').each(function() {
            var groupName  = $(this).attr('acGroupName');
            acSelections[groupName] = $(this);
        });

        // 2-stage forms with only one ac field will not have the acTypes defined
        // so create an object for when the user selects a type via the typeSelector
        if ( acTypes == undefined || acTypes == null ) {
            acTypes = new Object();
        }

        // forms with multi ac fields will have this defined in customFormData
        // this is helpful when the type to display is not a single word, like "Subject Area"
        hasMultipleTypeNames = false;
        if ( multipleTypeNames != undefined || multipleTypeNames != null ) {
            hasMultipleTypeNames = true;
        }
        // Used with the cancel link. If the user cancels after a type selection, this check
        // ensures that any a/c fields (besides the one associated with the type) will be reset
        clearAcSelections = false;
    }

    // Set up the form on page load
    function initPage() {
        if (!editMode) {
            editMode = 'add'; // edit vs add: default to add
        }

        //Flag to clear label of selected object from autocomplete on submission
        //This is used in the case where the label field is submitted only when a new object is being created
        if(!flagClearLabelForExisting) {
            flagClearLabelForExisting = null;
        }

        if (!formSteps) {
            // Don't override formSteps specified in form data
            if ( !fullViewOnly.length || editMode === 'edit' || editMode === 'repair' ) {
                formSteps = 1;
                // there may also be a 3-step form - look for this.subTypeSelector
            }
            else {
                formSteps = 2;
            }
        }

        bindEventListeners();

        $.each(acSelectors, function() {
            initAutocomplete($(this));
        });

        initElementData();

        initFormView();

        // Set the initial autocomplete help text in the acSelector fields.
        $.each(acSelectors, function() {
            addAcHelpText($(this));
        });
    }

    function initFormView() {
        var typeVal = typeSelector.val();

        // Put this case first, because in edit mode with
        // validation errors we just want initFormFullView.
        //        if ((!supportEdit) && (editMode == 'edit' || editMode == 'repair')) {
        if (editMode == 'edit' || editMode == 'repair' || editMode == 'error') {
            initFormWithValidationErrors();
        }
        else if (this.findValidationErrors()) {  // --- customFormUtils
            initFormWithValidationErrors();
        }

        // If type is already selected when the page loads (Firefox retains value
        // on a refresh), go directly to full view. Otherwise user has to reselect
        // twice to get to full view.
        else if ( formSteps == 1 || typeVal.length ) {
            initFormFullView();
        }
        else {
            initFormTypeView();
        }
    }

    function initFormTypeView() {
        setType(); // empty any previous values (perhaps not needed)
        this.hideFields(fullViewOnly); // --- customFormUtils
        button.hide();
        or.hide();
        requiredLegend.hide();

        this.cancel.unbind('click');
    }

    function initFormFullView() {
        setType();
        fullViewOnly.show();
        or.show();
        requiredLegend.show();
        button.show();
        setLabels();

        // Set the initial autocomplete help text in the acSelector fields.
        $.each(acSelectors, function() {
            addAcHelpText($(this));
        });

        this.cancel.unbind('click');
        if (formSteps > 1) {
            this.cancel.click(function() {
                customForm.clearFormData(); // clear any input and validation errors
                initFormTypeView();
                clearAcSelections = true;
                return false;
            });
            // In one-step forms, if there is a type selection field, but no value is selected,
            // hide the acSelector field. The type selection must be made first so that the
            // autocomplete type can be determined. If a type selection has been made,
            // unhide the acSelector field.
        } else if (typeSelector.length) {
            typeSelector.val() ? fullViewOnly.show() : this.hideFields(fullViewOnly);
        }
        if ( acSelectOnly ) {
            disableSubmit();
        }
    }

    function initFormWithValidationErrors() {
        // Call initFormFullView first, because showAutocompleteSelection needs
        // acType, which is set in initFormFullView.
        initFormFullView();

        $.each(acSelectors, function() {
            var $acSelection = acSelections[$(this).attr('acGroupName')];
            var uri   = $acSelection.find('input.acUriReceiver').val(),
            label = $(this).val();
            if (uri && uri != ">SUBMITTED VALUE WAS BLANK<") {
                showAutocompleteSelection(label, uri, $(this));
            }
        });

    }

    // Bind event listeners that persist over the life of the page. Event listeners
    // that depend on the view should be initialized in the view setup method.
    function bindEventListeners() {
        typeSelector.change(function() {
            var typeVal = $(this).val();

            // If an autocomplete selection has been made, undo it.
            // NEED TO LINK THE TYPE SELECTOR TO THE ACSELECTOR IT'S ASSOCIATED WITH
            // BECAUSE THERE COULD BE MORE THAN ONE AC FIELD. ASSOCIATION IS MADE VIA
            // THE SPECIAL "acGroupName" ATTRIBUTE WHICH IS SHARED AMONG THE SELECT AND
            // THE INPUT AND THE AC SELECTION DIV.
            if (editMode != "edit") {
                undoAutocompleteSelection($(this));
            }
            // Reinitialize view. If no type selection in a two-step form, go back to type view;
            // otherwise, reinitialize full view.
            if (!typeVal.length && formSteps > 1) {
                initFormTypeView();
            }
            else {
                initFormFullView();
            }
        });

        verifyMatch.click(function() {
            window.open($(this).attr('href'), 'verifyMatchWindow', 'width=640,height=640,scrollbars=yes,resizable=yes,status=yes,toolbar=no,menubar=no,location=no');
            return false;
        });

        // loop through all the acSelectors
        $.each(acSelectors, function() {
            $(this).focus(function() {
                deleteAcHelpText($(this));
            });
            $(this).blur(function() {
                addAcHelpText($(this));
            });
        });

        form.submit(function() {
            //TODO: update the following
            //custom submission for edit mode in case where existing object should not remove original object
            //if edit mode and custom flag and original uri not equivalent to new uri, then
            //clear out label field entirely
            //originally checked edit mode but want to add to work the same way in case an existing object
            //is selected since the n3 now governs how labels
            if(flagClearLabelForExisting != null) {
                //Find the elements that have had autocomplete executed, tagged by class "userSelected"
                form.find('.acSelection.userSelected').each(function() {
                    var groupName = $(this).attr("acGroupName");
                    var inputs = $(this).find("input.acUriReceiver");
                    //if user selected, then clear out the label since we only
                    //want to submit the label as value on form if it's a new label
                    if(inputs.length && $(inputs.eq(0)).attr(flagClearLabelForExisting)) {
                        var $selectorInput = $("input.acSelector[acGroupName='" + groupName + "']");
                        var $displayInput = $("input.display[acGroupName='" + groupName + "']");
                        $displayInput.val($selectorInput.val());
                        $selectorInput.val('');
                    }
                });
            }

            deleteAcHelpText();
        });
    }

    function initAutocomplete(selectedObj) {
        var acCache = {};

        getAcFilter();
        //If specific individuals are to be filtered out, add them here
        //to the filtering list
        getAcFilterForIndividuals();

        $(selectedObj).autocomplete({
            minLength: 3,
            source: function(request, response) {
                //Reset the URI of the input to one that says new uri required
                //That will be overwritten if value selected from autocomplete
                //We do this everytime the user types anything in the autocomplete box
                initDefaultBlankURI(selectedObj);
                if (request.term in acCache) {
                    // console.log('found term in cache');
                    response(acCache[request.term]);
                    return;
                }
                // console.log('not getting term from cache');
                $.ajax(
                    {
                        url: acUrl,
                        dataType: 'json',
                        data: {
                            term: request.term,
                            type: acTypes[$(selectedObj).attr('acGroupName')],
                            multipleTypes:(acMultipleTypes == undefined || acMultipleTypes == null)? null: acMultipleTypes
                        },
                        complete: function(xhr, status) {
                            // Not sure why, but we need an explicit json parse here.
                            var results = $.parseJSON(xhr.responseText);
                            var filteredResults = filterAcResults(results);
                            /*
                            if ( acTypes[$(selectedObj).attr('acGroupName')] == conceptClassURI ) {
                            filteredResults = customForm.removeConceptSubclasses(filteredResults);
                        }*/
                        if(doRemoveConceptSubclasses()) {
                            filteredResults = customForm.removeConceptSubclasses(filteredResults);
                        }

                        acCache[request.term] = filteredResults;
                        response(filteredResults);
                    }
                });
            },
            select: function(event, ui) {
                showAutocompleteSelection(ui.item.label, ui.item.uri, $(selectedObj));
                if ( $(selectedObj).attr('acGroupName') == typeSelector.attr('acGroupName') ) {
                    typeSelector.val(ui.item.msType);
                }
            }
        });
    }

    //Method to check whether we need to filter to individuals with a most specific type = Concept or other allowed subclasses
    function doRemoveConceptSubclasses() {
        //if this array of allowable subclasses was declared annd there is at least one element in it
        if(customForm.limitToConceptClasses && customForm.limitToConceptClasses.length) {
            return true;
        }
        return false;
    }

    // Store original or base text with elements that will have text substitutions.
    // Generally the substitution cannot be made on the current value, since that value
    // may have changed from the original. So we store the original text with the element to
    // use as a base for substitutions.
    function initElementData() {
        placeholderText = '###';
        labelsWithPlaceholders = form.find('label, .label').filter(function() {
            return $(this).html().match(placeholderText);
        });
        labelsWithPlaceholders.each(function(){
            $(this).data('baseText', $(this).html());
        });

        button.data('baseText', button.val());
    }

    var acFilter;
    
    //get autocomplete filter with sparql query
    function getAcFilter() {
        if (!sparqlForAcFilter) {
            //console.log('autocomplete filtering turned off');
            acFilter = null;
            return;
        }

        //console.log("sparql for autocomplete filter: " + sparqlForAcFilter);

        // Define acFilter here, so in case the sparql query fails
        // we don't get an error when referencing it later.
        acFilter = [];
        $.ajax(
            {
                url: sparqlQueryUrl,
                dataType: "json",
                data: {
                    query: sparqlForAcFilter
                },
                success: function(data, status, xhr) {
                    setAcFilter(data);
                }
            }
        );
    }

    function setAcFilter(data) {
        var key = data.head.vars[0];

        $.each(data.results.bindings, function() {
            acFilter.push(this[key].value);
        });
    }

    function filterAcResults(results) {
        var filteredResults;

        if (!acFilter || !acFilter.length) {
            //console.log('no autocomplete filtering applied');
            return results;
        }

        filteredResults = [];
        $.each(results, function() {
            if ($.inArray(this.uri, acFilter) == -1) {
                //console.log('adding ' + this.label + ' to filtered results');
                filteredResults.push(this);
            }
            else {
                //console.log('filtering out ' + this.label);
            }
        });
        return filteredResults;
    }

    //To filter out specific individuals, not part of a query
    //Pass in list of individuals to be filtered out
    function getAcFilterForIndividuals() {
        if (!acFilterForIndividuals || !acFilterForIndividuals.length) {
            acFilterForIndividuals = null;
            return;
        }
        //add this list to the ac filter list
        acFilter = acFilter.concat(acFilterForIndividuals);

    }

    //Updating this code to utilize an array to
    removeConceptSubclasses: function(array) {
        //Using map because the resulting array might be different from the original
        array = jQuery.map(array, function(arrayValue, i) {
            var allMsTypes = arrayValue["allMsTypes"];
            var removeElement = false;
            if(allMsTypes.length == 1 && !isAllowedConceptSubclass(arrayValue["msType"])) {
                //Remove from array
                removeElement = true;
            }  else if(allMsTypes.length > 1) {
                //If there are multiple most specific types returned, check if none of them equals concept
                removeElement = true;
                var j;

                for(j = 0; j < allMsTypes.length; j++) {
                    //this refers to the element itself
                    if(isAllowedConceptSubclass(allMsTypes[j])) {
                        //don't remove this element if one of the most specific types is a concept
                        removeElement = false;
                        break;
                    }
                }
            }

            if(removeElement)
            return null;
            else
            return arrayValue;
        });


        return array;
    }

    function isAllowedConceptSubclass(classURI) {
        if(customForm.limitToConceptClasses && customForm.limitToConceptClasses.length) {
            var len = customForm.limitToConceptClasses.length;
            var i;
            for(i = 0; i < len; i++) {
                if(classURI == customForm.limitToConceptClasses[i]) {
                    return true;
                }
            }
        }
        return false;
    }

    function showAutocompleteSelection(label, uri, selectedObj) {
        // hide the acSelector field and set it's value to the selected ac item
        this.hideFields($(selectedObj).parent());
        $(selectedObj).val(label);

        var $acDiv = acSelections[$(selectedObj).attr('acGroupName')];

        // provides a way to monitor selection in other js files, e.g. to hide fields upon selection
        $acDiv.addClass("userSelected");

        // If the form has a type selector, add type name to label in add mode. In edit mode,
        // use typeSelectorSpan html. The second case is an "else if" and not an else because
        // the template may not be passing the label to the acSelection macro or it may not be
        // using the macro at all and the label is hard-coded in the html.
        // ** With release 1.6 and display of all fields, more labels are hard-coded in html.
        // ** So check if there's a label before doing anything else.

        if ( $acDiv.find('label').html().length === 0 ) {

            if ( typeSelector.length && ($acDiv.attr('acGroupName') == typeSelector.attr('acGroupName')) ) {
                $acDiv.find('label').html('Selected ' + typeName + ':');
            }
            else if ( typeSelectorSpan.html() && ($acDiv.attr('acGroupName') == typeSelectorInput.attr('acGroupName')) ) {
                $acDiv.find('label').html('Selected ' + typeSelectorSpan.html() + ':');
            }
            else if ( $acDiv.find('label').html() == '' ) {
                $acDiv.find('label').html('Selected ' + multipleTypeNames[$(selectedObj).attr('acGroupName')] + ':');
            }
        }

        $acDiv.show();
        $acDiv.find("input").val(uri);
        $acDiv.find("span").html(label);
        $acDiv.find("a.verifyMatch").attr('href', baseHref + uri);

        $changeLink = $acDiv.find('a.changeSelection');
        $changeLink.click(function() {
            undoAutocompleteSelection($acDiv);
        });

        if ( acSelectOnly ) {
            //On initialization in this mode, submit button is disabled
            enableSubmit();
        }
    }

    function undoAutocompleteSelection(selectedObj) {
        // The test is not just for efficiency: undoAutocompleteSelection empties the acSelector value,
        // which we don't want to do if user has manually entered a value, since he may intend to
        // change the type but keep the value. If no new value has been selected, form initialization
        // below will correctly empty the value anyway.

        var $acSelectionObj = null;
        var $acSelector = null;

        // Check to see if the parameter is the typeSelector. If it is, we need to get the acSelection div
        // that is associated with it.  Also, when the type is changed, we need to determine whether the user
        // has selected an existing individual in the corresponding name field or typed the label for a new
        // individual. If the latter, we do not want to clear the value on type change. The clearAcSelectorVal
        // boolean controls whether the acSelector value gets cleared.

        var clearAcSelectorVal = true;

        if ( $(selectedObj).attr('id') == "typeSelector" ) {
            $acSelectionObj = acSelections[$(selectedObj).attr('acGroupName')];
            if ( $acSelectionObj.is(':hidden') ) {
                clearAcSelectorVal = false;
            }
            // if the type is being changed after a cancel, any additional a/c fields that may have been set
            // by the user should be "undone". Only loop through these if this is not the initial type selection
            if ( clearAcSelections ) {
                $.each(acSelections, function(i, acS) {
                    var $checkSelection = acSelections[i];
                    if ( $checkSelection.is(':hidden') && $checkSelection.attr('acGroupName') != $acSelectionObj.attr('acGroupName') ) {
                        resetAcSelection($checkSelection);
                        $acSelector = getAcSelector($checkSelection);
                        $acSelector.parent('p').show();
                    }
                });
            }
        }
        else {
            $acSelectionObj = $(selectedObj);
            typeSelector.val('');
        }

        $acSelector = getAcSelector($acSelectionObj);
        $acSelector.parent('p').show();
        resetAcSelection($acSelectionObj);
        if ( clearAcSelectorVal == true ) {
            $acSelector.val('');
            $("input.display[acGroupName='" + $acSelectionObj.attr('acGroupName') + "']").val("");
        }
        addAcHelpText($acSelector);

        //Resetting so disable submit button again for object property autocomplete
        if ( acSelectOnly ) {
            disableSubmit();
        }
        clearAcSelections = false;
    }

    // this is essentially a subtask of undoAutocompleteSelection
    function resetAcSelection(selectedObj) {
        this.hideFields($(selectedObj));
        $(selectedObj).removeClass('userSelected');
        $(selectedObj).find("input.acUriReceiver").val(blankSentinel);
        $(selectedObj).find("span").text('');
        $(selectedObj).find("a.verifyMatch").attr('href', baseHref);
    }

    // loops through the array of acSelector fields and returns the one
    // associated with the selected object
    function getAcSelector(selectedObj){
        var $selector = null
        $.each(acSelectors, function() {
            if ( $(this).attr('acGroupName') == $(selectedObj).attr('acGroupName') ) {
                $selector = $(this);
            }
        });
        return $selector;
    }

    // Set type uri for autocomplete, and type name for labels and button text.
    // Note: we still need this in edit mode, to set the text values.
    function setType() {
        var selectedType;
        // If there's no type selector, these values have been specified in customFormData,
        // and will not change over the life of the form.
        if (!typeSelector.length) {
            if ( editMode == 'edit' && (typeSelectorSpan.html() != null && typeSelectorInput.val() != null) ) {
                typeName = typeSelectorSpan.html();
                acTypes[typeSelectorInput.attr('acGroupName')] = typeSelectorInput.val();
            }
            return;
        }

        selectedType = typeSelector.find(':selected');
        var acTypeKey = typeSelector.attr('acGroupName');

        if ( templateDefinedAcTypes && !defaultAcType.length ) {
            defaultAcType = acTypes[acTypeKey];
        }
        if (selectedType.val().length) {
            acTypes[acTypeKey] = selectedType.val();
            typeName = selectedType.html();
            if ( editMode == 'edit' ) {
                var $acSelect = acSelections[acTypeKey];
                $acSelect.find('label').html( i18n.selectedString + ' ' + typeName + ':');
            }
        }
        // reset to empty values;
        else {
            if ( templateDefinedAcTypes ) {
                acTypes[acTypeKey] = defaultAcType;
            }
            else {
                acTypes = new Object();
            }
            typeName = defaultTypeName;
        }
    }

    // Set field labels based on type selection. Although these won't change in edit
    // mode, it's easier to specify the text here than in the ftl.
    function setLabels() {
        var typeName = getTypeNameForLabels();

        labelsWithPlaceholders.each(function() {
            var newLabel = $(this).data('baseText').replace(placeholderText, typeName);
            $(this).html(newLabel);
        });

    }

    function getTypeNameForLabels(selectedObj) {
        // If this.acType is empty, we are either in a one-step form with no type yet selected,
        // or in repair mode in a two-step form with no type selected. Use the default type
        // name specified in the form data.
        if ( !selectedObj || !hasMultipleTypeNames ) {
            if ( acTypes && typeName ) {
                return typeName;
            }
            else {
                return this.capitalize(defaultTypeName); // --- customFormUtils
            }
        }
        else if ( selectedObj && ( $(selectedObj).attr('acGroupName') == typeSelector.attr('acGroupName') ) ) {
            if ( acTypes && typeName ) {
                return typeName;
            }
            else {
                return this.capitalize(defaultTypeName); // --- customFormUtils
            }
        }
        else {
            var name = multipleTypeNames[$(selectedObj).attr('id')];
            return this.capitalize(name); // --- customFormUtils
        }
    }

    // Set the initial help text that appears in the autocomplete field and change the class name
    function addAcHelpText(selectedObj) {
        var typeText;
        // First case applies on page load; second case applies when the type gets changed. With multiple
        // ac fields there are cases where we also have to check if the help text is already there
        if (!$(selectedObj).val() || $(selectedObj).hasClass(acHelpTextClass) || $(selectedObj).val().substring(0, 18) == i18n.selectAnExisting ) {
            typeText = getTypeNameForLabels($(selectedObj));
            var helpText = i18n.selectAnExisting + " " + typeText + " " + i18n.orCreateNewOne ;
            //Different for object property autocomplete
            if ( acSelectOnly ) {
                helpText = i18n.selectAnExisting + " " + typeText;
            }
            $(selectedObj).val(helpText)
            .addClass(acHelpTextClass);
        }
    }

    function deleteAcHelpText(selectedObj) {
        // on submit, no selectedObj gets passed, so we need to check for this
        if ( selectedObj ) {
            if ($(selectedObj).hasClass(acHelpTextClass)) {
                $(selectedObj).val('')
                .removeClass(acHelpTextClass);
            }
        }
        else {
            $.each(acSelectors, function() {
                if ($(this).hasClass(acHelpTextClass)) {
                    $(this).val('')
                    .removeClass(acHelpTextClass);
                }
            });
        }
    }

    function disableSubmit() {
        //Disable submit button until selection made
        button.attr('disabled', 'disabled');
        button.addClass('disabledSubmit');
    }

    function enableSubmit() {
        button.removeAttr('disabled');
        button.removeClass('disabledSubmit');
    }

    function initDefaultBlankURI(selectedObj) {
        //get uri input for selected object and set to value specified as "blank sentinel"
        //If blank sentinel is neither null nor an empty string, this means if the user edits an
        //existing relationship to an object and does not select anything from autocomplete
        //from that object, the old relationship will be removed in n3 processing
        var $acDiv = acSelections[$(selectedObj).attr('acGroupName')];
        $acDiv.find("input").val(blankSentinel);
    }
}

$(document).ready(function() {
    //If undefined or set to false, load normally
    //If set to true, don't laod
    if(preventLoadFlag == undefined || !preventLoadFlag) {
        new CustomFormWithAutocomplete(customFormData, i18nStrings).onLoad();
    }
});
