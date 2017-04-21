package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.camel.converter.jaxp.StringSource;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.principal.PersonEigentumAnteilListElement;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.RaisonAcquisitionRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

import static ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper.masterIdAndVersionIdEquals;

/**
 * Processeur spécialisé pour traiter les mutations des droits de propriété.
 */
public class DroitRFProcessor implements MutationRFProcessor {

	@NotNull
	private final AyantDroitRFDAO ayantDroitRFDAO;

	@NotNull
	private final ImmeubleRFDAO immeubleRFDAO;

	@NotNull
	private final DroitRFDAO droitRFDAO;

	@NotNull
	private final ThreadLocal<Unmarshaller> unmarshaller;

	public DroitRFProcessor(@NotNull AyantDroitRFDAO ayantDroitRFDAO, @NotNull ImmeubleRFDAO immeubleRFDAO, @NotNull DroitRFDAO droitRFDAO, @NotNull XmlHelperRF xmlHelperRF) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.immeubleRFDAO = immeubleRFDAO;
		this.droitRFDAO = droitRFDAO;

		unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getDroitListContext().createUnmarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void process(@NotNull EvenementRFMutation mutation, boolean importInitial, @Nullable MutationsRFProcessorResults rapport) {

		if (mutation.getEtat() == EtatEvenementRF.TRAITE || mutation.getEtat() == EtatEvenementRF.FORCE) {
			throw new IllegalArgumentException("La mutation n°" + mutation.getId() + " est déjà traitée (état=[" + mutation.getEtat() + "]).");
		}

		final RegDate dateValeur = mutation.getParentImport().getDateEvenement();

		final String ayantDroitIdRF = mutation.getIdRF();
		final AyantDroitRF ayantDroit = ayantDroitRFDAO.find(new AyantDroitRFKey(ayantDroitIdRF), FlushMode.MANUAL);
		if (ayantDroit == null) {
			throw new IllegalArgumentException("L'ayant-droit avec l'idRF=[" + ayantDroitIdRF + "] n'existe pas.");
		}

		// on interpète le XML
		final List<EigentumAnteil> droitList;
		try {
			final String content = mutation.getXmlContent();
			if (content == null) {
				droitList = Collections.emptyList();
			}
			else {
				final StringSource source = new StringSource(content);
				final PersonEigentumAnteilListElement droitListImport = (PersonEigentumAnteilListElement) unmarshaller.get().unmarshal(source);
				droitList = droitListImport.getPersonEigentumAnteilOrGrundstueckEigentumAnteilOrHerrenlosEigentum();
			}
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}

		// on crée les droits en mémoire
		final List<DroitProprieteRF> droits = droitList.stream()
				.map(e -> DroitRFHelper.newDroitRF(e, idRef -> ayantDroit, this::findCommunaute, this::findImmeuble))
				.collect(Collectors.toList());

		// on les insère en DB
		switch (mutation.getTypeMutation()) {
		case CREATION:
			processCreation(importInitial ? null : dateValeur, ayantDroit, droits);
			break;
		case MODIFICATION:
			processModification(dateValeur, ayantDroit, droits);
			break;
		case SUPPRESSION:
			processSuppression(dateValeur, ayantDroit);
			break;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + mutation.getTypeMutation() + "]");
		}

		// on renseigne le rapport
		if (rapport != null) {
			rapport.addProcessed(mutation.getId(), TypeEntiteRF.DROIT, mutation.getTypeMutation());
		}
	}

	@NotNull
	private ImmeubleRF findImmeuble(@NotNull String idRf) {
		final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRf), FlushMode.MANUAL);
		if (immeuble == null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRf + "] n'existe pas dans la DB.");
		}
		if (immeuble.getDateRadiation() != null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRf + "] est radié, il ne devrait plus changer.");
		}
		return immeuble;
	}

	@Nullable
	private CommunauteRF findCommunaute(@Nullable String idRf) {
		if (idRf == null) {
			return null;
		}
		final CommunauteRF communaute = (CommunauteRF) ayantDroitRFDAO.find(new AyantDroitRFKey(idRf), FlushMode.MANUAL);
		if (communaute == null) {
			throw new IllegalArgumentException("La communauté idRF=[" + idRf + "] n'existe pas dans la DB.");
		}
		return communaute;
	}

	/**
	 * Traite l'ajout des droits de propriété sur un ayant-droit qui vient d'être créé.
	 */
	private void processCreation(@Nullable RegDate dateValeur, @NotNull AyantDroitRF ayantDroit, @NotNull List<DroitProprieteRF> droits) {
		if (!ayantDroit.getDroitsPropriete().isEmpty()) {
			throw new IllegalArgumentException("L'ayant-droit idRF=[" + ayantDroit.getIdRF() + "] possède déjà des droits alors que la mutation est de type CREATION.");
		}

		// on sauve les nouveaux droits
		droits.forEach(d -> {
			d.setAyantDroit(ayantDroit);
			d.setDateDebut(dateValeur);
			droitRFDAO.save(d);
		});
	}

	/**
	 * Traite la modification des droits de propriété sur un ayant-droit qui existe déjà et - potentiellement - possède déjà des droits.
	 * <p/>
	 * L'expérience montre qu'il y a trois types de changements possibles sur les droits d'un ayant-droit :
	 * <ol>
	 *     <li><b>les droits changent complètement</b> (= vente ou achat) : les <i>masterIdRF</i> et les <i>versionIdRF</i> changent.</li>
	 *     <li><b>les droits changent partiellement et de manière substentielle</b> (= modification de PPE, par exemple changement de la part ce co-propriété) : les <i>masterIdRF</i> restent les mêmes mais les <i>versionIdRF</i> changent.</li>
	 *     <li><b>les droits changent partiellement mais de manière non-substentielle</b> (=  modification d'intitulé) . les <i>masterIdRF</i> et les <i>versionIdRF</i> ne changent pas.</li>
	 * </ol>
	 */
	private void processModification(@NotNull RegDate dateValeur, @NotNull AyantDroitRF ayantDroit, @NotNull List<DroitProprieteRF> droits) {

		// on va chercher les droits de propriété actifs actuellement persistés
		final List<DroitProprieteRF> persisted = ayantDroit.getDroitsPropriete().stream()
				.filter(d -> d.isValidAt(null))
				.collect(Collectors.toList());

		// on détermine les changements
		final List<DroitProprieteRF> toAddList = new LinkedList<>(droits);
		final List<DroitProprieteRF> toCloseList = new LinkedList<>(persisted);

		// on supprime tous les droits qui n'ont pas changé
		CollectionsUtils.removeCommonElements(toAddList, toCloseList, DroitRFHelper::dataEquals);

		// on détermine les changements qui ne concernent que les raisons d'acquisition (= masterIdRF et versionIdRF égaux)
		// (tous les autres droits doivent être fermés / ouverts normalement)
		final List<Pair<DroitProprieteRF, DroitProprieteRF>> toUpdateList = CollectionsUtils.extractCommonElements(toAddList, toCloseList, DroitRFHelper::masterIdAndVersionIdEquals);

		// on met-à-jour tous les droits qui changent (c'est-à-dire les changements dans les raisons d'acquisition)
		toUpdateList.forEach(p -> processModification(p.getFirst(), p.getSecond()));

		// on ferme toutes les droits à fermer
		toCloseList.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));

		// on ajoute toutes les nouveaux droits
		toAddList.forEach(d -> {
			d.setAyantDroit(ayantDroit);
			d.setDateDebut(dateValeur);
			droitRFDAO.save(d);
		});
	}

	private void processModification(@NotNull DroitProprieteRF droit, @NotNull DroitProprieteRF persisted) {
		if (!masterIdAndVersionIdEquals(droit, persisted)) {
			throw new IllegalArgumentException("Les mastersIdRF/versionIdRF sont différents : [" + droit.getMasterIdRF() + "/" + droit.getVersionIdRF() + "] " +
					                                   "et [" + persisted.getMasterIdRF() + "/" + persisted.getVersionIdRF() + "]");
		}
		if (!Objects.equals(droit.getPart(), persisted.getPart())) {
			throw new IllegalArgumentException("Les parts ne sont pas égales entre l'ancien et le nouveau avec le masterIdRF=[" + droit.getMasterIdRF() + "]");
		}

		final List<RaisonAcquisitionRF> toDelete = new ArrayList<>(persisted.getRaisonsAcquisition());
		final List<RaisonAcquisitionRF> toAdd = new ArrayList<>(droit.getRaisonsAcquisition());
		CollectionsUtils.removeCommonElements(toDelete, toAdd, RaisonAcquisitionRFHelper::dataEquals);   // on supprime les raison d'acquisition qui n'ont pas changé

		// on annule toutes les raisons en trop (on ne maintient pas d'historique dans les raisons d'acquisition car il s'agit déjà d'un historique)
		toDelete.forEach(r -> r.setAnnule(true));

		// on ajoute toutes les nouvelles raisons
		toAdd.forEach(persisted::addRaisonAcquisition);
		persisted.calculateDateEtMotifDebut();
	}

	/**
	 * Traite la suppression (= fermeture) de tous les droits de propriété d'un ayant-droit.
	 */
	private void processSuppression(@NotNull RegDate dateValeur, @NotNull AyantDroitRF ayantDroit) {
		// on ferme tous les droits de propriété encore ouverts
		ayantDroit.getDroitsPropriete().stream()
				.filter(d -> d.isValidAt(null))
				.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));
	}
}
