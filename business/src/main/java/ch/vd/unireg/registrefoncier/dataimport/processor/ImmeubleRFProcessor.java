package ch.vd.unireg.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.camel.converter.jaxp.StringSource;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleAvecQuotePartRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.QuotePartRF;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.SurfaceTotaleRF;
import ch.vd.unireg.registrefoncier.dao.CommuneRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.helper.EstimationRFHelper;
import ch.vd.unireg.registrefoncier.dataimport.helper.ImmeubleRFHelper;
import ch.vd.unireg.registrefoncier.dataimport.helper.QuotePartRFHelper;
import ch.vd.unireg.registrefoncier.dataimport.helper.SituationRFHelper;
import ch.vd.unireg.registrefoncier.dataimport.helper.SurfaceTotaleRFHelper;
import ch.vd.unireg.registrefoncier.key.CommuneRFKey;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;

/**
 * Processeur spécialisé pour traiter les mutations sur les immeubles.
 */
public class ImmeubleRFProcessor implements MutationRFProcessor {

	//private static final Logger LOGGER = LoggerFactory.getLogger(ImmeubleRFProcessor.class);

	@NotNull
	private final CommuneRFDAO communeRFDAO;

	@NotNull
	private final ImmeubleRFDAO immeubleRFDAO;

	@NotNull
	private final CommunauteRFProcessor communauteRFProcessor;

	@NotNull
	private final ThreadLocal<Unmarshaller> unmarshaller;

	@NotNull
	private final EvenementFiscalService evenementFiscalService;

	public ImmeubleRFProcessor(@NotNull CommuneRFDAO communeRFDAO,
	                           @NotNull ImmeubleRFDAO immeubleRFDAO,
	                           @NotNull CommunauteRFProcessor communauteRFProcessor,
	                           @NotNull XmlHelperRF xmlHelperRF,
	                           @NotNull EvenementFiscalService evenementFiscalService) {
		this.communeRFDAO = communeRFDAO;
		this.immeubleRFDAO = immeubleRFDAO;
		this.communauteRFProcessor = communauteRFProcessor;
		this.unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getImmeubleContext().createUnmarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		});
		this.evenementFiscalService = evenementFiscalService;
	}

	@Override
	public void process(@NotNull EvenementRFMutation mutation, boolean importInitial, @Nullable MutationsRFProcessorResults rapport) {

		if (mutation.getEtat() == EtatEvenementRF.TRAITE || mutation.getEtat() == EtatEvenementRF.FORCE) {
			throw new IllegalArgumentException("La mutation n°" + mutation.getId() + " est déjà traitée (état=[" + mutation.getEtat() + "]).");
		}

		final RegDate dateValeur = mutation.getParentImport().getDateEvenement();

		final TypeMutationRF typeMutation = mutation.getTypeMutation();
		if (typeMutation == TypeMutationRF.CREATION || typeMutation == TypeMutationRF.MODIFICATION) {
			// on interpète le XML
			final Grundstueck immeubleImport;
			try {
				final StringSource source = new StringSource(mutation.getXmlContent());
				immeubleImport = (Grundstueck) unmarshaller.get().unmarshal(source);
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}

			// on crée l'immeuble en mémoire
			final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(immeubleImport, this::findCommune);

			// on traite la mutation
			if (typeMutation == TypeMutationRF.CREATION) {
				processCreation(importInitial ? null : dateValeur, immeuble);
			}
			else {
				processModification(dateValeur, immeuble);
			}
		}
		else if (typeMutation == TypeMutationRF.SUPPRESSION) {
			processSuppression(dateValeur, mutation.getIdRF());
		}
		else {
			throw new IllegalArgumentException("Type de mutation inconnu = [" + typeMutation + "]");
		}


		// on renseigne le rapport
		if (rapport != null) {
			rapport.addProcessed(mutation.getId(), TypeEntiteRF.IMMEUBLE, mutation.getTypeMutation());
		}
	}

	@NotNull
	private CommuneRF findCommune(@NotNull Integer noRf) {
		final CommuneRF commune = communeRFDAO.findActive(new CommuneRFKey(noRf));
		if (commune == null) {
			throw new ObjectNotFoundException("La commune RF avec le noRF=[" + noRf + "] n'existe pas dans la base.");
		}
		return commune;
	}

	private void processCreation(@Nullable RegDate dateValeur, @NotNull ImmeubleRF newImmeuble) {

		// on va chercher les nouvelles situations et estimations
		final SituationRF newSituation = CollectionsUtils.getFirst(newImmeuble.getSituations());     // par définition, le nouvel immeuble ne contient que l'état courant,
		if (newSituation == null) {                                                                  // il ne contient donc qu'un seul élément de chaque collection
			throw new IllegalArgumentException("L'immeuble idRF=[" + newImmeuble.getIdRF() + "] ne contient pas de situation.");
		}
		final EstimationRF newEstimation = CollectionsUtils.getFirst(newImmeuble.getEstimations());
		final SurfaceTotaleRF surfaceTotale = CollectionsUtils.getFirst(newImmeuble.getSurfacesTotales());

		// on renseigne les dates de début
		newSituation.setDateDebut(dateValeur);
		if (newEstimation != null) {
			newEstimation.setDateDebut(dateValeur);
		}
		if (surfaceTotale != null) {
			surfaceTotale.setDateDebut(dateValeur);
		}

		// on sauve l'immeuble dans la DB
		newImmeuble = immeubleRFDAO.save(newImmeuble);

		// on publie l'événement fiscal correspondant
		evenementFiscalService.publierCreationImmeuble(dateValeur, newImmeuble);
	}

	private void processModification(@NotNull RegDate dateValeur, @NotNull ImmeubleRF newImmeuble) {

		final String idRF = newImmeuble.getIdRF();

		final ImmeubleRF persisted = immeubleRFDAO.find(new ImmeubleRFKey(newImmeuble), FlushMode.MANUAL);
		if (persisted == null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] n'existe pas dans la DB.");
		}
		final ImmeubleAvecQuotePartRF persistedAvecQuote = (persisted instanceof ImmeubleAvecQuotePartRF ? (ImmeubleAvecQuotePartRF) persisted : null);
		final ImmeubleAvecQuotePartRF newAvecQuote = (newImmeuble instanceof ImmeubleAvecQuotePartRF ? (ImmeubleAvecQuotePartRF) newImmeuble : null);

		final boolean reactivation;
		if (persisted.getDateRadiation() != null) {
			reactivation = true;
			// [SIFISC-24013] si l'immeuble est radié, on le réactive
			persisted.setDateRadiation(null);
			// on réactive aussi sa dernière situation
			persisted.getSituations().stream()
					.max(new DateRangeComparator<>())
					.ifPresent(s -> s.setDateFin(null));
			// on publie l'événement fiscal correspondant
			evenementFiscalService.publierReactivationImmeuble(dateValeur, persisted);
		}
		else {
			reactivation = false;
		}

		// on met-à-jour l'egrid, si nécessaire (SIFISC-25610)
		if (!Objects.equals(persisted.getEgrid(), newImmeuble.getEgrid())) {
			persisted.setEgrid(newImmeuble.getEgrid());
			if (!reactivation) {
				evenementFiscalService.publierModificationEgridImmeuble(dateValeur, persisted);
			}
		}

		// on va chercher les situations, estimations, surfaces totales courantes et quotes-parts
		final SituationRF persistedSituation = persisted.getSituations().stream()
				.filter(s -> s.isValidAt(null))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] ne contient pas de situation dans la DB."));

		final EstimationRF persistedEstimation = persisted.getEstimations().stream()
				.filter(s -> s.isValidAt(null))
				.findFirst()
				.orElse(null);

		final SurfaceTotaleRF persistedSurfaceTotale = persisted.getSurfacesTotales().stream()
				.filter(s -> s.isValidAt(null))
				.findFirst()
				.orElse(null);

		final QuotePartRF persistedQuotePart;
		if (persistedAvecQuote != null) {
			persistedQuotePart = persistedAvecQuote.getQuotesParts().stream()
					.filter(q -> q.isValidAt(null))
					.findFirst()
					.orElse(null);
		}
		else {
			persistedQuotePart = null;
		}

		// on va chercher les nouvelles situations, estimations et quotes-parts
		final SituationRF newSituation = CollectionsUtils.getFirst(newImmeuble.getSituations());     // par définition, le nouvel immeuble ne contient que l'état courant,
		if (newSituation == null) {                                                                  // il ne contient donc qu'un seul élément de chaque collection
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] ne contient pas de situation.");
		}
		final EstimationRF newEstimation = CollectionsUtils.getFirst(newImmeuble.getEstimations());
		final SurfaceTotaleRF newSurfaceTotale = CollectionsUtils.getFirst(newImmeuble.getSurfacesTotales());
		final QuotePartRF newQuotePart = (newAvecQuote == null ? null : CollectionsUtils.getFirst(newAvecQuote.getQuotesParts()));

		// est-ce que la situation a changé ?
		if (!SituationRFHelper.dataEquals(persistedSituation, newSituation)) {
			// on ferme l'ancienne situation et on ajoute la nouvelle
			persistedSituation.setDateFin(dateValeur.getOneDayBefore());
			newSituation.setDateDebut(dateValeur);
			persisted.addSituation(newSituation);
			// on publie l'événement fiscal correspondant
			if (!reactivation) {
				evenementFiscalService.publierModificationSituationImmeuble(dateValeur, persisted);
			}
		}

		// est-ce que l'estimation changé ?
		if (!EstimationRFHelper.dataEquals(persistedEstimation, newEstimation, false)) {
			if (EstimationRFHelper.dataEquals(persistedEstimation, newEstimation, true)) {
				// [SIFISC-22995] le seul changement est le flag 'en révision' => on considère qu'il s'agit
				// d'une correction et on met-à-jour l'estimation actuellement persistée
				persistedEstimation.setEnRevision(newEstimation.isEnRevision());
				// on publie l'événement fiscal correspondant
				if (!reactivation) {
					evenementFiscalService.publierChangementEnRevisionEstimationFiscalImmeuble(dateValeur, persistedEstimation);
				}
			}
			else if (persistedEstimation != null && newEstimation != null && persistedEstimation.getDateDebutMetier() == newEstimation.getDateDebutMetier()) {
				// [SIFISC-22995] la nouvelle estimation est valable à partir de la même période que l'ancienne, il s'agit
				// vraisemblablement d'une correction => on annule l'estimation actuellement persistée
				persistedEstimation.setAnnule(true);
				newEstimation.setDateDebut(dateValeur);
				persisted.addEstimation(newEstimation);
				// on publie les événements fiscaux correspondants
				evenementFiscalService.publierAnnulationEstimationFiscalImmeuble(dateValeur, persistedEstimation);
				evenementFiscalService.publierDebutEstimationFiscalImmeuble(newEstimation.getDateDebutMetier(), newEstimation);
			}
			else {
				// on ferme l'ancienne estimation et on ajoute la nouvelle
				if (persistedEstimation != null) {
					persistedEstimation.setDateFin(dateValeur.getOneDayBefore());
				}
				if (newEstimation != null) {
					newEstimation.setDateDebut(dateValeur);
					persisted.addEstimation(newEstimation);
					// on publie l'événement fiscal correspondant
					if (!reactivation) {
						evenementFiscalService.publierDebutEstimationFiscalImmeuble(newEstimation.getDateDebutMetier(), newEstimation);
					}
				}
			}
			final Consumer<EstimationRF> fermetureListener = (reactivation ? null : e -> evenementFiscalService.publierFinEstimationFiscalImmeuble(e.getDateFinMetier(), e));
			final Consumer<EstimationRF> annulationListener = (reactivation ? null : e -> evenementFiscalService.publierAnnulationEstimationFiscalImmeuble(dateValeur, e));
			EstimationRFHelper.determineDatesFinMetier(persisted.getEstimations(),  // SIFISC-22995
			                                           fermetureListener,           // fermetureListener
			                                           annulationListener);         // annulationListener
		}

		// est-ce que la surface totale changé ?
		if (!SurfaceTotaleRFHelper.dataEquals(persistedSurfaceTotale, newSurfaceTotale)) {
			// on ferme l'ancienne surface et on ajoute la nouvelle
			if (persistedSurfaceTotale != null) {
				persistedSurfaceTotale.setDateFin(dateValeur.getOneDayBefore());
			}
			if (newSurfaceTotale != null) {
				newSurfaceTotale.setDateDebut(dateValeur);
				persisted.getSurfacesTotales().add(newSurfaceTotale);
			}
			// on publie l'événement fiscal correspondant
			if (!reactivation) {
				evenementFiscalService.publierModificationSurfaceTotaleImmeuble(dateValeur, persisted);
			}
		}

		// est-ce que la quote-part a changé ?
		if (!QuotePartRFHelper.dataEquals(persistedQuotePart, newQuotePart)) {
			if (persistedAvecQuote == null) {
				throw new ProgrammingException();
			}
			// on ferme l'ancienne quote-part et on ajoute la nouvelle
			if (persistedQuotePart != null) {
				persistedQuotePart.setDateFin(dateValeur.getOneDayBefore());
			}
			if (newQuotePart != null) {
				newQuotePart.setDateDebut(dateValeur);
				persistedAvecQuote.addQuotePart(newQuotePart);
			}
			// on publie l'événement fiscal correspondant
			if (!reactivation) {
				evenementFiscalService.publierModificationQuotePartImmeuble(dateValeur, persisted);
			}
		}
	}

	private void processSuppression(@NotNull RegDate dateValeur, @NotNull String idRF) {

		final ImmeubleRF persisted = immeubleRFDAO.find(new ImmeubleRFKey(idRF), FlushMode.MANUAL);
		if (persisted == null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] n'existe pas dans la DB.");
		}
		if (persisted.getDateRadiation() != null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] est déjà radié à la date du [" + RegDateHelper.dateToDisplayString(persisted.getDateRadiation()) + "].");
		}

		final RegDate dateRadiation = dateValeur.getOneDayBefore();

		// on ferme les situation, estimation fiscale, surface totale et quote-part courantes (note : les implantations ne sont pas possédées par l'immeuble, on ne les touche pas)
		persisted.getSituations().stream()
				.filter(s -> s.getDateFin() == null)
				.forEach(s -> s.setDateFin(dateRadiation));
		persisted.getEstimations().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(e -> e.getDateFin() == null)
				.forEach(e -> {
					e.setDateFin(dateRadiation);
					e.setDateFinMetier(dateRadiation);
				});
		persisted.getSurfacesTotales().stream()
				.filter(s -> s.getDateFin() == null)
				.forEach(s -> s.setDateFin(dateRadiation));

		// on radie l'immeuble
		persisted.setDateRadiation(dateRadiation);

		// [SIFISC-24968] on assigne aussi la date de fin métier sur les droits pointés sur cet immeuble, car le batch de calcul
		// des dates de fin métier des droits ne fonctionne qu'en cas de vente/achat de droits.
		persisted.getDroitsPropriete().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(d -> d.getDateFinMetier() == null)
				.forEach(d -> fermerDroit(d, dateRadiation, "Radiation"));

		// [SIFISC-26635] on assigne la date de fin métier sur les servitudes si tous les immeubles sont radiés (une servitude peut pointer vers plusieurs immeubles)
		persisted.getServitudes().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(s -> s.getDateFinMetier() == null)
				.filter(ImmeubleRFProcessor::isAllImmeublesRadies)
				.forEach(d -> fermerDroit(d, dateRadiation, "Radiation"));

		// [SIFISC-24595] on recalcule les éventuelles communautés
		communauteRFProcessor.processAll(persisted);

		if (persisted instanceof ImmeubleAvecQuotePartRF) {
			final ImmeubleAvecQuotePartRF persistedAvecQuote = (ImmeubleAvecQuotePartRF) persisted;
			persistedAvecQuote.getQuotesParts().stream()
					.filter(s -> s.getDateFin() == null)
					.forEach(s -> s.setDateFin(dateRadiation));
		}

		// on publie l'événement fiscal correspondant
		evenementFiscalService.publierRadiationImmeuble(dateValeur, persisted);
	}

	/**
	 * @return <i>vrai</i> si tous les immeubles de la servitude sont radiés; <i>faux</i> autrement.
	 */
	private static boolean isAllImmeublesRadies(ServitudeRF s) {
		return s.getImmeubles().stream()
				.filter(AnnulableHelper::nonAnnule)
				.allMatch(i -> i.getDateRadiation() != null);
	}

	private void fermerDroit(DroitRF d, RegDate dateFinMetier, String motifFin) {
		if (d.getDateFin() == null) {   // normalement, la date de fin technique est déjà renseignée par le processeur de mutation des droits, mais on ne prend pas de risque
			d.setDateFin(dateFinMetier);
		}
		d.setDateFinMetier(dateFinMetier);
		d.setMotifFin(motifFin);
	}
}
