package ch.vd.uniregctb.evenement.civil.interne.divorce;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.separation.SeparationOuDivorce;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour le divorce.
 * 
 * @author Pavel BLANCO
 */
public class Divorce extends SeparationOuDivorce {

	protected static Logger LOGGER = Logger.getLogger(Divorce.class);

	protected Divorce(EvenementCivilExterne evenement, EvenementCivilContext context, DivorceTranslationStrategy handler, EvenementCivilOptions options) throws EvenementCivilInterneException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Divorce(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.DIVORCE, date, numeroOfsCommuneAnnonce, conjoint, context);
	}
}
