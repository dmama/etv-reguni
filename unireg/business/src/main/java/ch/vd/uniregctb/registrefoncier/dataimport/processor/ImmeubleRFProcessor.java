package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.converter.jaxp.StringSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.EstimationRFHelper;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.ImmeubleRFHelper;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.SituationRFHelper;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.SurfaceTotaleRFHelper;
import ch.vd.uniregctb.registrefoncier.key.CommuneRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

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
	private final ThreadLocal<Unmarshaller> unmarshaller;

	public ImmeubleRFProcessor(@NotNull CommuneRFDAO communeRFDAO, @NotNull ImmeubleRFDAO immeubleRFDAO, @NotNull XmlHelperRF xmlHelperRF) {
		this.communeRFDAO = communeRFDAO;
		this.immeubleRFDAO = immeubleRFDAO;
		this.unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getImmeubleContext().createUnmarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void process(@NotNull EvenementRFMutation mutation, @Nullable MutationsRFProcessorResults rapport) {

		if (mutation.getEtat() == EtatEvenementRF.TRAITE || mutation.getEtat() == EtatEvenementRF.FORCE) {
			throw new IllegalArgumentException("La mutation n°" + mutation.getId() + " est déjà traitée (état=[" + mutation.getEtat() + "]).");
		}

		final RegDate dateValeur = mutation.getParentImport().getDateEvenement();

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

		// on l'insère en DB
		switch (mutation.getTypeMutation()) {
		case CREATION:
			processCreation(dateValeur, immeuble);
			break;
		case MODIFICATION:
			processModification(dateValeur, immeuble);
			break;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + mutation.getTypeMutation() + "]");
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

	private void processCreation(RegDate dateValeur, @NotNull ImmeubleRF newImmeuble) {

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

		immeubleRFDAO.save(newImmeuble);
	}

	private void processModification(RegDate dateValeur, @NotNull ImmeubleRF newImmeuble) {

		final String idRF = newImmeuble.getIdRF();

		final ImmeubleRF persisted = immeubleRFDAO.find(new ImmeubleRFKey(newImmeuble));
		if (persisted == null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] n'existe pas dans la DB.");
		}

		// on va chercher les situations, estimations et surfaces totales courantes
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

		// on va chercher les nouvelles situations et estimations
		final SituationRF newSituation = CollectionsUtils.getFirst(newImmeuble.getSituations());     // par définition, le nouvel immeuble ne contient que l'état courant,
		if (newSituation == null) {                                                                  // il ne contient donc qu'un seul élément de chaque collection
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] ne contient pas de situation.");
		}
		final EstimationRF newEstimation = CollectionsUtils.getFirst(newImmeuble.getEstimations());
		final SurfaceTotaleRF newSurfaceTotale = CollectionsUtils.getFirst(newImmeuble.getSurfacesTotales());

		// est-ce que la situation a changé ?
		if (!SituationRFHelper.dataEquals(persistedSituation, newSituation)) {
			// on ferme l'ancienne situation et on ajoute la nouvelle
			persistedSituation.setDateFin(dateValeur.getOneDayBefore());
			newSituation.setDateDebut(dateValeur);
			persisted.getSituations().add(newSituation);
		}

		// est-ce que l'estimation changé ?
		if (!EstimationRFHelper.dataEquals(persistedEstimation, newEstimation)) {
			// on ferme l'ancienne estimation et on ajoute la nouvelle
			if (persistedEstimation != null) {
				persistedEstimation.setDateFin(dateValeur.getOneDayBefore());
			}
			if (newEstimation != null) {
				newEstimation.setDateDebut(dateValeur);
				persisted.getEstimations().add(newEstimation);
			}
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
		}
	}
}
