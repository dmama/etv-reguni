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
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class InitialisationParentesProcessor {

	private static final Logger LOGGER = Logger.getLogger(InitialisationParentesProcessor.class);
	private static final int BATCH_SIZE = 20;

	private final RapportEntreTiersDAO rapportDAO;
	private final TiersDAO tiersDAO;
	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final MultipleSwitch interceptorSwitch;
	private final TiersService tiersService;

	public InitialisationParentesProcessor(RapportEntreTiersDAO rapportDAO, TiersDAO tiersDAO, PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
	                                       MultipleSwitch interceptorSwitch, TiersService tiersService) {
		this.rapportDAO = rapportDAO;
		this.tiersDAO = tiersDAO;
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.interceptorSwitch = interceptorSwitch;
		this.tiersService = tiersService;
	}

	public InitialisationParentesResults run(int nbThreads, @Nullable StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		// dabord, on vide la base de tous les rapports de parenté existants
		final int nbRemoved = removeExisting(status);
		LOGGER.info("Nombre d'anciens rapports de parenté effacés : " + nbRemoved);

		// ensuite, il faut charger les nouveaux...
		return initParentes(nbThreads, status);
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

	private InitialisationParentesResults initParentes(final int nbThreads, final StatusManager status) {
		final List<Long> ids = getNumerosPersonnesPhysiquesConnuesDuCivil(status);
		LOGGER.info("Nombre de personnes physiques connues dans le registre civil trouvées : " + ids.size());

		final String msg = "Génération des relations de parenté...";
		status.setMessage(msg, 0);
		final InitialisationParentesResults rapportFinal = new InitialisationParentesResults(nbThreads);

		final ParallelBatchTransactionTemplate<Long, InitialisationParentesResults> template = new ParallelBatchTransactionTemplate<>(ids, BATCH_SIZE, nbThreads,
		                                                                                                                                BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, InitialisationParentesResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, InitialisationParentesResults rapport) throws Exception {
				interceptorSwitch.pushState();
				interceptorSwitch.setEnabled(false);
				try {
					status.setMessage(msg, percent);
					for (Long idTiers : batch) {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idTiers);
						final List<Parente> creees = tiersService.initParentesDepuisFiliationsCiviles(pp);
						for (Parente creee : creees) {
							rapport.addParente(creee);
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
			public InitialisationParentesResults createSubRapport() {
				return new InitialisationParentesResults(nbThreads);
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
