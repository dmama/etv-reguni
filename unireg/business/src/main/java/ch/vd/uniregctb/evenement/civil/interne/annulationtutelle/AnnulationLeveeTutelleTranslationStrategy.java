package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.tutelle.AbstractTutelleTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Traitements métier des événements d'annulation de levée de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationLeveeTutelleTranslationStrategy extends AbstractTutelleTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new AnnulationLeveeTutelle(event, context, options);
	}

}