package ch.vd.unireg.evenement.civil.engine.regpp;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.ignore.EvenementCivilIgnore;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Classe de base des handlers d'événements civils qui ne nous intéressent pas (complètement ignorés)
 */
public abstract class EvenementCivilIgnoreTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new EvenementCivilIgnore(event, context, options);
	}
}
