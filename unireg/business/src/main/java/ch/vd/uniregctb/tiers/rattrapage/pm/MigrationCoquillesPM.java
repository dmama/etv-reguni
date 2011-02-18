package ch.vd.uniregctb.tiers.rattrapage.pm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.MigrationCoquillesPMRapport;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Job de migration des coquilles vides des PMs ([UNIREG-2612]).
 */
public class MigrationCoquillesPM extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(MigrationCoquillesPM.class);

	public static final String NAME = "MigrationCoquillesPM";
	private static final String CATEGORIE = "Database";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private RapportService rapportService;
	private ServicePersonneMoraleService servicePM;

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

		final Set<Long> delta = new HashSet<Long>();
		for (Long id : idsSource) {
			if (!idsTarget.contains(id)) {
				delta.add(id);
			}
		}

		final List<Long> sorted = new ArrayList<Long>(delta);
		Collections.sort(sorted);
		return sorted;
	}

	@SuppressWarnings({"unchecked"})
	private Set<Long> getTargetIds() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return hibernateTemplate.executeFind(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						final Query query = session.createQuery("select e.id from Entreprise as e");
						return query.list();
					}
				});
			}
		});

		return new HashSet<Long>(ids);
	}

	private List<Long> getSourceIds() {
		return servicePM.getAllIds();
	}

	private MigrationResults migrate(List<Long> ids, final StatusManager status, final RegDate dateTraitement) {

		final MigrationResults rapportFinal = new MigrationResults(dateTraitement);

		final BatchTransactionTemplate<Long, MigrationResults> template =
				new BatchTransactionTemplate<Long, MigrationResults>(ids, 100, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, MigrationResults>() {

			@Override
			public MigrationResults createSubRapport() {
				return new MigrationResults(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, MigrationResults rapport) throws Exception {
				status.setMessage("Traitement du lot " + batch.get(0) + " -> " + batch.get(batch.size() - 1) + " ...", percent);
				for (Long id : batch) {
					++ rapport.total;
					hibernateTemplate.save(new Entreprise(id));
					rapport.addPMTraitee(id);
				}
				return true;
			}
		});

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

			public Erreur(Long noCtb, Integer officeImpotID, ErreurType raison, String details) {
				super((noCtb == null ? 0 : noCtb), officeImpotID, details);
				this.raison = raison;
			}

			@Override
			public String getDescriptionRaison() {
				return raison.description;
			}
		}

		public MigrationResults(RegDate dateTraitement) {
			this.dateTraitement = dateTraitement;
		}

		public RegDate dateTraitement;
		/**
		 * Nombre total de personnes morales à migrer
		 */
		public int total;
		/**
		 * Ids des personnes morales migrées avec succès.
		 */
		public final List<Long> traitees = new ArrayList<Long>();
		/**
		 * Personnes morales n'ayant pas pu être migrées (avec description du problème)
		 */
		public final List<Erreur> erreurs = new ArrayList<Erreur>();
		/**
		 * Vrai si le traitement a été interrompu ou non
		 */
		public boolean interrompu;

		public void addPMTraitee(Long id) {
			traitees.add(id);
		}

		public void addErrorException(Long element, Exception e) {
			erreurs.add(new Erreur(element, null, ErreurType.EXCEPTION, e.getMessage()));
		}

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
