package ch.vd.uniregctb.mouvement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum EtatMouvementDossier {

	A_TRAITER("à traiter"),
	A_ENVOYER("à envoyer"),
	RETIRE("retiré"),
	TRAITE("traité"),
	RECU_BORDEREAU("reçu");

	private final String description;

	private static final List<EtatMouvementDossier> etatsTraites;

	private static final List<EtatMouvementDossier> etatsEnInstance;

	static {
		final List<EtatMouvementDossier> traites = new ArrayList<EtatMouvementDossier>(values().length);
		final List<EtatMouvementDossier> enInstance = new ArrayList<EtatMouvementDossier>(values().length);
		for (EtatMouvementDossier etat : values()) {
			if (etat.isTraite()) {
				traites.add(etat);
			}
			if (etat.isEnInstance()) {
				enInstance.add(etat);
			}
		}
		etatsTraites = Collections.unmodifiableList(traites);
		etatsEnInstance = Collections.unmodifiableList(enInstance);
	}

	private EtatMouvementDossier(String description) {
		this.description = description;
	}

	public boolean isTraite() {
		return this == TRAITE || this == RECU_BORDEREAU;
	}

	public boolean isEnInstance() {
		return this == A_TRAITER || this == A_ENVOYER;
	}

	public String getDescription() {
		return description;
	}

	public static List<EtatMouvementDossier> getEtatsTraites() {
		return etatsTraites;
	}

	public static List<EtatMouvementDossier> getEtatsEnInstance() {
		return etatsEnInstance;
	}
}
