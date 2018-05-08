package ch.vd.unireg.acces.parUtilisateur.view;

import ch.vd.unireg.general.view.UtilisateurView;
import ch.vd.unireg.tiers.view.TiersCriteriaView;

public class UtilisateurListPersonneView extends TiersCriteriaView {

	private static final long serialVersionUID = -6250274145751850365L;

	private String visaOperateur;
	private UtilisateurView utilisateurView;

	public String getVisaOperateur() {
		return visaOperateur;
	}

	public void setVisaOperateur(String visaOperateur) {
		this.visaOperateur = visaOperateur;
	}

	public UtilisateurView getUtilisateurView() {
		return utilisateurView;
	}

	public void setUtilisateurView(UtilisateurView utilisateurView) {
		this.utilisateurView = utilisateurView;
	}

}
