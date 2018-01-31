package ch.vd.uniregctb.mouvement.view;

import java.util.List;

public class BordereauEnvoiListView {

	private List<BordereauEnvoiView> bordereaux;

	private boolean montreDestinataire = true;

	public List<BordereauEnvoiView> getBordereaux() {
		return bordereaux;
	}

	public void setBordereaux(List<BordereauEnvoiView> bordereaux) {
		this.bordereaux = bordereaux;
	}

	public boolean isMontreDestinataire() {
		return montreDestinataire;
	}

	public void setMontreDestinataire(boolean montreDestinataire) {
		this.montreDestinataire = montreDestinataire;
	}
}
