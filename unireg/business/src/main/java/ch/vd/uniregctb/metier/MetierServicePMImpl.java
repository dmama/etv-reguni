package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
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
					List<DomicileHisto> histoPourCommune = tousLesDomicilesVD.get(domicile.getNoOfs());
					if (histoPourCommune == null) {
						histoPourCommune = new ArrayList<>();
						tousLesDomicilesVD.put(domicile.getNoOfs(), histoPourCommune);
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
}
