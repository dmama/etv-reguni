package ch.vd.unireg.interfaces.efacture.data;

import ch.vd.evd0025.v1.PayerStatus;

public enum TypeEtatDestinataire {
	NON_INSCRIT("Non inscrit"),
	NON_INSCRIT_SUSPENDU("Non inscrit suspendu"),
	INSCRIT("Inscrit"),
	INSCRIT_SUSPENDU("Inscrit suspendu"),
	DESINSCRIT("Désinscrit"),
	DESINSCRIT_SUSPENDU("Désinscrit suspendu");

	private final String description;

	TypeEtatDestinataire(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean isActivable() {
		return isSuspendu();
	}

	public boolean isSuspendu() {
		return this == INSCRIT_SUSPENDU || this == DESINSCRIT_SUSPENDU || this == NON_INSCRIT_SUSPENDU;
	}

	public boolean isSuspendable() {
		return this == INSCRIT || this == DESINSCRIT || this == NON_INSCRIT;
	}

	public boolean isInscrit() {
		return this == INSCRIT || this == INSCRIT_SUSPENDU;
	}

	public TypeEtatDestinataire avecSuspension() {
		switch (this) {
		case NON_INSCRIT:
			return NON_INSCRIT_SUSPENDU;
		case DESINSCRIT:
			return DESINSCRIT_SUSPENDU;
		case INSCRIT:
			return INSCRIT_SUSPENDU;
		default:
			if (!isSuspendu()) {
				throw new IllegalArgumentException();
			}
			return this;
		}
	}

	public TypeEtatDestinataire avecActivation() {
		switch (this) {
		case NON_INSCRIT_SUSPENDU:
			return NON_INSCRIT;
		case DESINSCRIT_SUSPENDU:
			return DESINSCRIT;
		case INSCRIT_SUSPENDU:
			return INSCRIT;
		default:
			if (isSuspendu()) {
				throw new IllegalArgumentException();
			}
			return this;
		}
	}

	public TypeEtatDestinataire avecInscription() {
		switch (this) {
		case NON_INSCRIT:
		case DESINSCRIT:
			return INSCRIT;
		case NON_INSCRIT_SUSPENDU:
		case DESINSCRIT_SUSPENDU:
			return INSCRIT_SUSPENDU;
		default:
			if (!isInscrit()) {
				throw new IllegalArgumentException();
			}
			return this;
		}
	}

	public TypeEtatDestinataire avecDesinscription() {
		switch (this) {
		case INSCRIT:
			return DESINSCRIT;
		case INSCRIT_SUSPENDU:
			return DESINSCRIT_SUSPENDU;
		default:
			if (isInscrit()) {
				throw new IllegalArgumentException();
			}
			return this;
		}
	}

	public static TypeEtatDestinataire valueOf(PayerStatus status) {
		switch (status) {
		case NON_INSCRIT:
			return TypeEtatDestinataire.NON_INSCRIT;
		case NON_INSCRIT_SUSPENDU:
			return TypeEtatDestinataire.NON_INSCRIT_SUSPENDU;
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
