package ch.vd.unireg.efacture;

import ch.vd.unireg.editique.TypeDocumentEditique;

public final class ArchiveKey {

	/**
	 * @see ch.vd.unireg.editique.TypeDocumentEditique#name()
	 * @see ch.vd.unireg.editique.TypeDocumentEditique
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
