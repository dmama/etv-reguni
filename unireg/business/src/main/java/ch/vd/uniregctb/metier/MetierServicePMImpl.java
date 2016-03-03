package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
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
import ch.vd.uniregctb.type.MotifFor;
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
	 * Méthode "magique" qui parcoure les établissements de l'entreprise, extrait les domiciles et qui ajuste les fors secondaires en prenant soin d'éviter
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
	public MetierServicePM.ResultatAjustementForsSecondaires calculAjustementForsSecondairesPourEtablissementsVD(Entreprise entreprise, RegDate dateAuPlusTot) throws EvenementOrganisationException {
		final List<ForFiscalSecondaire> aAnnulerResultat = new ArrayList<>();
		final List<ForAFermer> aFermerResultat = new ArrayList<>();
		final List<ForFiscalSecondaire> aCreerResultat = new ArrayList<>();

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

		// On ne coupe rien qui existe déjà! Sécurité.
		if (dateAuPlusTot != null) {
			for (Map.Entry<Integer, List<ForFiscalSecondaire>> entry : tousLesForsFiscauxSecondairesParCommune.entrySet()) {
				final ForFiscalSecondaire existant = DateRangeHelper.rangeAt(entry.getValue(), dateAuPlusTot);
				if (existant != null) {
					throw new EvenementOrganisationException(String.format("Une date au plus tôt %s est précisée pour le recalcul des fors secondaires, indiquant qu'on est en mode création. Mais " +
							                                                       "au moins un for secondaire valide débutant antiérieurement a été trouvé sur la commune %s. " +
							                                                       "Début %s%s. Impossible de continuer. Veuillez signaler l'erreur.",
					                                                       RegDateHelper.dateToDisplayString(dateAuPlusTot),
					                                                       existant.getNumeroOfsAutoriteFiscale(),
					                                                       RegDateHelper.dateToDisplayString(existant.getDateDebut()),
					                                                       existant.getDateFin() != null ? " , fin " + RegDateHelper.dateToDisplayString(existant.getDateFin()) : ""
					));
				}
			}
		}

		List<ForFiscalSecondaire> aCreer = new ArrayList<>();

		/* Fusion des ranges qui se chevauchent pour obtenir la liste des ranges tels qu'on les veut, les candidats.
		   Ensuite de quoi on détermine ceux à annuler et ceux à créer pour la commune en cours.
		 */
		for (Map.Entry<Integer, List<DomicileHisto>> domicilesPourCommune : tousLesDomicilesVD.entrySet()) {
			if (!domicilesPourCommune.getValue().isEmpty()) {
				final Integer noOfsCommune = domicilesPourCommune.getKey();

				final List<DateRange> rangesCandidatsPourCommune = DateRangeHelper.merge(domicilesPourCommune.getValue());

				// Determiner les fors à annuler dans la base Unireg (ils sont devenus redondant)
				final List<ForFiscalSecondaire> forFiscalSecondaires = tousLesForsFiscauxSecondairesParCommune.get(noOfsCommune);
				if (forFiscalSecondaires != null) {
					for (ForFiscalSecondaire forExistant : forFiscalSecondaires) {
						// Rechercher dans les nouveaux projetés
						boolean aConserver = false;
						DateRange rangeCandidatAEnlever = null;
						for (DateRange rangeCandidat : rangesCandidatsPourCommune) {
							if (DateRangeHelper.equals(forExistant, rangeCandidat)) {
								aConserver = true;
							} else {
								// Cas du for à fermer
								if (forExistant.getDateDebut() == rangeCandidat.getDateDebut()
										&& forExistant.getDateFin() == null && rangeCandidat.getDateFin() != null) {
									aFermerResultat.add(new ForAFermer(forExistant, rangeCandidat.getDateFin()));
									rangeCandidatAEnlever = rangeCandidat;
									aConserver = true;
								}
								// Le for ne doit plus exister, annulation
								else {
									aConserver = false;
								}
							}
						}
						if (!aConserver) {
							aAnnulerResultat.add(forExistant);
						}
						if (rangeCandidatAEnlever != null) {
							rangesCandidatsPourCommune.remove(rangeCandidatAEnlever);
						}
					}
				}

				// Determiner les fors à créer dans la base Unireg
				for (DateRange rangesCandidat : rangesCandidatsPourCommune) {
					// Recherche dans les anciens fors, pour la commune en cours
					boolean existe = false;
					if (forFiscalSecondaires != null) {
						for (ForFiscalSecondaire forExistant : forFiscalSecondaires) {
							if (DateRangeHelper.equals(rangesCandidat, forExistant)) {
								existe = true;
							}
						}
					}
					if (!existe) {
						aCreer.add(new ForFiscalSecondaire(rangesCandidat.getDateDebut(), MotifFor.DEBUT_EXPLOITATION,
						                                   rangesCandidat.getDateFin(), rangesCandidat.getDateFin() != null ? MotifFor.FIN_EXPLOITATION : null,
						                                   noOfsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ETABLISSEMENT_STABLE));
					}
				}
			}
		}

		for (ForFiscalSecondaire forACreer : aCreer) {
			// Cas du for précédant entièrement la dateAuPlusTot
			if (dateAuPlusTot != null && forACreer.getDateFin() != null && dateAuPlusTot.isAfter(forACreer.getDateFin())) {
				continue;
			}
			// Cas de la dateAuPlusTot qui tombe au milieu d'un for en cours
			else if (dateAuPlusTot != null && forACreer.isValidAt(dateAuPlusTot)) {
				forACreer = new ForFiscalSecondaire(dateAuPlusTot, forACreer.getMotifOuverture(), forACreer.getDateFin(), forACreer.getMotifFermeture(),
				                                    forACreer.getNumeroOfsAutoriteFiscale(), forACreer.getTypeAutoriteFiscale(), forACreer.getMotifRattachement());
			}
			aCreerResultat.add(forACreer);
		}

		return new ResultatAjustementForsSecondairesImpl(aAnnulerResultat, aFermerResultat, aCreerResultat);
	}

	public static class ForAFermer {
		private final ForFiscalSecondaire forFiscal;
		private final RegDate dateFermeture;

		public ForAFermer(ForFiscalSecondaire forFiscal, RegDate dateFermeture) {
			this.forFiscal = forFiscal;
			this.dateFermeture = dateFermeture;
		}

		public ForFiscalSecondaire getForFiscal() {
			return forFiscal;
		}

		public RegDate getDateFermeture() {
			return dateFermeture;
		}
	}

	public class ResultatAjustementForsSecondairesImpl implements MetierServicePM.ResultatAjustementForsSecondaires {
		private final List<ForFiscalSecondaire> aAnnuler;
		private final List<ForAFermer> aFermer;
		private final List<ForFiscalSecondaire> aCreer;

		public ResultatAjustementForsSecondairesImpl(List<ForFiscalSecondaire> aAnnuler, List<ForAFermer> aFermer, List<ForFiscalSecondaire> aCreer) {
			this.aAnnuler = aAnnuler;
			this.aFermer = aFermer;
			this.aCreer = aCreer;
		}

		@Override
		public List<ForFiscalSecondaire> getAAnnuler() {
			return aAnnuler;
		}

		@Override
		public List<ForAFermer> getAFermer() {
			return aFermer;
		}

		@Override
		public List<ForFiscalSecondaire> getACreer() {
			return aCreer;
		}
	}
}
