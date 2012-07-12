package ch.vd.uniregctb.multipart;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.fileupload.FileItem;

public class MockFileItem implements FileItem {

	private static final long serialVersionUID = -7978419912814131651L;

	private String fieldName;
	private final String contentType;
	private final String name;
	private final String value;
	private File writtenFile;
	private boolean deleted;
	public MockFileItem(String fieldName, String contentType, String name, String value) {

		this.fieldName = fieldName;
		this.contentType = contentType;
		this.name = name;
		this.value = value;
	}
	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(value.getBytes());
	}
	@Override
	public String getContentType() {
		return contentType;
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public boolean isInMemory() {
		return true;
	}
	@Override
	public long getSize() {
		return value.length();
	}
	@Override
	public byte[] get() {
		return value.getBytes();
	}
	@Override
	public String getString(String encoding) throws UnsupportedEncodingException {
		return new String(get(), encoding);
	}
	@Override
	public String getString() {
		return value;
	}
	@Override
	public void write(File file) throws Exception {
		this.writtenFile = file;
	}
	public File getWrittenFile() {
		return writtenFile;
	}
	@Override
	public void delete() {
		this.deleted = true;
	}
	public boolean isDeleted() {
		return deleted;
	}
	@Override
	public String getFieldName() {
		return fieldName;
	}
	@Override
	public void setFieldName(String s) {
		this.fieldName = s;
	}
	@Override
	public boolean isFormField() {
		return (this.name == null);
	}
	@Override
	public void setFormField(boolean b) {
		throw new UnsupportedOperationException();
	}
	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}
}