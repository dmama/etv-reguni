package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Evénement interne de création d'entreprises dont le siège principal est hors VD
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseHorsVD extends EvenementOrganisationInterneDeTraitement {

	private RegDate dateDeCreation;
	private final boolean isCreation;

	private final SiteOrganisation sitePrincipal;
	private final List<SiteOrganisation> succursalesRCVD;
	private final Domicile autoriteFiscalePrincipale;

	protected CreateEntrepriseHorsVD(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                 EvenementOrganisationContext context,
	                                 EvenementOrganisationOptions options,
	                                 boolean isCreation,
	                                 List<SiteOrganisation> succursalesRCVD) {
		super(evenement, organisation, entreprise, context, options);

		this.isCreation = isCreation;
		this.succursalesRCVD = succursalesRCVD;

		sitePrincipal = organisation.getSitePrincipal(getDateEvt()).getPayload();

		autoriteFiscalePrincipale = sitePrincipal.getDomicile(getDateEvt());
	}

	@Override
	public String describe() {
		return "Création d'une entreprise hors VD";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		final String messageWarning = "Une vérification est requise pour une nouvelle entreprise de catégorie « %s » dont le siège est hors canton avec présence sur VD.";

		// Déterminer la date de création

		if (succursalesRCVD.size() > 1) { // En réalité, on ne supporte qu'un seul établissement VD à la création, car c'est le scénario qui doit se produire à l'exclusion de tout autre.
			throw new EvenementOrganisationException(String.format("L'organisation %s hors canton (%s) n'est pas encore connu d'Unireg, mais a déjà plus d'une succursale au RC VD: %s. " +
					                                                       "Comme un événement n'en apporte qu'une nouvelle à la fois, un problème de données ou d'appariement est à craindre. Veuiller traiter à la main.",
			                                                       getOrganisation().getNumeroOrganisation(),
			                                                       getDescriptionSiteOrganisation(getOrganisation().getSitePrincipal(getDateEvt()).getPayload()),
			                                                       getDescriptionSitesOrganisation(succursalesRCVD)
			                                                       ));
		}

		final SiteOrganisation succursaleACreer = succursalesRCVD.get(0);
		final RegDate dateDeCreation = succursaleACreer.getDateInscriptionRCVd(getDateEvt());

		if (dateDeCreation == null) {
			throw new EvenementOrganisationException(String.format("Date d'inscription au RC VD introuvable pour la succursale au RC VD n°%s.",
			                                                       succursaleACreer.getNumeroSite()
			));
		}

		// Création & vérification de la surcharge corrective s'il y a lieu
		SurchargeCorrectiveRange surchargeCorrectiveRange = null;
		if (dateDeCreation.isBefore(getDateEvt())) {
			surchargeCorrectiveRange = new SurchargeCorrectiveRange(dateDeCreation, getDateEvt().getOneDayBefore());
			if (!surchargeCorrectiveRange.isAcceptable()) {
				throw new EvenementOrganisationException(
						String.format("Refus de créer dans Unireg une entreprise HC avec une date de création remontant à %s, %d jours avant la date de l'événement. La tolérance étant de %d jours. " +
								              "Il y a probablement une erreur d'identification, une erreur dans l'établissement VD retenu %s ou un problème de date.",
						              RegDateHelper.dateToDisplayString(dateDeCreation),
						              surchargeCorrectiveRange.getEtendue(),
						              OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC,
						              getDescriptionSiteOrganisation(succursaleACreer))
				);
			}
		}

		// Création de l'entreprise
		createEntreprise(dateDeCreation, suivis);

		// Création de l'établissement principal
		createAddEtablissement(sitePrincipal.getNumeroSite(), autoriteFiscalePrincipale, true, dateDeCreation, suivis);

		// Application de la surcharge corrective sur l'entreprise, si besoin
		if (dateDeCreation.isBefore(getDateEvt())) {
			appliqueDonneesCivilesSurPeriode(getEntreprise(), surchargeCorrectiveRange, getDateEvt(), warnings, suivis);
		}

		// Création de l'établissements secondaire (On ne prend que la succursale puisqu'on veut éviter les établissements REE)
		final Etablissement etablissementSecondaire = addEtablissementSecondaire(succursaleACreer, dateDeCreation, warnings, suivis);

		// Application de la surcharge corrective sur la succursale, si besoin
		if (dateDeCreation.isBefore(getDateEvt())) {
			appliqueDonneesCivilesSurPeriode(etablissementSecondaire, surchargeCorrectiveRange, getDateEvt(), warnings, suivis);
		}

		openRegimesFiscauxParDefautCHVD(getEntreprise(), getOrganisation(), dateDeCreation, suivis);

		final CategorieEntreprise categorieEntreprise = getContext().getTiersService().getCategorieEntreprise(getEntreprise(), getDateEvt());
		final boolean isSocieteDePersonnes = categorieEntreprise == CategorieEntreprise.SP;

		openForFiscalPrincipal(dateDeCreation,
		                       autoriteFiscalePrincipale,
		                       MotifRattachement.DOMICILE,
		                       null,
		                       isSocieteDePersonnes ? GenreImpot.REVENU_FORTUNE : GenreImpot.BENEFICE_CAPITAL,
		                       warnings, suivis);

		if (isSocieteDePersonnes) {
			warnings.addWarning(String.format("Nouvelle société de personnes, date de début à contrôler%s.", getOrganisation().isInscriteAuRC(getDateEvt()) ? " (Publication FOSC)" : ""));
		}
		else {
			// Réglages exercice commercial
			createAddBouclement(dateDeCreation, isCreation, suivis);

			if (!isCreation) {
				regleDateDebutPremierExerciceCommercial(getEntreprise(), dateDeCreation, suivis);
			}
		}

		// Ajoute les for secondaires
		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), warnings, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		if (getOrganisation().isSocieteIndividuelle(getDateEvt()) || getOrganisation().isSocieteSimple(getDateEvt())) {
			throw new EvenementOrganisationException(String.format("Genre d'entreprise non supportée!: %s", getOrganisation().getFormeLegale(getDateEvt()).getLibelle()));
		}

		if (succursalesRCVD.size() == 0) {
			erreurs.addErreur("Aucune succursale RC VD trouvée! Refus de créer l'entreprise hors canton.");
		}
	}

	private Commune getCommuneDomicile(SiteOrganisation site) {
		final Domicile domicile = site.getDomicile(getDateEvt());
		if (domicile != null) {
			return getContext().getServiceInfra().getCommuneByNumeroOfs(domicile.getNumeroOfsAutoriteFiscale(), getDateEvt());
		}
		return null;
	}

	private String getDescriptionSitesOrganisation(List<SiteOrganisation> sites) throws EvenementOrganisationException {
		StringBuilder sb = new StringBuilder();
		for (SiteOrganisation site : sites) {
			sb.append("[");
			sb.append(getDescriptionSiteOrganisation(site));
			sb.append("]");
		}
		return sb.toString();
	}

	private String getDescriptionSiteOrganisation(SiteOrganisation site) throws EvenementOrganisationException {
		String descriptionCommune = "(inconnue)";
		final Commune communeDomicile = getCommuneDomicile(site);
		if (communeDomicile != null) {
			descriptionCommune = communeDomicile.getNomOfficielAvecCanton();
		}
		final RegDate dateInscriptionRCVd = site.getDateInscriptionRCVd(getDateEvt());
		if (dateInscriptionRCVd == null) {
			throw new EvenementOrganisationException(String.format("Date d'inscription au RC VD introuvable pour la succursale au RC VD n°%s à %s",
			                                                       site.getNumeroSite(),
			                                                       descriptionCommune
			));
		}
		return String.format("%s (civil: n°%s) à %s inscription RC VD le %s", site.getNom(getDateEvt()), site.getNumeroSite(), descriptionCommune, RegDateHelper.dateToDisplayString(dateInscriptionRCVd));
	}
}
