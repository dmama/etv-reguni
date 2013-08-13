package ch.vd.uniregctb.parentes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.MultipleSwitch;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class CalculParentesProcessor {

	private static final Logger LOGGER = Logger.getLogger(CalculParentesProcessor.class);
	private static final int BATCH_SIZE = 20;

	private final RapportEntreTiersDAO rapportDAO;
	private final TiersDAO tiersDAO;
	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final MultipleSwitch interceptorSwitch;
	private final TiersService tiersService;

	public CalculParentesProcessor(RapportEntreTiersDAO rapportDAO, TiersDAO tiersDAO, PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
	                               MultipleSwitch interceptorSwitch, TiersService tiersService) {
		this.rapportDAO = rapportDAO;
		this.tiersDAO = tiersDAO;
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
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

		final ParallelBatchTransactionTemplate<Long, CalculParentesResults> template = new ParallelBatchTransactionTemplate<>(ids, BATCH_SIZE, nbThreads,
		                                                                                                                      BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                      transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, CalculParentesResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, CalculParentesResults rapport) throws Exception {
				interceptorSwitch.pushState();
				interceptorSwitch.setEnabled(false);
				try {
					status.setMessage(msg, percent);
					for (Long idTiers : batch) {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idTiers);
						final List<ParenteUpdateInfo> updates;
						if (mode == CalculParentesMode.FULL) {
							updates = tiersService.initParentesDepuisFiliationsCiviles(pp);
						}
						else {
							updates = tiersService.refreshParentesSurPersonnePhysique(pp, false);
						}
						for (ParenteUpdateInfo update : updates) {
							rapport.addParenteUpdate(update);
						}

						if (status.interrupted()) {
							break;
						}
					}
					return !status.interrupted();
				}
				finally {
					interceptorSwitch.popState();
				}
			}

			@Override
			public CalculParentesResults createSubRapport() {
				return new CalculParentesResults(nbThreads, mode);
			}
		});

		if (status.interrupted()) {
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
		final Set<Long> idsEnfants = getEnfantsAvecDoublonsSurParents();

		if (idsEnfants != null && idsEnfants.size() > 0) {
			status.setMessage("Elimination des doublons de parentés trouvés...");
			final ParallelBatchTransactionTemplate<Long, BatchResults> template = new ParallelBatchTransactionTemplate<>(idsEnfants, BATCH_SIZE, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
			                                                                                                             transactionManager, status, hibernateTemplate);
			template.execute(new BatchTransactionTemplate.BatchCallback<Long, BatchResults>() {
				@Override
				public boolean doInTransaction(List<Long> batch, BatchResults rapport) throws Exception {
					for (Long id : batch) {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(id);
						tiersService.refreshParentesSurPersonnePhysique(pp, false);
					}
					return true;
				}
			});
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
