package ch.vd.uniregctb.foncier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.Query;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class InitialisationIFoncProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(InitialisationIFoncProcessor.class);

	private static final int BATCH_SIZE = 20;           // nombre d'immeubles traités par transaction

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final RapprochementRFDAO rapprochementRFDAO;

	public InitialisationIFoncProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, RapprochementRFDAO rapprochementRFDAO) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.rapprochementRFDAO = rapprochementRFDAO;
	}

	public InitialisationIFoncResults run(RegDate dateReference, int nbThreads, StatusManager s) {

		final StatusManager statusManager = s != null ? s : new LoggingStatusManager(LOGGER);
		final InitialisationIFoncResults rapportFinal = new InitialisationIFoncResults(dateReference, nbThreads);

		// on commence en allant chercher les droits valides à la date de référence
		statusManager.setMessage("Récupération des droits valides à la date de référence...");
		final Map<Long, List<Long>> idsDroitsParImmeuble = getImmeublesAvecDroitsValides(dateReference);
		statusManager.setMessage("Récupéré " + idsDroitsParImmeuble.size() + " immeubles avec des droits à la date de référence.");

		// maintenant, il faut extraire les données de ces droits / immeubles
		// on a ici la garantie que tous les droits d'un immeuble seront traités dans le même lot, ça peut nous servir pour la suite
		final Collection<Long> idsImmeubles = idsDroitsParImmeuble.keySet();
		final ParallelBatchTransactionTemplateWithResults<Long, InitialisationIFoncResults> template = new ParallelBatchTransactionTemplateWithResults<>(idsImmeubles.iterator(), idsImmeubles.size(), BATCH_SIZE,
		                                                                                                                                                 nbThreads, Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                 transactionManager, statusManager,
		                                                                                                                                                 AuthenticationInterface.INSTANCE);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, InitialisationIFoncResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, InitialisationIFoncResults rapport) throws Exception {
				statusManager.setMessage("Extraction des données en cours...", progressMonitor.getProgressInPercent());
				for (Long idImmeuble : batch) {
					rapport.onNewImmeuble();

					final List<Long> idsDroits = idsDroitsParImmeuble.get(idImmeuble);
					final List<DroitRF> droits = idsDroits.stream()
							.map(id -> hibernateTemplate.get(DroitRF.class, id))
							.collect(Collectors.toCollection(() -> new ArrayList<>(idsDroits.size())));

					for (DroitRF droit : droits) {
						if (statusManager.interrupted()) {
							break;
						}
						traiterDroit(droit, rapport);
					}

					if (statusManager.interrupted()) {
						break;
					}
				}
				return !statusManager.interrupted();
			}

			@Override
			public InitialisationIFoncResults createSubRapport() {
				return new InitialisationIFoncResults(dateReference, nbThreads);
			}

		}, progressMonitor);

		// fin du processus
		rapportFinal.end();
		statusManager.setMessage("Extraction terminée.");

		return rapportFinal;
	}

	private void traiterDroit(DroitRF droit, InitialisationIFoncResults rapport) {
		final AyantDroitRF ayantDroit = droit.getAyantDroit();
		final ImmeubleRF immeuble = droit.getImmeuble();
		final Contribuable contribuable = getTiersRapproche(ayantDroit, rapport.dateReference);
		final SituationRF situation = getSituationValide(immeuble, rapport.dateReference);
		rapport.addDroit(contribuable, droit, situation);
	}

	@Nullable
	private Contribuable getTiersRapproche(AyantDroitRF ayantDroit, RegDate dateReference) {
		return rapprochementRFDAO.findByTiersRF(ayantDroit.getId(), true).stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(rapprochement -> rapprochement.isValidAt(dateReference))
				.findFirst()
				.map(RapprochementRF::getContribuable)
				.orElse(null);
	}

	@Nullable
	private SituationRF getSituationValide(ImmeubleRF immeuble, RegDate dateReference) {
		return immeuble.getSituations().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(situation -> situation.isValidAt(dateReference))
				.findFirst()
				.orElse(null);
	}

	/**
	 * @param date une date de référence
	 * @return une map indexée par l'identifiant de l'immeuble, auquel est associé la liste des droits sur l'immeuble
	 */
	private Map<Long, List<Long>> getImmeublesAvecDroitsValides(RegDate date) {
		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setReadOnly(true);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return transactionTemplate.execute(status -> hibernateTemplate.execute(session -> {
			final String hql = "SELECT DT.id, DT.immeuble.id FROM DroitRF DT WHERE DT.annulationDate IS NULL AND (DT.dateDebutMetier IS NULL OR DT.dateDebutMetier <= :ref) AND (DT.dateFinMetier IS NULL OR DT.dateFinMetier >= :ref)";
			final Query query = session.createQuery(hql);
			query.setParameter("ref", date);

			//noinspection unchecked
			final Iterator<Object[]> iterator = query.iterate();
			final Iterable<Object[]> iterable = () -> iterator;
			return StreamSupport.stream(iterable.spliterator(), false)
					.collect(Collectors.toMap(array -> (Long) array[1],
					                          array -> Collections.singletonList((Long) array[0]),
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));
		}));
	}
}
