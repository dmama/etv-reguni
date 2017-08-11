package ch.vd.uniregctb.regimefiscal.rattrapage;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.document.RattrapageRegimesFiscauxRapport;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalConsolide;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalService;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalServiceException;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

/**
 * Job de rattrapage des régimes fiscaux pour les entreprises qui n'en ont pas de non annulé. (SIFISC-23901)
 */
public class RattrapageRegimesFiscauxJob extends JobDefinition {

	public static final Logger LOGGER = LoggerFactory.getLogger(RattrapageRegimesFiscauxJob.class);

	public static final String NAME = "RattrapageRegimesFiscauxJob";
	public static final String SIMULATION = "SIMULATION";

	public static final Set<TypeEtatEntreprise> TYPES_ETAT_ENTREPRISE_RADIEE_DISSOUTE = EnumSet.of(TypeEtatEntreprise.RADIEE_RC,
	                                                                                               TypeEtatEntreprise.DISSOUTE,
	                                                                                               TypeEtatEntreprise.ABSORBEE);

	private PlatformTransactionManager transactionManager;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private RegimeFiscalService regimeFiscalService;
	private RapportService rapportService;
	private ParametreAppService parametreAppService;

	public RattrapageRegimesFiscauxJob(int sortOrder, String description) {
		super(NAME, JobCategory.TIERS, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setMandatory(true);
			param.setName(SIMULATION);
			param.setDescription("Mode simulation");
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, true);
		}
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		this.regimeFiscalService = regimeFiscalService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final boolean simulation = getBooleanValue(params, SIMULATION);

		final RattrapageRegimesFiscauxJobResults results = new RattrapageRegimesFiscauxJobResults(simulation);
		final List<Long> tiersSansRegime = getTiersSansRegime();

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		for (final Long tiersId : tiersSansRegime) {

			// On traite chaque tiers dans une transaction séparée, pour éviter qu'un problème sur un tiers ne vienne interrompre le traitement.
			final RattrapageRegimesFiscauxJobResults resultat = template.execute(new TransactionCallback<RattrapageRegimesFiscauxJobResults>() {
				@Override
				public RattrapageRegimesFiscauxJobResults doInTransaction(TransactionStatus status) {
					if (simulation) {
						status.setRollbackOnly();
					}
					try {
						return traiteTiers(tiersId, simulation);
					}
					catch (Exception e) {
						status.setRollbackOnly();
						final RattrapageRegimesFiscauxJobResults resultatTiers = new RattrapageRegimesFiscauxJobResults(simulation);
						resultatTiers.addErrorException(tiersId, e);
						return resultatTiers;
					}
				}
			});
			if (resultat != null) {
				results.addAll(resultat);
			}
			if (getStatusManager().isInterrupted()) {
				break;
			}
		}

		final RattrapageRegimesFiscauxRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success(String.format("%s des annonces IDE sur les tiers sous contrôle ACI terminé%s.", simulation ? "Simulation de l'envoi" : "Envoi", simulation ? "e" : ""), rapport);
	}


	private RattrapageRegimesFiscauxJobResults traiteTiers(Long tiersId, boolean simulation) throws RegimeFiscalServiceException {
		final Tiers tiers = tiersDAO.get(tiersId);
		final RattrapageRegimesFiscauxJobResults resultatEntreprise = new RattrapageRegimesFiscauxJobResults(simulation);
		if (tiers instanceof Entreprise) {
			final Entreprise entreprise = (Entreprise) tiers;

			final String derniereRaisonSociale = tiersService.getDerniereRaisonSociale(entreprise);

			final List<FormeLegaleHisto> formesLegales = tiersService.getFormesLegales(entreprise, false);
			final Set<FormeLegale> typesDeFormes = formesLegales.stream()
					.map(FormeLegaleHisto::getFormeLegale)
					.collect(Collectors.toSet());

			final List<RegimeFiscalConsolide> regimesFiscauxPrealables = regimeFiscalService.getRegimesFiscauxVDNonAnnulesTrie(entreprise);
			final List<String> problemes = new ArrayList<>();

			// Si on a déjà un régime valide sur l'entreprise, on décline poliment.
			if (regimesFiscauxPrealables.size() > 0) {
				final String libellesTypesRegimes = regimesFiscauxPrealables.stream().map(RegimeFiscalConsolide::getLibelle).collect(Collectors.joining(", "));
				problemes.add(String.format("Est déjà rattachée à au moins un régime fiscal de type(s): [%s]", libellesTypesRegimes));
			}

			// Dans le cas où la forme juridique aurait évolué avec le temps, on renonce.
			if (typesDeFormes.isEmpty()) {
				problemes.add("Formes juridiques introuvable");
			}
			else if (typesDeFormes.size() > 1) {
				final String listeFormesLegales = StringUtils.join(typesDeFormes.stream().map(FormeLegale::getLibelle).collect(Collectors.toSet()), ", ");
				problemes.add(String.format("A possédé plus d'une forme juridique: [%s]", listeFormesLegales));
			}

			// Déterminer la date à laquelle faire débuter le régime fiscal
			final RegDate dateCreationEntreprise = tiersService.getDateCreation(entreprise);
			if (dateCreationEntreprise == null) {
				problemes.add("Date de création de l'entreprise impossible à déterminer");
			}

			// Rapporter les problèmes trouvés et abandonner pour cette entreprise.
			if (problemes.size() > 0) {
				final String format = "Pas de traitement automatique pour l'entreprise n°%s: %s.";
				final String message = String.format(format, FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), String.join("; ", problemes));
				LOGGER.info(message);
				throw new RattrapageRegimesFiscauxJobException(message);
			}

			// Déterminer le type de régime fiscal à appliquer
			final TypeRegimeFiscal typeRegimeFiscal;
			final FormeLegale formeLegale = typesDeFormes.iterator().next();
			if (formeLegale == FormeLegale.N_0104_SOCIETE_EN_COMMANDITE || formeLegale == FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF) {
				typeRegimeFiscal = regimeFiscalService.getTypeRegimeFiscalSocieteDePersonnes();
			}
			else {
				typeRegimeFiscal = regimeFiscalService.getTypeRegimeFiscalIndetermine();
			}

			final RegDate assujEpoch = RegDate.get(parametreAppService.getPremierePeriodeFiscalePersonnesMorales(), 1, 1);
			final RegDate dateDeDebut = RegDateHelper.maximum(dateCreationEntreprise, assujEpoch, NullDateBehavior.EARLIEST);

			final EtatEntreprise etatActuel = entreprise.getEtatActuel();
			if (etatActuel != null) {
				final TypeEtatEntreprise typeEtatActuel = etatActuel.getType();
				final RegDate dateEtatActuel = etatActuel.getDateObtention();

				if (TYPES_ETAT_ENTREPRISE_RADIEE_DISSOUTE.contains(typeEtatActuel) && dateEtatActuel.isBefore(assujEpoch)) {
					// Si l'entreprise est terminée de longue date, on ne crée pas de régime.
					final String message = String.format("Pas de création de régime fiscal l'entreprise n°%s [%s], disparue en date du %s",
					                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), formeLegale.getLibelle(), RegDateHelper.dateToDisplayString(dateEtatActuel));
					LOGGER.info(message);
					throw new RattrapageRegimesFiscauxJobException(message);
				}
			}

			// Appliquer le type de régime.
			LOGGER.info(String.format("Création des régimes fiscaux VD et CH pour l'entreprise n°%s [%s], à partir du %s, type: %s.",
			                          FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), formeLegale.getLibelle(), RegDateHelper.dateToDisplayString(dateDeDebut), typeRegimeFiscal.getLibelleAvecCode()));
			tiersDAO.addAndSave(entreprise, new RegimeFiscal(dateDeDebut, null, RegimeFiscal.Portee.VD, typeRegimeFiscal.getCode()));
			tiersDAO.addAndSave(entreprise, new RegimeFiscal(dateDeDebut, null, RegimeFiscal.Portee.CH, typeRegimeFiscal.getCode()));

			final List<RegimeFiscalConsolide> regimesFiscauxVDNonAnnulesTrie = regimeFiscalService.getRegimesFiscauxVDNonAnnulesTrie(entreprise);
			resultatEntreprise.addRegimeFiscalInfo(entreprise.getNumero(), derniereRaisonSociale, formeLegale, dateCreationEntreprise, regimesFiscauxVDNonAnnulesTrie);
		}
		else {
			throw new IllegalArgumentException(String.format("Le tiers n°%s n'est pas une entreprise.", tiersId));
		}
		return resultatEntreprise;
	}


	/**
	 * Recherche les tiers qui n'ont pas au moins un régime fiscal
	 * @return la liste triée par ordre croissant des numéros de tiers concernés. Vide si aucun.
	 */
	@NotNull
	private List<Long> getTiersSansRegime() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		return template.execute(status -> tiersDAO.getEntreprisesSansRegimeFiscal());
	}

}
