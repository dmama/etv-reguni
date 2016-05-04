package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import java.util.ArrayList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.RangeUtil;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class DemenagementSiegeStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemenagementSiegeStrategy.class);

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est pertinente.
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
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		Domicile communeDeSiegeAvant = organisation.getSiegePrincipal(dateAvant);
		final Domicile communeDeSiegeApres = organisation.getSiegePrincipal(dateApres);


		if (communeDeSiegeApres == null) {
			return new TraitementManuel(event, organisation, null, context, options,
			                            String.format(
					                            "Autorité fiscale (siège) introuvable l'organisation n°%s %s. On est peut-être en présence d'un déménagement vers l'étranger.",
					                            organisation.getNumeroOrganisation(), organisation.getNom(dateApres))
			);
		}

		if (communeDeSiegeAvant == null) {
			if (isExisting(organisation, dateApres)) {
				return new TraitementManuel(event, organisation, null, context, options,
				                            String.format(
						                            "Autorité fiscale (siège) introuvable l'organisation n°%s %s. On est peut-être en présence d'un déménagement depuis l'étranger.",
						                            organisation.getNumeroOrganisation(), organisation.getNom(dateApres))
				);
			} else {
				// On doit comparer avec ce que l'on a dans Unireg, car l'organisation vient seulement d'être connue au civil.
				Audit.info("Entreprise connue d'Unireg mais nouvellement connue au civil. Utilisation des données fiscales d'Unireg.");

				final DateRanged<Etablissement> etablissementPrincipalRange = RangeUtil.getAssertLast(context.getTiersService().getEtablissementsPrincipauxEntreprise(entreprise), dateAvant);
				final Set<DomicileEtablissement> domiciles = etablissementPrincipalRange.getPayload().getDomiciles();
				final DateRange domicilePrincipal = RangeUtil.getAssertLast(new ArrayList<DateRange>(domiciles), dateApres);

				Integer noOfsAutoriteFiscale = ((DomicileEtablissement) domicilePrincipal).getNumeroOfsAutoriteFiscale();
				TypeAutoriteFiscale typeAutoriteFiscale = ((DomicileEtablissement) domicilePrincipal).getTypeAutoriteFiscale();

				communeDeSiegeAvant = new Domicile(domicilePrincipal.getDateDebut(), domicilePrincipal.getDateFin(), typeAutoriteFiscale, noOfsAutoriteFiscale);
			}
		}

		// Passé ce point on a forcément un déménagement

		if (communeDeSiegeAvant.getNoOfs() == communeDeSiegeApres.getNoOfs()) { // Pas un changement, pas de traitement
			LOGGER.info("Pas de changement d'autorité politique. La commune d'autorité fiscale reste no {}", communeDeSiegeAvant.getNoOfs());
			return null;
		}
		else if (isDemenagementVD(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Déménagement VD -> VD: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			return new DemenagementVD(event, organisation, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else if (isDemenagementHC(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Déménagement HC -> HC: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			return new DemenagementHC(event, organisation, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else if (isDepart(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Départ VD -> HC: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			return new DemenagementDepart(event, organisation, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else if (isArrivee(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Arrivée HC -> VD: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			return new DemenagementArrivee(event, organisation, entreprise, context, options, communeDeSiegeAvant, communeDeSiegeApres);
		}
		else {
			throw new EvenementOrganisationException(
					String.format("Il existe manifestement un type de siège qu'Unireg ne sait pas traiter. Type avant: %s. Type après: %s. Impossible de continuer.",
					              communeDeSiegeAvant.getTypeAutoriteFiscale(), communeDeSiegeApres.getTypeAutoriteFiscale()));
		}
	}

	private boolean isDemenagementVD(Domicile siegeAvant, Domicile siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	private boolean isDemenagementHC(Domicile siegeAvant, Domicile siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC;
	}

	private boolean isDepart(Domicile siegeAvant, Domicile siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				(siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS);
	}

	private boolean isArrivee(Domicile siegeAvant, Domicile siegeApres) {
		return (siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}
}
