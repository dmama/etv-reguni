package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;

public class ImportRFCriteriaView {

	private boolean aTraiter = true;
	private boolean enTraitement = true;
	private boolean traite = true;
	private boolean enErreur = true;
	private boolean force = true;

	public boolean isaTraiter() {
		return aTraiter;
	}

	public void setaTraiter(boolean aTraiter) {
		this.aTraiter = aTraiter;
	}

	public boolean isEnTraitement() {
		return enTraitement;
	}

	public void setEnTraitement(boolean enTraitement) {
		this.enTraitement = enTraitement;
	}

	public boolean isTraite() {
		return traite;
	}

	public void setTraite(boolean traite) {
		this.traite = traite;
	}

	public boolean isEnErreur() {
		return enErreur;
	}

	public void setEnErreur(boolean enErreur) {
		this.enErreur = enErreur;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public List<EtatEvenementRF> buildEtats() {
		List<EtatEvenementRF> etats = new ArrayList<>(4);
		if (aTraiter) {
			etats.add(EtatEvenementRF.A_TRAITER);
		}
		if (enTraitement) {
			etats.add(EtatEvenementRF.EN_TRAITEMENT);
		}
		if (traite) {
			etats.add(EtatEvenementRF.TRAITE);
		}
		if (enErreur) {
			etats.add(EtatEvenementRF.EN_ERREUR);
		}
		if (force) {
			etats.add(EtatEvenementRF.FORCE);
		}
		return etats;
	}
}
