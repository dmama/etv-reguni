package ch.vd.unireg.foncier;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Query;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.DroitVirtuelHeriteRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.ServitudeCombinationIterator;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.tiers.Contribuable;

public class InitialisationIFoncProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(InitialisationIFoncProcessor.class);

	private static final int BATCH_SIZE = 20;           // nombre d'immeubles traités par transaction

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final RegistreFoncierService registreFoncierService;

	public InitialisationIFoncProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, RegistreFoncierService registreFoncierService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.registreFoncierService = registreFoncierService;
	}

	public InitialisationIFoncResults run(RegDate dateReference, int nbThreads, @Nullable Integer ofsCommune, StatusManager s) {

		final StatusManager statusManager = s != null ? s : new LoggingStatusManager(LOGGER);
		final InitialisationIFoncResults rapportFinal = new InitialisationIFoncResults(dateReference, nbThreads, ofsCommune, registreFoncierService);

		// on commence en allant chercher les immeubles et leurs droits
		statusManager.setMessage("Récupération des immeubles...");
		final Map<Long, List<InfoDroit>> infosDroitsParImmeuble = getImmeublesConcernes(ofsCommune);
		statusManager.setMessage("Récupéré " + infosDroitsParImmeuble.size() + " immeubles.");

		// maintenant, il faut extraire les données de ces droits / immeubles
		// on a ici la garantie que tous les droits d'un immeuble seront traités dans le même lot, ça peut nous servir pour la suite
		final Collection<Long> idsImmeubles = infosDroitsParImmeuble.keySet();
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

					// si on a demandé une commune particulière, on ignore complètement les immeubles ailleurs
					final ImmeubleRF immeuble = hibernateTemplate.get(ImmeubleRF.class, idImmeuble);
					final SituationRF situation = getSituationValide(immeuble, dateReference);
					if (ofsCommune != null && (situation == null || situation.getCommune().getNoOfs() != ofsCommune)) {
						continue;
					}

					// suivi des opérations
					rapport.onNewImmeuble();

					final EstimationRF estimationFiscale = getEstimationFiscaleValide(immeuble, dateReference);
					final List<InfoDroit> infosDroits = infosDroitsParImmeuble.get(idImmeuble);
					if (infosDroits.isEmpty()) {
						// immeuble sans aucun droit connu
						rapport.addImmeubleSansDroit(immeuble, situation, estimationFiscale);
					}
					else {
						// ok, il y a des droits, y a-t-il des droits valides à la date de référence ?
						final List<InfoDroit> atReference = infosDroits.stream()
								.filter(info -> info.isValidAt(dateReference))
								.collect(Collectors.toList());
						if (atReference.isEmpty()) {
							// non, aucun droit à la date de référence
							rapport.addImmeubleSansDroitADateReference(immeuble, situation, estimationFiscale);
						}
						else {
							// certains droits au moins sont valides à la date de référence, il faut les lister
							atReference.stream()
									.filter(droit -> !statusManager.isInterrupted())
									.map(info -> hibernateTemplate.get(DroitRF.class, info.getIdDroit()))
									.forEach(droit -> traiterDroit(droit, rapport));
						}
					}

					if (statusManager.isInterrupted()) {
						break;
					}
				}
				return !statusManager.isInterrupted();
			}

			@Override
			public InitialisationIFoncResults createSubRapport() {
				return new InitialisationIFoncResults(dateReference, nbThreads, ofsCommune, registreFoncierService);
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.warn(e.getMessage(), e);
				}
			}
		}, progressMonitor);

		// fin du processus
		rapportFinal.end();
		statusManager.setMessage("Extraction terminée.");

		return rapportFinal;
	}

	private void traiterDroit(DroitRF droit, InitialisationIFoncResults rapport) {
		if (droit instanceof DroitProprieteRF) {
			traiterDroitPropriete((DroitProprieteRF) droit, rapport);
		}
		else if (droit instanceof ServitudeRF) {
			traiterServitude((ServitudeRF) droit, rapport);
		}
		else {
			throw new IllegalArgumentException("Type de droit inconnu = [" + droit.getClass().getSimpleName() + "]");
		}
	}

	private void traiterDroitPropriete(DroitProprieteRF droit, InitialisationIFoncResults rapport) {
		final AyantDroitRF ayantDroit = droit.getAyantDroit();
		final ImmeubleRF immeuble = droit.getImmeuble();
		final Contribuable contribuable = getTiersRapproche(ayantDroit, rapport.dateReference);
		final SituationRF situation = getSituationValide(immeuble, rapport.dateReference);
		final EstimationRF estimation = getEstimationFiscaleValide(immeuble, rapport.dateReference);

		final List<DroitVirtuelHeriteRF> droitsVirtuels = registreFoncierService.determineDroitsVirtuelsHerites(droit, contribuable, rapport.dateReference);
		if (droitsVirtuels.isEmpty()) {
			// il n'y a pas d'héritage/fusion valide à la date de référence : on ajoute le droit réel
			rapport.addDroitPropriete(contribuable, droit, situation, estimation);
		}
		else {
			// [SIFISC-27899] il y a un ou plusieurs héritages/fusions valides à la date de référence : on ajoute les droits virtuels et leurs ayants-droits respectifs
			droitsVirtuels.forEach(virtuel -> {
				final Contribuable heritier = hibernateTemplate.get(Contribuable.class, virtuel.getHeritierId());
				if (heritier == null) {
					throw new TiersNotFoundException(virtuel.getHeritierId());
				}
				rapport.addDroitVirtuel(heritier, virtuel, situation, estimation);
			});
		}
	}

	private void traiterServitude(ServitudeRF servitude, InitialisationIFoncResults rapport) {

		// une servitude peut contenir plusieurs bénénificiaires et plusieurs immeubles -> on calcule toutes
		// les combinaisons possibles et on insère une ligne par combinaison
		final ServitudeCombinationIterator iterator = new ServitudeCombinationIterator(Collections.singletonList(servitude).iterator());
		while (iterator.hasNext()) {
			final ServitudeRF combinaison = iterator.next();

			final AyantDroitRF ayantDroit = combinaison.getAyantDroits().iterator().next(); // par définition, il n'y a plus qu'un ayant-droit dans la combinaison
			final ImmeubleRF immeuble = combinaison.getImmeubles().iterator().next(); // par définition, il n'y a plus qu'un ayant-droit dans la combinaison
			final Contribuable contribuable = getTiersRapproche(ayantDroit, rapport.dateReference);
			final SituationRF situation = getSituationValide(immeuble, rapport.dateReference);
			final EstimationRF estimation = getEstimationFiscaleValide(immeuble, rapport.dateReference);
			rapport.addServitude(contribuable, combinaison, situation, estimation);
		}
	}

	@Nullable
	private Contribuable getTiersRapproche(AyantDroitRF ayantDroit, RegDate dateReference) {
		return registreFoncierService.getContribuableRapproche(ayantDroit, dateReference);
	}

	@Nullable
	private SituationRF getSituationValide(ImmeubleRF immeuble, RegDate dateReference) {
		return registreFoncierService.getSituation(immeuble, dateReference);
	}

	@Nullable
	private EstimationRF getEstimationFiscaleValide(ImmeubleRF immeuble, RegDate dateReference) {
		return registreFoncierService.getEstimationFiscale(immeuble, dateReference);
	}

	private static class InfoDroit implements DateRange {

		private final long idDroit;
		private final RegDate dateDebut;
		private final RegDate dateFin;

		public InfoDroit(long idDroit, RegDate dateDebut, RegDate dateFin) {
			this.idDroit = idDroit;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
		}

		@Nullable
		public static InfoDroit get(@Nullable Long idDroit, RegDate dateDebut, RegDate dateFin) {
			if (idDroit == null) {
				return null;
			}
			return new InfoDroit(idDroit, dateDebut, dateFin);
		}

		public long getIdDroit() {
			return idDroit;
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}
	}

	/**
	 * @return une map indexée par l'identifiant de l'immeuble, auquel est associé la liste des droits sur l'immeuble
	 */
	private Map<Long, List<InfoDroit>> getImmeublesConcernes(@Nullable Integer ofsCommune) {
		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setReadOnly(true);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return transactionTemplate.execute(status -> hibernateTemplate.execute(session -> {

			final Query queryDroits;
			final Query queryServitudes;
			if (ofsCommune == null) {
				final String hqlDroits = "SELECT IMM.id, DT.id, DT.dateDebutMetier, DT.dateFinMetier FROM ImmeubleRF IMM LEFT OUTER JOIN IMM.droitsPropriete DT WHERE DT.annulationDate IS NULL AND IMM.annulationDate IS NULL";
				final String hqlServitudes = "SELECT IMM.id, DT.id, DT.dateDebutMetier, DT.dateFinMetier FROM ImmeubleRF IMM LEFT OUTER JOIN IMM.servitudes DT WHERE DT.annulationDate IS NULL AND IMM.annulationDate IS NULL";
				queryDroits = session.createQuery(hqlDroits);
				queryServitudes = session.createQuery(hqlServitudes);
			}
			else {
				final String hqlDroits = "SELECT IMM.id, DT.id, DT.dateDebutMetier, DT.dateFinMetier FROM ImmeubleRF IMM LEFT OUTER JOIN IMM.droitsPropriete DT LEFT OUTER JOIN IMM.situations SITU WHERE DT.annulationDate IS NULL AND IMM.annulationDate IS NULL AND ((SITU.noOfsCommuneSurchargee IS NULL AND SITU.commune.noOfs = :commune) OR SITU.noOfsCommuneSurchargee = :commune)";
				final String hqlServitudes = "SELECT IMM.id, DT.id, DT.dateDebutMetier, DT.dateFinMetier FROM ImmeubleRF IMM LEFT OUTER JOIN IMM.servitudes DT LEFT OUTER JOIN IMM.situations SITU WHERE DT.annulationDate IS NULL AND IMM.annulationDate IS NULL AND ((SITU.noOfsCommuneSurchargee IS NULL AND SITU.commune.noOfs = :commune) OR SITU.noOfsCommuneSurchargee = :commune)";
				queryDroits = session.createQuery(hqlDroits);
				queryDroits.setParameter("commune", ofsCommune);
				queryServitudes = session.createQuery(hqlServitudes);
				queryServitudes.setParameter("commune", ofsCommune);
			}

			final Map<Long, List<InfoDroit>> mapDroits = mapDroitsParImmeuble(queryDroits);
			final Map<Long, List<InfoDroit>> mapServitudes = mapDroitsParImmeuble(queryServitudes);
			mapServitudes.forEach((key, value) -> mapDroits.merge(key, value, ListUtils::union));

			return mapDroits;
		}));
	}

	private static Map<Long, List<InfoDroit>> mapDroitsParImmeuble(Query query) {
		//noinspection unchecked
		final Iterator<Object[]> iterator = query.iterate();
		final Iterable<Object[]> iterable = () -> iterator;
		return StreamSupport.stream(iterable.spliterator(), false)
				.map(row -> Pair.of((Long) row[0], InfoDroit.get((Long) row[1], (RegDate) row[2], (RegDate) row[3])))
				.collect(Collectors.toMap(Pair::getLeft,
				                          pair -> pair.getRight() == null ? Collections.<InfoDroit>emptyList() : Collections.singletonList(pair.getRight()),
				                          ListUtils::union));
	}
}
