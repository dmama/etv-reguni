package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.converter.jaxp.StringSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.PersonEigentumAnteilListElement;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

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
		final AyantDroitRF ayantDroit = ayantDroitRFDAO.find(new AyantDroitRFKey(ayantDroitIdRF));
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
		final List<DroitRF> droits = droitList.stream()
				.map(e -> (PersonEigentumAnteil) e)
				.map(e -> DroitRFHelper.newDroitRF(e, importInitial, idRef -> ayantDroit, this::findCommunaute, this::findImmeuble))
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
		final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRf));
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
		final CommunauteRF communaute = (CommunauteRF) ayantDroitRFDAO.find(new AyantDroitRFKey(idRf));
		if (communaute == null) {
			throw new IllegalArgumentException("La communauté idRF=[" + idRf + "] n'existe pas dans la DB.");
		}
		return communaute;
	}

	/**
	 * Traite l'ajout des droits sur un immeuble qui vient d'être créé.
	 */
	private void processCreation(@Nullable RegDate dateValeur, @NotNull AyantDroitRF ayantDroit, @NotNull List<DroitRF> droits) {
		if (!ayantDroit.getDroits().isEmpty()) {
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
	 * Traite la modification des droits sur un immeuble qui existe déjà et - potentiellement - possède déjà des droits.
	 */
	private void processModification(@NotNull RegDate dateValeur, @NotNull AyantDroitRF ayantDroit, @NotNull List<DroitRF> droits) {

		// on va chercher les droits actifs actuellement persistés
		final List<DroitRF> persisted = ayantDroit.getDroits().stream()
				.filter(d -> d.isValidAt(null))
				.collect(Collectors.toList());

		// on détermine les changements
		List<DroitRF> toAddList = new LinkedList<>(droits);
		List<DroitRF> toCloseList = new LinkedList<>(persisted);
		CollectionsUtils.removeCommonElements(toAddList, toCloseList, (left, right) -> DroitRFHelper.dataEquals(left, right, false));   // on supprime les droits égaux en tant compte des motifs (pour être le plus précis possible)
		CollectionsUtils.removeCommonElements(toAddList, toCloseList, (left, right) -> DroitRFHelper.dataEquals(left, right, true));    // on supprime les droits égaux sans tenir compte des motifs (pour être le plus complet possible)

		// on ferme toutes les droits à fermer
		toCloseList.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));

		// on ajoute toutes les nouveaux droits
		toAddList.forEach(d -> {
			d.setAyantDroit(ayantDroit);
			d.setDateDebut(dateValeur);
			droitRFDAO.save(d);
		});
	}

	/**
	 * Traite la suppression (= fermeture) de tous les droits d'un propriétaire.
	 */
	private void processSuppression(@NotNull RegDate dateValeur, @NotNull AyantDroitRF ayantDroit) {
		// on ferme tous les droits encore ouverts
		ayantDroit.getDroits().stream()
				.filter(d -> d.isValidAt(null))
				.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));
	}
}
