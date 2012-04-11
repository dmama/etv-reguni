package ch.vd.uniregctb.mouvement.view;

import java.util.List;

public class MouvementMasseImpressionBordereauxView {

	private List<BordereauListElementView> bordereaux;

	private boolean montreExpediteur;

	public List<BordereauListElementView> getBordereaux() {
		return bordereaux;
	}

	public void setBordereaux(List<BordereauListElementView> bordereaux) {
		this.bordereaux = bordereaux;
	}

	public boolean isMontreExpediteur() {
		return montreExpediteur;
	}

	public void setMontreExpediteur(boolean montreExpediteur) {
		this.montreExpediteur = montreExpediteur;
	}
}
