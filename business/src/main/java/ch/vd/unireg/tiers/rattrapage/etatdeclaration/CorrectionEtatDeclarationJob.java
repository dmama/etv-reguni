package ch.vd.unireg.tiers.rattrapage.etatdeclaration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.document.CorrectionEtatDeclarationRapport;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.tache.TacheSynchronizerInterceptor;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.validation.ValidationInterceptor;

/**
 * [UNIREG-3183] Job qui supprime tous les doublons sur les états des déclarations
 */
public class CorrectionEtatDeclarationJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(CorrectionEtatDeclarationJob.class);

	public static final String NAME = "CorrectionEtatDeclarationJob";
	public static final int BATCH_SIZE = 20;

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private RapportService rapportService;
	private TacheSynchronizerInterceptor tacheSynchronizerInterceptor;
	private ValidationInterceptor validationInterceptor;
	private TiersService tiersService;
	private AdresseService adresseService;

	public CorrectionEtatDeclarationJob(int order, String description) {
		super(NAME, JobCategory.DB, order, description);
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
	public void setTacheSynchronizerInterceptor(TacheSynchronizerInterceptor tacheSynchronizerInterceptor) {
		this.tacheSynchronizerInterceptor = tacheSynchronizerInterceptor;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
		this.validationInterceptor = validationInterceptor;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager;
		if (getStatusManager() == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}
		else {
			statusManager = getStatusManager();
		}

		audit.success("Démarrage du traitement de suppression des doublons des états des déclarations.");

		statusManager.setMessage("Recherche des doublons sur les états de déclarations...");

		final List<Long> ids = retrieveIdsDeclarations();

		final CorrectionEtatDeclarationResults results;
		tacheSynchronizerInterceptor.setOnTheFlySynchronization(false); // on désactive la synchronisation des tâches parce qu'il s'agit d'un job de rattrapage et qu'on ne veut pas modifier toute la planète
		validationInterceptor.setEnabled(false); // on désactive la validation parce qu'on veut juste supprimer des doublons sur les états, il ne s'agit pas de modifier des données métier.
		try {
			results = traiteDeclarations(statusManager, ids);
		}
		finally {
			validationInterceptor.setEnabled(true);
			tacheSynchronizerInterceptor.setOnTheFlySynchronization(true);
		}

		final CorrectionEtatDeclarationRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);

		audit.success("Traitement de suppression des doublons des états des déclarations terminé.", rapport);
	}

	private CorrectionEtatDeclarationResults traiteDeclarations(final StatusManager status, List<Long> ids) {

		final CorrectionEtatDeclarationResults rapportFinal = new CorrectionEtatDeclarationResults(tiersService, adresseService);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, CorrectionEtatDeclarationResults> t = new BatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		t.execute(rapportFinal, new BatchWithResultsCallback<Long, CorrectionEtatDeclarationResults>() {
			@Override
			public CorrectionEtatDeclarationResults createSubRapport() {
				return new CorrectionEtatDeclarationResults(tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, CorrectionEtatDeclarationResults rapport) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, rapport);
				return true;
			}
		}, progressMonitor);

		final int count = rapportFinal.doublons.size();
		if (status.isInterrupted()) {
			status.setMessage("La suppression des doublons des états des déclarations d'impôt a été interrompue."
					+ " Nombre de doublons supprimés au moment de l'interruption = " + count);
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("La suppression des doublons des états des déclarations d'impôt est terminée."
					+ " Nombre de doublons supprimés = " + count + ". Nombre d'erreurs = " + rapportFinal.erreurs.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	@SuppressWarnings({"unchecked"})
	private void traiterBatch(final List<Long> batch, CorrectionEtatDeclarationResults rapport) {

		// on évite de charger les déclarations, parce que cela fait charger les tiers associées et c'est coûteux
		final Map<Long, List<EtatDeclaration>> map = hibernateTemplate.execute(new HibernateCallback<Map<Long, List<EtatDeclaration>>>() {
			@Override
			public Map<Long, List<EtatDeclaration>> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery("select e.declaration.id, e from EtatDeclaration e where e.declaration.id in (:ids)");
				query.setParameterList("ids", batch);
				final List lines = query.list();

				final Map<Long, List<EtatDeclaration>> map = new HashMap<>();
				for (Object line : lines) {
					final Object[] values = (Object[]) line;
					final Long diId = (Long) values[0];
					final EtatDeclaration etat = (EtatDeclaration) values[1];

					final List<EtatDeclaration> etats = map.computeIfAbsent(diId, k -> new ArrayList<>());
					etats.add(etat);
				}

				return map;
			}
		});

		for (List<EtatDeclaration> etats : map.values()) {
			traiterEtatsDeclaration(etats, rapport);
		}
	}

	private void traiterEtatsDeclaration(List<EtatDeclaration> etats, CorrectionEtatDeclarationResults rapport) {

		rapport.addEtatsAvantTraitement(etats);

		if (etats == null || etats.isEmpty()) {
			return;
		}

		boolean foundDoublon;
		do {
			foundDoublon = false;
			for (EtatDeclaration etat : etats) {
				if (removeDoublons(etat, etats, rapport)) {
					foundDoublon = true;
					break; // on recommence l'itération au début parce que la collection a été modifiée par la méthode
				}
			}
		}
		while (foundDoublon);
	}

	/**
	 * Supprime tous les doublons de l'état spécifié dans la collection spécifiée.
	 *
	 * @param etat    un état de déclaration
	 * @param etats   une collection avec potientellement des doublons
	 * @param rapport le rapport du batch qui sera complété
	 * @return <b>vrai</b> si au moins un doublon a été détecté et supprimé de la collection; <b>faux</b> autrement.
	 */
	private boolean removeDoublons(EtatDeclaration etat, List<EtatDeclaration> etats, CorrectionEtatDeclarationResults rapport) {
		boolean foundDoublon = false;
		for (Iterator<EtatDeclaration> iterator = etats.iterator(); iterator.hasNext();) {
			final EtatDeclaration e = iterator.next();
			if (etat != e && isDoublon(etat, e)) {
				rapport.addDoublonSupprime(e);
				iterator.remove();

				hibernateTemplate.execute(new HibernateCallback<Object>() {
					@Override
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						session.delete(e);
						return null;
					}
				});
				foundDoublon = true;
			}
		}
		return foundDoublon;
	}

	/**
	 * Détermine si deux états sont identiques d'un point de vue métier.
	 *
	 * @param left  un premier état
	 * @param right un second état
	 * @return <b>vrai</b> si les deux états sont équivalents d'un point de vue métier; ou <b>faux</b> dans les autres cas.
	 */
	private boolean isDoublon(EtatDeclaration left, EtatDeclaration right) {
		return left.getEtat() == right.getEtat() &&
				left.getDateObtention() == right.getDateObtention() &&
				dateEquals(left.getAnnulationDate(), right.getAnnulationDate());
	}

	private static boolean dateEquals(Date left, Date right) {
		if (left == null && right == null) {
			return true;
		}
		//noinspection SimplifiableIfStatement
		if (right == null || left == null) {
			return false;
		}
		return RegDateHelper.get(left) == RegDateHelper.get(right);
	}

	private static final String QUERY_STRING = "select distinct" +
			"    e1.declaration.id " +
			"from " +
			"    EtatDeclaration e1, EtatDeclaration e2 " +
			"where " +
			"    e1.id != e2.id " +
			"    and e1.declaration.id = e2.declaration.id " +
			"    and e1.dateObtention = e2.dateObtention " +
			"    and e1.class = e2.class " +
			"    and e1.annulationDate is null " +
			"    and e2.annulationDate is null ";

	private List<Long> retrieveIdsDeclarations() {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		final List<Long> ids = template.execute(status -> hibernateTemplate.find(QUERY_STRING, null));
		Collections.sort(ids);
		return ids;
	}
}
