package ch.vd.unireg.acces.parUtilisateur.view;

import ch.vd.unireg.general.view.UtilisateurView;
import ch.vd.unireg.tiers.view.TiersCriteriaView;

public class UtilisateurListPersonneView extends TiersCriteriaView {

	private static final long serialVersionUID = 6799591065529941434L;

	private long noIndividuOperateur;
	private UtilisateurView utilisateurView;

	public long getNoIndividuOperateur() {
		return noIndividuOperateur;
	}

	public void setNoIndividuOperateur(long noIndividuOperateur) {
		this.noIndividuOperateur = noIndividuOperateur;
	}

	public UtilisateurView getUtilisateurView() {
		return utilisateurView;
	}

	public void setUtilisateurView(UtilisateurView utilisateurView) {
		this.utilisateurView = utilisateurView;
	}

}
