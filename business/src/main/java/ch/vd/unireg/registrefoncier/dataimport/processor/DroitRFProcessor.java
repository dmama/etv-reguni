package ch.vd.unireg.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.converter.jaxp.StringSource;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.DroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.EigentumAnteilListElement;
import ch.vd.unireg.registrefoncier.dataimport.helper.DroitRFHelper;
import ch.vd.unireg.registrefoncier.key.AyantDroitRFKey;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;

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
	private final CommunauteRFProcessor communauteRFProcessor;

	@NotNull
	private final ThreadLocal<Unmarshaller> unmarshaller;

	private final AffaireRFListener evenementFiscalSender;

	public DroitRFProcessor(@NotNull AyantDroitRFDAO ayantDroitRFDAO,
	                        @NotNull ImmeubleRFDAO immeubleRFDAO,
	                        @NotNull DroitRFDAO droitRFDAO,
	                        @NotNull CommunauteRFProcessor communauteRFProcessor,
	                        @NotNull XmlHelperRF xmlHelperRF,
	                        @NotNull EvenementFiscalService evenementFiscalService) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.immeubleRFDAO = immeubleRFDAO;
		this.droitRFDAO = droitRFDAO;
		this.communauteRFProcessor = communauteRFProcessor;

		this.unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getDroitListContext().createUnmarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		});

		this.evenementFiscalSender = new AffaireRFListener() {
			@Override
			public void onCreation(DroitProprieteRF droit) {
				// on publie l'événement fiscal correspondant
				evenementFiscalService.publierOuvertureDroitPropriete(droit.getDateDebutMetier(), droit);
			}

			@Override
			public void onUpdateDateDebut(@NotNull DroitProprieteRF droit, @Nullable RegDate dateDebutMetierInitiale, @Nullable String motifDebutInitial) {
				// on publie l'événement fiscal correspondant
				evenementFiscalService.publierModificationDroitPropriete(droit.getDateDebutMetier(), droit);
			}

			@Override
			public void onUpdateDateFin(@NotNull DroitProprieteRF droit, @Nullable RegDate dateFinMetierInitiale, @Nullable String motifFinInitial) {
				// on publie l'événement fiscal correspondant
				evenementFiscalService.publierModificationDroitPropriete(droit.getDateFinMetier(), droit);
			}

			@Override
			public void onOtherUpdate(@NotNull DroitProprieteRF droit) {
				// on publie l'événement fiscal correspondant
				evenementFiscalService.publierModificationDroitPropriete(droit.getDateDebutMetier(), droit);
			}

			@Override
			public void onClosing(@NotNull DroitProprieteRF droit) {
				// on publie l'événement fiscal correspondant
				evenementFiscalService.publierFermetureDroitPropriete(droit.getDateFinMetier(), droit);
			}
		};
	}

	@Override
	public void process(@NotNull EvenementRFMutation mutation, boolean importInitial, @Nullable MutationsRFProcessorResults rapport) {

		if (mutation.getEtat() == EtatEvenementRF.TRAITE || mutation.getEtat() == EtatEvenementRF.FORCE) {
			throw new IllegalArgumentException("La mutation n°" + mutation.getId() + " est déjà traitée (état=[" + mutation.getEtat() + "]).");
		}

		final RegDate dateValeur = mutation.getParentImport().getDateEvenement();

		final String immeubleIdRF = mutation.getIdRF();
		final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(immeubleIdRF), FlushMode.MANUAL);
		if (immeuble == null) {
			throw new IllegalArgumentException("L'immeuble avec l'idRF=[" + immeubleIdRF + "] n'existe pas.");
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
				final EigentumAnteilListElement droitListImport = (EigentumAnteilListElement) unmarshaller.get().unmarshal(source);
				droitList = droitListImport.getPersonEigentumAnteilOrGrundstueckEigentumAnteilOrHerrenlosEigentum();
			}
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}

		// on crée les droits en mémoire
		final List<DroitProprieteRF> droits = droitList.stream()
				.map(e -> DroitRFHelper.newDroitRF(e, this::findAyantDroit, this::findCommunaute, id -> immeuble))
				.collect(Collectors.toList());

		// on les insère en DB
		switch (mutation.getTypeMutation()) {
		case CREATION:
			processCreation(importInitial ? null : dateValeur, immeuble, droits);
			break;
		case MODIFICATION:
			processModification(dateValeur, immeuble, droits);
			break;
		case SUPPRESSION:
			processSuppression(dateValeur, immeuble);
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
	private AyantDroitRF findAyantDroit(@NotNull String idRf) {
		final AyantDroitRF ayantDroit = ayantDroitRFDAO.find(new AyantDroitRFKey(idRf), FlushMode.MANUAL);
		if (ayantDroit == null) {
			throw new IllegalArgumentException("L'ayant-droit idRF=[" + idRf + "] n'existe pas dans la DB.");
		}
		return ayantDroit;
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
	 * Traite l'ajout des droits de propriété sur un immeuble qui vient d'être créé.
	 */
	private void processCreation(@Nullable RegDate dateValeur, ImmeubleRF immeuble, @NotNull List<DroitProprieteRF> droits) {
		if (!immeuble.getDroitsPropriete().isEmpty()) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + immeuble.getIdRF() + "] possède déjà des droits alors que la mutation est de type CREATION.");
		}

		// on sauve les nouveaux droits
		final AffaireRF affaire = new AffaireRF(dateValeur, immeuble);
		affaire.apply(droitRFDAO, droits, Collections.emptyList(), Collections.emptyList(), evenementFiscalSender);

		// on recalcule ce qu'il faut sur les communautés de l'immeuble
		communauteRFProcessor.processAll(immeuble);
	}

	/**
	 * Traite la modification des droits de propriété sur un immeuble qui existe déjà et - potentiellement - possède déjà des droits.
	 * <p/>
	 * L'expérience montre qu'il y a trois types de changements possibles sur les droits d'un immeuble :
	 * <ol>
	 *     <li><b>les droits changent complètement</b> (= vente ou achat) : les <i>masterIdRF</i> et les <i>versionIdRF</i> changent.</li>
	 *     <li><b>les droits changent partiellement et de manière substentielle</b> (= modification de PPE, par exemple changement de la part ce co-propriété) : les <i>masterIdRF</i> restent les mêmes mais les <i>versionIdRF</i> changent.</li>
	 *     <li><b>les droits changent partiellement mais de manière non-substentielle</b> (=  modification d'intitulé) . les <i>masterIdRF</i> et les <i>versionIdRF</i> ne changent pas.</li>
	 * </ol>
	 */
	private void processModification(@NotNull RegDate dateValeur, ImmeubleRF immeuble, @NotNull List<DroitProprieteRF> droits) {

		// on va chercher les droits de propriété actifs actuellement persistés
		final List<DroitProprieteRF> persisted = immeuble.getDroitsPropriete().stream()
				.filter(d -> d.isValidAt(null))
				.collect(Collectors.toList());

		// on détermine les changements
		final List<DroitProprieteRF> toAddList = new LinkedList<>(droits);
		final List<DroitProprieteRF> toCloseList = new LinkedList<>(persisted);

		// on enlève des deux liste tous les droits qui n'ont pas changé
		CollectionsUtils.removeCommonElements(toAddList, toCloseList, DroitRFHelper::dataEquals);

		// on détermine les changements qui ne concernent que les raisons d'acquisition (= masterIdRF et versionIdRF égaux)
		// (tous les autres droits doivent être fermés / ouverts normalement)
		final List<Pair<DroitProprieteRF, DroitProprieteRF>> toUpdateList = CollectionsUtils.extractCommonElements(toAddList, toCloseList, DroitRFHelper::masterIdAndVersionIdEquals);

		// on applique les changements détectés
		final AffaireRF affaire = new AffaireRF(dateValeur, immeuble);
		affaire.apply(droitRFDAO, toAddList, toUpdateList, toCloseList, evenementFiscalSender);

		// on recalcule ce qu'il faut sur les communautés de l'immeuble
		communauteRFProcessor.processAll(immeuble);
	}

	/**
	 * Traite la suppression (= fermeture) de tous les droits de propriété d'un immeuble.
	 */
	private void processSuppression(@NotNull RegDate dateValeur, ImmeubleRF immeuble) {
		// on ferme tous les droits de propriété encore ouverts
		final List<DroitProprieteRF> toCloseList = immeuble.getDroitsPropriete().stream()
				.filter(d -> d.getDateFin() == null)
				.collect(Collectors.toList());

		final AffaireRF affaire = new AffaireRF(dateValeur, immeuble);
		affaire.apply(droitRFDAO, Collections.emptyList(), Collections.emptyList(), toCloseList, evenementFiscalSender);

		// on recalcule ce qu'il faut sur les communautés de l'immeuble
		communauteRFProcessor.processAll(immeuble);
	}
}
