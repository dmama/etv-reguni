package ch.vd.uniregctb.foncier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalException;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class EnvoiFormulairesDemandeDegrevementICIProcessor {

	private static final int BATCH_SIZE = 10;
	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiFormulairesDemandeDegrevementICIProcessor.class);

	private final PlatformTransactionManager transactionManager;
	private final AutreDocumentFiscalService autreDocumentFiscalService;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;

	public EnvoiFormulairesDemandeDegrevementICIProcessor(PlatformTransactionManager transactionManager, AutreDocumentFiscalService autreDocumentFiscalService, HibernateTemplate hibernateTemplate, TiersService tiersService) {
		this.transactionManager = transactionManager;
		this.autreDocumentFiscalService = autreDocumentFiscalService;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
	}

	public EnvoiFormulairesDemandeDegrevementICIResults run(final int nbThreads, @Nullable final Integer nbMaxEnvois, final RegDate dateTraitement, StatusManager statusManager) {

		final StatusManager status = Optional.ofNullable(statusManager).orElseGet(() -> new LoggingStatusManager(LOGGER));

		// recherche des couples contribuable/immeuble à inspecter (on les classe contribuable par contribuable pour ne pas risquer d'envoyer des
		// formulaires sur le même contribuable dans deux threads de traitement séparés)
		status.setMessage("Récupération des couples contribuable/immeuble à inspecter...");
		final List<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable> couples = findCouples(dateTraitement);

		final EnvoiFormulairesDemandeDegrevementICIResults rapportFinal = new EnvoiFormulairesDemandeDegrevementICIResults(nbThreads, nbMaxEnvois, dateTraitement);
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
				return !status.interrupted() && (localMaxEnvois == null || localMaxEnvois > 0) && (nbMaxEnvois == null || envoyesSansMoi + rapport.getEnvois().size() < nbMaxEnvois);
			}

			@Override
			public EnvoiFormulairesDemandeDegrevementICIResults createSubRapport() {
				return new EnvoiFormulairesDemandeDegrevementICIResults(nbThreads, nbMaxEnvois, dateTraitement);
			}
		}, progressMonitor);

		rapportFinal.setInterrupted(status.interrupted());
		rapportFinal.end();
		status.setMessage("Traitement terminé");
		return rapportFinal;
	}

	private void traiterContribuable(long noContribuable,
	                                 List<EnvoiFormulairesDemandeDegrevementICIResults.DroitImmeuble> idsDroitImmeuble,
	                                 EnvoiFormulairesDemandeDegrevementICIResults rapport,
	                                 @Nullable Integer localMaxEnvois) throws AutreDocumentFiscalException {

		final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, noContribuable);

		// si le contribuable est complètement exonéré (d'après le régime fiscal), on n'a rien à faire
		if (isExonereTotalement(entreprise, rapport.dateTraitement)) {
			rapport.addContribuableTotalementExonere(entreprise, idsDroitImmeuble);
			return;
		}

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
			final ImmeubleRF immeuble = droit.getImmeuble();

			// s'il s'agit d'un droit qui n'est pas un droit de propriété, on l'ignore
			if (!(droit instanceof DroitProprieteRF)) {
				rapport.addDroitNonPropriete(entreprise, immeuble, droit.getClass());
				continue;
			}

			// quelle est l'estimation fiscale courante ?
			// si on trie les estimations fiscales par leur "année de référence", alors la "courante"
			// est celle qui correspond à l'année la plus grande inférieure ou égale à l'année de la date de traitement
			final NavigableMap<Integer, EstimationRF> sortedEstimations = immeuble.getEstimations().stream()
					.filter(AnnulableHelper::nonAnnule)
					.map(estim -> Pair.of(getAnneeReferenceEstimationFiscale(estim), estim))
					.filter(pair -> pair.getKey().isPresent())
					.collect(Collectors.toMap(pair -> pair.getKey().get(),
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

			// toutes les demandes de dégrèvement déjà envoyées pour cet immeuble
			final List<DemandeDegrevementICI> demandes = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, false).stream()
					.filter(demande -> demande.getImmeuble() == immeuble)
					.collect(Collectors.toList());

			final Integer anneeSuivantDebutDroit = Stream.of(droit.getDateDebutOfficielle(), droit.getDateDebut())
					.filter(Objects::nonNull)
					.findFirst()
					.map(date -> date.year() + 1)
					.orElse(null);
			if (anneeSuivantDebutDroit != null) {
				final DateRange rangeAnneeSuivantDebutDroit = new DateRangeHelper.Range(RegDate.get(anneeSuivantDebutDroit, 1, 1), RegDate.get(anneeSuivantDebutDroit, 12, 31));

				// recherche de dégrèvement actif sur l'année suivant le début de droit
				final boolean hasDegrevementActifAnneeSuivantDebutDroit = degrevements.getOrDefault(immeuble.getId(), Collections.emptyList()).stream()
						.anyMatch(deg -> DateRangeHelper.intersect(rangeAnneeSuivantDebutDroit, deg));
				if (hasDegrevementActifAnneeSuivantDebutDroit) {
					rapport.addDegrevementActifAnneeSuivantDebutDroit(entreprise, anneeSuivantDebutDroit, immeuble);
					continue;
				}

				// recherche de demande de dégrèvement dont la PF est l'année suivante le début du droit
				final DemandeDegrevementICI demandeAnneeSuivantDebutDroit = demandes.stream()
						.filter(demande -> demande.getPeriodeFiscale() != null && demande.getPeriodeFiscale().intValue() == anneeSuivantDebutDroit.intValue())
						.findFirst()
						.orElse(null);
				if (demandeAnneeSuivantDebutDroit != null) {
					rapport.addDemandeDegrevementPourAnneeSuivantDebutDroit(entreprise, anneeSuivantDebutDroit, demandeAnneeSuivantDebutDroit);
					continue;
				}
			}

			// recherche d'une demande de dégrèvement dont la PF est l'année suivant le début de l'estimation fiscale en cours
			// (la date de début est justement le début de l'année qui suit l'estimation fiscale, non ?)
			final Optional<Integer> anneeDebutValiditeDerniereEstimationFiscale = getAnneeDebutValiditeEstimationFiscale(estimationCourante);
			if (anneeDebutValiditeDerniereEstimationFiscale.isPresent()) {
				final DemandeDegrevementICI demandePourEstimationFiscale = demandes.stream()
						.filter(demande -> demande.getPeriodeFiscale() != null && demande.getPeriodeFiscale().intValue() == anneeDebutValiditeDerniereEstimationFiscale.get())
						.findFirst()
						.orElse(null);
				if (demandePourEstimationFiscale != null) {
					rapport.addDemandeDegrevementPourAnneeEstimationFiscale(entreprise, anneeDebutValiditeDerniereEstimationFiscale.get(), demandePourEstimationFiscale);
					continue;
				}
			}

			// calcul de la période fiscale pour envoi du document
			final Integer periodeFiscale = Stream.of(anneeSuivantDebutDroit, anneeDebutValiditeDerniereEstimationFiscale.orElse(null), rapport.dateTraitement.year() + 1)
					.filter(Objects::nonNull)
					.max(Comparator.naturalOrder())
					.orElse(null);
			if (periodeFiscale == null) {
				rapport.addErreurPeriodeFiscaleNonDeterminable(entreprise, immeuble);
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
	 * @return la plus petite estimation fiscale de la map qui a le même montant que la plus grande (dans une série continue depuis la plus grande)
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

	private static final List<Pair<Pattern, ToIntFunction<Matcher>>> ANNEE_ESTIMATION_FISCALE_PATTERNS = buildPatternsAnneeEstimationFiscale();

	@NotNull
	private static List<Pair<Pattern, ToIntFunction<Matcher>>> buildPatternsAnneeEstimationFiscale() {
		final List<Pair<Pattern, ToIntFunction<Matcher>>> list = new ArrayList<>();
		list.add(Pair.of(Pattern.compile("(?:EF|RF|RG)\\s*(\\d{4})", Pattern.CASE_INSENSITIVE), EnvoiFormulairesDemandeDegrevementICIProcessor::groupOneToInt));
		list.add(Pair.of(Pattern.compile("(?:RF|RF|RG)\\s*(\\d{2})", Pattern.CASE_INSENSITIVE), EnvoiFormulairesDemandeDegrevementICIProcessor::groupOneToIntPlusSiecle));
		list.add(Pair.of(Pattern.compile("(\\d{4})"), EnvoiFormulairesDemandeDegrevementICIProcessor::groupOneToInt));
		list.add(Pair.of(Pattern.compile("\\d{1,2}\\.\\d{1,2}\\.(\\d{4})"), EnvoiFormulairesDemandeDegrevementICIProcessor::groupOneToInt));
		list.add(Pair.of(Pattern.compile("(\\d{4})\\s*(?:RF|RG|RP|rév\\.|T\\.|enrévision)", Pattern.CASE_INSENSITIVE), EnvoiFormulairesDemandeDegrevementICIProcessor::groupOneToInt));
		return Collections.unmodifiableList(list);
	}

	private static int groupOneToInt(Matcher matcher) {
		return Integer.valueOf(matcher.group(1));
	}

	private static int groupOneToIntPlusSiecle(Matcher matcher) {
		final int anneeSansSiecle = groupOneToInt(matcher);
		return anneeSansSiecle + (RegDate.get().year() / 100) * 100 - (anneeSansSiecle > 50 ? 100 : 0);
	}

	@NotNull
	private static Optional<Integer> getAnneeReferenceEstimationFiscale(EstimationRF estimation) {
		// on cherche d'abord dans la référence, et si on ne trouve rien d'interprétable, on se rabat sur la date d'estimation
		// (et on ajoute 1 à l'année)
		final String reference = StringUtils.trimToNull(estimation.getReference());
		if (reference != null) {
			final Optional<Integer> fromReference = ANNEE_ESTIMATION_FISCALE_PATTERNS.stream()
					.map(pair -> Pair.of(pair.getKey().matcher(reference), pair.getValue()))
					.filter(pair -> pair.getKey().matches())
					.map(pair -> pair.getValue().applyAsInt(pair.getKey()))
					.findFirst();
			if (fromReference.isPresent()) {
				return fromReference;
			}
		}

		// si la date d'estimation fiscale est remplie, allons-y, sinon, tant pis
		return Optional.ofNullable(estimation.getDateInscription()).map(RegDate::year);
	}

	@NotNull
	static Optional<Integer> getAnneeDebutValiditeEstimationFiscale(EstimationRF estimation) {
		// on cherche d'abord dans la référence, et si on ne trouve rien d'interprétable, on se rabat sur la date d'estimation
		// (et on ajoute 1 à l'année)
		return getAnneeReferenceEstimationFiscale(estimation).map(annee -> annee + 1);
	}

	private boolean isExonereTotalement(Entreprise entreprise, RegDate dateReference) {
		// TODO dans un premier temps, les régimes fiscaux concernés sont en dur... plus tard, c'est FiDoR qui fournira l'information

		// exonération selon régimes fiscaux
		final Set<String> regimesFiscauxExoneres = new HashSet<>(Arrays.asList("749", "759", "769"));
		final RegimeFiscal rf = DateRangeHelper.rangeAt(entreprise.getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee.VD), dateReference);
		if (rf != null && regimesFiscauxExoneres.contains(rf.getCode())) {
			return true;
		}

		// exonération selon forme juridique
		final Set<FormeLegale> formesLegalesExonerees = EnumSet.of(FormeLegale.N_0104_SOCIETE_EN_COMMANDITE, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF);
		final FormeLegaleHisto flh = DateRangeHelper.rangeAt(tiersService.getFormesLegales(entreprise, false), dateReference);
		return flh != null && formesLegalesExonerees.contains(flh.getFormeLegale());
	}

	/**
	 * Liste des identifiants d'immeuble associés à un contribuable, par contribuable, pour lesquels il existe des immeubles pour lesquels on pourrait devoir envoyer une demande de dégrèvement
	 * @param dateTraitement date de traitement
	 * @return une map dont la clé est un numéro de contribuable, et les valeurs les identifiants d'immeuble correspondants
	 */
	@NotNull
	List<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable> findCouples(RegDate dateTraitement) {

		final String hql = "SELECT DISTINCT RAPP.contribuable.id, DT.id, DT.immeuble.id"
				+ " FROM RapprochementRF AS RAPP"
				+ " JOIN RAPP.tiersRF.droits AS DT"
				+ " WHERE RAPP.contribuable.class = 'Entreprise'"
				+ " AND (RAPP.dateDebut IS NULL OR RAPP.dateDebut <= :dateTraitement)"
				+ " AND (RAPP.dateFin IS NULL OR RAPP.dateFin >= :dateTraitement)"
				+ " AND RAPP.annulationDate IS NULL"
				+ " AND DT.annulationDate IS NULL"
				+ " AND DT.dateDebutOfficielle <= :debutAnnee"
				+ " AND (DT.dateFin IS NULL OR DT.dateFin >= :debutAnnee)"
				+ " ORDER BY DT.immeuble.id, RAPP.contribuable.id";         // ordonné d'abord par immeuble pour que la TreeMap soit plus équilibrée (ordre d'entrée aléatoire sur la clé...)

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final SortedMap<Long, List<EnvoiFormulairesDemandeDegrevementICIResults.DroitImmeuble>> map = template.execute(status -> hibernateTemplate.execute(session -> {
			final Query query = session.createQuery(hql);
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
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
					                          TreeMap::new));
		}));

		return map.entrySet().stream()
				.map(entry -> new EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable(entry.getKey(), entry.getValue()))
				.collect(Collectors.toCollection(LinkedList::new));
	}
}
