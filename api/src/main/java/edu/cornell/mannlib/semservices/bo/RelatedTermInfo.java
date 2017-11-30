package edu.cornell.mannlib.semservices.bo;


/**Information for broader,narrower, exact**/
public class RelatedTermInfo {
	private String label;
	private String uri;
	public RelatedTermInfo(String inputLabel, String inputURI) {
		this.label = inputLabel;
		this.uri = inputURI;
	}
	public String getLabel() {
		return label;
	}
	public String getUri() {
		return uri;
	}
	
	
	
}
