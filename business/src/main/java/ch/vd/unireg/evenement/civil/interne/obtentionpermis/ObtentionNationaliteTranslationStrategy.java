package ch.vd.unireg.evenement.civil.interne.obtentionpermis;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.type.TypeEvenementCivilEch;

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
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
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
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) {
		return event.getType() == TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE;
	}
}
