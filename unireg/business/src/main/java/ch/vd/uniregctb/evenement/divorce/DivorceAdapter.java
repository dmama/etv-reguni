package ch.vd.uniregctb.evenement.divorce;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.separation.SeparationOuDivorceAdapter;

/**
 * Adapter pour le divorce.
 * 
 * @author Pavel BLANCO
 */
public class DivorceAdapter extends SeparationOuDivorceAdapter implements Divorce {

	protected static Logger LOGGER = Logger.getLogger(DivorceAdapter.class);

	protected DivorceAdapter(EvenementCivilData evenement, EvenementCivilContext context, DivorceHandler handler) throws EvenementAdapterException {
		super(evenement, context, handler);
	}
}
