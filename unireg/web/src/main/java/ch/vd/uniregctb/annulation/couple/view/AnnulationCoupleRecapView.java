package ch.vd.uniregctb.annulation.couple.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.general.view.TiersGeneralView;

public class AnnulationCoupleRecapView {

	private TiersGeneralView couple;

	private RegDate dateMenageCommun;

	public TiersGeneralView getCouple() {
		return couple;
	}

	public void setCouple(TiersGeneralView couple) {
		this.couple = couple;
	}

	public RegDate getDateMenageCommun() {
		return dateMenageCommun;
	}

	public void setDateMenageCommun(RegDate dateMenageCommun) {
		this.dateMenageCommun = dateMenageCommun;
	}

}
