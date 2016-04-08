package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tache.TacheService;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.ForsParTypeAt;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;
import ch.vd.uniregctb.validation.ValidationInterceptor;
import ch.vd.uniregctb.validation.ValidationService;

public class MetierServicePMImpl implements MetierServicePM {

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private ServiceInfrastructureService serviceInfra;
	private ServiceOrganisationService serviceOrganisationService;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private RemarqueDAO remarqueDAO;
	private ValidationService validationService;
	private ValidationInterceptor validationInterceptor;
	private EFactureService eFactureService;
	private ParametreAppService parametreAppService;
	private EvenementFiscalService evenementFiscalService;
	private TacheService tacheService;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setInfrastructureService(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setServiceOrganisationService(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setRemarqueDAO(RemarqueDAO remarqueDAO) {
		this.remarqueDAO = remarqueDAO;
	}

	/**
	 * @param adresseService the adresseService to set
	 */
	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
		this.validationInterceptor = validationInterceptor;
	}

	public void seteFactureService(EFactureService eFactureService) {
		this.eFactureService = eFactureService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	/**
	 * Méthode qui parcoure les établissements de l'entreprise et qui ajuste les fors secondaires en prenant soin d'éviter
	 * les chevauchements. Elle crée les fors nécessaires, ferme ceux qui se terminent et annule ceux qui sont devenus redondants.
	 *
	 * La méthode gère les fors secondaires uniquement sur VD
	 *
	 * On peut passer une date de coupure qui "tranche" le début d'historique des fors secondaire à créer. Sert à faire démarrer le for secondaire d'une nouvelle entreprise à j + 1 comme
	 * le for principal. Laisser vide dans les cas non création.
	 *
	 * @param entreprise l'entreprise concernée
	 * @param dateAuPlusTot la date de coupure pour la création d'entreprise.
	 */
	@Override
	public AjustementForsSecondairesResult calculAjustementForsSecondairesPourEtablissementsVD(Entreprise entreprise, RegDate dateAuPlusTot) throws MetierServiceException {

		List<DateRanged<Etablissement>> etablissements = tiersService.getEtablissementsSecondairesEntreprise(entreprise);

		// Les domiciles VD classés par commune
		final Map<Integer, List<DomicileHisto>> tousLesDomicilesVD = new HashMap<>();
		for (DateRanged<Etablissement> etablissement : etablissements) {
			final List<DomicileHisto> domiciles = tiersService.getDomiciles(etablissement.getPayload(), false);
			if (domiciles != null && !domiciles.isEmpty()) {
				for (DomicileHisto domicile : domiciles) {
					if (domicile.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						continue; // On ne crée des fors secondaires que pour VD
					}
					List<DomicileHisto> histoPourCommune = tousLesDomicilesVD.get(domicile.getNumeroOfsAutoriteFiscale());
					if (histoPourCommune == null) {
						histoPourCommune = new ArrayList<>();
						tousLesDomicilesVD.put(domicile.getNumeroOfsAutoriteFiscale(), histoPourCommune);
					}
					histoPourCommune.add(domicile);
				}
			}
		}

		// Charger les historiques de fors secondaire établissement stables existant pour chaque commune
		final Map<Integer, List<ForFiscalSecondaire>> tousLesForsFiscauxSecondairesParCommune =
				entreprise.getForsFiscauxSecondairesActifsSortedMapped(MotifRattachement.ETABLISSEMENT_STABLE);

		return AjustementForsSecondairesHelper.getResultatAjustementForsSecondaires(tousLesDomicilesVD, tousLesForsFiscauxSecondairesParCommune, dateAuPlusTot);
	}

	@Override
	public RattachementOrganisationResult rattacheOrganisationEntreprise(Organisation organisation, Entreprise entreprise, RegDate date) throws MetierServiceException {

		if (organisation.getSitePrincipal(date) == null) {
			throw new MetierServiceException(String.format("L'organisation %d n'a pas de site principal à la date demandée %s.", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(date)));
		}

		if (entreprise.getNumeroEntreprise() != null && entreprise.getNumeroEntreprise() != organisation.getNumeroOrganisation()) {
			throw new MetierServiceException(String.format("L'entreprise %d est déjà rattachée à une autre organisation!", entreprise.getNumero()));
		}

		// Rapprochement de l'entreprise
		entreprise.setNumeroEntreprise(organisation.getNumeroOrganisation());

		final RaisonSocialeFiscaleEntreprise raisonSociale = getAssertLast(entreprise.getRaisonsSocialesNonAnnuleesTriees(), date);
		if (raisonSociale != null && raisonSociale.getDateFin() == null) {
			tiersService.closeRaisonSocialeFiscale(raisonSociale, date.getOneDayBefore());
		}

		final FormeJuridiqueFiscaleEntreprise formeJuridique = getAssertLast(entreprise.getFormesJuridiquesNonAnnuleesTriees(), date);
		if (formeJuridique != null && formeJuridique.getDateFin() == null) {
			tiersService.closeFormeJuridiqueFiscale(formeJuridique, date.getOneDayBefore());
		}

		final CapitalFiscalEntreprise capital = getAssertLast(entreprise.getCapitauxNonAnnulesTries(), date);
		if (capital != null && capital.getDateFin() == null) {
			tiersService.closeCapitalFiscal(capital, date.getOneDayBefore());
		}

		RattachementOrganisationResult result = new RattachementOrganisationResult(entreprise);

		// Rapprochement de l'établissement principal
		SiteOrganisation site = organisation.getSitePrincipal(date).getPayload();
		final List<DateRanged<Etablissement>> etablissementsPrincipauxEntreprise = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
		if (etablissementsPrincipauxEntreprise.isEmpty() || CollectionsUtils.getLastElement(etablissementsPrincipauxEntreprise) == null) {
			throw new MetierServiceException(String.format("L'entreprise %s ne possède pas d'établissement principal!", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
		}
		DateRanged<Etablissement> etablissementPrincipalRange = CollectionsUtils.getLastElement(etablissementsPrincipauxEntreprise);
		if (etablissementPrincipalRange.getDateDebut().isAfter(date)) {
			throw new MetierServiceException(String.format("L'établissement principal %d commence à une date postérieure à la tentative de rapprochement du %s. Impossible de continuer.", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(date)));
		}

		final Etablissement etablissementPrincipal = etablissementPrincipalRange.getPayload();

		final List<DomicileEtablissement> sortedDomiciles = etablissementPrincipal.getSortedDomiciles(false);
		if (sortedDomiciles.isEmpty()) {
			throw new MetierServiceException(String.format("L'établissement principal %d n'a pas de domicile en date du %s. Impossible de continuer le rapprochement.", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(date)));
		}
		final DomicileEtablissement domicile = getAssertLast(sortedDomiciles, date);

		if (domicile != null && domicile.getNumeroOfsAutoriteFiscale().equals(organisation.getSiegePrincipal(date).getNoOfs())) {
			etablissementPrincipal.setNumeroEtablissement(site.getNumeroSite());
			tiersService.closeDomicileEtablissement(domicile, date.getOneDayBefore());
			result.addEtablissementRattache(etablissementPrincipal);
		} else {
			throw new MetierServiceException(String.format("L'établissement principal %s n'a pas de domicile ou celui-ci ne correspond pas avec celui que rapporte le régistre civil.", FormatNumeroHelper.numeroCTBToDisplay(etablissementPrincipal.getNumero())));
		}

		// Traitement minimum des établissements secondaires (vérifier si déjà connu et ne pas ajouter aux listes)
		Map<Long, SiteOrganisation> matches = new HashMap<>();

		for (SiteOrganisation siteSecondaire : organisation.getSitesSecondaires(date)) {
			matches.put(siteSecondaire.getNumeroSite(), siteSecondaire);
		}

		for (Etablissement etablissementSecondaire : tiersService.getEtablissementsSecondairesEntreprise(entreprise, date)) {
			if (etablissementSecondaire.isConnuAuCivil()) {
				if (matches.get(etablissementSecondaire.getNumeroEtablissement()) != null) {
					matches.remove(etablissementSecondaire.getNumeroEtablissement());
					result.addEtablissementRattache(etablissementSecondaire);
				}
				else {
					throw new MetierServiceException(String.format("L'établissement secondaire %s est connu au civil mais son numéro ne correspond à aucun des établissements civil!", FormatNumeroHelper.numeroCTBToDisplay(etablissementSecondaire.getNumeroEtablissement())));
				}
			} else {
				result.addEtablissementNonRattache(etablissementSecondaire);
			}
		}

		for (Map.Entry<Long, SiteOrganisation> entry : matches.entrySet()) {
			result.addSiteNonRattache(entry.getValue());
		}

		// TODO: Vrai rapprochement des établissements secondaires

		return result;
	}

	private <T extends DateRange> T getAssertLast(List<T> entites, RegDate date) throws MetierServiceException {
		if (entites != null && !entites.isEmpty()) {
			T lastRange = CollectionsUtils.getLastElement(entites);
			if (lastRange == null) {
				throw new MetierServiceException(String.format("Erreur de données: null trouvé dans une collection de périodes %s", entites.getClass()));
			}
			if (lastRange.getDateDebut().isAfter(date)) {
				throw new MetierServiceException(String.format("La période valide à la date demandée %s n'est pas la dernière de l'historique!", RegDateHelper.dateToDisplayString(date)));
			}
			return lastRange;
		}
		return null;
	}

	@Override
	public void faillite(Entreprise entreprise, RegDate datePrononceFaillite, String remarqueAssociee) throws MetierServiceException {

		// pas de for principal ouvert -> pas possible d'aller plus loin
		final ForsParTypeAt forsParType = entreprise.getForsParTypeAt(null, false);
		if (forsParType.principal == null) {
			throw new MetierServiceException("Tous les fors fiscaux de l'entreprise sont déjà fermés.");
		}

		// 1. on ferme tous les rapports entre tiers ouvert (mandats et établissements secondaires)

		for (RapportEntreTiers ret : CollectionsUtils.merged(entreprise.getRapportsSujet(), entreprise.getRapportsObjet())) {
			if (!ret.isAnnule() && ret.getDateFin() == null) {
				final boolean aFermer = (ret instanceof ActiviteEconomique && !((ActiviteEconomique) ret).isPrincipal()) || (ret instanceof Mandat);
				if (aFermer) {
					ret.setDateFin(datePrononceFaillite);
				}
			}
		}

		// 2. on ferme les fors ouverts avec le motif FAILLITE

		// on traite d'abord les fors secondaires, puis seulement ensuite le for principal
		boolean hasImmeuble = false;
		for (ForFiscalSecondaire fs : forsParType.secondaires) {
			hasImmeuble |= fs.getMotifRattachement() == MotifRattachement.IMMEUBLE_PRIVE;
			tiersService.closeForFiscalSecondaire(entreprise, fs, datePrononceFaillite, MotifFor.FAILLITE);
		}
		tiersService.closeForFiscalPrincipal(forsParType.principal, datePrononceFaillite, MotifFor.FAILLITE);

		// 3. si for immeuble fermé, on crée une tâche de contrôle de dossier

		if (hasImmeuble) {
			tacheService.genereTacheControleDossier(entreprise);
		}

		// 4. nouvel état fiscal

		entreprise.addEtat(new EtatEntreprise(datePrononceFaillite, TypeEtatEntreprise.EN_FAILLITE, TypeGenerationEtatEntreprise.MANUELLE));

		// 5. publication de l'événement fiscal de faillite et appel aux créanciers

		evenementFiscalService.publierEvenementFiscalInformationComplementaire(entreprise,
		                                                                       EvenementFiscalInformationComplementaire.TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS,
		                                                                       datePrononceFaillite);

		// 6. éventuellement ajout d'une remarque sur l'entreprise

		if (StringUtils.isNotBlank(remarqueAssociee)) {
			final Remarque remarque = new Remarque();
			remarque.setTexte(StringUtils.abbreviate(remarqueAssociee, LengthConstants.TIERS_REMARQUE));
			remarque.setTiers(entreprise);
			remarqueDAO.save(remarque);
		}
	}

	@Override
	public void demenageSiege(Entreprise entreprise, RegDate dateDebutNouveauSiege, TypeAutoriteFiscale taf, int noOfs) throws MetierServiceException {

		// pas de for principal ouvert -> pas possible d'aller plus loin
		final ForsParTypeAt forsParType = entreprise.getForsParTypeAt(null, false);
		if (forsParType.principal == null) {
			throw new MetierServiceException("Tous les fors fiscaux de l'entreprise sont déjà fermés.");
		}

		// 1. on s'occupe de toute façon du for principal de l'entreprise

		final ForFiscalPrincipal forPrincipal = forsParType.principal;
		final TypeAutoriteFiscale tafCourant = forPrincipal.getTypeAutoriteFiscale();

		// le motif de changement
		final MotifFor motif;
		if (tafCourant == taf) {
			motif = MotifFor.DEMENAGEMENT_VD;
		}
		else if (taf == TypeAutoriteFiscale.PAYS_HS) {
			motif = MotifFor.DEPART_HS;
		}
		else if (tafCourant == TypeAutoriteFiscale.PAYS_HS) {
			motif = MotifFor.ARRIVEE_HS;
		}
		else if (taf == TypeAutoriteFiscale.COMMUNE_HC) {
			motif = MotifFor.DEPART_HC;
		}
		else {
			motif = MotifFor.ARRIVEE_HC;
		}
		tiersService.addForPrincipal(entreprise, dateDebutNouveauSiege, motif, null, null, forPrincipal.getMotifRattachement(), noOfs, taf, forPrincipal.getGenreImpot());

		// 2. si l'entreprise n'est pas liée au civil, il faut faire la même chose sur le domicile de l'établissement principal

		if (!entreprise.isConnueAuCivil()) {

			// récupération de l'établissement principal courant
			final List<DateRanged<Etablissement>> etbsPrincipaux = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
			if (etbsPrincipaux != null && !etbsPrincipaux.isEmpty()) {
				final DateRanged<Etablissement> etbPrincipalCourant = CollectionsUtils.getLastElement(etbsPrincipaux);
				if (etbPrincipalCourant == null) {
					// pas d'établissement principal ?? bizarre, non ?
					throw new MetierServiceException("Etablissement principal introuvable sur une entreprise inconnue du registre civil.");
				}
				else if (etbPrincipalCourant.getDateFin() != null) {
					throw new MetierServiceException("Le lien vers l'établissement principal est fermé.");
				}

				// il faut trouver le dernier domicile de cet établissement
				final Etablissement etbPrincipal = etbPrincipalCourant.getPayload();
				final List<DomicileEtablissement> domiciles = etbPrincipal.getSortedDomiciles(false);
				if (domiciles.isEmpty()) {
					throw new MetierServiceException(String.format("Aucun domicile connu sur l'établissement principal de l'entreprise (%s)", FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero())));
				}

				final DomicileEtablissement dernierDomicile = CollectionsUtils.getLastElement(domiciles);
				if (dernierDomicile.getDateFin() != null) {
					throw new MetierServiceException(String.format("Le dernier domicile connu sur l'établissement principal (%s) de l'entreprise est déjà fermé.",
					                                               FormatNumeroHelper.numeroCTBToDisplay(etbPrincipal.getNumero())));
				}

				tiersService.closeDomicileEtablissement(dernierDomicile, dateDebutNouveauSiege.getOneDayBefore());
				tiersService.addDomicileEtablissement(etbPrincipal, taf, noOfs, dateDebutNouveauSiege, null);
			}
			else {
				// pas d'établissement principal ?? bizarre, non ?
				throw new MetierServiceException("Etablissement principal introuvable sur une entreprise inconnue du registre civil.");
			}

		}
	}
}
