package ch.vd.unireg.foncier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalException;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.ModeExoneration;
import ch.vd.unireg.interfaces.infra.data.PlageExonerationFiscale;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.ModeExonerationHisto;
import ch.vd.unireg.regimefiscal.RegimeFiscalConsolide;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.Entreprise;

public class EnvoiFormulairesDemandeDegrevementICIProcessor {

	private static final int BATCH_SIZE = 10;
	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiFormulairesDemandeDegrevementICIProcessor.class);

	private final PlatformTransactionManager transactionManager;
	private final AutreDocumentFiscalService autreDocumentFiscalService;
	private final HibernateTemplate hibernateTemplate;
	private final ParametreAppService parametreAppService;
	private final RegistreFoncierService registreFoncierService;
	private final RegimeFiscalService regimeFiscalService;

	public EnvoiFormulairesDemandeDegrevementICIProcessor(ParametreAppService parametreAppService, PlatformTransactionManager transactionManager,
	                                                      AutreDocumentFiscalService autreDocumentFiscalService, HibernateTemplate hibernateTemplate,
	                                                      RegistreFoncierService registreFoncierService, RegimeFiscalService regimeFiscalService) {
		this.transactionManager = transactionManager;
		this.autreDocumentFiscalService = autreDocumentFiscalService;
		this.hibernateTemplate = hibernateTemplate;
		this.parametreAppService = parametreAppService;
		this.registreFoncierService = registreFoncierService;
		this.regimeFiscalService = regimeFiscalService;
	}

	private RegDate getDateDebutPriseEnCompteMutationRF() {
		final Integer[] parts = parametreAppService.getDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI();
		return RegDate.get(parts[2], parts[1], parts[0]);
	}

	public EnvoiFormulairesDemandeDegrevementICIResults run(final int nbThreads, @Nullable final Integer nbMaxEnvois, final RegDate dateTraitement, StatusManager statusManager) {

		final StatusManager status = Optional.ofNullable(statusManager).orElseGet(() -> new LoggingStatusManager(LOGGER));

		// recherche des couples contribuable/immeuble à inspecter (on les classe contribuable par contribuable pour ne pas risquer d'envoyer des
		// formulaires sur le même contribuable dans deux threads de traitement séparés)
		status.setMessage("Récupération des couples contribuable/immeuble à inspecter...");
		final List<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable> couples = findCouples(dateTraitement);

		final RegDate dateSeuilMutationRF = getDateDebutPriseEnCompteMutationRF();
		final EnvoiFormulairesDemandeDegrevementICIResults rapportFinal = new EnvoiFormulairesDemandeDegrevementICIResults(nbThreads, nbMaxEnvois, dateTraitement, dateSeuilMutationRF, registreFoncierService);
		final ParallelBatchTransactionTemplateWithResults<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable, EnvoiFormulairesDemandeDegrevementICIResults> template
				= new ParallelBatchTransactionTemplateWithResults<>(couples,
				                                                    BATCH_SIZE,
				                                                    nbThreads,
				                                                    Behavior.REPRISE_AUTOMATIQUE,
				                                                    transactionManager,
				                                                    status,
				                                                    AuthenticationInterface.INSTANCE);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		template.execute(rapportFinal, new BatchWithResultsCallback<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable, EnvoiFormulairesDemandeDegrevementICIResults>() {
			@Override
			public boolean doInTransaction(List<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable> list, EnvoiFormulairesDemandeDegrevementICIResults rapport) throws Exception {
				final int envoyesSansMoi = rapportFinal.getEnvois().size();
				final Integer localMaxEnvois = Optional.ofNullable(nbMaxEnvois).map(max -> max - envoyesSansMoi).orElse(null);
				if (localMaxEnvois == null || localMaxEnvois > 0) {
					status.setMessage("Envoi des formulaires", progressMonitor.getProgressInPercent());
					for (EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable info : list) {
						traiterContribuable(info.getNoContribuable(), info.getIdsDroitsImmeubles(), rapport, localMaxEnvois);
					}
				}
				return !status.isInterrupted() && (localMaxEnvois == null || localMaxEnvois > 0) && (nbMaxEnvois == null || envoyesSansMoi + rapport.getEnvois().size() < nbMaxEnvois);
			}

			@Override
			public EnvoiFormulairesDemandeDegrevementICIResults createSubRapport() {
				return new EnvoiFormulairesDemandeDegrevementICIResults(nbThreads, nbMaxEnvois, dateTraitement, dateSeuilMutationRF, registreFoncierService);
			}
		}, progressMonitor);

		rapportFinal.setInterrupted(status.isInterrupted());
		rapportFinal.end();
		status.setMessage("Traitement terminé");
		return rapportFinal;
	}

	private static Optional<Integer> getAnneeDebutValidite(EstimationRF estimationRF) {
		return Optional.ofNullable(estimationRF.getDateDebutMetier()).map(RegDate::year);
	}

	private void traiterContribuable(long noContribuable,
	                                 List<EnvoiFormulairesDemandeDegrevementICIResults.DroitImmeuble> idsDroitImmeuble,
	                                 EnvoiFormulairesDemandeDegrevementICIResults rapport,
	                                 @Nullable Integer localMaxEnvois) throws AutreDocumentFiscalException {

		final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, noContribuable);

		// dégrèvements actifs, chargés une fois pour toute, indexés par identifiant d'immeuble
		final Map<Long, List<DegrevementICI>> degrevements = entreprise.getAllegementsFonciersNonAnnulesTries(DegrevementICI.class).stream()
				.collect(Collectors.toMap(degrevement -> degrevement.getImmeuble().getId(),
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		// le contribuable n'est pas complètement exonéré... il faut donc vérifier quels sont les formulaires à émettre...
		// et on peut boucler sur les immeubles
		for (EnvoiFormulairesDemandeDegrevementICIResults.DroitImmeuble idDroitImmeuble : idsDroitImmeuble) {

			// la règle dit qu'il n'y a pas d'envoi si...
			// - pas d'estimation fiscale sur l'immeuble (ou à zéro)
			// - il existe des données de dégrèvement pour ce contribuable et cet immeuble valable pour l'année suivant le début du droit
			// - il existe déjà une demande de dégrèvement non-annulée avec la PF égale à l'année suivant le début du droit
			// - il existe déjà une demande de dégrèvement non-annulée dont la PF est l'année suivant l'estimation fiscale en cours

			final DroitRF droit = hibernateTemplate.get(DroitRF.class, idDroitImmeuble.getIdDroit());

			// s'il s'agit d'un droit qui n'est pas un droit de propriété, on l'ignore
			if (!(droit instanceof DroitProprieteRF)) {
				rapport.addDroitNonPropriete(entreprise, droit.getImmeubleList(), droit.getClass());
				continue;
			}
			final ImmeubleRF immeuble = ((DroitProprieteRF)droit).getImmeuble();

			// quelle est l'estimation fiscale courante ?
			// si on trie les estimations fiscales par leur "année de référence", alors la "courante"
			// est celle qui correspond à l'année la plus grande inférieure ou égale à l'année de la date de traitement
			final NavigableMap<Integer, EstimationRF> sortedEstimations = immeuble.getEstimations().stream()
					.filter(AnnulableHelper::nonAnnule)
					.map(estim -> Pair.of(getAnneeDebutValidite(estim), estim))
					.filter(pair -> pair.getKey().isPresent())
					.map(pair -> Pair.of(pair.getKey().get(), pair.getValue()))
					.collect(Collectors.toMap(Pair::getKey,
					                          Pair::getValue,
					                          (e1, e2) -> Stream.of(e1, e2).max(Comparator.comparing(EstimationRF::getId)).get(),
					                          TreeMap::new));

			final NavigableMap<Integer, EstimationRF> pastEstimations = sortedEstimations.headMap(rapport.dateTraitement.year(), true);
			final EstimationRF estimationCourante = getFirstEstimationWithSameAmountAsLast(pastEstimations);

			// si pas d'estimation fiscale, ou estimation à zéro, pas de demande à faire...
			if (estimationCourante == null || estimationCourante.getMontant() == null || estimationCourante.getMontant() == 0L) {
				rapport.addImmeubleSansEstimationFiscale(entreprise, immeuble);
				continue;
			}

			// [SIFISC-23412] si la date de mutation est antérieure à la date seuil de mutation, alors on ne fait rien
			final Pair<String, RegDate> critereSeuil = Stream.of(Pair.of("Début du droit", droit.getDateDebutMetier()),
			                                                     Pair.of("Estimation fiscale", Stream.of(estimationCourante.getDateInscription(),
			                                                                                             estimationCourante.getAnneeReference() != null ? RegDate.get(estimationCourante.getAnneeReference(), 12, 31) : null)
					                                                     .filter(Objects::nonNull)
					                                                     .findFirst()
					                                                     .orElse(null)))
					.filter(pair -> pair.getValue() != null)
					.max(Comparator.comparing(Pair::getValue))
					.orElse(null);
			if (critereSeuil == null) {
				rapport.addDateDebutNonDeterminable(entreprise, immeuble);
				continue;
			}
			else if (critereSeuil.getRight().isBefore(rapport.dateSeuilMutationRF)) {
				rapport.addDateDebutAvantSeuilMutationRF(entreprise, immeuble, critereSeuil.getRight(), critereSeuil.getLeft());
				continue;
			}

			// toutes les demandes de dégrèvement déjà envoyées pour cet immeuble
			final List<DemandeDegrevementICI> demandesDejaEnvoyeesSurImmeuble = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, false).stream()
					.filter(demande -> demande.getImmeuble() == immeuble)
					.collect(Collectors.toList());

			final Optional<Integer> anneeSuivantDebutDroit = Stream.of(droit.getDateDebutMetier(), droit.getDateDebut())
					.filter(Objects::nonNull)
					.findFirst()
					.map(date -> date.year() + 1);
			if (anneeSuivantDebutDroit.isPresent()) {
				final int annee = anneeSuivantDebutDroit.get();
				final DateRange rangeAnneeSuivantDebutDroit = new DateRangeHelper.Range(RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));

				// recherche de dégrèvement actif sur l'année suivant le début de droit
				final boolean hasDegrevementActifAnneeSuivantDebutDroit = degrevements.getOrDefault(immeuble.getId(), Collections.emptyList()).stream()
						.anyMatch(deg -> DateRangeHelper.intersect(rangeAnneeSuivantDebutDroit, deg));
				if (hasDegrevementActifAnneeSuivantDebutDroit) {
					rapport.addDegrevementActifAnneeSuivantDebutDroit(entreprise, annee, immeuble);
					continue;
				}

				// recherche de demande de dégrèvement dont la PF est l'année suivante le début du droit
				final DemandeDegrevementICI demandeAnneeSuivantDebutDroit = demandesDejaEnvoyeesSurImmeuble.stream()
						.filter(demande -> demande.getPeriodeFiscale() != null && demande.getPeriodeFiscale() == annee)
						.findFirst()
						.orElse(null);
				if (demandeAnneeSuivantDebutDroit != null) {
					rapport.addDemandeDegrevementPourAnneeSuivantDebutDroit(entreprise, annee, demandeAnneeSuivantDebutDroit);
					continue;
				}
			}

			// recherche d'une demande de dégrèvement dont la PF est l'année suivant le début de l'estimation fiscale en cours
			// (la date de début est justement le début de l'année qui suit l'estimation fiscale, non ?)
			final Optional<Integer> anneeDebutPriseEnCompteDerniereEstimationFiscale = getAnneeDebutValidite(estimationCourante)
					.map(annee -> annee + 1);
			if (anneeDebutPriseEnCompteDerniereEstimationFiscale.isPresent()) {
				final DemandeDegrevementICI demandePourEstimationFiscale = demandesDejaEnvoyeesSurImmeuble.stream()
						.filter(demande -> demande.getPeriodeFiscale() != null && demande.getPeriodeFiscale().intValue() == anneeDebutPriseEnCompteDerniereEstimationFiscale.get())
						.findFirst()
						.orElse(null);
				if (demandePourEstimationFiscale != null) {
					rapport.addDemandeDegrevementPourAnneeEstimationFiscale(entreprise, anneeDebutPriseEnCompteDerniereEstimationFiscale.get(), demandePourEstimationFiscale);
					continue;
				}
			}

			// calcul de la période fiscale pour envoi du document
			final Integer periodeFiscaleCandidate = Stream.of(anneeSuivantDebutDroit, anneeDebutPriseEnCompteDerniereEstimationFiscale)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.max(Comparator.naturalOrder())
					.orElse(null);
			if (periodeFiscaleCandidate == null) {
				rapport.addErreurPeriodeFiscaleNonDeterminable(entreprise, immeuble);
				continue;
			}

			// [SIFISC-22867][SIFISC-23884] c'est seulement ici, une fois la PF connue, que l'on peut contrôler l'exonération totale du contribuable
			final MutableInt periodeFiscaleConfirmee = new MutableInt(periodeFiscaleCandidate);
			try {
				if (isExonereTotalement(entreprise, periodeFiscaleCandidate)) {

					// [SIFISC-23884] s'il est exonéré totalement pour la période fiscale donnée, mais que son exonération
					// est interrompue plus tard, il faut prévoir l'envoi d'une demande pour la PF qui suit l'année de la dernière
					// validité de l'exonération
					final List<ModeExonerationHisto> exonerations = regimeFiscalService.getExonerations(entreprise, GenreImpotExoneration.ICI);
					final ModeExonerationHisto exonerationValide = DateRangeHelper.rangeAt(exonerations, RegDate.get(periodeFiscaleCandidate, 1, 1));
					if (exonerationValide != null && exonerationValide.getDateFin() != null && exonerationValide.getDateFin().year() < rapport.dateTraitement.year() + 1) {
						// on propose une nouvelle date (au plus tard l'année suivant l'année de la date de traitement)
						periodeFiscaleConfirmee.setValue(exonerationValide.getDateFin().year() + 1);
					}
					else {
						rapport.addContribuableTotalementExonere(entreprise, immeuble, periodeFiscaleCandidate);
						continue;
					}
				}
			}
			catch (RegimeFiscalIndetermineException e) {
				rapport.addContribuableAvecRegimeFiscalIndetermine(entreprise, immeuble, periodeFiscaleCandidate);
				continue;
			}
			final int periodeFiscale = periodeFiscaleConfirmee.intValue();

			// [SIFISC-25066] Si le droit est clôturé avant le début de la période pour laquelle on veut envoyer un formulaire, ce n'est pas la peine de l'envoyer...
			if (droit.getDateFinMetier() != null && droit.getDateFinMetier().year() < periodeFiscale) {
				rapport.addDroitClotureAvantDebutPeriodeVisee(entreprise, immeuble, droit.getDateFinMetier(), periodeFiscale);
				continue;
			}

			// [SIFISC-23163] s'il y a un formulaire de demande qui a déjà été envoyé pour une PF entre les années suivant le début de droit et suivant la dernière estimation fiscale et l'année
			// prochaine (= année de la date de traitement + 1), alors on n'en renvoie pas de nouvelle, ça ne sert à rien
			final Optional<DemandeDegrevementICI> demandePourPfDejaEnvoyeeSuffisante = demandesDejaEnvoyeesSurImmeuble.stream()
					.filter(demande -> demande.getPeriodeFiscale() != null && demande.getPeriodeFiscale() <= periodeFiscale)
					.filter(demande -> demande.getPeriodeFiscale() >= Stream.of(anneeSuivantDebutDroit, anneeDebutPriseEnCompteDerniereEstimationFiscale)
							.filter(Optional::isPresent)
							.map(Optional::get)
							.max(Comparator.naturalOrder())
							.orElse(periodeFiscale))
					.max(Comparator.comparing(DemandeDegrevementICI::getPeriodeFiscale));
			if (demandePourPfDejaEnvoyeeSuffisante.isPresent()) {
				rapport.addDemandeDegrevementEnvoyeeDepuisDernierChangement(entreprise, demandePourPfDejaEnvoyeeSuffisante.get());
				continue;
			}

			// [SIFISC-23412] il ne faut pas envoyer de formulaire s'il existe un formulaire pour une PF ultérieure
			final Optional<DemandeDegrevementICI> demandeUlterieure = demandesDejaEnvoyeesSurImmeuble.stream()
					.filter(demande -> demande.getPeriodeFiscale() != null && demande.getPeriodeFiscale() >= periodeFiscale)
					.max(Comparator.comparing(DemandeDegrevementICI::getPeriodeFiscale));
			if (demandeUlterieure.isPresent()) {
				rapport.addDemandeDegrevementEnvoyeePourPeriodeUlterieureAPeriodeVisee(entreprise, demandeUlterieure.get(), periodeFiscale);
				continue;
			}

			// [SIFISC-23412] il ne faut pas envoyer de formulaire s'il existe un dégrèvement pour une PF ultérieure
			final Optional<DegrevementICI> degrevementUlterieur = degrevements.getOrDefault(immeuble.getId(), Collections.emptyList()).stream()
					.filter(deg -> deg.getDateDebut().year() >= periodeFiscale)
					.max(Comparator.comparingInt(deg -> deg.getDateDebut().year()));
			if (degrevementUlterieur.isPresent()) {
				rapport.addDegrevementUlterieurAPeriodeVisee(entreprise, degrevementUlterieur.get(), periodeFiscale);
				continue;
			}

			// tout est bon, on peut envoyer le document
			rapport.addEnvoiDemandeDegrevement(entreprise, immeuble, periodeFiscale);

			// envoi du document en mode batch
			autreDocumentFiscalService.envoyerDemandeDegrevementICIBatch(entreprise, immeuble, periodeFiscale, rapport.dateTraitement);

			// on arrête si on en a fait assez
			if (localMaxEnvois != null && rapport.getEnvois().size() >= localMaxEnvois) {
				break;
			}
		}
	}

	/**
	 * @param estimations une map triée (par année de référence) d'estimations fiscales
	 * @return la plus petite estimation fiscale de la map qui a le même montant que la plus récente (dans une série continue depuis la plus récente)
	 */
	static EstimationRF getFirstEstimationWithSameAmountAsLast(NavigableMap<Integer, EstimationRF> estimations) {
		if (estimations.isEmpty()) {
			return null;
		}

		final List<EstimationRF> triees = new ArrayList<>(estimations.values());
		final int size = triees.size();
		if (size == 1) {
			return triees.get(0);
		}

		EstimationRF estimationCandidate = triees.get(size - 1);
		final Long montant = estimationCandidate.getMontant();
		for (int cursor = size - 2 ; cursor >= 0 ; -- cursor) {
			final EstimationRF estimationCursor = triees.get(cursor);
			final Long montantCursor = estimationCursor.getMontant();
			if (!Objects.equals(montant, montantCursor)) {
				break;
			}
			estimationCandidate = estimationCursor;
		}
		return estimationCandidate;
	}

	private static class RegimeFiscalIndetermineException extends Exception {
	}

	private boolean isExonereTotalement(Entreprise entreprise, int periodeFiscale) throws RegimeFiscalIndetermineException {
		final RegDate dateReference = RegDate.get(periodeFiscale, 1, 1);        // premier janvier de la période fiscale considérée, car il me semble que c'est la date charnière pour l'ICI
		final RegimeFiscalConsolide rf = DateRangeHelper.rangeAt(regimeFiscalService.getRegimesFiscauxVDNonAnnulesTrie(entreprise), dateReference);
		if (rf == null) {
			// pas de régime fiscal... pas d'exonération
			return false;
		}
		if (rf.isIndetermine()) {
			throw new RegimeFiscalIndetermineException();
		}

		final PlageExonerationFiscale exoneration = rf.getExonerationICI(periodeFiscale);
		return exoneration != null && exoneration.getMode() == ModeExoneration.TOTALE;
	}

	/**
	 * Liste des identifiants d'immeuble associés à un contribuable, par contribuable, pour lesquels il existe des immeubles pour lesquels on pourrait devoir envoyer une demande de dégrèvement
	 * @param dateTraitement date de traitement
	 * @return une map dont la clé est un numéro de contribuable, et les valeurs les identifiants d'immeuble correspondants
	 */
	@NotNull
	List<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable> findCouples(RegDate dateTraitement) {

		// les droits de propriété pointent vers un immeuble
		final String hqlProps = "SELECT DISTINCT CTB.id, DT.id, DT.immeuble.id"
				+ " FROM RapprochementRF AS RAPP"
				+ " JOIN RAPP.tiersRF.droitsPropriete AS DT"
				+ " JOIN RAPP.contribuable AS CTB"
				+ " WHERE type(CTB) = Entreprise"
				+ " AND (RAPP.dateDebut IS NULL OR RAPP.dateDebut <= :dateTraitement)"
				+ " AND (RAPP.dateFin IS NULL OR RAPP.dateFin >= :dateTraitement)"
				+ " AND RAPP.annulationDate IS NULL"
				+ " AND DT.annulationDate IS NULL"
				+ " AND DT.dateDebutMetier <= :debutAnnee"
				+ " AND (DT.dateFin IS NULL OR DT.dateFin >= :debutAnnee)"
				+ " ORDER BY DT.immeuble.id, CTB.id";         // ordonné d'abord par immeuble pour que la TreeMap soit plus équilibrée (ordre d'entrée aléatoire sur la clé...)

		// les servitudes pointent vers plusieurs immeubles
		final String hqlServ = "SELECT DISTINCT CTB.id, SERV.id, IMM.id"
				+ " FROM RapprochementRF AS RAPP"
				+ " JOIN RAPP.tiersRF.beneficesServitudes AS BENE"
				+ " JOIN RAPP.contribuable AS CTB"
				+ " JOIN BENE.servitude AS SERV"
				+ " JOIN SERV.charges AS CHARG"
				+ " JOIN CHARG.immeuble AS IMM"
				+ " WHERE type(CTB) = Entreprise"
				+ " AND (RAPP.dateDebut IS NULL OR RAPP.dateDebut <= :dateTraitement)"
				+ " AND (RAPP.dateFin IS NULL OR RAPP.dateFin >= :dateTraitement)"
				+ " AND RAPP.annulationDate IS NULL"
				+ " AND SERV.annulationDate IS NULL"
				+ " AND SERV.dateDebutMetier <= :debutAnnee"
				+ " AND (SERV.dateFinMetier IS NULL OR SERV.dateFinMetier >= :debutAnnee)"
				+ " ORDER BY IMM.id, CTB.id";         // ordonné d'abord par immeuble pour que la TreeMap soit plus équilibrée (ordre d'entrée aléatoire sur la clé...)

		final SortedMap<Long, List<EnvoiFormulairesDemandeDegrevementICIResults.DroitImmeuble>> mapProps = executeFindInfoDroitsHql(dateTraitement, hqlProps);
		final SortedMap<Long, List<EnvoiFormulairesDemandeDegrevementICIResults.DroitImmeuble>> mapServ = executeFindInfoDroitsHql(dateTraitement, hqlServ);
		for (Map.Entry<Long, List<EnvoiFormulairesDemandeDegrevementICIResults.DroitImmeuble>> entry : mapServ.entrySet()) {
			mapProps.merge(entry.getKey(), entry.getValue(), ListUtils::union);
		}

		return mapProps.entrySet().stream()
				.map(entry -> new EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable(entry.getKey(), entry.getValue()))
				.collect(Collectors.toCollection(LinkedList::new));
	}

	private SortedMap<Long, List<EnvoiFormulairesDemandeDegrevementICIResults.DroitImmeuble>> executeFindInfoDroitsHql(RegDate dateTraitement, String hqlProps) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query query = session.createQuery(hqlProps);
			query.setParameter("dateTraitement", dateTraitement);
			query.setParameter("debutAnnee", RegDate.get(dateTraitement.year(), 1, 1));

			//noinspection unchecked
			final Iterator<Object[]> iterator = query.iterate();
			return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
					.map(row -> {
						final Long noContribuable = (Long) row[0];
						final Long idDroit = (Long) row[1];
						final Long idImmeuble = (Long) row[2];
						return Pair.of(noContribuable, new EnvoiFormulairesDemandeDegrevementICIResults.DroitImmeuble(idDroit, idImmeuble));
					})
					.collect(Collectors.toMap(Pair::getLeft,
					                          pair -> Collections.singletonList(pair.getRight()),
					                          ListUtils::union,
					                          TreeMap::new));
		}));
	}
}
