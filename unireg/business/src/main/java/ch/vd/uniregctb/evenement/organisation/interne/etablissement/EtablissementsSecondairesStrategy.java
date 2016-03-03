package ch.vd.uniregctb.evenement.organisation.interne.etablissement;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;

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

		List<SiteOrganisation> sitesAvant = organisation.getSitesSecondaires(dateAvant);
		List<SiteOrganisation> sitesApres = organisation.getSitesSecondaires(dateApres);

		determineChangementsEtablissements(sitesAvant, sitesApres, etablissementsAFermer, sitesACreer, context);

		List<EtablissementsSecondaires.Demenagement> demenagements = determineChangementsDomiciles(sitesAvant, sitesApres, dateApres, context);

		if (!etablissementsAFermer.isEmpty() || !sitesACreer.isEmpty() || !demenagements.isEmpty()) {
			LOGGER.info(String.format("Modification des établissements secondaires de l'entreprise %s (civil: %s).", entreprise.getNumero(), organisation.getNumeroOrganisation()));
			return new EtablissementsSecondaires(event, organisation, entreprise, context, options, etablissementsAFermer, sitesACreer, demenagements);
		}

		LOGGER.info("Pas de modification des établissements secondaires");
		return null;
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
					nouveau = false;
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
				if (ancienSite.getNumeroSite() == presentSite.getNumeroSite()) {
					final Domicile domicileAvant = ancienSite.getDomicile(date.getOneDayBefore());
					final Domicile domicileApres = presentSite.getDomicile(date);

					Etablissement etablissement = context.getTiersDAO().getEtablissementByNumeroSite(ancienSite.getNumeroSite());
					if (etablissement != null) {
						final List<DomicileEtablissement> sortedDomiciles = etablissement.getSortedDomiciles(false);

						DomicileEtablissement domicileEtablissement = null;
						for (DomicileEtablissement de : sortedDomiciles) {
							if (de.getNumeroOfsAutoriteFiscale() == domicileAvant.getNoOfs()) {
								domicileEtablissement = de;
								break;
							}
						}

						/* Determiner les déménagements pour ce Site/Etablissement. On le fait en comparant les données
						   civiles, mais seulement pour les sites déjà connus d'Unireg.
						 */
						if (domicileEtablissement != null && domicileAvant.getNoOfs() != domicileApres.getNoOfs()) {
							demenagements.add(new EtablissementsSecondaires.Demenagement(etablissement, domicileEtablissement, domicileApres, date));
						}
					}
				}
			}
		}
		return demenagements;
	}

}
