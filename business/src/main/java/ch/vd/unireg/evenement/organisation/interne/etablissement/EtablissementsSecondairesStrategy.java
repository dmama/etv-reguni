package ch.vd.unireg.evenement.organisation.interne.etablissement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.evenement.organisation.interne.MessageWarningPreExectution;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;

/**
 * Modification des établissements secondaires
 *
 * @author Raphaël Marmier, 2016-02-26.
 */
public class EtablissementsSecondairesStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(EtablissementsSecondairesStrategy.class);

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public EtablissementsSecondairesStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, final Organisation organisation, Entreprise entreprise) throws EvenementOrganisationException {

		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();


		final List<Etablissement> etablissementsAFermer = new ArrayList<>();
		final List<SiteOrganisation> sitesACreer = new ArrayList<>();

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange == null) {
			Audit.info("Organisation nouvelle au civil mais déjà connue d'Unireg. Des établissements secondaires ont peut-être changé.");
			// FIXME: Cela pose la question de savoir si on ne devrait pas utiliser Unireg comme "avant" dans ces cas là?
			return new MessageWarningPreExectution(event, organisation, null, context, options,
			                                       String.format("L'organisation n°%d est déjà connue d'Unireg, mais nouvelle au civil. Veuillez vérifier la transition entre les données du registre " +
					                                             "fiscal et du registre civil, notamment les établissements secondaires.", organisation.getNumeroOrganisation()));
		} else {

			List<SiteOrganisation> sitesVDAvant = uniquementSitesActifs(organisation.getSitesSecondaires(dateAvant), dateAvant);
			List<SiteOrganisation> sitesVDApres = uniquementSitesActifs(organisation.getSitesSecondaires(dateApres), dateApres);

			try {
				determineChangementsEtablissements(sitesVDAvant, sitesVDApres, etablissementsAFermer, sitesACreer, context);
			} catch (EvenementOrganisationException e) {
				return new TraitementManuel(event, organisation, entreprise, context, options,
				                            String.format("Erreur lors de la determination des changements d'établissements secondaires de l'entreprise n°%s (civil: %d): %s",
				                                          FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), organisation.getNumeroOrganisation(), e.getMessage())
				);
			}

			try {
				List<EtablissementsSecondaires.Demenagement> demenagements = determineChangementsDomiciles(sitesVDAvant, sitesVDApres, dateApres, context);

				if (!etablissementsAFermer.isEmpty() || !sitesACreer.isEmpty() || !demenagements.isEmpty()) {
					LOGGER.info(String.format("Modification des établissements secondaires de l'entreprise %s (civil: %d).",
					                          FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), organisation.getNumeroOrganisation()));
					return new EtablissementsSecondaires(event, organisation, entreprise, context, options, etablissementsAFermer, sitesACreer, demenagements);
				}
			} catch (EvenementOrganisationException e) {
				return new TraitementManuel(event, organisation, entreprise, context, options,
				                            String.format("Erreur lors de la determination des changements de domicile des établissements secondaires de l'entreprise n°%s (civil: %d): %s",
				                                          FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), organisation.getNumeroOrganisation(), e.getMessage())
				);
			}
		}

		LOGGER.info("Pas de modification des établissements secondaires");
		return null;
	}

	private List<SiteOrganisation> uniquementSitesActifs(List<SiteOrganisation> sitesSecondaires, RegDate date) {
		List<SiteOrganisation> filtre = new ArrayList<>(sitesSecondaires.size());
		for (SiteOrganisation site : sitesSecondaires) {
			Domicile domicile = site.getDomicile(date);
			if (domicile != null
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
					break;
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
					if (etablissement != null && !Objects.equals(domicileAvant.getNumeroOfsAutoriteFiscale(), domicileApres.getNumeroOfsAutoriteFiscale())) {
						demenagements.add(new EtablissementsSecondaires.Demenagement(etablissement, presentSite, domicileAvant, domicileApres, date));
					}
				}
			}
		}
		return demenagements;
	}
}
