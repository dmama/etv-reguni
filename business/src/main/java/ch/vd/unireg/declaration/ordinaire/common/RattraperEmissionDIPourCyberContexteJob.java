package ch.vd.unireg.declaration.ordinaire.common;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.document.RattraperEmissionDIPourCyberContexteRapport;
import ch.vd.unireg.evenement.cybercontexte.EvenementCyberContexteException;
import ch.vd.unireg.evenement.cybercontexte.EvenementCyberContexteSender;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.tiers.Contribuable;

/**
 * [FISCPROJ-499] Ce job permet de re-émettre les événements de mise-à-disposition des DIs dans le contexte de la cyberfiscalité.
 */
public class RattraperEmissionDIPourCyberContexteJob extends JobDefinition {

	public static final String NAME = "RattraperEmissionDIPourCyberContexteJob";

	private static final String PERIODE_DEBUT = "PERIODE_DEBUT";
	private static final String NB_THREADS = "NB_THREADS";
	private static final int BATCH_SIZE = 100;

	private RapportService rapportService;
	private DeclarationImpotOrdinaireDAO declarationImpotOrdinaireDAO;
	private EvenementCyberContexteSender evenementCyberContexteSender;
	private PlatformTransactionManager transactionManager;

	public RattraperEmissionDIPourCyberContexteJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Première période fiscale");
			param.setName(PERIODE_DEBUT);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 2017);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 8);

		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int periodeDebut = getStrictlyPositiveIntegerValue(params, PERIODE_DEBUT);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final StatusManager status = getStatusManager();

		final RattraperEmissionDIPourCyberContexteResults results = emmettreEvenements(periodeDebut, nbThreads, status);
		final RattraperEmissionDIPourCyberContexteRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("Les Dis émises existantes ont été publiées dans le contexte de la cyberfiscalité.", rapport);
	}

	/**
	 * Chercher les déclarations d'impôts émises et envoie un événement d'émission de déclaration d'impôt dans le contexte Cyber.
	 *
	 * @param periodeDebut  la période fiscale minimale à prendre en compte (= les périodes précédentes ne seront pas considérées)
	 * @param nbThreads     le nombre de threads
	 * @param statusManager un status manager
	 * @return les résultats du traitement
	 */
	private RattraperEmissionDIPourCyberContexteResults emmettreEvenements(int periodeDebut, int nbThreads, @NotNull StatusManager statusManager) {

		// on va chercher les ids des déclarations émises
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> diIds = template.execute(status -> declarationImpotOrdinaireDAO.findIdsDeclarationsOrdinairesEmisesFrom(periodeDebut));

		final RattraperEmissionDIPourCyberContexteResults rapportFinal = new RattraperEmissionDIPourCyberContexteResults(periodeDebut, nbThreads);

		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, RattraperEmissionDIPourCyberContexteResults> batchTemplate =
				new ParallelBatchTransactionTemplateWithResults<>(diIds, BATCH_SIZE, nbThreads,
				                                                  Behavior.REPRISE_AUTOMATIQUE, transactionManager,
				                                                  statusManager, AuthenticationInterface.INSTANCE);

		// on émet les événements qui vont bien pour toutes ces déclarations
		batchTemplate.execute(rapportFinal, new BatchWithResultsCallback<Long, RattraperEmissionDIPourCyberContexteResults>() {
			@Override
			public boolean doInTransaction(List<Long> batchIds, RattraperEmissionDIPourCyberContexteResults rapport) {
				statusManager.setMessage("Envoi des messages dans le contexte Cyber ...", progressMonitor.getProgressInPercent());
				batchIds.forEach(id -> emettreEvenementCyber(id, rapport));
				return true;
			}

			@Override
			public RattraperEmissionDIPourCyberContexteResults createSubRapport() {
				return new RattraperEmissionDIPourCyberContexteResults(periodeDebut, nbThreads);
			}
		}, progressMonitor);

		rapportFinal.interrompu = statusManager.isInterrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Emet un événement d'émission de déclaration d'impôt dans le contexte Cyber.
	 *
	 * @param idDI    l'id d'une déclaration d'impôt
	 * @param rapport le rapport à compléter
	 */
	private void emettreEvenementCyber(@NotNull Long idDI, @NotNull RattraperEmissionDIPourCyberContexteResults rapport) {

		rapport.addDiTrouvee();

		final DeclarationImpotOrdinaire di = declarationImpotOrdinaireDAO.get(idDI);
		if (di == null) {
			throw new ProgrammingException("La di avec l'id = [" + idDI + "] n'existe pas.");
		}

		final Contribuable contribuable = di.getTiers();
		final Long ctbId = contribuable.getNumero();
		final Integer numeroSequence = di.getNumero();
		final Integer periodeFiscale = di.getAnneePeriodeFiscale();
		if (periodeFiscale == null) {
			throw new IllegalArgumentException("La période fiscale de la déclaration id = [" + idDI + "] n'est pas renseignée.");
		}

		final String codeControle = di.getCodeControle();
		if (StringUtils.isBlank(codeControle)) {
			rapport.addDiIgnoree(idDI, ctbId, periodeFiscale, numeroSequence, "Le code de contrôle est vide.");
			return;
		}

		// on envoie l'événement d'émission de la DI dans le contexte Cyber
		try {
			evenementCyberContexteSender.sendEmissionDeclarationEvent(ctbId, periodeFiscale, numeroSequence, codeControle, RegDate.get());
			rapport.addDiTraitee(idDI, ctbId, periodeFiscale, numeroSequence);
		}
		catch (EvenementCyberContexteException e) {
			rapport.addErrorTraitement(idDI, ctbId, periodeFiscale, numeroSequence, e.getMessage());
		}
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setDeclarationImpotOrdinaireDAO(DeclarationImpotOrdinaireDAO declarationImpotOrdinaireDAO) {
		this.declarationImpotOrdinaireDAO = declarationImpotOrdinaireDAO;
	}

	public void setEvenementCyberContexteSender(EvenementCyberContexteSender evenementCyberContexteSender) {
		this.evenementCyberContexteSender = evenementCyberContexteSender;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
