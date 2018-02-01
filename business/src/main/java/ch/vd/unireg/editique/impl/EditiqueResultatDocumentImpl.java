package ch.vd.unireg.editique.impl;

import ch.vd.unireg.editique.EditiqueResultatDocument;
import ch.vd.unireg.editique.TypeDocumentEditique;

public final class EditiqueResultatDocumentImpl extends BaseEditiqueResultatImpl implements EditiqueResultatDocument {

	private final String contentType;
	private final TypeDocumentEditique documentType;
	private final byte[] content;

	public EditiqueResultatDocumentImpl(String idDocument, String contentType, TypeDocumentEditique documentType, byte[] content) {
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
	public TypeDocumentEditique getDocumentType() {
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
