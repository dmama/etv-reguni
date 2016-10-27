package ch.vd.uniregctb.registrefoncier.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.helper.EstimationRFHelper;
import ch.vd.uniregctb.registrefoncier.helper.ImmeubleRFHelper;
import ch.vd.uniregctb.registrefoncier.helper.SituationRFHelper;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public class ImmeubleRFProcessor implements MutationRFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImmeubleRFProcessor.class);

	@NotNull
	private final ImmeubleRFDAO immeubleRFDAO;

	@NotNull
	private final XmlHelperRF xmlHelperRF;

	@NotNull
	private final Unmarshaller unmarshaller;

	public ImmeubleRFProcessor(@NotNull ImmeubleRFDAO immeubleRFDAO, @NotNull XmlHelperRF xmlHelperRF) {
		this.immeubleRFDAO = immeubleRFDAO;
		this.xmlHelperRF = xmlHelperRF;

		try {
			unmarshaller = xmlHelperRF.getImmeubleContext().createUnmarshaller();
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void process(@NotNull EvenementRFMutation mutation) {

		if (mutation.getEtat() == EtatEvenementRF.TRAITE || mutation.getEtat() == EtatEvenementRF.FORCE) {
			throw new IllegalArgumentException("La mutation n°" + mutation.getId() + " est déjà traitée (état=[" + mutation.getEtat() + "]).");
		}

		final RegDate dateValeur = mutation.getParentImport().getDateEvenement();

		// on interpète le XML
		final Grundstueck immeubleImport;
		try (final InputStream is = mutation.getXmlContent().getBinaryStream()) {
			immeubleImport = (Grundstueck) unmarshaller.unmarshal(is);
		}
		catch (SQLException | IOException | JAXBException e) {
			throw new RuntimeException(e);
		}

		// on crée l'immeuble en mémoire
		final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(immeubleImport);

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

		// on met-à-jour le statut de la mutation
		mutation.setEtat(EtatEvenementRF.TRAITE);
	}

	private void processCreation(RegDate dateValeur, @NotNull ImmeubleRF newImmeuble) {


		if (immeubleRFDAO.find(new ImmeubleRFKey(newImmeuble)) != null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + newImmeuble.getIdRF() + "] existe déjà dans la DB.");
		}

		// on va chercher les nouvelles situations et estimations
		final SituationRF newSituation = newImmeuble.getSituations().iterator().next();     // par définition, le nouvel immeuble ne contient que l'état courant,
		final EstimationRF newEstimation = newImmeuble.getEstimations().iterator().next();  // il ne contient donc qu'une seule situation et une seule estimation fiscale.

		// on renseigne les dates de début
		newSituation.setDateDebut(dateValeur);
		newEstimation.setDateDebut(dateValeur);

		immeubleRFDAO.save(newImmeuble);
	}

	private void processModification(RegDate dateValeur, @NotNull ImmeubleRF newImmeuble) {

		final String idRF = newImmeuble.getIdRF();

		final ImmeubleRF persisted = immeubleRFDAO.find(new ImmeubleRFKey(newImmeuble));
		if (persisted == null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] n'existe pas dans la DB.");
		}

		// on va chercher les situations et estimations courantes
		final SituationRF persistedSituation = persisted.getSituations().stream()
				.filter(s -> s.isValidAt(null))
				.findFirst()
				.orElse(null);
		if (persistedSituation == null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] ne contient pas de situation dans la DB.");
		}

		final EstimationRF persistedEstimation = persisted.getEstimations().stream()
				.filter(s -> s.isValidAt(null))
				.findFirst()
				.orElse(null);
		if (persistedEstimation == null) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + idRF + "] ne contient pas d'estimation fiscale dans la DB.");
		}

		// on va chercher les nouvelles situations et estimations
		final SituationRF newSituation = newImmeuble.getSituations().iterator().next();     // par définition, le nouvel immeuble ne contient que l'état courant,
		final EstimationRF newEstimation = newImmeuble.getEstimations().iterator().next();  // il ne contient donc qu'une seule situation et une seule estimation fiscale.

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
			persistedEstimation.setDateFin(dateValeur.getOneDayBefore());
			newEstimation.setDateDebut(dateValeur);
			persisted.getEstimations().add(newEstimation);
		}
	}
}
