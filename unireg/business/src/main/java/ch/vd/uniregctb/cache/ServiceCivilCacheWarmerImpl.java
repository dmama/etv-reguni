package ch.vd.uniregctb.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

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

	@Override
	public void warmIndividusPourTiers(Collection<Long> noTiers, @Nullable RegDate date, AttributeIndividu... parties) {

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
				if (nosIndividus != null && nosIndividus.size() > 1) { // il est inutile de préchauffer un seul individu à la fois
					serviceCivilService.getIndividus(nosIndividus, date, parties);

					final long end = System.nanoTime();
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(String.format("Récupéré %d individu(s) en %d ms", nosIndividus.size(), (end - start) / 1000000L));
					}
				}
			}
		}
	}
}
