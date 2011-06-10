package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.validation.ValidationInterceptor;

public class OfficeImpotIndexerImpl implements OfficeImpotIndexer {

	private static final int BATCH_SIZE = 100;

	private HibernateTemplate hibernateTemplate;
	private OfficeImpotHibernateInterceptor oidInterceptor;
	private PlatformTransactionManager transactionManager;
	private GlobalTiersIndexer tiersIndexer;
	private ValidationInterceptor validationInterceptor;

	private int total; // nombre total de tiers à traiter
	private int current; // nombre de tiers déjà traités

	public void setOidInterceptor(OfficeImpotHibernateInterceptor oidInterceptor) {
		this.oidInterceptor = oidInterceptor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersIndexer(GlobalTiersIndexer tiersIndexer) {
		this.tiersIndexer = tiersIndexer;
	}

	public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
		this.validationInterceptor = validationInterceptor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void indexTiersAvecOfficeImpotInconnu(final StatusManager status) {
		final List<Long> ids = getIdsTiersWithNullOID();
		processAllTiers(ids, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void indexTousLesTiers(final StatusManager status) {
		final List<Long> ids = getIdsAllTiers();
		processAllTiers(ids, status);
	}

	/**
	 * Découpe la liste d'ids en sous-listes de taille fixes.
	 */
	private List<List<Long>> split(List<Long> ids, int batchSize) {

		List<List<Long>> batches = new ArrayList<List<Long>>();

		List<Long> batch = new ArrayList<Long>(batchSize);
		batches.add(batch);

		final int size = ids.size();
		for (int i = 0; i < size; ++i) {
			batch.add(ids.get(i));

			if (batch.size() == batchSize) {
				batch = new ArrayList<Long>(batchSize);
				batches.add(batch);
			}
		}

		return batches;
	}

	/**
	 * Met-à-jour l'OID de tous les tiers spécifiés, en travaillant par batchs de taille déterminée (BATCH_SIZE)
	 */
	private void processAllTiers(List<Long> ids, StatusManager status) {

		try {
			// désactive les intercepteurs suivants pour des raisons de performance
			oidInterceptor.setEnabled(false); // on met-à-jour l'OID à la main
			tiersIndexer.setOnTheFlyIndexation(false); // l'OID n'est pas une information indexée
			validationInterceptor.setEnabled(false); // l'OID n'est pas utilisée dans la validation des tiers

			total = ids.size();
			current = 0;

			// découpe la liste en batches de taille déterminée
			final List<List<Long>> batches = split(ids, BATCH_SIZE);

			for (final List<Long> batch : batches) {
				processTiers(batch, status);
				if (status.interrupted()) {
					break;
				}
			}
		}
		finally {
			tiersIndexer.setOnTheFlyIndexation(true);
			oidInterceptor.setEnabled(true);
			validationInterceptor.setEnabled(true);
		}
	}

	/**
	 * Met-à-jour l'OID sur la liste des tiers spécifiés.
	 */
	private void processTiers(final List<Long> ids, final StatusManager status) {

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus s) {

				current += ids.size();

				final int percent = (current * 100) / total;
				status.setMessage("Calcul de l'office d'impôt du tiers " + current + " sur " + total + " (id=" + ids.get(0) + ")", percent);
				
				oidInterceptor.updateOfficeID(ids);
				return null;
			}
		});
	}

	final private static String queryAllTiers = // ------------------------------
	"SELECT                                                                                         "
			+ "    tiers.id                                                                         "
			+ "FROM                                                                                 "
			+ "    Tiers AS tiers                                                                   "
			+ "WHERE                                                                                "
			+ "    tiers.annulationDate IS null                                                     "
			+ "    AND                                                                              "
			+ "        0 < (                                                                        "
			+ "            SELECT                                                                   "
			+ "                COUNT(fors)                                                          "
			+ "            FROM                                                                     "
			+ "                ForFiscal AS fors                                                    "
			+ "            WHERE                                                                    "
			+ "                fors.tiers.id = tiers.id                                             "
			+ "                AND fors.annulationDate IS null                                      " // on ignore les fors annulés
			+ "                AND (fors.modeImposition IS null OR fors.modeImposition != 'SOURCE') " // ... et les fors sources
			+ "                AND fors.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'              " // ... et les fors non-vaudois
			+ "         )                                                                           "
			+ "ORDER BY tiers.id ASC                                                                ";

	/**
	 * @return un itérateur sur tous les contribuables ayant au moins un for fiscal actif dans la commune vaudoise spécifiée durant l'année
	 *         spécifiée <b>ou</b> dans l'année précédente (de manière à détecter les fin d'assujettissement).
	 */
	protected List<Long> getIdsAllTiers() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final List<Long> ids = hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final Query queryObject = session.createQuery(queryAllTiers);
						//noinspection unchecked
						return queryObject.list();
					}
				});

				return ids;
			}
		});
	}

	final private static String queryTiersWithNullOID = // ------------------------------
	"SELECT                                                                                         "
			+ "    tiers.id                                                                         "
			+ "FROM                                                                                 "
			+ "    Tiers AS tiers                                                                   "
			+ "WHERE                                                                                "
			+ "    tiers.annulationDate IS null                                                     "
			+ "    AND tiers.officeImpotId IS null                                                  "
			+ "    AND                                                                              "
			+ "        0 < (                                                                        "
			+ "            SELECT                                                                   "
			+ "                COUNT(fors)                                                          "
			+ "            FROM                                                                     "
			+ "                ForFiscal AS fors                                                    "
			+ "            WHERE                                                                    "
			+ "                fors.tiers.id = tiers.id                                             "
			+ "                AND fors.annulationDate IS null                                      " // on ignore les fors annulés
			+ "                AND (fors.modeImposition IS null OR fors.modeImposition != 'SOURCE') " // ... et les fors sources
			+ "                AND fors.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'              " // ... et les fors non-vaudois
			+ "         )                                                                           "
			+ "ORDER BY tiers.id ASC                                                                ";

	/**
	 * @return un itérateur sur tous les contribuables ayant au moins un for fiscal actif dans la commune vaudoise spécifiée durant l'année
	 *         spécifiée <b>ou</b> dans l'année précédente (de manière à détecter les fin d'assujettissement).
	 */
	protected List<Long> getIdsTiersWithNullOID() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final List<Long> ids = hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final Query queryObject = session.createQuery(queryTiersWithNullOID);
						//noinspection unchecked
						return queryObject.list();
					}
				});

				return ids;
			}
		});
	}
}
