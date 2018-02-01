package ch.vd.uniregctb.acces.parDossier.view;

import java.util.List;

import ch.vd.uniregctb.general.view.TiersGeneralView;

public class DossierEditRestrictionView {

	private TiersGeneralView dossier;

	private List<DroitAccesView> restrictions;

	public TiersGeneralView getDossier() {
		return dossier;
	}

	public void setDossier(TiersGeneralView dossier) {
		this.dossier = dossier;
	}

	public List<DroitAccesView> getRestrictions() {
		return restrictions;
	}

	public void setRestrictions(List<DroitAccesView> restrictions) {
		this.restrictions = restrictions;
	}

}
