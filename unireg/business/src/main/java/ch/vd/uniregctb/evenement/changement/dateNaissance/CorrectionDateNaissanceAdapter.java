package ch.vd.uniregctb.evenement.changement.dateNaissance;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

public class CorrectionDateNaissanceAdapter extends GenericEvenementAdapter implements CorrectionDateNaissance {

	protected CorrectionDateNaissanceAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	public RegDate getDateNaissance() {
		return this.getDate();
	}

}
