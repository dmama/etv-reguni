package ch.vd.uniregctb.mouvement.view;

import java.util.List;

import ch.vd.uniregctb.mouvement.EtatMouvementDossier;

public class BordereauEnvoiReceptionView extends BordereauEnvoiView {

	private List<MouvementDetailView> mvts;

	private long[] selection;

	public List<MouvementDetailView> getMvts() {
		return mvts;
	}

	public void setMvts(List<MouvementDetailView> mvts) {
		this.mvts = mvts;
	}

	public long[] getSelection() {
		return selection;
	}

	public void setSelection(long[] selection) {
		this.selection = selection;
	}

	public boolean isAuMoinsUnSelectionnable() {
		boolean auMoinsUn = false;
		for (MouvementDetailView mvt : mvts) {
			if (!mvt.isAnnule() && mvt.getEtatMouvement() == EtatMouvementDossier.TRAITE) {
				auMoinsUn = true;
				break;
			}
		}
		return auMoinsUn;
	}
}
