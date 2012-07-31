package ch.vd.unireg.interfaces.efacture.data;

import ch.vd.evd0025.v1.PayerStatus;

public enum TypeEtatDestinataire {
	INSCRIT("Inscrit"),
	INSCRIT_SUSPENDU("Inscrit suspendu"),
	DESINSCRIT("Désinscrit"),
	DESINSCRIT_SUSPENDU("Désinscrit suspendu");

	private final String description;

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

	public static TypeEtatDestinataire valueOf (PayerStatus status) {
		switch (status) {
		case DESINSCRIT:
			return TypeEtatDestinataire.DESINSCRIT;
		case DESINSCRIT_SUSPENDU:
			return TypeEtatDestinataire.DESINSCRIT_SUSPENDU;
		case INSCRIT:
			return TypeEtatDestinataire.INSCRIT;
		case INSCRIT_SUSPENDU:
			return TypeEtatDestinataire.INSCRIT_SUSPENDU;
		default:
			throw new IllegalArgumentException("Le statut du destinataire suivant n'est pas reconnu " + status);
		}
	}
}
