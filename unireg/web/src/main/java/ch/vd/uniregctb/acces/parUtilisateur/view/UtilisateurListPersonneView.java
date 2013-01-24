package ch.vd.uniregctb.acces.parUtilisateur.view;

import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class UtilisateurListPersonneView extends TiersCriteriaView{

	/**
	 *
	 */
	private static final long serialVersionUID = 452698227505576337L;

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
