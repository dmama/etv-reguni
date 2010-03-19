package ch.vd.uniregctb.situationfamille;

import java.util.List;

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
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.type.TarifImpotSource;

/**
 * Processor qui permet de réinitialiser à la valeur NORMAL les barêmes double-gains sur les situations de famille actives des
 * ménages-communs sourciers.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ReinitialiserBaremeDoubleGainProcessor {

	private static final Logger LOGGER = Logger.getLogger(ReinitialiserBaremeDoubleGainProcessor.class);

	private final int BATCH_SIZE = 100;

	private final SituationFamilleService service;
	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;

	private ReinitialiserBaremeDoubleGainResults rapport;

	public ReinitialiserBaremeDoubleGainProcessor(SituationFamilleService service, HibernateTemplate hibernateTemplate,
			PlatformTransactionManager transactionManager) {
		this.service = service;
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
	}

	public ReinitialiserBaremeDoubleGainResults run(final RegDate dateTraitement, StatusManager s) {
		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Récupération des situations de famille avec double-gain...");

		final List<Long> dis = retrieveSituationsDoubleGain(dateTraitement);

		status.setMessage("Début du traitement des situations de famille...");

		final ReinitialiserBaremeDoubleGainResults rapportFinal = new ReinitialiserBaremeDoubleGainResults(dateTraitement);

		final BatchTransactionTemplate<Long, ReinitialiserBaremeDoubleGainResults> template = new BatchTransactionTemplate<Long, ReinitialiserBaremeDoubleGainResults>(dis, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, ReinitialiserBaremeDoubleGainResults>() {

			@Override
			public ReinitialiserBaremeDoubleGainResults createSubRapport() {
				return new ReinitialiserBaremeDoubleGainResults(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ReinitialiserBaremeDoubleGainResults r) throws Exception {

				rapport = r;
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				traiterBatch(batch, dateTraitement);
				return true;
			}
		});

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;

	}

	/**
	 * Traite tout le batch des situation de famille, une par une.
	 *
	 * @param batch
	 *            le batch des situations à traiter
	 * @param dateTraitement
	 *            la date de traitement. Voir {@link ReinitialiserBaremeDoubleGainProcessor#traiterSituation(Long, RegDate)}.
	 */
	private void traiterBatch(List<Long> batch, RegDate dateTraitement) {
		for (Long id : batch) {
			traiterSituation(id, dateTraitement);
		}
	}

	/**
	 * Traite une situation de famille. C'est-à-dire vérifier qu'elle possède bien un tarif double-gain; puis si c'est bien le cas, la
	 * terminer au 31 décembre de l'année précédente et en créer une nouvelle avec un tarif normal à partir du 1er janvier.
	 *
	 * @param id
	 *            l'id de la situation de famille à traiter
	 * @param dateTraitement
	 *            la date de traitement.
	 */
	protected void traiterSituation(Long id, RegDate dateTraitement) {

		Assert.notNull(id, "L'id doit être spécifié.");

		final SituationFamilleMenageCommun situation = (SituationFamilleMenageCommun) hibernateTemplate.get(
				SituationFamilleMenageCommun.class, id);
		Assert.notNull(situation, "La situation de famille n'existe pas.");

		if (situation.getTarifApplicable() != TarifImpotSource.DOUBLE_GAIN) {
			rapport.addIgnoreBaremeNonDoubleGain(situation, "Attendu = DOUBLE_GAIN, constaté = " + situation.getTarifApplicable()
					+ ". Erreur dans la requête SQL ?");
			return;
		}

		final Contribuable contribuable = situation.getContribuable();
		Assert.notNull(contribuable, "La situation de famille n'est pas rattachée à un contribuable.");

		// Crée une nouvelle situation de famille identique à la précédente, mais avec le tarif NORMAL
		SituationFamilleMenageCommun nouvelle = (SituationFamilleMenageCommun) situation.duplicate();
		nouvelle.setDateDebut(RegDate.get(dateTraitement.year(), 1, 1));
		nouvelle.setTarifApplicable(TarifImpotSource.NORMAL);

		nouvelle = (SituationFamilleMenageCommun) service.addSituationFamille(nouvelle, contribuable);
		rapport.addSituationsTraitee(situation, nouvelle);
	}

	private static final String QUERY_STRING = "SELECT "// -------------------------------------------------------
			+ "    sit.id "// ------------------------------------------------------------------------------------
			+ "FROM "// ------------------------------------------------------------------------------------------
			+ "    SituationFamilleMenageCommun AS sit " // ------------------------------------------------------
			+ "WHERE "// -----------------------------------------------------------------------------------------
			+ "    sit.annulationDate IS NULL "// ----------------------------------------------------------------
			+ "    AND sit.dateDebut <= :date "// ----------------------------------------------------------------
			+ "    AND (sit.dateFin IS NULL OR sit.dateFin >= :date) "// -----------------------------------------
			+ "    AND sit.tarifApplicable = 'DOUBLE_GAIN' "// ---------------------------------------------------
			+ "ORDER BY "// --------------------------------------------------------------------------------------
			+ "    sit.id";

	@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
	protected List<Long> retrieveSituationsDoubleGain(final RegDate dateValidite) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException {
						Query query = session.createQuery(QUERY_STRING);
						query.setParameter("date", dateValidite.index());
						return query.list();
					}
				});
			}
		});

		return ids;
	}

	/**
	 * Uniquement pour le testing.
	 */
	protected void setRapport(ReinitialiserBaremeDoubleGainResults rapport) {
		this.rapport = rapport;
	}
}
