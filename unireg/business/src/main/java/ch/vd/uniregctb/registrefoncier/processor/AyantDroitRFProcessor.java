package ch.vd.uniregctb.registrefoncier.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.converter.jaxp.StringSource;
import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.helper.AyantDroitRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

/**
 * Processeur spécialisé pour traiter les mutations sur les ayants-droits.
 */
public class AyantDroitRFProcessor implements MutationRFProcessor {

	@NotNull
	private final AyantDroitRFDAO ayantDroitRFDAO;

	@NotNull
	private final ThreadLocal<Unmarshaller> unmarshaller;

	public AyantDroitRFProcessor(@NotNull AyantDroitRFDAO ayantDroitRFDAO, @NotNull XmlHelperRF xmlHelperRF) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;

		// TODO (msi) effacer les données du thread-local lorsque le processor est détruit
		unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getProprietaireContext().createUnmarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void process(@NotNull EvenementRFMutation mutation) {

		if (mutation.getEtat() == EtatEvenementRF.TRAITE || mutation.getEtat() == EtatEvenementRF.FORCE) {
			throw new IllegalArgumentException("La mutation n°" + mutation.getId() + " est déjà traitée (état=[" + mutation.getEtat() + "]).");
		}

		// on interpète le XML
		final Personstamm proprietaireImport;
		try {
			final StringSource source = new StringSource(mutation.getXmlContent());
			proprietaireImport = (Personstamm) unmarshaller.get().unmarshal(source);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}

		// on crée l'ayant-droit en mémoire
		final AyantDroitRF ayantDroit = AyantDroitRFHelper.newAyantDroitRF(proprietaireImport);

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
