package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.converter.jaxp.StringSource;
import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceBatimentRF;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.BatimentRFHelper;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.ImplantationRFHelper;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.SurfaceBatimentRFHelper;
import ch.vd.uniregctb.registrefoncier.key.BatimentRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

/**
 * Processeur spécialisé pour traiter les mutations sur les bâtiments.
 */
public class BatimentRFProcessor implements MutationRFProcessor {

	//private static final Logger LOGGER = LoggerFactory.getLogger(ImmeubleRFProcessor.class);

	@NotNull
	private final BatimentRFDAO batimentRFDAO;

	@NotNull
	private final ImmeubleRFDAO immeubleRFDAO;

	@NotNull
	private final ThreadLocal<Unmarshaller> unmarshaller;

	public BatimentRFProcessor(@NotNull BatimentRFDAO batimentRFDAO, @NotNull ImmeubleRFDAO immeubleRFDAO, @NotNull XmlHelperRF xmlHelperRF) {
		this.batimentRFDAO = batimentRFDAO;
		this.immeubleRFDAO = immeubleRFDAO;
		this.unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getBatimentContext().createUnmarshaller();
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


		final TypeMutationRF typeMutation = mutation.getTypeMutation();
		if (typeMutation == TypeMutationRF.CREATION || typeMutation == TypeMutationRF.MODIFICATION) {

			// on interpète le XML
			final Gebaeude batimentImport;
			try {
				final String content = mutation.getXmlContent();
				final StringSource source = new StringSource(content);
				batimentImport = (Gebaeude) unmarshaller.get().unmarshal(source);
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}

			// on crée le bâtiment en mémoire
			final BatimentRF batiment = BatimentRFHelper.newBatimentRF(batimentImport, this::findImmeuble);

			// on traite la mutation
			if (typeMutation == TypeMutationRF.CREATION) {
				processCreation(dateValeur, batiment);
			}
			else {
				processModification(dateValeur, batiment);
			}
		}
		else if (typeMutation == TypeMutationRF.SUPPRESSION) {
			processSuppression(dateValeur, mutation.getIdRF());
		}
		else {
			throw new IllegalArgumentException("Type de mutation inconnu = [" + typeMutation + "]");
		}
	}

	@NotNull
	private ImmeubleRF findImmeuble(@NotNull String idRf) {
		final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRf));
		if (immeuble == null) {
			throw new ObjectNotFoundException("L'immeuble RF avec l'idRF=[" + idRf + "] n'existe pas dans la base.");
		}
		return immeuble;
	}

	private void processCreation(RegDate dateValeur, @NotNull BatimentRF newBatiment) {

		// on renseigne les dates de début des surfaces et implantations
		newBatiment.getSurfaces().forEach(s -> s.setDateDebut(dateValeur));
		newBatiment.getImplantations().forEach(s -> s.setDateDebut(dateValeur));

		batimentRFDAO.save(newBatiment);
	}

	private void processModification(RegDate dateValeur, @NotNull BatimentRF newBatiment) {

		final String masterIdRF = newBatiment.getMasterIdRF();

		final BatimentRF persisted = batimentRFDAO.find(new BatimentRFKey(masterIdRF));
		if (persisted == null) {
			throw new IllegalArgumentException("Le bâtiment avec le masterIdRF=[" + masterIdRF + "] n'existe pas dans la DB.");
		}

		// on va chercher les surfaces et les implantations courantes
		final SurfaceBatimentRF persistedSurface = persisted.getSurfaces().stream()
				.filter(s -> s.isValidAt(null))
				.findFirst()
				.orElse(null);

		final List<ImplantationRF> persistedImplantations = persisted.getImplantations().stream()
				.filter(s -> s.isValidAt(null))
				.collect(Collectors.toList());

		// on va chercher les nouvelles situations et estimations
		final SurfaceBatimentRF newSurface = CollectionsUtils.getFirst(newBatiment.getSurfaces());     // par définition, le nouveau bâtiment ne contient zéro ou une surface courante,
		final Set<ImplantationRF> newImplantations = newBatiment.getImplantations();

		// on détermine les changements sur la surface
		if (!SurfaceBatimentRFHelper.dataEquals(persistedSurface, newSurface)) {
			// on ferme l'ancienne surface et on ajoute la nouvelle
			if (persistedSurface != null) {
				persistedSurface.setDateFin(dateValeur.getOneDayBefore());
			}
			if (newSurface != null) {
				newSurface.setDateDebut(dateValeur);
				persisted.addSurface(newSurface);
			}
		}

		// on détermine les changements sur implantations
		{
			final List<ImplantationRF> toAddList = new LinkedList<>(newImplantations);
			final List<ImplantationRF> toCloseList = new LinkedList<>(persistedImplantations);
			CollectionsUtils.removeCommonElements(toAddList, toCloseList, ImplantationRFHelper::dataEquals);

			// on ferme toutes les implantations à fermer
			toCloseList.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));

			// on ajoute toutes les nouvelles implantations sur le bâtiment déjà persisté
			toAddList.forEach(d -> {
				d.setDateDebut(dateValeur);
				persisted.addImplantation(d);
			});
		}
	}

	private void processSuppression(RegDate dateValeur, String masterIdRF) {

		final BatimentRF persisted = batimentRFDAO.find(new BatimentRFKey(masterIdRF));
		if (persisted == null) {
			throw new IllegalArgumentException("Le bâtiment avec le masterIdRF=[" + masterIdRF + "] n'existe pas dans la DB.");
		}

		// on ferme toutes les implantations et surfaces encore ouvertes
		persisted.getImplantations().stream()
				.filter(d -> d.isValidAt(null))
				.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));
		persisted.getSurfaces().stream()
				.filter(d -> d.isValidAt(null))
				.forEach(d -> d.setDateFin(dateValeur.getOneDayBefore()));
	}
}
