package ch.vd.uniregctb.evenement.changement.nationalite;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

public class RemiseBlancDateFinNationaliteAdapter extends GenericEvenementAdapter implements RemiseBlancDateFinNationalite {

	protected RemiseBlancDateFinNationaliteAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}
}
