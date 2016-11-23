package ch.vd.uniregctb.registrefoncier.processor;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.converter.jaxp.StringSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.helper.CommuneRFHelper;
import ch.vd.uniregctb.registrefoncier.key.CommuneRFKey;

public class CommuneRFProcessor implements MutationRFProcessor {

	@NotNull
	private final CommuneRFDAO communeRFDAO;

	@NotNull
	private final ServiceInfrastructureService infraService;

	@NotNull
	private final ThreadLocal<Unmarshaller> unmarshaller;

	public CommuneRFProcessor(@NotNull CommuneRFDAO communeRFDAO, @NotNull ServiceInfrastructureService infraService, @NotNull XmlHelperRF xmlHelperRF) {
		this.communeRFDAO = communeRFDAO;
		this.infraService = infraService;
		this.unmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getCommuneContext().createUnmarshaller();
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

		// on interpète le XML
		final GrundstueckNummer communeImport;
		try {
			final StringSource source = new StringSource(mutation.getXmlContent());
			communeImport = (GrundstueckNummer) unmarshaller.get().unmarshal(source);
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}

		// on crée la commune en mémoire
		final CommuneRF commune = CommuneRFHelper.newCommuneRF(communeImport, (nomCommune) -> findNoOfs(nomCommune, dateValeur));

		// on l'insère en DB
		switch (mutation.getTypeMutation()) {
		case CREATION:
			processCreation(commune);
			break;
		case MODIFICATION:
			processModification(dateValeur, commune);
			break;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + mutation.getTypeMutation() + "]");
		}

	}

	private void processCreation(@NotNull CommuneRF commune) {
		// on ajoute la nouvelle valeur (on ne met pas de date de début car on ne sait pas réellement quand la commune a été crée, juste quand elle apparaît dans l'import)
		communeRFDAO.save(commune);
	}

	/**
	 * Note msi : une analyse du fichier d'import du RF montre qu'il est *probable* que le RF réutilise le numéro d'une commune en cas
	 *            de fusion (e.g. A=1, B=2, C=3 --fusion--> D=1). Le code ci-dessus permet donc de donner des dates de validité aux communes
	 *            dans le cas d'une réutilisation du numéro.
	 */
	private void processModification(@NotNull RegDate dateValeur, @NotNull CommuneRF commune) {

		// on va chercher la commune persistée
		final CommuneRF persisted = communeRFDAO.findActive(new CommuneRFKey(commune.getNoRf()));
		if (persisted == null) {
			throw new IllegalArgumentException("La commune noRF=[" + commune.getNoOfs() + "] n'existe pas dans la DB.");
		}

		// on ferme l'ancienne valeur
		persisted.setDateFin(dateValeur.getOneDayBefore());

		// on ajoute la nouvelle valeur
		commune.setDateDebut(dateValeur);
		communeRFDAO.save(commune);
	}

	@NotNull
	private Integer findNoOfs(@NotNull String nomCommune, @Nullable RegDate dateValeur) {
		final Commune commune = infraService.findCommuneByNomOfficiel(nomCommune, dateValeur);
		if (commune == null) {
			throw new IllegalArgumentException("La commune RF avec le nom=[" + nomCommune + "] est introuvable dans l'infrastructure fiscale.");
		}
		return commune.getNoOFS();
	}
}
