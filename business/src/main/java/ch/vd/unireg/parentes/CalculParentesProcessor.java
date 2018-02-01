package ch.vd.unireg.parentes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.MultipleSwitch;
import ch.vd.unireg.common.ParallelBatchTransactionTemplate;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.tiers.ParenteUpdateResult;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiersDAO;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class CalculParentesProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(CalculParentesProcessor.class);
	private static final int BATCH_SIZE = 20;

	private final RapportEntreTiersDAO rapportDAO;
	private final TiersDAO tiersDAO;
	private final PlatformTransactionManager transactionManager;
	private final MultipleSwitch interceptorSwitch;
	private final TiersService tiersService;

	public CalculParentesProcessor(RapportEntreTiersDAO rapportDAO, TiersDAO tiersDAO, PlatformTransactionManager transactionManager, MultipleSwitch interceptorSwitch, TiersService tiersService) {
		this.rapportDAO = rapportDAO;
		this.tiersDAO = tiersDAO;
		this.transactionManager = transactionManager;
		this.interceptorSwitch = interceptorSwitch;
		this.tiersService = tiersService;
	}

	public CalculParentesResults run(int nbThreads, CalculParentesMode mode, @Nullable StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		if (mode == CalculParentesMode.FULL) {
			// dabord, on vide la base de tous les rapports de parenté existants
			final int nbRemoved = removeExisting(status);
			LOGGER.info("Nombre d'anciens rapports de parenté effacés : " + nbRemoved);
		}

		// ensuite, il faut faire le refresh
		return refreshParentes(nbThreads, mode, status);
	}

	private int removeExisting(StatusManager status) {
		status.setMessage("Effacement des parentés existantes...");
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TxCallback<Integer>() {
			@Override
			public Integer execute(TransactionStatus status) throws Exception {
				return rapportDAO.removeAllOfKind(TypeRapportEntreTiers.PARENTE);
			}
		});
	}

	private CalculParentesResults refreshParentes(final int nbThreads, final CalculParentesMode mode, final StatusManager status) {
		final List<Long> ids;
		if (mode == CalculParentesMode.REFRESH_DIRTY) {
			ids = getNumerosPersonnesPhysiquesDirty(status);
		}
		else {
			ids = getNumerosPersonnesPhysiquesConnuesDuCivil(status);
		}
		LOGGER.info("Nombre de personnes physiques à inspecter : " + ids.size());

		final String msg = "Calcul des relations de parenté...";
		status.setMessage(msg, 0);
		final CalculParentesResults rapportFinal = new CalculParentesResults(nbThreads, mode);

		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, CalculParentesResults> template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, nbThreads,
		                                                                                                                                            Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                            transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, CalculParentesResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, CalculParentesResults rapport) throws Exception {
				interceptorSwitch.pushState();
				interceptorSwitch.setEnabled(false);
				try {
					status.setMessage(msg, progressMonitor.getProgressInPercent());
					for (Long idTiers : batch) {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idTiers);
						final ParenteUpdateResult result;
						if (mode == CalculParentesMode.FULL) {
							result = tiersService.initParentesDepuisFiliationsCiviles(pp);
						}
						else {
							result = tiersService.refreshParentesSurPersonnePhysique(pp, false);
						}
						for (ParenteUpdateInfo update : result.getUpdates()) {
							rapport.addParenteUpdate(update);
						}
						for (ParenteUpdateResult.Error error : result.getErrors()) {
							rapport.addError(error.getNoCtb(), error.getErrorMsg());
						}

						if (status.isInterrupted()) {
							break;
						}
					}
					return !status.isInterrupted();
				}
				finally {
					interceptorSwitch.popState();
				}
			}

			@Override
			public CalculParentesResults createSubRapport() {
				return new CalculParentesResults(nbThreads, mode);
			}
		}, progressMonitor);

		if (status.isInterrupted()) {
			status.setMessage("Génération des parentés interrompue.");
			rapportFinal.interrupted = true;
		}
		else {
			// élimination des doublons éventuels
			eliminationDoublonsParentes(nbThreads, status);
			status.setMessage("Génération des parentés terminée.");
		}
		rapportFinal.end();

		return rapportFinal;
	}

	private List<Long> getNumerosPersonnesPhysiquesConnuesDuCivil(StatusManager status) {
		status.setMessage("Récupération des identifiants des personnes physiques connues du civil...");
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<List<Long>>() {
			@Override
			public List<Long> execute(TransactionStatus status) throws Exception {
				return tiersDAO.getIdsConnusDuCivil();
			}
		});
	}

	private List<Long> getNumerosPersonnesPhysiquesDirty(StatusManager status) {
		status.setMessage("Récupération des identifiants des personnes physiques à rafraîchir...");
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<List<Long>>() {
			@Override
			public List<Long> execute(TransactionStatus status) throws Exception {
				return tiersDAO.getIdsParenteDirty();
			}
		});
	}

	private void eliminationDoublonsParentes(int nbThreads, StatusManager status) {
		status.setMessage("Récupération des éventuels doublons de parentés à éliminer...");
		final List<Long> idsEnfants = new ArrayList<>(getEnfantsAvecDoublonsSurParents());

		if (idsEnfants.size() > 0) {
			status.setMessage("Elimination des doublons de parentés trouvés...");
			final ParallelBatchTransactionTemplate<Long> template = new ParallelBatchTransactionTemplate<>(idsEnfants, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE,
			                                                                                               transactionManager, status, AuthenticationInterface.INSTANCE);
			template.execute(new BatchCallback<Long>() {
				@Override
				public boolean doInTransaction(List<Long> batch) throws Exception {
					for (Long id : batch) {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(id);
						tiersService.refreshParentesSurPersonnePhysique(pp, false);
					}
					return true;
				}
			}, null);
		}
	}

	private Set<Long> getEnfantsAvecDoublonsSurParents() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<Set<Long>>() {
			@Override
			public Set<Long> doInTransaction(TransactionStatus status) {
				final List<Pair<Long, Long>> candidats = rapportDAO.getDoublonsCandidats(TypeRapportEntreTiers.PARENTE);
				final Set<Long> enfants = new HashSet<>(candidats.size());
				for (Pair<Long, Long> couple : candidats) {
					enfants.add(couple.getLeft());      // (sujet, objet) -> l'enfant, qui est le sujet de la relation, est à gauche
				}
				return enfants;
			}
		});
	}
}
