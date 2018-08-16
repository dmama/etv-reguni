package ch.vd.unireg.regimefiscal.changement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

/**
 * Processeur qui effectue le changement de régimes fiscaux en masse sur les entreprises concernées.
 */
public class ChangementRegimesFiscauxProcessor {

	public static final Logger LOGGER = LoggerFactory.getLogger(ChangementRegimesFiscauxProcessor.class);

	private final PlatformTransactionManager transactionManager;
	private final TiersDAO tiersDAO;
	private final TiersService tiersService;
	private final RegimeFiscalService regimeFiscalService;

	public ChangementRegimesFiscauxProcessor(PlatformTransactionManager transactionManager, TiersDAO tiersDAO, TiersService tiersService, RegimeFiscalService regimeFiscalService) {
		this.transactionManager = transactionManager;
		this.tiersDAO = tiersDAO;
		this.tiersService = tiersService;
		this.regimeFiscalService = regimeFiscalService;
	}

	/**
	 * Effectue le changement des régimes fiscaux sur les entreprises concernées
	 *
	 * @param ancienCode     le code des régimes fiscaux à fermer
	 * @param nouveauCode    le codes des nouveaux régimes fiscaux à ouvrir
	 * @param dateChangement la date d'ouverture des nouveaux régimes fiscaux
	 * @param nbThreads      le nombre de threads à utiliser
	 * @param s              un status manager
	 * @return les résultats du processing
	 */
	public ChangementRegimesFiscauxJobResults process(String ancienCode, String nouveauCode, RegDate dateChangement, int nbThreads, StatusManager s) {

		StatusManager statusManager = (s == null ? new LoggingStatusManager(LOGGER) : s);

		// on vérifie que les codes existent (les appels lèvent des exceptions si ce n'est pas le cas)
		final TypeRegimeFiscal ancienType = regimeFiscalService.getTypeRegimeFiscal(ancienCode);
		final TypeRegimeFiscal nouveauType = regimeFiscalService.getTypeRegimeFiscal(nouveauCode);

		final ChangementRegimesFiscauxJobResults rapportFinal = new ChangementRegimesFiscauxJobResults(ancienType, nouveauType, dateChangement);

		// on charge la liste des entreprises à traiter
		final List<Long> ids = getEntreprisesATraiter(ancienCode, dateChangement);
		rapportFinal.setTotal(ids.size());

		// on traite chaque entreprise
		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, ChangementRegimesFiscauxJobResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(ids, 100, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, ChangementRegimesFiscauxJobResults>() {

			private final ThreadLocal<Long> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Long> entrepriseIds, ChangementRegimesFiscauxJobResults rapport) throws Exception {
				first.set(entrepriseIds.get(0));
				statusManager.setMessage("Traitement des entreprises...", monitor.getProgressInPercent());
				entrepriseIds.forEach(id -> processEntreprise(id, ancienCode, nouveauCode, dateChangement, rapport));
				return !statusManager.isInterrupted();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					final Long mutId = first.get();
					LOGGER.warn("Erreur pendant le traitement de l'entreprise n°" + mutId, e);
				}
			}

			@Override
			public ChangementRegimesFiscauxJobResults createSubRapport() {
				return new ChangementRegimesFiscauxJobResults(ancienType, nouveauType, dateChangement);
			}
		}, monitor);

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Ferme les régimes fiscaux existants qui correspondent à l'ancien code et ouvre de nouveaux régimes avec le nouveau code.
	 */
	void processEntreprise(Long id, String ancienCode, String nouveauCode, RegDate dateChangement, ChangementRegimesFiscauxJobResults rapport) {

		final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
		if (entreprise == null) {
			throw new TiersNotFoundException(id);
		}

		// précondition technique
		final Set<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscaux().stream()
				.filter(r -> r.getCode().equals(ancienCode))
				.filter(r -> r.isValidAt(dateChangement))
				.collect(Collectors.toSet());
		if (regimesFiscaux.isEmpty()) {
			throw new ProgrammingException("L'entreprise n°" + id + " ne possède pas de régime fiscal de type = [" + ancienCode + "] valide le " + RegDateHelper.dateToDisplayString(dateChangement));
		}

		final String raisonSociale = getRaisonSociale(entreprise);
		final RegDate dateCreation = getDateCreation(entreprise);

		// on ne touche pas aux entreprises dont les régimes à modifier commencent justement à la date de changement
		if (regimesFiscaux.stream()
				.anyMatch(r -> r.getDateDebut() == dateChangement)) {
			rapport.addErreur(id, raisonSociale, dateCreation, "L'entreprise possède déjà des régimes fiscaux qui commencent justement le " + RegDateHelper.dateToDisplayString(dateChangement));
			return;
		}

		// on ne touche pas aux entreprises déjà modifiées (elles doivent être traitées à la main)
		if (regimesFiscaux.stream()
				.anyMatch(r -> r.getDateFin() != null)) {
			rapport.addErreur(id, raisonSociale, dateCreation, "L'entreprise possède des régimes fiscaux modifiés après le " + RegDateHelper.dateToDisplayString(dateChangement));
			return;
		}

		// [FISCPROJ-90] on ferme les anciens régimes et on ouvre les nouveaux
		final List<RegimeFiscal> aAjouter = new ArrayList<>(2);
		regimesFiscaux.forEach(ancien -> {
			ancien.setDateFin(dateChangement.getOneDayBefore());
			aAjouter.add(new RegimeFiscal(dateChangement, null, ancien.getPortee(), nouveauCode));
		});
		aAjouter.forEach(entreprise::addRegimeFiscal);

		rapport.addTraite(id, raisonSociale, dateCreation);
	}

	private RegDate getDateCreation(Entreprise entreprise) {
		try {
			return tiersService.getDateCreation(entreprise);
		}
		catch (RuntimeException e) {
			return null;
		}
	}

	private String getRaisonSociale(Entreprise entreprise) {
		try {
			return tiersService.getDerniereRaisonSociale(entreprise);
		}
		catch (RuntimeException e) {
			return "Erreur: " + e.getMessage();
		}
	}

	private List<Long> getEntreprisesATraiter(String ancienCode, RegDate dateChangement) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		return template.execute(status -> tiersDAO.getEntreprisesAvecRegimeFiscalAt(ancienCode, dateChangement));
	}

}
