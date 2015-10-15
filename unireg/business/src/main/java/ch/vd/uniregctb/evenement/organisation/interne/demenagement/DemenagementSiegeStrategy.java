package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class DemenagementSiegeStrategy extends AbstractOrganisationStrategy {

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est pertinente.
	 *
	 * Spécifications:
	 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise,
	                                                   EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) throws EvenementOrganisationException {

		// On vérifie qu'on a bien retrouvé l'entreprise concernée par ce type de changement
		// TODO: Retrouver aussi les entreprises n'ayant pas d'id cantonal.
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final Siege communeDeSiegeAvant = organisation.getSiegePrincipal(dateAvant);
		final Siege communeDeSiegeApres = organisation.getSiegePrincipal(dateApres);

		if (communeDeSiegeAvant == null || communeDeSiegeApres == null) { // Sièges hors Suisse
			throw new EvenementOrganisationException("Les changements de siège impliquant un/des sièges hors Suisse ne sont pas pris en charge. Veuillez traiter manuellement.");
		}
		else if (communeDeSiegeAvant.getNoOfs() == communeDeSiegeApres.getNoOfs()) { // Pas un changement, pas de traitement
			return null;
		}
		else if (isDemenagementVD(communeDeSiegeAvant, communeDeSiegeApres)) {
			return new DemenagementVD(event, organisation, entreprise, context, options);
		}
		else if (isDemenagementHC(communeDeSiegeAvant, communeDeSiegeApres)) {
			return new DemenagementHC(event, organisation, entreprise, context, options);
		}
		else if (isDepart(communeDeSiegeAvant, communeDeSiegeApres)) {
			return new DemenagementDepart(event, organisation, entreprise, context, options);
		}
		else if (isArrivee(communeDeSiegeAvant, communeDeSiegeApres)) {
			return new DemenagementArrivee(event, organisation, entreprise, context, options);
		}
		else {
			throw new EvenementOrganisationException("Il existe un type de siège dont Unireg n'a pas pleinement connaissance. Impossible de continuer.");
		}
	}

	private boolean isDemenagementVD(Siege siegeAvant, Siege siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	private boolean isDemenagementHC(Siege siegeAvant, Siege siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC;
	}

	private boolean isDepart(Siege siegeAvant, Siege siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				(siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS);
	}

	private boolean isArrivee(Siege siegeAvant, Siege siegeApres) {
		return (siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}
}
