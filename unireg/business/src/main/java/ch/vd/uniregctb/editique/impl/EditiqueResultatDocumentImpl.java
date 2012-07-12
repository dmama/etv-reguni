package ch.vd.uniregctb.editique.impl;

import ch.vd.uniregctb.editique.EditiqueResultatDocument;

public final class EditiqueResultatDocumentImpl extends BaseEditiqueResultatImpl implements EditiqueResultatDocument {

	private final String contentType;
	private final String documentType;
	private final byte[] content;

	public EditiqueResultatDocumentImpl(String idDocument, String contentType, String documentType, byte[] content) {
		super(idDocument);
		this.contentType = contentType;
		this.documentType = documentType;
		this.content = content;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getDocumentType() {
		return documentType;
	}

	@Override
	public byte[] getDocument() {
		return content;
	}

	@Override
	public String getToStringComplement() {
		return String.format("contentType='%s', documentType='%s'", contentType, documentType);
	}
}
