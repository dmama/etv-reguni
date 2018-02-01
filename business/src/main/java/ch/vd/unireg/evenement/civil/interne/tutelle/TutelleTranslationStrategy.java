package ch.vd.unireg.evenement.civil.interne.tutelle;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

public class TutelleTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		throw new EvenementCivilException("Tous les événements civil de tutelles (mesure, levée, annulation, ...) ne peuvent plus être traités avec RcPers.");
	}

}
