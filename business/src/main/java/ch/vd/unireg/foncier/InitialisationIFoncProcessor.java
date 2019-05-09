package ch.vd.unireg.foncier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.ProgrammingException;
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
import ch.vd.unireg.registrefoncier.ServitudeHelper;
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

		// on va cherche le nombre d'immeubles
		statusManager.setMessage("Récupération du nombre d'immeubles...");
		final Long immeublesCount = getImmeublesCount(ofsCommune);
		rapportFinal.setNbImmeublesInspectes(immeublesCount);

		// on va chercher les droits valides à la date de référence
		statusManager.setMessage("Récupération des droits...");
		final List<Long> droitsIds = getDroitsConcernes(dateReference, ofsCommune);

		// on traite les droits
		traiterDroits(droitsIds, rapportFinal, statusManager);

		statusManager.setMessage("Récupération des immeubles sans droits...");
		final Set<Long> immeublesIdsSansAucunDroit = getImmeublesIdsSansAucunDroit(ofsCommune);
		final Set<Long> immeublesIdsSansDroitALaDate = getImmeublesIdsSansDroitALaDate(dateReference, ofsCommune);
		immeublesIdsSansDroitALaDate.removeAll(immeublesIdsSansAucunDroit); // inutile de garder les immeubles sans aucun droit dans cette liste

		// on traite les immeubles sans droits
		traiterImmeublesSansAucunDroit(immeublesIdsSansAucunDroit, rapportFinal, statusManager);
		traiterImmeublesSansDroitActif(immeublesIdsSansDroitALaDate, rapportFinal, statusManager);

		// fin du processus
		rapportFinal.end();
		statusManager.setMessage("Extraction terminée.");

		return rapportFinal;
	}

	/**
	 * Traite les droits
	 *
	 * @param droitsIds     les ids des droits à traiter
	 * @param rapportFinal  le rapport final
	 * @param statusManager un status manager
	 */
	private void traiterDroits(@NotNull List<Long> droitsIds, @NotNull InitialisationIFoncResults rapportFinal, @NotNull StatusManager statusManager) {

		final RegDate dateReference = rapportFinal.dateReference;

		final ParallelBatchTransactionTemplateWithResults<Long, InitialisationIFoncResults> template = new ParallelBatchTransactionTemplateWithResults<>(droitsIds, BATCH_SIZE,
		                                                                                                                                                 rapportFinal.nbThreads, Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                 transactionManager, statusManager,
		                                                                                                                                                 AuthenticationInterface.INSTANCE);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, InitialisationIFoncResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, InitialisationIFoncResults rapport) {
				statusManager.setMessage("Extraction des données en cours...", progressMonitor.getProgressInPercent());
				for (Long idDroit : batch) {

					final DroitRF droit = hibernateTemplate.get(DroitRF.class, idDroit);
					if (droit == null) {
						throw new ObjectNotFoundException("Le droit id = " + idDroit + " n'existe pas");
					}
					if (!droit.getRangeMetier().isValidAt(dateReference)) {
						throw new ProgrammingException("Le droit n°" + idDroit + " n'est pas valide à la date de référence = " + RegDateHelper.dateToDisplayString(dateReference));
					}

					traiterDroit(droit, rapport);

					if (statusManager.isInterrupted()) {
						break;
					}
				}
				return !statusManager.isInterrupted();
			}

			@Override
			public InitialisationIFoncResults createSubRapport() {
				return new InitialisationIFoncResults(dateReference, rapportFinal.nbThreads, rapportFinal.ofsCommune, registreFoncierService);
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.warn(e.getMessage(), e);
				}
			}
		}, progressMonitor);
	}

	/**
	 * Ajoute au rapport tous les immeubles sans droit actif à la date de référence.
	 *
	 * @param immeublesIdsAvecDroitMaisPasALaDate les ids des immeubles sans droit actif à la date de référence
	 * @param rapportFinal                        le rapport final
	 * @param statusManager                       un status manager
	 */
	private void traiterImmeublesSansDroitActif(@NotNull Set<Long> immeublesIdsAvecDroitMaisPasALaDate, @NotNull InitialisationIFoncResults rapportFinal, @NotNull StatusManager statusManager) {

		final RegDate dateReference = rapportFinal.dateReference;
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();

		final ArrayList<Long> ids = new ArrayList<>(immeublesIdsAvecDroitMaisPasALaDate);
		ids.sort(Comparator.naturalOrder());

		final ParallelBatchTransactionTemplateWithResults<Long, InitialisationIFoncResults> template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE,
		                                                                                                                                                 rapportFinal.nbThreads, Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                 transactionManager, statusManager,
		                                                                                                                                                 AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, InitialisationIFoncResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, InitialisationIFoncResults rapport) {
				for (Long immeubleId : batch) {

					final ImmeubleRF immeuble = hibernateTemplate.get(ImmeubleRF.class, immeubleId);
					if (immeuble == null) {
						throw new ObjectNotFoundException("L'immeuble avec l'id = " + immeubleId + " n'existe pas");
					}

					final EstimationRF estimationFiscale = getEstimationFiscaleValide(immeuble, dateReference);
					final SituationRF situation = getSituationValide(immeuble, dateReference);
					// non, aucun droit à la date de référence
					rapport.addImmeubleSansDroitADateReference(immeuble, situation, estimationFiscale);

					if (statusManager.isInterrupted()) {
						break;
					}
				}
				return !statusManager.isInterrupted();
			}

			@Override
			public InitialisationIFoncResults createSubRapport() {
				return new InitialisationIFoncResults(dateReference, rapportFinal.nbThreads, rapportFinal.ofsCommune, registreFoncierService);
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.warn(e.getMessage(), e);
				}
			}
		}, progressMonitor);
	}

	/**
	 * Ajoute au rapport tous les immeubles sans aucun droit connu.
	 *
	 * @param immeublesIdsSansAucunDroit les ids des immeubles sans aucun droit connu
	 * @param rapportFinal               le rapport final
	 * @param statusManager              un status manager
	 */
	private void traiterImmeublesSansAucunDroit(@NotNull Set<Long> immeublesIdsSansAucunDroit, @NotNull InitialisationIFoncResults rapportFinal, @NotNull StatusManager statusManager) {

		final RegDate dateReference = rapportFinal.dateReference;
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();

		final ArrayList<Long> ids = new ArrayList<>(immeublesIdsSansAucunDroit);
		ids.sort(Comparator.naturalOrder());

		final ParallelBatchTransactionTemplateWithResults<Long, InitialisationIFoncResults> template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE,
		                                                                                                                                                 rapportFinal.nbThreads, Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                                 transactionManager, statusManager,
		                                                                                                                                                 AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, InitialisationIFoncResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, InitialisationIFoncResults rapport) {
				for (Long immeubleId : batch) {

					final ImmeubleRF immeuble = hibernateTemplate.get(ImmeubleRF.class, immeubleId);
					if (immeuble == null) {
						throw new ObjectNotFoundException("L'immeuble avec l'id = " + immeubleId + " n'existe pas");
					}

					final EstimationRF estimationFiscale = getEstimationFiscaleValide(immeuble, dateReference);
					final SituationRF situation = getSituationValide(immeuble, dateReference);
					// immeuble sans aucun droit connu
					rapport.addImmeubleSansDroit(immeuble, situation, estimationFiscale);

					if (statusManager.isInterrupted()) {
						break;
					}
				}
				return !statusManager.isInterrupted();
			}

			@Override
			public InitialisationIFoncResults createSubRapport() {
				return new InitialisationIFoncResults(dateReference, rapportFinal.nbThreads, rapportFinal.ofsCommune, registreFoncierService);
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.warn(e.getMessage(), e);
				}
			}
		}, progressMonitor);
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

	private void traiterDroitPropriete(@NotNull DroitProprieteRF droit, @NotNull InitialisationIFoncResults rapport) {

		// si on a demandé une commune particulière, on ignore complètement les immeubles situés ailleurs à la date de référence
		final ImmeubleRF immeuble = droit.getImmeuble();
		final SituationRF situation = getSituationValide(immeuble, rapport.dateReference);
		if (rapport.ofsCommune != null && (situation == null || situation.getCommune().getNoOfs() != rapport.ofsCommune)) {
			return;
		}

		final AyantDroitRF ayantDroit = droit.getAyantDroit();
		final Contribuable contribuable = getTiersRapproche(ayantDroit, rapport.dateReference);
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

	private void traiterServitude(@NotNull ServitudeRF servitude, @NotNull InitialisationIFoncResults rapport) {

		// une servitude peut contenir plusieurs bénéficiaires et plusieurs immeubles -> on calcule toutes
		// les combinaisons possibles et on insère une ligne par combinaison
		final List<ServitudeRF> combinaisons = ServitudeHelper.combinate(servitude,
	                                                                     b -> b.isValidAt(rapport.dateReference),
	                                                                     c -> c.isValidAt(rapport.dateReference));
		combinaisons.forEach(combinaison -> {
			final AyantDroitRF ayantDroit = combinaison.getBenefices().iterator().next().getAyantDroit(); // par définition, il n'y a plus qu'un ayant-droit dans la combinaison
			final ImmeubleRF immeuble = combinaison.getCharges().iterator().next().getImmeuble(); // par définition, il n'y a plus qu'un ayant-droit dans la combinaison

			// si on a demandé une commune particulière, on ne prend que les immeubles situés sur la commune à la date de référence
			final SituationRF situation = getSituationValide(immeuble, rapport.dateReference);
			if (rapport.ofsCommune == null || (situation != null && situation.getCommune().getNoOfs() == rapport.ofsCommune)) {
				final Contribuable contribuable = getTiersRapproche(ayantDroit, rapport.dateReference);
				final EstimationRF estimation = getEstimationFiscaleValide(immeuble, rapport.dateReference);
				rapport.addServitude(contribuable, combinaison, situation, estimation);
			}
		});
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

	/**
	 * @return la liste des tous les droits de propriété et servitudes existants sur le canton ou la commune.
	 */
	private List<Long> getDroitsConcernes(@NotNull RegDate dateReference, @Nullable Integer ofsCommune) {

		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setReadOnly(true);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return transactionTemplate.execute(status -> hibernateTemplate.execute(session -> {

			final Query queryDroits;
			final Query queryServitudes;
			if (ofsCommune == null) {
				final String hqlDroits = "SELECT id FROM DroitProprieteRF " +
						"WHERE annulationDate is null AND " +
						"(dateDebutMetier is null OR dateDebutMetier <= :dateReference) AND " +
						"(dateFinMetier is null OR dateFinMetier >= :dateReference)";
				final String hqlServitudes = "SELECT id FROM ServitudeRF " +
						"WHERE annulationDate is null AND " +
						"(dateDebutMetier is null OR dateDebutMetier <= :dateReference) AND " +
						"(dateFinMetier is null OR dateFinMetier >= :dateReference)";
				queryDroits = session.createQuery(hqlDroits);
				queryDroits.setParameter("dateReference", dateReference);
				queryServitudes = session.createQuery(hqlServitudes);
				queryServitudes .setParameter("dateReference", dateReference);
			}
			else {
				final String hqlDroits =
						"SELECT drt.id FROM DroitProprieteRF drt " +
								"JOIN drt.immeuble imm " +
								"JOIN imm.situations sit " +
								"WHERE drt.annulationDate is null AND " +
								"   sit.annulationDate is null AND " +
								"   (drt.dateDebutMetier is null OR drt.dateDebutMetier <= :dateReference) AND " +
								"   (drt.dateFinMetier is null OR drt.dateFinMetier >= :dateReference) AND " +
								"   (sit.dateDebut is null OR sit.dateDebut <= :dateReference) AND " +
								"   (sit.dateFin is null OR sit.dateFin >= :dateReference) AND " +
								"   ((sit.noOfsCommuneSurchargee is null AND sit.commune.noOfs = :commune) OR sit.noOfsCommuneSurchargee = :commune)";
				final String hqlServitudes =
						"SELECT serv.id FROM ServitudeRF serv " +
								"JOIN serv.charges chg " +
								"JOIN chg.immeuble imm " +
								"JOIN imm.situations sit " +
								"WHERE serv.annulationDate is null AND " +
								"   chg.annulationDate is null AND " +
								"   sit.annulationDate is null AND " +
								"   (serv.dateDebutMetier is null OR serv.dateDebutMetier <= :dateReference) AND " +
								"   (serv.dateFinMetier is null OR serv.dateFinMetier >= :dateReference) AND " +
								"   (chg.dateDebut is null OR chg.dateDebut <= :dateReference) AND " +
								"   (chg.dateFin is null OR chg.dateFin >= :dateReference) AND " +
								"   (sit.dateDebut is null OR sit.dateDebut <= :dateReference) AND " +
								"   (sit.dateFin is null OR sit.dateFin >= :dateReference) AND " +
								"   ((sit.noOfsCommuneSurchargee is null AND sit.commune.noOfs = :commune) OR sit.noOfsCommuneSurchargee = :commune)";
				queryDroits = session.createQuery(hqlDroits);
				queryDroits.setParameter("dateReference", dateReference);
				queryDroits.setParameter("commune", ofsCommune);
				queryServitudes = session.createQuery(hqlServitudes);
				queryServitudes .setParameter("dateReference", dateReference);
				queryServitudes.setParameter("commune", ofsCommune);
			}

			final Set<Long> mapDroits = mapDroits(queryDroits);
			final Set<Long> mapServitudes = mapDroits(queryServitudes);

			return Stream.concat(mapDroits.stream(), mapServitudes.stream())
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toList());
		}));
	}

	private static Set<Long> mapDroits(@NotNull Query query) {
		final Set<Long> set = new HashSet<>();
		//noinspection unchecked
		final Iterator<Long> iterator = query.iterate();
		while (iterator.hasNext()) {
			set.add(iterator.next());
		}
		return set;
	}

	/**
	 * @param ofsCommune un numéro OFS de commune
	 * @return le nombre d'immeubles valides sur le canton ou la commune
	 */
	@NotNull
	private Long getImmeublesCount(@Nullable Integer ofsCommune) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query query;
			if (ofsCommune == null) {
				query = session.createQuery("SELECT count(*) FROM ImmeubleRF WHERE annulationDate is null");
			}
			else {
				query = session.createQuery("SELECT count(*) FROM ImmeubleRF i LEFT JOIN i.situations s " +
						                            "WHERE i.annulationDate IS NULL AND " +
						                            "((s.noOfsCommuneSurchargee IS NULL AND s.commune.noOfs = :commune) OR s.noOfsCommuneSurchargee = :commune)");
				query.setParameter("commune", ofsCommune);
			}
			//noinspection unchecked
			final Number count = (Number) query.uniqueResult();
			return count.longValue();
		}));
	}

	/**
	 * @param ofsCommune un numéro OFS de commune
	 * @return les ids des tous immeubles valides sur le canton ou la commune et sans aucun droit
	 */
	@NotNull
	private Set<Long> getImmeublesIdsSansAucunDroit(@Nullable Integer ofsCommune) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query query;
			if (ofsCommune == null) {
				query = session.createQuery("SELECT i.id FROM ImmeubleRF i WHERE i.annulationDate is null AND " +
						                            "NOT EXISTS (FROM DroitProprieteRF d " +
						                            "            WHERE d.annulationDate is null AND d.immeuble = i" +
						                            ") AND " +
						                            "NOT EXISTS (FROM ServitudeRF s " +
						                            "            JOIN s.charges charge" +
						                            "            WHERE s.annulationDate is null AND " +
						                            "            charge.annulationDate is null AND " +
						                            "            charge.immeuble = i" +
						                            ")");
			}
			else {
				query = session.createQuery("SELECT i.id FROM ImmeubleRF i LEFT JOIN i.situations si " +
						                            "WHERE i.annulationDate IS NULL AND " +
						                            "NOT EXISTS (FROM DroitProprieteRF d " +
						                            "            WHERE d.annulationDate is null AND d.immeuble = i" +
						                            ") AND " +
						                            "NOT EXISTS (FROM ServitudeRF s " +
						                            "            JOIN s.charges charge" +
						                            "            WHERE s.annulationDate is null AND " +
						                            "            charge.annulationDate is null AND " +
						                            "            charge.immeuble = i" +
						                            ") AND " +
				                                    "((si.noOfsCommuneSurchargee IS NULL AND si.commune.noOfs = :commune) OR si.noOfsCommuneSurchargee = :commune)");
				query.setParameter("commune", ofsCommune);
			}
			//noinspection unchecked
			return new HashSet<Long>(query.list());
		}));
	}

	/**
	 * @param dateReference une date de référence
	 * @param ofsCommune    un numéro OFS de commune
	 * @return les ids des tous immeubles valides sur le canton ou la commune et sans droit à la date donnée.
	 */
	@NotNull
	private Set<Long> getImmeublesIdsSansDroitALaDate(@NotNull RegDate dateReference, @Nullable Integer ofsCommune) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query query;
			if (ofsCommune == null) {
				query = session.createQuery("SELECT i.id FROM ImmeubleRF i WHERE i.annulationDate is null AND " +
						                            "NOT EXISTS (FROM DroitProprieteRF d " +
						                            "            WHERE d.annulationDate is null AND d.immeuble = i AND " +
						                            "            (d.dateDebutMetier is null OR d.dateDebutMetier <= :dateReference) AND " +
						                            "            (d.dateFinMetier is null OR d.dateFinMetier >= :dateReference)" +
						                            ") AND " +
						                            "NOT EXISTS (FROM ServitudeRF s " +
						                            "            JOIN s.charges charge" +
						                            "            WHERE s.annulationDate is null AND " +
						                            "            charge.annulationDate is null AND " +
						                            "            charge.immeuble = i AND " +
						                            "            (s.dateDebutMetier is null OR s.dateDebutMetier <= :dateReference) AND " +
						                            "            (s.dateFinMetier is null OR s.dateFinMetier >= :dateReference) AND" +
						                            "            (charge.dateDebut is null OR charge.dateDebut <= :dateReference) AND " +
						                            "            (charge.dateFin is null OR charge.dateFin >= :dateReference)" +
						                            ")");
			}
			else {
				query = session.createQuery("SELECT i.id FROM ImmeubleRF i LEFT JOIN i.situations si WHERE i.annulationDate is null AND " +
						                            "NOT EXISTS (FROM DroitProprieteRF d " +
						                            "            WHERE d.annulationDate is null AND d.immeuble = i AND " +
						                            "            (d.dateDebutMetier is null OR d.dateDebutMetier <= :dateReference) AND " +
						                            "            (d.dateFinMetier is null OR d.dateFinMetier >= :dateReference)" +
						                            ") AND " +
						                            "NOT EXISTS (FROM ServitudeRF s " +
						                            "            JOIN s.charges charge" +
						                            "            WHERE s.annulationDate is null AND " +
						                            "            charge.annulationDate is null AND " +
						                            "            charge.immeuble = i AND " +
						                            "            (s.dateDebutMetier is null OR s.dateDebutMetier <= :dateReference) AND " +
						                            "            (s.dateFinMetier is null OR s.dateFinMetier >= :dateReference) AND" +
						                            "            (charge.dateDebut is null OR charge.dateDebut <= :dateReference) AND " +
						                            "            (charge.dateFin is null OR charge.dateFin >= :dateReference)" +
						                            ") AND " +
				                                    "((si.noOfsCommuneSurchargee IS NULL AND si.commune.noOfs = :commune) OR si.noOfsCommuneSurchargee = :commune)");
				query.setParameter("commune", ofsCommune);
			}
			query.setParameter("dateReference", dateReference);
			//noinspection unchecked
			return new HashSet<Long>(query.list());
		}));
	}
}
