/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.semservices.bo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Concept {

   private String definedBy;
   private String conceptId;
   private String bestMatch;
   private String label;
   private String type;
   private String definition;
   private String uri;
   private String schemeURI;
   private List<RelatedTermInfo> broaderURIList;
   private List<RelatedTermInfo> narrowerURIList;
   private List<RelatedTermInfo> exactMatchURIList;
   private List<RelatedTermInfo> closeMatchURIList;
   private List<String> altLabelList;
   /** Additional information to be stored here **/
   private Map<String, String> additionalInformation;
   
   /**
    * default constructor
    */
   public Concept() {
      this.broaderURIList = new ArrayList<RelatedTermInfo>();
      this.narrowerURIList = new ArrayList<RelatedTermInfo>();
      this.exactMatchURIList = new ArrayList<RelatedTermInfo>();
      this.closeMatchURIList = new ArrayList<RelatedTermInfo>();
      this.additionalInformation = new HashMap<String, String>();
   }
   
   /**
    * @return the conceptId
    */
   public String getConceptId() {
      return conceptId;
   }
   /**
    * @param conceptId the conceptId to set
    */
   public void setConceptId(String conceptId) {
      this.conceptId = conceptId;
   }
   /**
    * @return the label
    */
   public String getLabel() {
      return label;
   }
   /**
    * @param label the label to set
    */
   public void setLabel(String label) {
      this.label = label;
   }
   /**
    * @return the type
    */
   public String getType() {
      return type;
   }
   /**
    * @param type the type to set
    */
   public void setType(String type) {
      this.type = type;
   }
   /**
    * @return the definition
    */
   public String getDefinition() {
      return definition;
   }
   /**
    * @param definition the definition to set
    */
   public void setDefinition(String definition) {
      this.definition = definition;
   }
   /**
    * @return the uri
    */
   public String getUri() {
      return uri;
   }
   /**
    * @param uri the uri to set
    */
   public void setUri(String uri) {
      this.uri = uri;
   }
   /**
    * @return the definedBy
    */
   public String getDefinedBy() {
      return definedBy;
   }
   /**
    * @param definedBy the definedBy to set
    */
   public void setDefinedBy(String definedBy) {
      this.definedBy = definedBy;
   }
   /**
    * @return the schemeURI
    */
   public String getSchemeURI() {
      return schemeURI;
   }
   /**
    * @param schemeURI the schemeURI to set
    */
   public void setSchemeURI(String schemeURI) {
      this.schemeURI = schemeURI;
   }
   /**
    * @return the bestMatch
    */
   public String getBestMatch() {
      return bestMatch;
   }
   /**
    * @param bestMatch the bestMatch to set
    */
   public void setBestMatch(String bestMatch) {
      this.bestMatch = bestMatch;
   }
public List<RelatedTermInfo> getBroaderURIList() {
	return broaderURIList;
}
public void setBroaderURIList(List<RelatedTermInfo> broaderURIList) {
	this.broaderURIList = broaderURIList;
}
public List<RelatedTermInfo> getNarrowerURIList() {
	return narrowerURIList;
}
public void setNarrowerURIList(List<RelatedTermInfo> narrowerURIList) {
	this.narrowerURIList = narrowerURIList;
}
public List<RelatedTermInfo> getExactMatchURIList() {
	return exactMatchURIList;
}
public void setExactMatchURIList(List<RelatedTermInfo> exactMatchURIList) {
	this.exactMatchURIList = exactMatchURIList;
}
public List<RelatedTermInfo> getCloseMatchURIList() {
	return closeMatchURIList;
}
public void setCloseMatchURIList(List<RelatedTermInfo> closeMatchURIList) {
	this.closeMatchURIList = closeMatchURIList;
}

public List<String> getAltLabelList() {
	return altLabelList;
}

public void setAltLabelList(List<String> altLabelList) {
	this.altLabelList = altLabelList;
}

public void setAdditionalInformation(HashMap<String, String> info) {
	this.additionalInformation = info;
}

public Map<String, String> getAdditionalInformation() {
	return this.additionalInformation;
}



}

