package ch.vd.uniregctb.evenement.civil.engine.regpp;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.ignore.EvenementCivilIgnore;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Classe de base des handlers d'événements civils qui ne nous intéressent pas (complètement ignorés)
 */
public abstract class EvenementCivilIgnoreTranslationStrategy implements EvenementCivilTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new EvenementCivilIgnore(event, context, options);
	}
}
