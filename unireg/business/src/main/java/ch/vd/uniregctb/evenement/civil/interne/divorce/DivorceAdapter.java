package ch.vd.uniregctb.evenement.civil.interne.divorce;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.separation.SeparationOuDivorceAdapter;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour le divorce.
 * 
 * @author Pavel BLANCO
 */
public class DivorceAdapter extends SeparationOuDivorceAdapter {

	protected static Logger LOGGER = Logger.getLogger(DivorceAdapter.class);

	protected DivorceAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, DivorceHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected DivorceAdapter(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.DIVORCE, date, numeroOfsCommuneAnnonce, conjoint, context);
	}
}
