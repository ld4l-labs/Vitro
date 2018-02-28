/* $This file is distributed under the terms of the license in /doc/license.txt$ */
package edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.fields;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;

import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.dao.WebappDaoFactory;
import edu.cornell.mannlib.vitro.webapp.edit.n3editing.VTwo.EditConfigurationVTwo;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.impl.RDFServiceUtils;

public class IndividualsMatchingQuery implements FieldOptions {
    
    private static final Log log = LogFactory.getLog(IndividualsMatchingQuery.class);    
    private VitroRequest vreq;
    private String sparqlQuery;
    private String labelVariable = "label";
    private String uriVariable = "uri";
    
    //The query is assumed to return variables value and label
    public IndividualsMatchingQuery(String sparqlQuery, VitroRequest vreq) throws Exception {
        super();
		this.vreq = vreq;
		this.sparqlQuery = sparqlQuery;
    }
    
  
    
    @Override
    public Map<String, String> getOptions(
            EditConfigurationVTwo editConfig, 
            String fieldName, 
            WebappDaoFactory wDaoFact) {
        HashMap<String, String> optionsMap = new LinkedHashMap<String, String>();
        int optionsCount = 0;
        ResultSet rs = executeQuery(sparqlQuery, vreq);
        if(rs != null) {
        	while(rs.hasNext()) {
        		QuerySolution qs = rs.nextSolution();
        		if(qs.contains(uriVariable) && 
        				qs.get(uriVariable).isResource() &&
        				qs.contains(labelVariable) &&
        				qs.get(labelVariable).isLiteral()) {
        			Literal label = qs.getLiteral(labelVariable);
        			Resource uri = qs.getResource(uriVariable);
        			
        			if(label != null && uri != null) {
        				optionsMap.put(uri.getURI(), label.getString());
        			}
        		}
        	}
        }
       
        
        return optionsMap;
    }


	private ResultSet executeQuery (String sparqlQuery, VitroRequest vreq) {
	    try {
            RDFService rdfService = getRdfService(vreq);
            return RDFServiceUtils.sparqlSelectQuery(sparqlQuery, rdfService);
        } catch (Exception e) {
           log.error("Error occurred retrieving values for " + sparqlQuery);
        }
	    return null;
	}

	private RDFService getRdfService(HttpServletRequest req) {
		return RDFServiceUtils.getRDFService(new VitroRequest(req));
	}
	
	  public Comparator<String[]> getCustomComparator() {
	    	return null;
	    }

}

