package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.converter.jaxp.StringSource;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.GrundstueckExport.BodenbedeckungList;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.SurfaceAuSolRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.SurfaceAuSolRFHelper;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

/**
 * Processeur spécialisé pour traiter les mutations des surfaces au sol.
 */
public class SurfaceAuSolRFProcessor implements MutationRFProcessor {

	@NotNull
	private final ImmeubleRFDAO immeubleRFDAO;

	@NotNull
	private final SurfaceAuSolRFDAO surfaceAuSolRFDAO;

	@NotNull
	private final ThreadLocal<Unmarshaller> unmarshaller;

	public SurfaceAuSolRFProcessor(@NotNull ImmeubleRFDAO immeubleRFDAO, @NotNull SurfaceAuSolRFDAO surfaceAuSolRFDAO, @NotNull XmlHelperRF xmlHelperRF) {
		this.immeubleRFDAO = immeubleRFDAO;
		this.surfaceAuSolRFDAO = surfaceAuSolRFDAO;

		unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getSurfaceListContext().createUnmarshaller();
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

		final String idImmeubleRF = mutation.getIdRF();
		final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idImmeubleRF), FlushMode.MANUAL);
		if (immeuble == null) {
			throw new IllegalArgumentException("L'immeuble avec l'idRF=[" + idImmeubleRF + "] n'existe pas.");
		}

		final TypeMutationRF typeMutation = mutation.getTypeMutation();
		if (typeMutation == TypeMutationRF.CREATION || typeMutation == TypeMutationRF.MODIFICATION) {

			if (immeuble.getDateRadiation() != null) {
				throw new IllegalArgumentException("L'immeuble idRF=[" + idImmeubleRF + "] est radié, il ne devrait plus changer.");
			}

			// on interpète le XML
			final List<Bodenbedeckung> surfaceList;
			try {
				final StringSource source = new StringSource(mutation.getXmlContent());
				final BodenbedeckungList surfaceListImport = (BodenbedeckungList) unmarshaller.get().unmarshal(source);
				surfaceList = surfaceListImport.getBodenbedeckung();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}

			// on crée les surfaces en mémoire
			final List<SurfaceAuSolRF> surfaces = surfaceList.stream()
					.map(SurfaceAuSolRFHelper::newSurfaceAuSolRF)
					.collect(Collectors.toList());

			// on traite la mutation
			if (typeMutation == TypeMutationRF.CREATION) {
				processCreation(importInitial ? null : dateValeur, immeuble, surfaces);
			}
			else {
				processModification(dateValeur, immeuble, surfaces);
			}
		}
		else if (typeMutation == TypeMutationRF.SUPPRESSION) {
			processSuppression(dateValeur, immeuble);
		}
		else {
			throw new IllegalArgumentException("Type de mutation inconnu = [" + typeMutation + "]");
		}

		// on renseigne le rapport
		if (rapport != null) {
			rapport.addProcessed(mutation.getId(), TypeEntiteRF.SURFACE_AU_SOL, mutation.getTypeMutation());
		}
	}

	/**
	 * Traite l'ajout des surfaces au sol sur un immeuble qui vient d'être créé.
	 */
	private void processCreation(@Nullable RegDate dateValeur, @NotNull ImmeubleRF immeuble, @NotNull List<SurfaceAuSolRF> surfaces) {
		if (!immeuble.getSurfacesAuSol().isEmpty()) {
			throw new IllegalArgumentException("L'immeuble idRF=[" + immeuble.getIdRF() + "] possède déjà des surfaces au sol alors que la mutation est de type CREATION.");
		}

		// on sauve les nouvelles surfaces
		surfaces.forEach(s -> {
			s.setImmeuble(immeuble);
			s.setDateDebut(dateValeur);
			surfaceAuSolRFDAO.save(s);
		});
	}

	/**
	 * Traite la modification des surfaces au sol sur un immeuble qui existe déjà et - potentiellement - possède déjà des surfaces au sol.
	 */
	private void processModification(@NotNull RegDate dateValeur, @NotNull ImmeubleRF immeuble, @NotNull List<SurfaceAuSolRF> surfaces) {

		// on va chercher les surfaces actives actuellement persistées
		final List<SurfaceAuSolRF> persisted = immeuble.getSurfacesAuSol().stream()
				.filter(s -> s.isValidAt(null))
				.collect(Collectors.toList());

		// on détermine les changements
		List<SurfaceAuSolRF> toAddList = new LinkedList<>(surfaces);
		List<SurfaceAuSolRF> toCloseList = new LinkedList<>(persisted);

		final Iterator<SurfaceAuSolRF> aiter = toAddList.iterator();
		while (aiter.hasNext()) {
			final SurfaceAuSolRF toAdd = aiter.next();

			final Iterator<SurfaceAuSolRF> citer = toCloseList.iterator();
			while (citer.hasNext()) {
				final SurfaceAuSolRF toClose = citer.next();

				if (SurfaceAuSolRFHelper.dataEquals(toAdd, toClose)) {
					// les deux surfaces sont équivalentes, cela veut dire que la surface correspondante n'a pas changé. On la supprime donc des deux listes.
					aiter.remove();
					citer.remove();
					break;
				}
			}
		}

		// on ferme toutes les surfaces à fermer
		toCloseList.forEach(s -> s.setDateFin(dateValeur.getOneDayBefore()));

		// on ajoute toutes les nouvelles surfaces
		toAddList.forEach(s -> {
			s.setImmeuble(immeuble);
			s.setDateDebut(dateValeur);
			surfaceAuSolRFDAO.save(s);
		});
	}

	private void processSuppression(@NotNull RegDate dateValeur, @NotNull ImmeubleRF immeuble) {
		// on ferme toutes les surfaces au sol encore ouvertes
		immeuble.getSurfacesAuSol().stream()
				.filter(d -> d.isValidAt(null))
				.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));
	}
}
