package ch.vd.uniregctb.tiers;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;

/**
 * Processor qui applique la date limite d'exclusion à tous les contribuables spécifiés par leur numéros, et génère un rapport.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ExclureContribuablesEnvoiProcessor {

	private static final Logger LOGGER = Logger.getLogger(ExclureContribuablesEnvoiProcessor.class);

	private static final int BATCH_SIZE = 100;

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

		final BatchTransactionTemplate<Long, ExclureContribuablesEnvoiResults> template = new BatchTransactionTemplate<Long, ExclureContribuablesEnvoiResults>(ctbIds, BATCH_SIZE,
				Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, ExclureContribuablesEnvoiResults>() {

			@Override
			public ExclureContribuablesEnvoiResults createSubRapport() {
				return new ExclureContribuablesEnvoiResults(ctbIds, dateLimite);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ExclureContribuablesEnvoiResults r) throws Exception {

				rapport = r;
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				traiterBatch(batch, dateLimite);
				return true;
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

			final Contribuable ctb = hibernateTemplate.get(Contribuable.class, id);
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
