package ch.vd.uniregctb.tiers.rattrapage.etatdeclaration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.document.CorrectionEtatDeclarationRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tache.TacheSynchronizerInterceptor;
import ch.vd.uniregctb.validation.ValidationInterceptor;

/**
 * [UNIREG-3183] Job qui supprime tous les doublons sur les états des déclarations
 */
public class CorrectionEtatDeclarationJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(CorrectionEtatDeclarationJob.class);

	public static final String NAME = "CorrectionEtatDeclarationJob";
	private static final String CATEGORIE = "Database";
	public static final int BATCH_SIZE = 20;

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private RapportService rapportService;
	private TacheSynchronizerInterceptor tacheSynchronizerInterceptor;
	private ValidationInterceptor validationInterceptor;

	public CorrectionEtatDeclarationJob(int order, String description) {
		super(NAME, CATEGORIE, order, description);
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

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager;
		if (getStatusManager() == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}
		else {
			statusManager = getStatusManager();
		}

		Audit.success("Démarrage du traitement de suppression des doublons des états des déclarations.");

		statusManager.setMessage("Recherche des doublons sur les états de déclarations...");

		final List<Long> ids = retrieveIdsDeclarations();

		final CorrectionEtatDeclarationResults results;
		tacheSynchronizerInterceptor
				.setOnTheFlySynchronization(false); // on désactive la synchronisation des tâches parce qu'il s'agit d'un job de rattrapage et qu'on ne veut pas modifier toute la planète
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

		Audit.success("Traitement de suppression des doublons des états des déclarations terminé.", rapport);
	}

	private CorrectionEtatDeclarationResults traiteDeclarations(final StatusManager status, List<Long> ids) {

		final CorrectionEtatDeclarationResults rapportFinal = new CorrectionEtatDeclarationResults();

		final BatchTransactionTemplate<Long, CorrectionEtatDeclarationResults> t =
				new BatchTransactionTemplate<Long, CorrectionEtatDeclarationResults>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		t.execute(rapportFinal, new BatchCallback<Long, CorrectionEtatDeclarationResults>() {
			@Override
			public CorrectionEtatDeclarationResults createSubRapport() {
				return new CorrectionEtatDeclarationResults();
			}

			@Override
			public boolean doInTransaction(List<Long> batch, CorrectionEtatDeclarationResults rapport) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);
				traiterBatch(batch, rapport);
				return true;
			}
		});

		final int count = rapportFinal.doublons.size();
		if (status.interrupted()) {
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
			public Map<Long, List<EtatDeclaration>> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery("select e.declaration.id, e from EtatDeclaration e where e.declaration.id in (:ids)");
				query.setParameterList("ids", batch);
				final List lines = query.list();

				final Map<Long, List<EtatDeclaration>> map = new HashMap<Long, List<EtatDeclaration>>();
				for (Object line : lines) {
					final Object[] values = (Object[]) line;
					final Long diId = (Long) values[0];
					final EtatDeclaration etat = (EtatDeclaration) values[1];

					List<EtatDeclaration> etats = map.get(diId);
					if (etats == null) {
						etats = new ArrayList<EtatDeclaration>();
						map.put(diId, etats);
					}
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
				hibernateTemplate.delete(e); // on efface l'état à la main
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
		return RegDate.get(left) == RegDate.get(right);
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
		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			public List<Long> doInTransaction(TransactionStatus status) {
				//noinspection unchecked
				return hibernateTemplate.find(QUERY_STRING);
			}
		});
		Collections.sort(ids);
		return ids;
	}
}
