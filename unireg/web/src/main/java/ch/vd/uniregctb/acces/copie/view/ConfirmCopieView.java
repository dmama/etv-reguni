package ch.vd.uniregctb.acces.copie.view;

import java.util.List;

import ch.vd.uniregctb.acces.parUtilisateur.view.DroitAccesUtilisateurView;
import ch.vd.uniregctb.general.view.UtilisateurView;

public class ConfirmCopieView {

	private UtilisateurView utilisateurReferenceView;
	private UtilisateurView utilisateurDestinationView;
	private List<DroitAccesUtilisateurView> droitsAccesView;
	public UtilisateurView getUtilisateurReferenceView() {
		return utilisateurReferenceView;
	}
	public void setUtilisateurReferenceView(UtilisateurView utilisateurReferenceView) {
		this.utilisateurReferenceView = utilisateurReferenceView;
	}
	public UtilisateurView getUtilisateurDestinationView() {
		return utilisateurDestinationView;
	}
	public void setUtilisateurDestinationView(UtilisateurView utilisateurDestinationView) {
		this.utilisateurDestinationView = utilisateurDestinationView;
	}
	public List<DroitAccesUtilisateurView> getDroitsAccesView() {
		return droitsAccesView;
	}
	public void setDroitsAccesView(List<DroitAccesUtilisateurView> droitsAccesView) {
		this.droitsAccesView = droitsAccesView;
	}

}
