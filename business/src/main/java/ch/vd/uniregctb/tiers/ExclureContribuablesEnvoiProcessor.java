package ch.vd.uniregctb.tiers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.hibernate.HibernateTemplate;

/**
 * Processor qui applique la date limite d'exclusion à tous les contribuables spécifiés par leur numéros, et génère un rapport.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ExclureContribuablesEnvoiProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExclureContribuablesEnvoiProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final AdresseService adresseService;

	public ExclureContribuablesEnvoiProcessor(HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, TiersService tiersService, AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	public ExclureContribuablesEnvoiResults run(final List<Long> ctbIds, final RegDate dateLimite, StatusManager s) {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Début du traitement...");

		final ExclureContribuablesEnvoiResults rapportFinal = new ExclureContribuablesEnvoiResults(ctbIds, dateLimite, tiersService, adresseService);

		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, ExclureContribuablesEnvoiResults> template = new BatchTransactionTemplateWithResults<>(ctbIds, BATCH_SIZE,
		                                                                                                                                       Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, ExclureContribuablesEnvoiResults>() {

			@Override
			public ExclureContribuablesEnvoiResults createSubRapport() {
				return new ExclureContribuablesEnvoiResults(ctbIds, dateLimite, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ExclureContribuablesEnvoiResults r) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, dateLimite, r);
				return true;
			}
		}, progressMonitor);

		rapportFinal.interrompu = status.isInterrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Applique la date limite d'exclusion à tous les contribuables spécifiés par leur numéros.
	 */
	private void traiterBatch(List<Long> batch, RegDate dateLimite, ExclureContribuablesEnvoiResults r) {

		for (Long id : batch) {

			final Contribuable ctb = hibernateTemplate.get(Contribuable.class, id);
			if (ctb == null) {
				r.addErrorCtbInconnu(id);
				continue;
			}

			final RegDate dateExistante = ctb.getDateLimiteExclusionEnvoiDeclarationImpot();
			if (dateExistante == null || dateExistante.isBeforeOrEqual(dateLimite)) {
				// tout va bien, on met la date
				ctb.setDateLimiteExclusionEnvoiDeclarationImpot(dateLimite);
			}
			else {
				// il y a déjà une date, et elle est plus grande -> on ignore
				r.addIgnoreDateLimiteExistante(ctb, "Date limite existante = " + RegDateHelper.dateToDisplayString(dateExistante));
			}
		}
	}

}
