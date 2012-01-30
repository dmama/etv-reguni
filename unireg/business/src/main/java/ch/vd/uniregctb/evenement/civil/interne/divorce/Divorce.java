package ch.vd.uniregctb.evenement.civil.interne.divorce;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.separation.SeparationOuDivorce;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Individu;

/**
 * Adapter pour le divorce.
 * 
 * @author Pavel BLANCO
 */
public class Divorce extends SeparationOuDivorce {

	protected static Logger LOGGER = Logger.getLogger(Divorce.class);

	protected Divorce(EvenementCivilRegPP evenement, EvenementCivilContext context, DivorceTranslationStrategy handler, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public Divorce(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Divorce(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, conjoint, context);
	}
}
