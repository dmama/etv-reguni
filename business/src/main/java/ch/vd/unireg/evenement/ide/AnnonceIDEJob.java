package ch.vd.unireg.evenement.ide;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.NativeQuery;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.document.AnnoncesIDERapport;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

public class AnnonceIDEJob extends JobDefinition {

	public static final String NAME = "AnnonceIDEJob";

	public static final String SIMULATION = "SIMULATION";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private Dialect dialect;
	private TiersDAO tiersDAO;
	private ServiceIDEService serviceIDEService;
	private RapportService rapportService;


	public AnnonceIDEJob(int sortOrder, String description) {
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

	public void setServiceIDEService(ServiceIDEService serviceIDEService) {
		this.serviceIDEService = serviceIDEService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final boolean simulation = getBooleanValue(params, SIMULATION);

		List<Long> tiersAEvaluer = getTiersAEvaluer();
		final RegDate aujourdhui = RegDate.get();

		final AnnonceIDEJobResults results = new AnnonceIDEJobResults(simulation);

		AuthenticationHelper.pushPrincipal(AuthenticationHelper.getCurrentPrincipal());
		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

			for (final Long tiersId : tiersAEvaluer) {

				// On traite chaque tiers dans une transaction séparée, pour éviter qu'un problème sur un tiers ne vienne interrompre le traitement.
				final AnnonceIDEJobResults resultat = template.execute(status -> {
					if (simulation) {
						status.setRollbackOnly();
					}
					try {
						return traiteTiers(tiersId, aujourdhui, simulation);
					}
					catch (Exception e) {
						// On doit faire le ménage si un problème est survenu pendant l'envoi afin d'éviter de croire qu'on a émis l'annonce alors que ce n'est pas le cas.
						status.setRollbackOnly();
						final AnnonceIDEJobResults resultatTiers = new AnnonceIDEJobResults(simulation);
						resultatTiers.addErrorException(tiersId, e);
						return resultatTiers;
					}
				});
				if (resultat != null) {
					results.addAll(resultat);
				}
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		final AnnoncesIDERapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		audit.success(String.format("%s des annonces IDE sur les tiers sous contrôle ACI terminé%s.", simulation ? "Simulation de l'envoi" : "Envoi", simulation ? "e" : ""), rapport);
	}

	protected AnnonceIDEJobResults traiteTiers(Long tiersId, RegDate date, boolean simulation) throws ServiceIDEException {
		final Tiers tiers = tiersDAO.get(tiersId);
		final AnnonceIDEJobResults resultatEntreprise = new AnnonceIDEJobResults(simulation);
		if (tiers instanceof Entreprise) {
			final Entreprise entreprise = (Entreprise) tiers;
			if (serviceIDEService.isServiceIDEObligEtendues(entreprise, date)) {
				final BaseAnnonceIDE annonceIDE;
				if (simulation) {
					annonceIDE = serviceIDEService.simuleSynchronisationIDE(entreprise);
				}
				else {
					annonceIDE = serviceIDEService.synchroniseIDE(entreprise);
				}
				if (annonceIDE != null) {
					resultatEntreprise.addAnnonceIDE(entreprise.getNumero(), annonceIDE);
				}
			}
		}
		else {
			throw new IllegalArgumentException("Le tiers n'est pas une entreprise.");
		}

		return hibernateTemplate.executeWithNewSession(session -> {
			final NativeQuery query = session.createNativeQuery("update TIERS set IDE_DIRTY = " + dialect.toBooleanValueString(false) + " where NUMERO = :id");
			query.setParameter("id", tiers.getNumero());
			query.executeUpdate();

			session.flush();
			return resultatEntreprise.getAnnoncesIDE().isEmpty() ? null : resultatEntreprise;
		});
	}

	/**
	 * Recherche les tiers pour lesquels il faut considérer l'envoi d'une annonce à l'IDE (qui ont le flag ide_dirty == true)
	 * @return la liste des numéros de tiers concernés. Vide si aucun.
	 */
	@NotNull
	private List<Long> getTiersAEvaluer() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		return template.execute(status -> hibernateTemplate.executeWithNewSession(session -> {
			final NativeQuery query = session.createNativeQuery("SELECT t.numero FROM tiers t WHERE t.ide_dirty = " + dialect.toBooleanValueString(true) + " AND t.annulation_date is null");
			//noinspection unchecked
			final List<Number> list = query.list();
			return list.stream()
					.map(Number::longValue)
					.collect(Collectors.toList());
		}));
	}

}
