package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.converter.jaxp.StringSource;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.ServitudesRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;
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
				return xmlHelperRF.getServitudeEtendueContext().createUnmarshaller();
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
		final TypeMutationRF typeMutation = mutation.getTypeMutation();
		final String servitudeIdRF = mutation.getIdRF();

		if (typeMutation == TypeMutationRF.CREATION || typeMutation == TypeMutationRF.MODIFICATION) {

			// on interpète le XML
			final DienstbarkeitExtendedElement dienstbarkeit;
			try {
				final String content = mutation.getXmlContent();
				final StringSource source = new StringSource(content);
				dienstbarkeit = (DienstbarkeitExtendedElement) unmarshaller.get().unmarshal(source);
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}

			// on crée la servitude en mémoire
			final ServitudeRF servitude = ServitudesRFHelper.newServitudeRF(dienstbarkeit, this::findAyantDroit, this::findImmeuble);

			// on traite la mutation
			if (typeMutation == TypeMutationRF.CREATION) {
				processCreation(importInitial ? null : dateValeur, servitude);
			}
			else {
				processModification(dateValeur, servitude);
			}
		}
		else if (typeMutation == TypeMutationRF.SUPPRESSION) {
			processSuppression(dateValeur, servitudeIdRF);
		}
		else {
			throw new IllegalArgumentException("Type de mutation inconnu = [" + typeMutation + "]");
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

	/**
	 * Traite l'ajout d'une servitude.
	 */
	private void processCreation(@Nullable RegDate dateValeur, @NotNull ServitudeRF servitude) {

		// on sauve la nouvelle servitude
		servitude.setDateDebut(dateValeur);
		droitRFDAO.save(servitude);
	}

	/**
	 * Traite la modification d'une servitude sur un ayant-droit
	 */
	private void processModification(@NotNull RegDate dateValeur, @NotNull ServitudeRF servitude) {

		final DroitRF persisted = droitRFDAO.find(new DroitRFKey(servitude.getMasterIdRF()));
		if (persisted == null) {
			throw new IllegalArgumentException("La servitude idRF=[" + servitude.getMasterIdRF() + "] n'existe pas dans la DB.");
		}

		// FIXME (msi) que faire ?
		throw new NotImplementedException();
	}

	/**
	 * Traite la suppression (= fermeture) d'une servitude
	 */
	private void processSuppression(@NotNull RegDate dateValeur, String servitudeIdRF) {
		final DroitRF persisted = droitRFDAO.find(new DroitRFKey(servitudeIdRF));
		if (persisted == null) {
			throw new IllegalArgumentException("La servitude idRF=[" + servitudeIdRF + "] n'existe pas dans la DB.");
		}
		// on ferme la servitude
		persisted.setDateFin(dateValeur.getOneDayBefore());
		if (persisted.getDateFinMetier() == null) {
			// on renseigne la date de fin métier à  la même valeur que la date technique, car :
			// - il n'y a pas forcément une nouvelle servitude qui suivra et donc il n'est pas possible de déduire la date de fin métier
			//   de la date de début métier de la servitude suivante (comme pour les droits de propriété)
			// - il n'y a pas d'autre date disponible et il faut bien renseigner quelque chose.
			persisted.setDateFinMetier(dateValeur.getOneDayBefore());
		}
	}
}
