package ch.vd.uniregctb.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Service de préchauffage du cache du service civil
 */
public class ServiceCivilCacheWarmerImpl implements ServiceCivilCacheWarmer {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilCacheWarmerImpl.class);

	private TiersDAO tiersDAO;

	private ServiceCivilService serviceCivilService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	/**
	 * Va chercher les individus liés aux tiers dont les numéros sont passés en paramètres
	 * @param noTiers numéros des tiers concernés
	 * @param date date de référence pour le cache
	 * @param parties parties des individus à mémoriser dans le cache
	 */
	public void warmIndividusPourTiers(Collection<Long> noTiers, RegDate date, AttributeIndividu... parties) {

		if (serviceCivilService.isWarmable()) {

			final long start = System.nanoTime();

			final Set<Long> idsTiers;
			if (noTiers instanceof Set) {
				idsTiers = (Set<Long>) noTiers;
			}
			else if (noTiers != null && !noTiers.isEmpty()) {
				idsTiers = new HashSet<Long>(noTiers);
			}
			else {
				idsTiers = Collections.emptySet();
			}

			if (!idsTiers.isEmpty()) {
				final Set<Long> nosIndividus = tiersDAO.getNumerosIndividu(idsTiers, true);
				if (nosIndividus != null && !nosIndividus.isEmpty()) {
					serviceCivilService.getIndividus(nosIndividus, date, parties);

					final long end = System.nanoTime();
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info(String.format("Récupéré %d individu(s) en %d ms", nosIndividus.size(), (end - start) / 1000000L));
					}
				}
			}
		}
	}
}
