package org.esupportail.emargement.beans;

public class ExportResult {

	byte[] bytes;

	String contentType;

	String filename;

	public ExportResult(byte[] bytes, String contentType, String filename) {
		this.bytes = bytes;
		this.contentType = contentType;
		this.filename = filename;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
}
