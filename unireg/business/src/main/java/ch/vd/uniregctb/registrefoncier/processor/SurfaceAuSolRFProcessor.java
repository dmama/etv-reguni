package ch.vd.uniregctb.registrefoncier.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.converter.jaxp.StringSource;
import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.GrundstueckExport.BodenbedeckungList;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.SurfaceAuSolRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.helper.SurfaceAuSolRFHelper;
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

		// TODO (msi) effacer les données du thread-local lorsque le processor est détruit
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
	public void process(@NotNull EvenementRFMutation mutation) {

		if (mutation.getEtat() == EtatEvenementRF.TRAITE || mutation.getEtat() == EtatEvenementRF.FORCE) {
			throw new IllegalArgumentException("La mutation n°" + mutation.getId() + " est déjà traitée (état=[" + mutation.getEtat() + "]).");
		}

		final RegDate dateValeur = mutation.getParentImport().getDateEvenement();

		final String idImmeubleRF = mutation.getIdRF();
		final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idImmeubleRF));
		if (immeuble == null) {
			throw new IllegalArgumentException("L'immeuble avec l'idRF=[" + idImmeubleRF + "] n'existe pas.");
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

		// on les insère en DB
		switch (mutation.getTypeMutation()) {
		case CREATION:
			processCreation(dateValeur, immeuble, surfaces);
			break;
		case MODIFICATION:
			processModification(dateValeur, immeuble, surfaces);
			break;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + mutation.getTypeMutation() + "]");
		}

	}

	/**
	 * Traite l'ajout des surfaces au sol sur un immeuble qui vient d'être créé.
	 */
	private void processCreation(@NotNull RegDate dateValeur, @NotNull ImmeubleRF immeuble, @NotNull List<SurfaceAuSolRF> surfaces) {
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
}
