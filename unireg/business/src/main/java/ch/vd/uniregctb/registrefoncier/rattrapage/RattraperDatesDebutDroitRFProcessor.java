package ch.vd.uniregctb.registrefoncier.rattrapage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;

public class RattraperDatesDebutDroitRFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RattraperDatesDebutDroitRFProcessor.class);

	private final ImmeubleRFDAO immeubleRFDAO;
	private final PlatformTransactionManager transactionManager;
	private final RegistreFoncierService registreFoncierService;

	public RattraperDatesDebutDroitRFProcessor(@NotNull ImmeubleRFDAO immeubleRFDAO,
	                                           @NotNull PlatformTransactionManager transactionManager,
	                                           @NotNull RegistreFoncierService registreFoncierService) {
		this.immeubleRFDAO = immeubleRFDAO;
		this.transactionManager = transactionManager;
		this.registreFoncierService = registreFoncierService;
	}

	/**
	 * @param dataSelection le type d'immeuble à processer
	 * @param immeubleIds   les ids d'immeuble à processer (si dataSelection=EXPLICIT_SELECTION)
	 * @param nbThreads     le nombre de threads
	 * @param sm            un statut manager
	 * @return les résultats du processing
	 */
	public RattraperDatesDebutDroitRFProcessorResults process(@NotNull RattrapageDataSelection dataSelection,
	                                                          @Nullable List<Long> immeubleIds,
	                                                          int nbThreads,
	                                                          @Nullable StatusManager sm) {

		final StatusManager statusManager = (sm == null ? new LoggingStatusManager(LOGGER) : sm);

		// recherche des immeubles concernés
		final List<Long> ids;
		if (dataSelection == RattrapageDataSelection.EXPLICIT_SELECTION) {
			ids = immeubleIds;
		}
		else {
			ids = findImmeubleIdsToProcess();
		}

		final RattraperDatesDebutDroitRFProcessorResults rapportFinal = new RattraperDatesDebutDroitRFProcessorResults(dataSelection, nbThreads, immeubleRFDAO, registreFoncierService);

		// on traite chaque immeuble
		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, RattraperDatesDebutDroitRFProcessorResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(ids, 100, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, RattraperDatesDebutDroitRFProcessorResults>() {

			private final ThreadLocal<Long> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Long> immeubleIds, RattraperDatesDebutDroitRFProcessorResults rapport) throws Exception {
				first.set(immeubleIds.get(0));
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Processing immovables properties ids={}", Arrays.toString(immeubleIds.toArray()));
				}
				statusManager.setMessage("Traitement des immeubles...", monitor.getProgressInPercent());
				immeubleIds.forEach(id -> processImmeuble(id, rapport));
				return !statusManager.isInterrupted();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					final Long mutId = first.get();
					LOGGER.warn("Erreur pendant le traitement de l'immeuble n°" + mutId, e);
				}
			}

			@Override
			public RattraperDatesDebutDroitRFProcessorResults createSubRapport() {
				return new RattraperDatesDebutDroitRFProcessorResults(dataSelection, nbThreads, immeubleRFDAO, registreFoncierService);
			}
		}, monitor);

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Les droits fermés et ouverts <i>pour un immeuble particulier</i> suite à un import particulier des données RF.
	 */
	private static class ChangementsDroits {
		private final RegDate dateImport;
		private final List<DroitProprieteRF> fermes;
		private final List<DroitProprieteRF> ouverts;

		public ChangementsDroits(@NotNull RegDate dateImport, @NotNull List<DroitProprieteRF> droits) {
			final RegDate veilleImport = dateImport.getOneDayBefore();
			this.dateImport = dateImport;
			this.fermes = droits.stream()
					.filter(d -> d.getDateFin() == veilleImport)
					.collect(Collectors.toList());
			this.ouverts = droits.stream()
					.filter(d -> d.getDateDebut() == dateImport)
					.collect(Collectors.toList());
		}

		/**
		 * @return la date de l'import qui a provoqué la fermeture et l'ouverture des droits
		 */
		public RegDate getDateImport() {
			return dateImport;
		}

		/**
		 * Recalcule les dates de début des droits ouverts suite à l'improt.
		 *
		 * @param rapport le rapport d'exécution à mettre-à-jour
		 */
		public void recalculerDatesDebutDroitsOuverts(@NotNull RattraperDatesDebutDroitRFProcessorResults rapport) {
			ouverts.forEach(d -> recalculerDateDebut(d, rapport));
		}

		private void recalculerDateDebut(@NotNull DroitProprieteRF droit, @NotNull RattraperDatesDebutDroitRFProcessorResults rapport) {
			final RegDate dateDebutOriginale = droit.getDateDebutMetier();
			final String motifDebutOriginal = droit.getMotifDebut();

			// on force le recalcul de la date de début du droit
			droit.calculateDateEtMotifDebut(this::findDroitPrecedent);

			final RegDate dateDebutCourant = droit.getDateDebutMetier();
			final String motifDebutCourant = droit.getMotifDebut();

			if (dateDebutCourant != dateDebutOriginale || !Objects.equals(motifDebutCourant, motifDebutOriginal)) {
				// le droit a été mis-à-jour
				rapport.addUpdated(droit, dateDebutOriginale, motifDebutOriginal);
			}
			else {
				rapport.addUntouched(droit);
			}
		}

		@Nullable
		private DroitProprieteRF findDroitPrecedent(@NotNull DroitProprieteRF courant) {

			// 1. on recherche le droit précédent par masterId (SIFISC-24987)
			DroitProprieteRF droitPrecedent = fermes.stream()
					.filter(d -> d.getMasterIdRF().equals(courant.getMasterIdRF()))
					.max(new DateRangeComparator<>())
					.orElse(null);
			if (droitPrecedent != null) {
				return droitPrecedent;
			}

			// 2. on recherche le droit précédent par propriétaire (SIFISC-25971)
			droitPrecedent = fermes.stream()
					.filter(d -> d.getAyantDroit().getId().equals(courant.getAyantDroit().getId()))
					.max(new DateRangeComparator<>())
					.orElse(null);
			if (droitPrecedent != null) {
				return droitPrecedent;
			}

			// pas trouvé
			return null;
		}
	}

	void processImmeuble(long id, @NotNull RattraperDatesDebutDroitRFProcessorResults rapport) {
		final ImmeubleRF immeuble = immeubleRFDAO.get(id);
		if (immeuble == null) {
			throw new ObjectNotFoundException("L'immeuble avec l'id=[" + id + "] n'existe pas");
		}

		rapport.addProcessed(immeuble);

		// on va chercher les droits à analyser (on ignore les droits annulés, comme il se doit)
		final List<DroitProprieteRF> droits = immeuble.getDroitsPropriete().stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toList());

		// on déduit les dates d'import où les droits ont été modifiés à partir de leurs dates de début/fin
		final Set<RegDate> datesImport = droits.stream()
				.map(RattraperDatesDebutDroitRFProcessor::extractDatesImport)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		// on analyse les droits et construit la liste des changements pour chaque date d'import
		final List<ChangementsDroits> changements = datesImport.stream()
				.map(d -> new ChangementsDroits(d, droits))
				.collect(Collectors.toList());
		changements.sort(Comparator.comparing(ChangementsDroits::getDateImport));

		// on recalcule et rattrape si nécessaire les dates de début sur les droits ouverts
		changements.forEach(c -> c.recalculerDatesDebutDroitsOuverts(rapport));
	}

	/**
	 * @param droit un droit de propriété
	 * @return les dates d'import où le droit a été ouvert ou fermé.
	 */
	private static Collection<RegDate> extractDatesImport(@NotNull DroitProprieteRF droit) {
		final RegDate dateDebut = droit.getDateDebut();
		final RegDate dateFin = droit.getDateFin();

		if (dateDebut != null && dateFin != null) {
			return Arrays.asList(dateDebut, dateFin.getOneDayAfter());
		}
		else if (dateDebut != null) {
			return Collections.singletonList(dateDebut);
		}
		else if (dateFin != null) {
			// les droits sont fermés à la veille de la date d'import, il faut donc aller chercher le lendemeain pour retomber sur ses pattes
			return Collections.singletonList(dateFin.getOneDayAfter());
		}
		else {
			return Collections.emptyList();
		}
	}

	private List<Long> findImmeubleIdsToProcess() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> immeubleRFDAO.findImmeubleIdsAvecDatesDeFinDroitsACalculer());
	}

}
