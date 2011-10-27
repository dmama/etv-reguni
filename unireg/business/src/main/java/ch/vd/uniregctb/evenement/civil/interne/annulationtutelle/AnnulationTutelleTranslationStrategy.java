package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.tutelle.AbstractTutelleTranslationStrategy;

/**
 * Traitements métier des événements d'annulation de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationTutelleTranslationStrategy extends AbstractTutelleTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationTutelle(event, context, options);
	}

}
