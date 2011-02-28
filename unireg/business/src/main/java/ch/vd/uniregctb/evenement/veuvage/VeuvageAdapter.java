package ch.vd.uniregctb.evenement.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Adapter de l'événement de veuvage.
 * 
 * @author Pavel BLANCO
 *
 */
public class VeuvageAdapter extends GenericEvenementAdapter implements Veuvage {

	protected VeuvageAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	public RegDate getDateVeuvage() {
		return getDate();
	}

}
