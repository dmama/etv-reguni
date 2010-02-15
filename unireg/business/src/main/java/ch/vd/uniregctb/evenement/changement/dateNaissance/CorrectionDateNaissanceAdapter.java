package ch.vd.uniregctb.evenement.changement.dateNaissance;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

public class CorrectionDateNaissanceAdapter extends GenericEvenementAdapter implements CorrectionDateNaissance {

	public RegDate getDateNaissance() {
		return this.getDate();
	}

}
