package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

public class RegimeFiscalListEditView extends RegimeFiscalView {

	private boolean last;

	public RegimeFiscalListEditView(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, TypeRegimeFiscal type) {
		super(id, annule, dateDebut, dateFin, type);
	}

	public boolean isLast() {
		return last;
	}

	public void setLast(boolean last) {
		this.last = last;
	}
}
