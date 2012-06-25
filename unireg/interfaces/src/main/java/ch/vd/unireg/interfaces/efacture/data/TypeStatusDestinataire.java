package ch.vd.unireg.interfaces.efacture.data;

public enum TypeStatusDestinataire {
	INSCRIT("Inscrit"),
	INSCRIT_SUSPENDU("Inscrit suspendu"),
	DESINSCRIT("Désinscrit"),
	DESINSCRIT_SUSPENDU("Désinscrit suspendu");

	private String description;

	private TypeStatusDestinataire(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
