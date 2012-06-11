package ch.vd.uniregctb.efacture;

public final class ArchiveKey {

	/**
	 * @see ch.vd.uniregctb.editique.TypeDocumentEditique#name()
	 * @see ch.vd.uniregctb.editique.TypeDocumentEditique
	 */
	private final String typeDocument;
	private final String key;

	public ArchiveKey(String typeDocument, String key) {
		this.typeDocument = typeDocument;
		this.key = key;
	}

	public String getTypeDocument() {
		return typeDocument;
	}

	public String getKey() {
		return key;
	}
}
