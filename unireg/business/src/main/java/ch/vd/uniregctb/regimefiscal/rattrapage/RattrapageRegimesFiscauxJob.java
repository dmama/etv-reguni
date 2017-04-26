package ch.vd.uniregctb.regimefiscal.rattrapage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.document.RattrapageRegimesFiscauxRapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalConsolide;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscal;
import ch.vd.uniregctb.regimefiscal.ServiceRegimeFiscalException;
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
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

/**
 * Job de rattrapage des régimes fiscaux pour les entreprises qui n'en ont pas de non annulé. (SIFISC-23901)
 */
public class RattrapageRegimesFiscauxJob extends JobDefinition {

	public static final Logger LOGGER = LoggerFactory.getLogger(RattrapageRegimesFiscauxJob.class);

	public static final String NAME = "RattrapageRegimesFiscaux";

	public static final String SIMULATION = "SIMULATION";
	private static final RegDate ASSUJ_EPOCH = RegDate.get(2009, 1, 1);
	public static final EnumSet<TypeEtatEntreprise> TYPES_ETAT_ENTREPRISE_RADIEE_DISSOUTE = EnumSet.of(TypeEtatEntreprise.RADIEE_RC, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.ABSORBEE);

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private Dialect dialect;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private ServiceRegimeFiscal serviceRegimeFiscal;
	private RapportService rapportService;


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

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setServiceRegimeFiscal(ServiceRegimeFiscal serviceRegimeFiscal) {
		this.serviceRegimeFiscal = serviceRegimeFiscal;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final boolean simulation = getBooleanValue(params, SIMULATION);

		List<Long> tiersSansRegime = new ArrayList<>(getTiersSansRegime());
		tiersSansRegime.sort(Comparator.naturalOrder());

		final RegDate aujourdhui = RegDate.get();

		final RattrapageRegimesFiscauxJobResults results = new RattrapageRegimesFiscauxJobResults(simulation);

		AuthenticationHelper.pushPrincipal(AuthenticationHelper.getCurrentPrincipal());
		try {
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
							return traiteTiers(tiersId, aujourdhui, simulation);
						}
						catch (Exception e) {
							// On doit faire le ménage si un problème est survenu pendant l'envoi afin d'éviter de croire qu'on a émis l'annonce alors que ce n'est pas le cas.
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
				if (getStatusManager().interrupted()) {
					break;
				}
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		final RattrapageRegimesFiscauxRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success(String.format("%s des annonces IDE sur les tiers sous contrôle ACI terminé%s.", simulation ? "Simulation de l'envoi" : "Envoi", simulation ? "e" : ""), rapport);
	}


	private RattrapageRegimesFiscauxJobResults traiteTiers(Long tiersId, RegDate date, boolean simulation) throws ServiceRegimeFiscalException {
		final Tiers tiers = tiersDAO.get(tiersId);
		final RattrapageRegimesFiscauxJobResults resultatEntreprise = new RattrapageRegimesFiscauxJobResults(simulation);
		if (tiers instanceof Entreprise) {
			final Entreprise entreprise = (Entreprise) tiers;

			final String derniereRaisonSociale = tiersService.getDerniereRaisonSociale(entreprise);

			final List<FormeLegaleHisto> formesLegales = tiersService.getFormesLegales(entreprise, false);
			final Set<FormeLegale> typesDeFormes = formesLegales.stream()
					.map(FormeLegaleHisto::getFormeLegale)
					.collect(Collectors.toSet());

			List<RegimeFiscalConsolide> regimesFiscauxPrealables = serviceRegimeFiscal.getRegimesFiscauxVDNonAnnulesTrie(entreprise);

			List<String> problemes = new ArrayList<>(2);

			// Si on a déjà un régime valide sur l'entreprise, on décline poliment.
			if (regimesFiscauxPrealables.size() > 0) {
				String libellesTypesRegimes = String.join(", ", regimesFiscauxPrealables.stream().map(RegimeFiscalConsolide::getLibelle).collect(Collectors.toSet()));
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
			TypeRegimeFiscal typeRegimeFiscal;
			final FormeLegale formeLegale = typesDeFormes.iterator().next();

			if (formeLegale == FormeLegale.N_0104_SOCIETE_EN_COMMANDITE || formeLegale == FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF) {
				typeRegimeFiscal = serviceRegimeFiscal.getTypeRegimeFiscalSocieteDePersonnes();
			}
			else {
				typeRegimeFiscal = serviceRegimeFiscal.getTypeRegimeFiscalIndetermine();
			}

			RegDate dateDeDebut;
			if (dateCreationEntreprise != null && dateCreationEntreprise.isAfterOrEqual(ASSUJ_EPOCH)) {
				dateDeDebut = dateCreationEntreprise;
			}
			else {
				dateDeDebut = ASSUJ_EPOCH;
			}

			final EtatEntreprise etatActuel = entreprise.getEtatActuel();
			if (etatActuel != null) {
				final TypeEtatEntreprise typeEtatActuel = etatActuel.getType();
				final RegDate dateEtatActuel = etatActuel.getDateObtention();

				if (TYPES_ETAT_ENTREPRISE_RADIEE_DISSOUTE.contains(typeEtatActuel) && dateEtatActuel.isBefore(ASSUJ_EPOCH)) {
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

			final List<RegimeFiscalConsolide> regimesFiscauxVDNonAnnulesTrie = serviceRegimeFiscal.getRegimesFiscauxVDNonAnnulesTrie(entreprise);
			resultatEntreprise.addRegimeFiscalInfo(entreprise.getNumero(), derniereRaisonSociale, formeLegale, dateCreationEntreprise, regimesFiscauxVDNonAnnulesTrie);
		}
		else {
			throw new IllegalArgumentException(String.format("Le tiers n°%s n'est pas une entreprise.", tiersId));
		}
		return resultatEntreprise;
	}


	/**
	 * Recherche les tiers qui n'ont pas au moins un régime fiscal
	 * @return la liste des numéros de tiers concernés. Vide si aucun.
	 */
	@NotNull
	private Set<Long> getTiersSansRegime() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		return template.execute(new TransactionCallback<Set<Long>>() {
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.executeWithNewSession(session -> {
					return tiersDAO.getEntreprisesSansRegimeFiscal();
				});
			}
		});
	}

}
