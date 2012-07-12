package ch.vd.uniregctb.efacture;

import ch.vd.uniregctb.editique.TypeDocumentEditique;

public final class ArchiveKey {

	/**
	 * @see ch.vd.uniregctb.editique.TypeDocumentEditique#name()
	 * @see ch.vd.uniregctb.editique.TypeDocumentEditique
	 */
	private final TypeDocumentEditique typeDocument;
	private final String key;

	public ArchiveKey(TypeDocumentEditique typeDocument, String key) {
		this.typeDocument = typeDocument;
		this.key = key;
	}

	public String getTypeDocument() {
		return typeDocument != null ? typeDocument.name() : null;
	}

	public String getKey() {
		return key;
	}
}
