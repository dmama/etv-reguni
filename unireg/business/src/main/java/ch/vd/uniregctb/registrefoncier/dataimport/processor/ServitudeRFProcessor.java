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

import ch.vd.capitastra.rechteregister.DienstbarkeitDiscrete;
import ch.vd.capitastra.rechteregister.DienstbarkeitDiscreteList;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.ServitudesRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

/**
 * Processeur spécialisé pour traiter les mutations des servitudes.
 */
public class ServitudeRFProcessor implements MutationRFProcessor {

	@NotNull
	private final AyantDroitRFDAO ayantDroitRFDAO;

	@NotNull
	private final ImmeubleRFDAO immeubleRFDAO;

	@NotNull
	private final DroitRFDAO droitRFDAO;

	@NotNull
	private final ThreadLocal<Unmarshaller> unmarshaller;

	public ServitudeRFProcessor(@NotNull AyantDroitRFDAO ayantDroitRFDAO, @NotNull ImmeubleRFDAO immeubleRFDAO, @NotNull DroitRFDAO droitRFDAO, @NotNull XmlHelperRF xmlHelperRF) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.immeubleRFDAO = immeubleRFDAO;
		this.droitRFDAO = droitRFDAO;

		unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getServitudeListContext().createUnmarshaller();
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
		final List<DienstbarkeitDiscrete> servitudesList;
		try {
			final String content = mutation.getXmlContent();
			if (content == null) {
				servitudesList = Collections.emptyList();
			}
			else {
				final StringSource source = new StringSource(content);
				final DienstbarkeitDiscreteList servitudesListImport = (DienstbarkeitDiscreteList) unmarshaller.get().unmarshal(source);
				servitudesList = servitudesListImport.getDienstbarkeitDiscretes();
			}
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}

		// on crée les servitudes en mémoire
		final List<ServitudeRF> servitudes = servitudesList.stream()
				.map(e -> ServitudesRFHelper.newServitudeRF(e, idRef -> ayantDroit, this::findCommunaute, this::findImmeuble))
				.collect(Collectors.toList());

		// on les insère en DB
		switch (mutation.getTypeMutation()) {
		case CREATION:
			processCreation(importInitial ? null : dateValeur, ayantDroit, servitudes);
			break;
		case MODIFICATION:
			processModification(dateValeur, ayantDroit, servitudes);
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
	 * Traite l'ajout des servitudes sur un ayant-droit qui vient d'être créé.
	 */
	private void processCreation(@Nullable RegDate dateValeur, @NotNull AyantDroitRF ayantDroit, @NotNull List<ServitudeRF> droits) {
		if (!ayantDroit.getDroits().isEmpty()) {
			throw new IllegalArgumentException("L'ayant-droit idRF=[" + ayantDroit.getIdRF() + "] possède déjà des droits alors que la mutation est de type CREATION.");
		}

		// on sauve les nouvelles servitudes
		droits.forEach(d -> {
			d.setAyantDroit(ayantDroit);
			d.setDateDebut(dateValeur);
			droitRFDAO.save(d);
		});
	}

	/**
	 * Traite la modification des servitudes sur un ayant-droit qui existe déjà et - potentiellement - possède déjà des droits.
	 */
	private void processModification(@NotNull RegDate dateValeur, @NotNull AyantDroitRF ayantDroit, @NotNull List<ServitudeRF> droits) {

		// on va chercher les servitudes actives actuellement persistées
		final List<DroitRF> persisted = ayantDroit.getDroits().stream()
				.filter(d -> d.isValidAt(RegDate.get()))
				.filter(d -> d instanceof ServitudeRF)
				.collect(Collectors.toList());

		// on détermine les changements
		List<DroitRF> toAddList = new LinkedList<>(droits);
		List<DroitRF> toCloseList = new LinkedList<>(persisted);
		CollectionsUtils.removeCommonElements(toAddList, toCloseList, ServitudesRFHelper::dataEquals);

		// on ferme toutes les servitudes à fermer
		toCloseList.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));

		// on ajoute toutes les nouvelles servitudes
		toAddList.forEach(d -> {
			d.setAyantDroit(ayantDroit);
			d.setDateDebut(dateValeur);
			droitRFDAO.save(d);
		});
	}

	/**
	 * Traite la suppression (= fermeture) de toutes les servitudes d'un ayant-droit.
	 */
	private void processSuppression(@NotNull RegDate dateValeur, @NotNull AyantDroitRF ayantDroit) {
		// on ferme toutes les servitudes encore ouvertes
		ayantDroit.getDroits().stream()
				.filter(d -> d.isValidAt(RegDate.get()))
				.filter(d -> d instanceof ServitudeRF)
				.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));
	}
}
