package ch.vd.unireg.evenement.civil.interne.divorce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.separation.SeparationOuDivorce;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Adapter pour le divorce.
 * 
 * @author Pavel BLANCO
 */
public class Divorce extends SeparationOuDivorce {

	protected static Logger LOGGER = LoggerFactory.getLogger(Divorce.class);

	protected Divorce(EvenementCivilRegPP evenement, EvenementCivilContext context, DivorceTranslationStrategy handler, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public Divorce(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
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
