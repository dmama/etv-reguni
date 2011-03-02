package ch.vd.uniregctb.evenement.civil.interne.divorce;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.separation.SeparationOuDivorceAdapter;

/**
 * Adapter pour le divorce.
 * 
 * @author Pavel BLANCO
 */
public class DivorceAdapter extends SeparationOuDivorceAdapter implements Divorce {

	protected static Logger LOGGER = Logger.getLogger(DivorceAdapter.class);

	protected DivorceAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, DivorceHandler handler) throws EvenementCivilInterneException {
		super(evenement, context, handler);
	}
}
