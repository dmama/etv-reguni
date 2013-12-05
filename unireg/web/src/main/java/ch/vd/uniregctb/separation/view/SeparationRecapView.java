package ch.vd.uniregctb.separation.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractRecapView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.type.EtatCivil;

public class SeparationRecapView extends AbstractRecapView {

	private RegDate dateSeparation;

	private EtatCivil etatCivil;
	
	private TiersGeneralView couple;

	private String remarque;
	
	public RegDate getDateSeparation() {
		return dateSeparation;
	}

	public void setDateSeparation(RegDate dateSeparation) {
		this.dateSeparation = dateSeparation;
	}

	public TiersGeneralView getCouple() {
		return couple;
	}

	public void setCouple(TiersGeneralView couple) {
		this.couple = couple;
	}

	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public void setEtatCivil(EtatCivil etatCivil) {
		this.etatCivil = etatCivil;
	}

	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}



}
