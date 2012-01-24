package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Gère l'arrivée d'un individu dans les cas suivants:
 * <ul>
 * <li>déménagement d'une commune vaudoise à l'autre (intra-cantonal)</li>
 * <li>déménagement d'un canton à l'autre (inter-cantonal)</li>
 * <li>arrivée en Suisse</li>
 * </ul>
 */
public class ArriveeTranslationStrategy implements EvenementCivilTranslationStrategy, EvenementCivilEchTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilExterne event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		// arrivée principale ou secondaire ?
		if (isArriveePrincipale(event)) {
			return new ArriveePrincipale(event, context, options);
		}
		else {
			return new ArriveeSecondaire(event, context, options);
		}
	}

	private boolean isArriveePrincipale(EvenementCivilExterne event) {
		switch (event.getType()) {
			case ARRIVEE_DANS_COMMUNE:
			case ARRIVEE_PRINCIPALE_HC:
			case ARRIVEE_PRINCIPALE_HS:
			case ARRIVEE_PRINCIPALE_VAUDOISE:
			case DEMENAGEMENT_DANS_COMMUNE:
				return true;

			case ARRIVEE_SECONDAIRE:
				return false;

			default:
				throw new IllegalArgumentException("Type d'arrivée non supporté : " + event.getType());
		}
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		// arrivée principale ou secondaire ?
		if (isArriveePrincipale(event, context)) {
			return new ArriveePrincipale(event, context, options);
		}
		else {
			return new ArriveeSecondaire(event, context, options);
		}
	}

	private boolean isArriveePrincipale(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {

		try {
			final AdressesCiviles adressesCiviles = context.getAdresseService().getAdressesCiviles(event.getNumeroIndividu(), event.getDateEvenement(), false);

			// si on trouve une adresse principale qui commence le jour de l'événement, alors c'est une arrivée principale
			// sinon, si on trouve une adresse secondaire qui commence ce jour-là, c'est une arrivée secondaire
			// sinon... on n'en sait rien... et boom !

			if (adressesCiviles.principale != null && RegDateHelper.equals(adressesCiviles.principale.getDateDebut(), event.getDateEvenement())) {
				return true;
			}
			else if (adressesCiviles.secondaire != null && RegDateHelper.equals(adressesCiviles.secondaire.getDateDebut(), event.getDateEvenement())) {
				return false;
			}
			else {
				throw new EvenementCivilException("Impossible de déterminer si arrivée principale ou secondaire");
			}
		}
		catch (AdresseException e) {
			throw new EvenementCivilException("Erreur lors de la récupération des adresses civiles", e);
		}
	}
}
