package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.tutelle.AbstractTutelleTranslationStrategy;

/**
 * Traitements métier des événements d'annulation de levée de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationLeveeTutelleTranslationStrategy extends AbstractTutelleTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilException {
		return new AnnulationLeveeTutelle(event, context);
	}

}