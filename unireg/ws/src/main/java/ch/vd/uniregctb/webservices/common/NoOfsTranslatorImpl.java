package ch.vd.uniregctb.webservices.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class NoOfsTranslatorImpl implements NoOfsTranslator {

	private static final Logger LOGGER = Logger.getLogger(NoOfsTranslatorImpl.class);

	/**
	 * Mapping nos Ofs -> nos techniques pour les communes suisses dont les deux numéros sont différents.
	 */
	private Map<Integer, Integer> communes = null;

	private boolean exposeNosTechniques;

	private ServiceInfrastructureService infraService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExposeNosTechniques(boolean exposeNosTechniques) {
		this.exposeNosTechniques = exposeNosTechniques;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int translateCommune(int noOfs) {
		if (exposeNosTechniques) {
			final Integer noTechnique = getCommunes().get(noOfs);
			if (noTechnique != null) {
				// on a trouvé un numéro technique différent du numéro Ofs, on le retourne
				return noTechnique;
			}
			else {
				return noOfs;
			}
		}
		else {
			// on expose simplement le numéro Ofs
			return noOfs;
		}
	}

	private Map<Integer, Integer> getCommunes() {
		if (communes == null) {
			communes = buildCommunes();
		}
		return communes;
	}

	private synchronized Map<Integer, Integer> buildCommunes() {
		if (communes == null) {

			LOGGER.info("Chargement des correspondances entre numéros OFS et numéros techniques des communes");

			final List<Commune> listeCommunes = infraService.getCommunes();
			final Map<Integer, Integer> map = new HashMap<Integer, Integer>();
			for (Commune commune : listeCommunes) {
				//noinspection deprecation
				final int numeroTechnique = commune.getNumeroTechnique();
				if (commune.getNoOFSEtendu() != numeroTechnique) {
					map.put(commune.getNoOFSEtendu(), numeroTechnique);
				}
			}

			LOGGER.info(String.format("Récupéré %d commune(s) dont les numéros OFS et technique sont différents", map.size()));
			communes = map;
		}
		return communes;
	}
}
