package retrofit.converter.server;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "document")
public class Document {
	private String docId;
	private String mimeType;
	private String folderName;

	public String getDocId() {
		return docId;
	}

	@XmlAttribute
	public void setDocId(String docId) {
		this.docId = docId;
	}

	public String getMimeType() {
		return mimeType;
	}

	@XmlAttribute
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getFolderName() {
		return folderName;
	}

	@XmlAttribute
	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}
}
