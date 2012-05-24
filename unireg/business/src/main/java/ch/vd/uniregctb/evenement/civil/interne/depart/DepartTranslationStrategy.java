package ch.vd.uniregctb.evenement.civil.interne.depart;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Gère le départ d'un individu dans les cas suivants: <ul> <li>DEPART_SECONDAIRE : déménagement d'une commune vaudoise à l'autre (intra-cantonal) pour l'adresse secondaire</li> <li>DEPART_COMMUNE :
 * déménagement d'un canton à l'autre (inter-cantonal) ou Départ de Suisse</li> </ul>
 */
public class DepartTranslationStrategy implements EvenementCivilTranslationStrategy, EvenementCivilEchTranslationStrategy {

	@Override
	public EvenementCivilInterne create(EvenementCivilRegPP event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		final EvenementCivilInterne interne;
		switch (event.getType()) {
		case DEPART_COMMUNE:
			interne = new DepartPrincipal(event, context, options);
			break;
		case DEPART_SECONDAIRE:
			interne = new DepartSecondaire(event, context, options);
			break;
		default:
			throw new IllegalArgumentException("Type d'événement non supporté par la stratégie : " + event.getType());
		}
		return interne;
	}


	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		//Départ de la commune ou secondaire
		// si on trouve une adresse principale qui se termine le jour de l'événement, alors c'est une départ principal (de la commune)
		// sinon, si on trouve une adresse secondaire qui se termine ce jour-là, c'est un départ secondaire
		// sinon... on n'en sait rien... et boom !
		if (isDepartPrincipal(event, context)) {
			return new DepartPrincipal(event, context, options);
		}
		else {
			return new DepartSecondaire(event, context, options);
		}
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		return false;
	}

	private boolean isDepartPrincipal(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		try {
			final AdressesCiviles adressesCiviles = context.getAdresseService().getAdressesCiviles(event.getNumeroIndividu(), event.getDateEvenement(), false);

			// si on trouve une adresse principale qui fini le jour de l'événement, alors c'est un départ principal
			// sinon, si on trouve une adresse secondaire qui fini ce jour, c'est une arrivée secondaire
			// sinon... on n'en sait rien... et boom !

			if (adressesCiviles.principale != null && adressesCiviles.principale.getDateFin() == event.getDateEvenement()) {
				return true;
			}
			else if (adressesCiviles.secondaire != null && adressesCiviles.secondaire.getDateFin() == event.getDateEvenement()) {
				return false;
			}
			else {
				throw new EvenementCivilException("Aucune adresse principale ou secondaire ne se termine à la date de l'événement.");
			}
		}
		catch (AdresseException e) {
			throw new EvenementCivilException("Erreur lors de la récupération des adresses civiles", e);
		}
	}
}
