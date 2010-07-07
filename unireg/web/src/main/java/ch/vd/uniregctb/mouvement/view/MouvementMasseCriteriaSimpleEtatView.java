package ch.vd.uniregctb.mouvement.view;

import java.util.Arrays;
import java.util.Collection;

import ch.vd.uniregctb.mouvement.EtatMouvementDossier;

/**
 * Vue pour la recherche de mouvements de dossiers en masse dans laquelle
 * les états de mouvements sont utilisés un par un (= tous ou un seul identifié)
 */
public class MouvementMasseCriteriaSimpleEtatView extends MouvementMasseCriteriaView {

	/**
	 * Etat du mouvement
	 */
	private EtatMouvementDossier etatMouvement;

	public EtatMouvementDossier getEtatMouvement() {
		return etatMouvement;
	}

	public void setEtatMouvement(EtatMouvementDossier etatMouvement) {
		this.etatMouvement = etatMouvement;
	}

	@Override
	public void init(boolean montreInitiateur) {
		super.init(montreInitiateur);
		this.etatMouvement = null;
	}

	@Override
	public Collection<EtatMouvementDossier> getEtatsRecherches() {
		if (etatMouvement == null) {
			return null;
		}
		else if (etatMouvement == EtatMouvementDossier.TRAITE) {
			return EtatMouvementDossier.getEtatsTraites();
		}
		else {
			return Arrays.asList(etatMouvement);
		}
	}
}
