package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.converter.jaxp.StringSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.AyantDroitRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

/**
 * Processeur spécialisé pour traiter les mutations sur les ayants-droits.
 */
public class AyantDroitRFProcessor implements MutationRFProcessor {

	@NotNull
	private final AyantDroitRFDAO ayantDroitRFDAO;

	@NotNull
	private final ThreadLocal<Unmarshaller> proprietaireUnmarshaller;
	private final ThreadLocal<Unmarshaller> communauteUnmarshaller;

	public AyantDroitRFProcessor(@NotNull AyantDroitRFDAO ayantDroitRFDAO, @NotNull XmlHelperRF xmlHelperRF) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;

		proprietaireUnmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getProprietaireContext().createUnmarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		});
		communauteUnmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getCommunauteContext().createUnmarshaller();
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

		// on interpète le XML
		Rechteinhaber ayantDroitImport;
		try {
			// on essaie pour voir si on a un propriétaire
			final StringSource source = new StringSource(mutation.getXmlContent());
			ayantDroitImport = (Personstamm) proprietaireUnmarshaller.get().unmarshal(source);
		}
		catch (JAXBException e1) {
			try {
				// c'est pas un propriétaire, c'est peut-être une communauté
				final StringSource source = new StringSource(mutation.getXmlContent());
				ayantDroitImport = (Gemeinschaft) communauteUnmarshaller.get().unmarshal(source);
			}
			catch (JAXBException e2) {
				throw new RuntimeException(e2);
			}
		}

		// on crée l'ayant-droit en mémoire
		final AyantDroitRF ayantDroit = AyantDroitRFHelper.newAyantDroitRF(ayantDroitImport);

		// on l'insère en DB
		switch (mutation.getTypeMutation()) {
		case CREATION:
			processCreation(ayantDroit);
			break;
		case MODIFICATION:
			processModification(ayantDroit);
			break;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + mutation.getTypeMutation() + "]");
		}

		// on renseigne le rapport
		if (rapport != null) {
			rapport.addProcessed(mutation.getId(), TypeEntiteRF.AYANT_DROIT, mutation.getTypeMutation());
		}
	}

	private void processCreation(AyantDroitRF ayantDroit) {
		ayantDroitRFDAO.save(ayantDroit);
	}

	private void processModification(AyantDroitRF ayantDroit) {

		final String idRF = ayantDroit.getIdRF();

		final AyantDroitRF persisted = ayantDroitRFDAO.find(new AyantDroitRFKey(idRF));
		if (persisted == null) {
			throw new IllegalArgumentException("L'ayant-droit idRF=[" + idRF + "] n'existe pas dans la DB.");
		}

		ayantDroit.copyDataTo(persisted);
	}
}
