package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import java.util.List;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchContext;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;

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
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilEchContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return isNationaliteSuisse(event, context)
				? new SuppressionNationaliteSuisse(event, context, options)
				: new SuppressionNationaliteNonSuisse(event, context, options);
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilEchContext context) throws EvenementCivilException {
		return !isNationaliteSuisse(event, context);
	}

	private static boolean isNationaliteSuisse(EvenementCivilEch event, EvenementCivilEchContext context) throws EvenementCivilException {
		final Individu individuAvantAnnulation = getIndividuAvant(event, context);
		if (hasNationaliteSuisse(individuAvantAnnulation.getNationalites())) {
			final Individu individuApresAnnulation = getIndividuFromEvent(event.getId(), context);
		    return !hasNationaliteSuisse(individuApresAnnulation.getNationalites());
		}
		else {
			// la nationalité suisse n'était pas présente dans les nationalités avant annulation, donc
			// cette annulation ne peut pas concerner la nationalité suisse...
			return false;
		}
	}
	
	private static boolean hasNationaliteSuisse(List<Nationalite> nationalites) {
		boolean suisseTrouvee = false;
		if (nationalites != null && nationalites.size() > 0) {
			for (Nationalite nationalite : nationalites) {
				suisseTrouvee = nationalite.getPays().isSuisse();
				if (suisseTrouvee) {
					break;
				}
			}
		}
		return suisseTrouvee;
	}
}
