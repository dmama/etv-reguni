package ch.vd.uniregctb.webservices.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class NoOfsTranslatorImpl implements NoOfsTranslator, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(NoOfsTranslatorImpl.class);

	/**
	 * Mapping nos Ofs -> nos techniques pour les communes suisses dont les deux numéros sont différents.
	 */
	private final Map<Integer, Integer> communes = new HashMap<Integer, Integer>();

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
			final Integer noTechnique = communes.get(noOfs);
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

	@Override
	public void afterPropertiesSet() throws Exception {
		if (exposeNosTechniques) {
			final List<Commune> listeCommunes = infraService.getCommunes();
			for (Commune commune : listeCommunes) {
				//noinspection deprecation
				final int numeroTechnique = commune.getNumeroTechnique();
				if (commune.getNoOFSEtendu() != numeroTechnique) {
					communes.put(commune.getNoOFSEtendu(), numeroTechnique);
				}
			}

			LOGGER.info(String.format("Récupéré %d commune(s) dont les numéros OFS et technique sont différents", communes.size()));
		}
	}
}
