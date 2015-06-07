/**
 * 
 */
package structures;

import utils.Utils;
import json.JSONException;
import json.JSONObject;

/**
 * @author hongning
 * @version 0.1
 * @category data structure
 * data structure for a forum discussion post 
 */
public class Review {
	//unique ID from the corresponding website
	String m_ID;
	public String getID() {
		return m_ID;
	}
	
	String m_pros;
	public String getPros() {
		return m_pros;
	}
	public void setPros(String m_pros) {
	
		this.m_pros = m_pros;
	}
	
	
	String m_cons;
	public String getCons() {
		return m_cons;
	}
	public void setCons(String m_cons) {
		this.m_cons = m_cons;
	}

	//Constructor.
	public Review(JSONObject json) throws NumberFormatException {
		
		setPros(Utils.getJSONValue(json, "Pros"));
		setCons(Utils.getJSONValue(json, "Cons"));
		
	}
	
	public JSONObject getJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("Pros", m_pros);//must contain
		json.put("Cons", m_cons);//must contain
		return json;
	}
}
