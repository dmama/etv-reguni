package ch.vd.uniregctb.evenement.organisation.interne.etablissement;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Modification des établissement secondaires
 *
 * @author Raphaël Marmier, 2016-02-26.
 */
public class EtablissementsSecondairesStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtablissementsSecondairesStrategy.class);

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
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

		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();


		final List<Etablissement> etablissementsAFermer = new ArrayList<>();
		final List<SiteOrganisation> sitesACreer = new ArrayList<>();

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange == null) {
			LOGGER.info("Organisation nouvelle au civil mais déjà connue d'Unireg.");
			return null; // On n'existait pas hier, en fait.
		} else {

			List<SiteOrganisation> sitesVDAvant = uniquementSitesVDActifs(organisation.getSitesSecondaires(dateAvant), dateAvant);
			List<SiteOrganisation> sitesVDApres = uniquementSitesVDActifs(organisation.getSitesSecondaires(dateApres), dateApres);

			determineChangementsEtablissements(sitesVDAvant, sitesVDApres, etablissementsAFermer, sitesACreer, context);

			List<EtablissementsSecondaires.Demenagement> demenagements = determineChangementsDomiciles(sitesVDAvant, sitesVDApres, dateApres, context);

			if (!etablissementsAFermer.isEmpty() || !sitesACreer.isEmpty() || !demenagements.isEmpty()) {
				LOGGER.info(String.format("Modification des établissements secondaires de l'entreprise %s (civil: %s).", entreprise.getNumero(), organisation.getNumeroOrganisation()));
				return new EtablissementsSecondaires(event, organisation, entreprise, context, options, etablissementsAFermer, sitesACreer, demenagements);
			}
		}

		LOGGER.info("Pas de modification des établissements secondaires");
		return null;
	}

	private List<SiteOrganisation> uniquementSitesVDActifs(List<SiteOrganisation> sitesSecondaires, RegDate date) {
		List<SiteOrganisation> filtre = new ArrayList<>(sitesSecondaires.size());
		for (SiteOrganisation site : sitesSecondaires) {
			Domicile domicile = site.getDomicile(date);
			if (domicile != null
					&& domicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
					&& site.isActif(date)) {
				filtre.add(site);
			}
		}
		return filtre;
	}

	protected void determineChangementsEtablissements(List<SiteOrganisation> sitesAvant, List<SiteOrganisation> sitesApres, List<Etablissement> etablissementsAFermer, List<SiteOrganisation> siteACreer, EvenementOrganisationContext context) throws
			EvenementOrganisationException {

		// Determiner les sites qui ont disparus
		for (SiteOrganisation ancienSite : sitesAvant) {
			boolean disparu = true;
			for (SiteOrganisation presentSite : sitesApres) {
				if (ancienSite.getNumeroSite() == presentSite.getNumeroSite()) {
					disparu = false;
				}
			}
			if (disparu) {
				Etablissement etablissement = context.getTiersDAO().getEtablissementByNumeroSite(ancienSite.getNumeroSite());
				if (etablissement != null) {
					etablissementsAFermer.add(etablissement);
				}
			}
		}

		for (SiteOrganisation presentSite : sitesApres) {
			boolean nouveau = true;
			for (SiteOrganisation ancienSite : sitesAvant) {
				if (presentSite.getNumeroSite() == ancienSite.getNumeroSite()) {
					Etablissement etablissement = context.getTiersDAO().getEtablissementByNumeroSite(ancienSite.getNumeroSite());
					nouveau = etablissement == null;
					break;
				}
			}
			if (nouveau) {
				siteACreer.add(presentSite);
			}
		}
	}

	protected List<EtablissementsSecondaires.Demenagement> determineChangementsDomiciles(List<SiteOrganisation> sitesAvant, List<SiteOrganisation> sitesApres, RegDate date, EvenementOrganisationContext context) throws
			EvenementOrganisationException {

		final List<EtablissementsSecondaires.Demenagement> demenagements = new ArrayList<>();

		// Determiner les sites qui ont déménagé.
		for (SiteOrganisation ancienSite : sitesAvant) {
			for (SiteOrganisation presentSite : sitesApres) {
				if (ancienSite.getNumeroSite() == presentSite.getNumeroSite()) { // On ne retient que les cas ou l'établissement n'a pas changé.

					// On compare les domiciles tels que les voit RCEnt
					final Domicile domicileAvant = ancienSite.getDomicile(date.getOneDayBefore());
					final Domicile domicileApres = presentSite.getDomicile(date);

					// On considère qu'on est en présence d'un déménagement que si on connait déjà l'établissement dans Unireg.
					Etablissement etablissement = context.getTiersDAO().getEtablissementByNumeroSite(ancienSite.getNumeroSite());
					if (etablissement != null && domicileAvant.getNoOfs() != domicileApres.getNoOfs()) {
						demenagements.add(new EtablissementsSecondaires.Demenagement(etablissement, domicileAvant, domicileApres, date));
					}
				}
			}
		}
		return demenagements;
	}
}
