package ch.vd.uniregctb.tiers;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;

/**
 * Processor qui applique la date limite d'exclusion à tous les contribuables spécifiés par leur numéros, et génère un rapport.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ExclureContribuablesEnvoiProcessor {

	private static final Logger LOGGER = Logger.getLogger(ExclureContribuablesEnvoiProcessor.class);

	private final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;

	private ExclureContribuablesEnvoiResults rapport;

	public ExclureContribuablesEnvoiProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
	}

	public ExclureContribuablesEnvoiResults run(final List<Long> ctbIds, final RegDate dateLimite, StatusManager s) {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Début du traitement...");

		final ExclureContribuablesEnvoiResults rapportFinal = new ExclureContribuablesEnvoiResults(ctbIds, dateLimite);

		final BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(ctbIds, BATCH_SIZE,
				Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.execute(new BatchCallback<Long>() {

			private Long idCtb = null;

			@Override
			public void beforeTransaction() {
				rapport = new ExclureContribuablesEnvoiResults(ctbIds, dateLimite);
				idCtb = null;
			}

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {

				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				if (batch.size() == 1) {
					idCtb = batch.get(0);
				}
				traiterBatch(batch, dateLimite);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (willRetry) {
					// le batch va être rejoué -> on peut ignorer le rapport
					rapport = null;
				}
				else {
					// on ajoute l'exception directement dans le rapport final
					rapportFinal.addErrorException(idCtb, e);
					rapport = null;
				}
			}

			@Override
			public void afterTransactionCommit() {
				rapportFinal.add(rapport);
			}
		});

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Applique la date limite d'exclusion à tous les contribuables spécifiés par leur numéros.
	 */
	private void traiterBatch(List<Long> batch, RegDate dateLimite) {

		for (Long id : batch) {

			final Contribuable ctb = (Contribuable) hibernateTemplate.get(Contribuable.class, id);
			if (ctb == null) {
				rapport.addErrorCtbInconnu(id);
				continue;
			}

			final RegDate dateExistante = ctb.getDateLimiteExclusionEnvoiDeclarationImpot();
			if (dateExistante == null || dateExistante.isBeforeOrEqual(dateLimite)) {
				// tout va bien, on met la date
				ctb.setDateLimiteExclusionEnvoiDeclarationImpot(dateLimite);
			}
			else {
				// il y a déjà une date, et elle est plus grande -> on ignore
				rapport.addIgnoreDateLimiteExistante(ctb, "Date limite existante = " + RegDateHelper.dateToDisplayString(dateExistante));
			}
		}
	}

}
