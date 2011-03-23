package ch.vd.uniregctb.annulation.separation.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.general.view.TiersGeneralView;

public class AnnulationSeparationRecapView {

	private TiersGeneralView premierePersonne;

	private TiersGeneralView secondePersonne;

	private RegDate dateSeparation;

	public TiersGeneralView getPremierePersonne() {
		return premierePersonne;
	}

	public void setPremierePersonne(TiersGeneralView premierePersonne) {
		this.premierePersonne = premierePersonne;
	}

	public TiersGeneralView getSecondePersonne() {
		return secondePersonne;
	}

	public void setSecondePersonne(TiersGeneralView secondePersonne) {
		this.secondePersonne = secondePersonne;
	}

	public RegDate getDateSeparation() {
		return dateSeparation;
	}

	public void setDateSeparation(RegDate dateSeparation) {
		this.dateSeparation = dateSeparation;
	}

}
