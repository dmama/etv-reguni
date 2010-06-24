package ch.vd.uniregctb.mouvement.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.vd.uniregctb.mouvement.EtatMouvementDossier;

/**
 * Vue pour la recherche de mouvements de dossiers en masse dans laquelle
 * les états de mouvements peuvent être utilisés à plusieurs (choix multiples)
 */
public class MouvementMasseCriteriaMutiplesEtatsView extends MouvementMasseCriteriaView {

	private boolean wantTous = false;
	private boolean wantATraiter = false;
	private boolean wantAEnvoyer = false;
	private boolean wantRetire = false;
	private boolean wantTraite = false;
	private boolean wantRecu = false;

	public boolean isWantTous() {
		return wantTous;
	}

	public void setWantTous(boolean wantTous) {
		this.wantTous = wantTous;
	}

	public boolean isWantATraiter() {
		return wantATraiter;
	}

	public void setWantATraiter(boolean wantATraiter) {
		this.wantATraiter = wantATraiter;
	}

	public boolean isWantAEnvoyer() {
		return wantAEnvoyer;
	}

	public void setWantAEnvoyer(boolean wantAEnvoyer) {
		this.wantAEnvoyer = wantAEnvoyer;
	}

	public boolean isWantRetire() {
		return wantRetire;
	}

	public void setWantRetire(boolean wantRetire) {
		this.wantRetire = wantRetire;
	}

	public boolean isWantTraite() {
		return wantTraite;
	}

	public void setWantTraite(boolean wantTraite) {
		this.wantTraite = wantTraite;
	}

	public boolean isWantRecu() {
		return wantRecu;
	}

	public void setWantRecu(boolean wantRecu) {
		this.wantRecu = wantRecu;
	}

	@Override
	public Collection<EtatMouvementDossier> getEtatsRecherches() {
		if (wantTous) {
			return null;
		}
		else {
			final List<EtatMouvementDossier> etats = new ArrayList<EtatMouvementDossier>(EtatMouvementDossier.values().length);
			if (wantAEnvoyer) {
				etats.add(EtatMouvementDossier.A_ENVOYER);
			}
			if (wantRetire) {
				etats.add(EtatMouvementDossier.RETIRE);
			}
			if (wantATraiter) {
				etats.add(EtatMouvementDossier.A_TRAITER);
			}
			if (wantRecu) {
				etats.add(EtatMouvementDossier.RECU_BORDEREAU);
			}
			if (wantTraite) {
				etats.add(EtatMouvementDossier.TRAITE);
			}
			return etats;
		}
	}

	@Override
	public void init(boolean montreInitiateur) {
		super.init(montreInitiateur);
		wantTous = getDefaultWantTous();
		wantATraiter = getDefaultWantATraiter();
		wantAEnvoyer = getDefaultWantAEnvoyer();
		wantRetire = getDefaultWantRetire();
		wantRecu = getDefaultWantRecu();
	}

	protected boolean getDefaultWantATraiter() {
		return false;
	}

	protected boolean getDefaultWantAEnvoyer() {
		return false;
	}

	protected boolean getDefaultWantRetire() {
		return false;
	}

	protected boolean getDefaultWantRecu() {
		return false;
	}

	protected boolean getDefaultWantTous() {
		return true;
	}
}
