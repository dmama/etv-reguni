package ch.vd.uniregctb.tiers.rattrapage.pm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.document.MigrationCoquillesPMRapport;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Job de migration des coquilles vides des PMs ([UNIREG-2612]).
 */
public class MigrationCoquillesPM extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationCoquillesPM.class);

	public static final String NAME = "MigrationCoquillesPM";
	private static final String CATEGORIE = "Database";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private RapportService rapportService;
	private ServicePersonneMoraleService servicePM;
	private TiersService tiersService;
	private AdresseService adresseService;

	public MigrationCoquillesPM(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServicePM(ServicePersonneMoraleService servicePM) {
		this.servicePM = servicePM;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager status;
		if (getStatusManager() == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		else {
			status = getStatusManager();
		}
		final RegDate dateTraitement = RegDate.get();

		final List<Long> ids = determineIdsToMigrate();
		LOGGER.info("Nombre de PMs devant être migrées = " + ids.size());
		final MigrationResults results = migrate(ids, status, dateTraitement);
		final MigrationCoquillesPMRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La migration des coquilles vides des personnes morales au " + RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}

	private List<Long> determineIdsToMigrate() {

		final List<Long> idsSource = getSourceIds();
		final Set<Long> idsTarget = getTargetIds();

		final Set<Long> delta = new HashSet<>();
		for (Long id : idsSource) {
			if (!idsTarget.contains(id)) {
				delta.add(id);
			}
		}

		final List<Long> sorted = new ArrayList<>(delta);
		Collections.sort(sorted);
		return sorted;
	}

	private Set<Long> getTargetIds() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
						final Query query = session.createQuery("select e.id from Entreprise as e");
						//noinspection unchecked
						return query.list();
					}
				});
			}
		});

		return new HashSet<>(ids);
	}

	private List<Long> getSourceIds() {
		return servicePM.getAllIds();
	}

	private MigrationResults migrate(List<Long> ids, final StatusManager status, final RegDate dateTraitement) {

		final MigrationResults rapportFinal = new MigrationResults(dateTraitement, tiersService, adresseService);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, MigrationResults> template =
				new BatchTransactionTemplateWithResults<>(ids, 100, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, MigrationResults>() {

			@Override
			public MigrationResults createSubRapport() {
				return new MigrationResults(dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, MigrationResults rapport) throws Exception {
				status.setMessage("Traitement du lot " + batch.get(0) + " -> " + batch.get(batch.size() - 1) + " ...", progressMonitor.getProgressInPercent());
				for (Long id : batch) {
					++ rapport.total;
					hibernateTemplate.merge(new Entreprise(id));
					rapport.addPMTraitee(id);
				}
				return true;
			}
		}, progressMonitor);

		final int count = rapportFinal.traitees.size();

		if (status.interrupted()) {
			status.setMessage("La migration des coquilles des personnes morale a été interrompue." + " Nombre de personne morales migrées au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage(
					"La migration des coquilles des personnes morale est terminée." + " Nombre de personnes morales migrées = " + count + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	public static class MigrationResults extends JobResults<Long, MigrationResults> {

		public static enum ErreurType {
			EXCEPTION(EXCEPTION_DESCRIPTION);

			private final String description;

			private ErreurType(String description) {
				this.description = description;
			}

			public String description() {
				return description;
			}
		}

		public static class Erreur extends Info {
			public final ErreurType raison;

			public Erreur(Long noCtb, Integer officeImpotID, ErreurType raison, String details, String nomCtb) {
				super((noCtb == null ? 0 : noCtb), officeImpotID, details, nomCtb);
				this.raison = raison;
			}

			@Override
			public String getDescriptionRaison() {
				return raison.description;
			}
		}

		public MigrationResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
			super(tiersService, adresseService);
			this.dateTraitement = dateTraitement;
		}

		public final RegDate dateTraitement;
		/**
		 * Nombre total de personnes morales à migrer
		 */
		public int total;
		/**
		 * Ids des personnes morales migrées avec succès.
		 */
		public final List<Long> traitees = new ArrayList<>();
		/**
		 * Personnes morales n'ayant pas pu être migrées (avec description du problème)
		 */
		public final List<Erreur> erreurs = new ArrayList<>();
		/**
		 * Vrai si le traitement a été interrompu ou non
		 */
		public boolean interrompu;

		public void addPMTraitee(Long id) {
			traitees.add(id);
		}

		@Override
		public void addErrorException(Long element, Exception e) {
			erreurs.add(new Erreur(element, null, ErreurType.EXCEPTION, e.getMessage(), getNom(element)));
		}

		@Override
		public void addAll(MigrationResults right) {
			this.total += right.total;
			this.traitees.addAll(right.traitees);
			this.erreurs.addAll(right.erreurs);
		}

		public RegDate getDateTraitement() {
			return dateTraitement;
		}

		public boolean isInterrompu() {
			return interrompu;
		}
	}

}
