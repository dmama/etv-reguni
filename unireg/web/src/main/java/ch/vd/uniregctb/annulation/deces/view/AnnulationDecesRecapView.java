package ch.vd.uniregctb.annulation.deces.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.general.view.TiersGeneralView;

public class AnnulationDecesRecapView {

	private TiersGeneralView personne;

	private RegDate dateDeces;

	public TiersGeneralView getPersonne() {
		return personne;
	}

	public void setPersonne(TiersGeneralView personne) {
		this.personne = personne;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(RegDate dateDeces) {
		this.dateDeces = dateDeces;
	}

}
