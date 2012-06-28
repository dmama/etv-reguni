package ch.vd.uniregctb.type;

public enum TypeEtatDestinataire {
	INSCRIT("Inscrit"),
	INSCRIT_SUSPENDU("Inscrit suspendu"),
	DESINSCRIT("Désinscrit"),
	DESINSCRIT_SUSPENDU("Désinscrit suspendu");

	private String description;

	private TypeEtatDestinataire(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean isActivable(){
		return this == INSCRIT_SUSPENDU || this == DESINSCRIT_SUSPENDU ;
	}

	public boolean isSuspendable(){
		return this == INSCRIT || this == DESINSCRIT;
	}
}
