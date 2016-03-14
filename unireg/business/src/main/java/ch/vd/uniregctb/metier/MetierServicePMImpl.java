package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
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
			final List<DomicileHisto> domiciles = tiersService.getDomiciles(etablissement.getPayload());
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


}
