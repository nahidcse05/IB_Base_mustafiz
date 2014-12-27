/**
 * 
 */
package structures;

import json.JSONException;
import json.JSONObject;

/**
 * @author hongning
 * @version 0.1
 * @category data structure
 * data structure for a forum discussion post 
 */
public class Post {
	//unique ID from the corresponding website
	String m_ID;
	public String getID() {
		return m_ID;
	}

	//unique ProductID from the corresponding website
	String m_ProductID;
	public String getProductID() {
			return m_ProductID;
		}

	//ProductName from the corresponding website
		String m_ProductName;
		public String getProductName() {
				return m_ProductName;
			}
	
	//author's displayed name
	String m_author;
	public String getAuthor() {
		return m_author;
	}
	public void setAuthor(String author) {
		this.m_author = author;
	}

	//unique author ID from the corresponding website
	String m_authorID;	
	public String getAuthorID() {
		return m_authorID;
	}
	public void setAuthorID(String authorID) {
		this.m_authorID = authorID;
	}

	//post title (might not be available in some medical forums)
	String m_title;//not available in WebMD
	public String getTitle() {
		return m_title;
	}
	public void setTitle(String title) {
		if (!title.isEmpty())
			this.m_title = title;
	}

	//post content
	String m_content;
	public String getContent() {
		return m_content;
	}
	public void setContent(String content) {
		if (!content.isEmpty())
			this.m_content = content;
	}

	//timestamp of the post
	String m_date;
	public String getDate() {
		return m_date;
	}
	public void setDate(String date) {
		this.m_date = date;
	}
	
	//post ID that this post is reply to
	String m_replyToID;
	public String getReplyToID() {
		return m_replyToID;
	}
	public void setReplyToID(String replyToID) {
		this.m_replyToID = replyToID;
	}
	
	//only used in eHealth to keep track of reply-to relation
	int m_level; 
	public int getLevel() {
		return m_level;
	}
	public void setLevel(int level) {
		this.m_level = level;
	}
	
	//Used for classification.
	int m_label;
	public void setLabel(int overall){
		this.m_label = overall;
	}
	public int getLabel(){
		return this.m_label;
	}
	
	//Constructor.
	public Post(String ID) {
		m_ID = ID;
	}
	//Constructor.
	public Post(JSONObject json) throws NumberFormatException {
		try {
			if (json.has("Overall")){
				if(json.getString("Overall").equals("None")){
					System.out.print('R');
				} else{
					double label = Double.parseDouble(json.getString("Overall"));
					if(label <= 0 && label > 5){
						System.out.print('L');
					} else setLabel((int)label);
				}
			}
			if (json.has("Date"))
				setDate(json.getString("Date"));
			if (json.has("Content"))
				setContent(json.getString("Content"));
			if (json.has("Title")) 
				setTitle(json.getString("Title"));
			if (json.has("ReviewID"))
				m_ID = json.getString("ReviewID");
			if (json.has("Author"))
				setAuthor(json.getString("Author"));
			if (json.has("ProductID"))
				m_ProductID = json.getString("ProductID");
			if(json.has("Name"))
				m_ProductName = json.getString("Name");
				
		} catch (Exception e) {
			System.out.print("");
		}
	}
	
	public JSONObject getJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("postID", m_ID);//must contain
		json.put("author", m_author);//must contain
		json.put("authorID", m_authorID);//must contain
		json.put("replyTo", m_replyToID);//might be missing
		json.put("date", m_date);//must contain
		json.put("title", m_title);//might be missing
		json.put("content", m_content);//must contain
		return json;
	}
}
