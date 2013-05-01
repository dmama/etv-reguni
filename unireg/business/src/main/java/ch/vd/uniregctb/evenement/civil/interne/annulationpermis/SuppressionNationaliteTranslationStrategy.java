package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Règles métiers permettant de traiter les événements de suppression de nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public class SuppressionNationaliteTranslationStrategy extends AnnulationPermisOuNationaliteTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		final EvenementCivilInterne interne;
		switch (event.getType()) {
			case SUP_NATIONALITE_SUISSE:
				interne = new SuppressionNationaliteSuisse(event, context, options);
				break;
			case SUP_NATIONALITE_NON_SUISSE:
				interne = new SuppressionNationaliteNonSuisse(event, context, options);
				break;
		    default:
			    throw new IllegalArgumentException("Type d'événement non supporté par la stratégie : " + event.getType());
		}
		return interne;
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		Assert.isEqual(ActionEvenementCivilEch.ANNULATION, event.getAction());

		final EvenementCivilInterne interne;
		switch (event.getType()) {
			case NATURALISATION:
				interne = new SuppressionNationaliteSuisse(event, context, options);
				break;
			case CHGT_NATIONALITE_ETRANGERE:
				interne = new SuppressionNationaliteNonSuisse(event, context, options);
				break;
			default:
				throw new IllegalArgumentException("Type d'événement non supporté par la stratégie : " + event.getType());
		}
		return interne;
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		return event.getType() == TypeEvenementCivilEch.CHGT_NATIONALITE_ETRANGERE;
	}
}
