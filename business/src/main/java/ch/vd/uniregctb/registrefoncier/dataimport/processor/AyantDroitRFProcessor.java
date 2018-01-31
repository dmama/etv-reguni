package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.converter.jaxp.StringSource;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.AyantDroitRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

/**
 * Processeur spécialisé pour traiter les mutations sur les ayants-droits.
 */
public class AyantDroitRFProcessor implements MutationRFProcessor {

	@NotNull
	private final AyantDroitRFDAO ayantDroitRFDAO;

	@NotNull
	private final ImmeubleRFDAO immeubleRFDAO;

	@NotNull
	private final ThreadLocal<Unmarshaller> ayantDroitUnmarshaller;
	private final ThreadLocal<Unmarshaller> beneficiairesUnmarshaller;

	public AyantDroitRFProcessor(@NotNull AyantDroitRFDAO ayantDroitRFDAO, @NotNull ImmeubleRFDAO immeubleRFDAO, @NotNull XmlHelperRF xmlHelperRF) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.immeubleRFDAO = immeubleRFDAO;

		ayantDroitUnmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getAyantDroitContext().createUnmarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		});
		beneficiairesUnmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getBeneficiaireContext().createUnmarshaller();
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
			ayantDroitImport = (Rechteinhaber) ayantDroitUnmarshaller.get().unmarshal(source);
		}
		catch (JAXBException e1) {
			try {
				// c'est pas un propriétaire, c'est peut-être un bénéficiaire de servitude
				final StringSource source = new StringSource(mutation.getXmlContent());
				ayantDroitImport = (ch.vd.capitastra.rechteregister.Personstamm) beneficiairesUnmarshaller.get().unmarshal(source);
			}
			catch (JAXBException e2) {
				throw new RuntimeException(e2);
			}
		}

		// on crée l'ayant-droit en mémoire
		final AyantDroitRF ayantDroit = AyantDroitRFHelper.newAyantDroitRF(ayantDroitImport, this::findImmeuble);

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

	private void processCreation(AyantDroitRF ayantDroit) {
		ayantDroitRFDAO.save(ayantDroit);
	}

	private void processModification(AyantDroitRF ayantDroit) {

		final String idRF = ayantDroit.getIdRF();

		final AyantDroitRF persisted = ayantDroitRFDAO.find(new AyantDroitRFKey(idRF), FlushMode.MANUAL);
		if (persisted == null) {
			throw new IllegalArgumentException("L'ayant-droit idRF=[" + idRF + "] n'existe pas dans la DB.");
		}

		ayantDroit.copyDataTo(persisted);
	}
}
