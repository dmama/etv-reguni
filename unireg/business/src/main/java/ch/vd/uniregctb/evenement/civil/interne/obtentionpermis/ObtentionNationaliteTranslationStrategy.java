package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchContext;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Règles métiers permettant de traiter les événements de démangement vaudois.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public class ObtentionNationaliteTranslationStrategy extends ObtentionPermisCOuNationaliteSuisseTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		final EvenementCivilInterne interne;
		switch (event.getType()) {
			case NATIONALITE_SUISSE:
				interne = new ObtentionNationaliteSuisse(event, context, options);
				break;
			case NATIONALITE_NON_SUISSE:
				interne = new ObtentionNationaliteNonSuisse(event, context, options);
				break;
			default:
				throw new IllegalArgumentException("Type d'événement non supporté par la stratégie : " + event.getType());
		}
		return interne;
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilEchContext context, EvenementCivilOptions options) throws EvenementCivilException {
		final EvenementCivilInterne interne;
		switch (event.getType()) {
			case NATURALISATION:
				interne = new ObtentionNationaliteSuisse(event, context, options);
				break;
			case CHGT_NATIONALITE_ETRANGERE:
				interne = new ObtentionNationaliteNonSuisse(event, context, options);
				break;
			default:
				throw new IllegalArgumentException("Type d'événement non supporté par la stratégie : " + event.getType());
		}
		return interne;
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilEchContext context) {
		return event.getType() == TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE;
	}
}
