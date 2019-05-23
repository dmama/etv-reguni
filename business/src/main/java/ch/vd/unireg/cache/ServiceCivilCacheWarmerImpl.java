package ch.vd.unireg.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.IndividuConnectorException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.tiers.TiersDAO;

/**
 * Service de préchauffage du cache du service civil
 */
public class ServiceCivilCacheWarmerImpl implements ServiceCivilCacheWarmer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCivilCacheWarmerImpl.class);

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
	public void warmIndividusPourTiers(Collection<Long> noTiers, @Nullable RegDate date, boolean includesComposantsMenage, AttributeIndividu... parties) {

		if (serviceCivilService.isWarmable()) {

			final Set<Long> idsTiers;
			if (noTiers instanceof Set) {
				idsTiers = (Set<Long>) noTiers;
			}
			else if (noTiers != null && !noTiers.isEmpty()) {
				idsTiers = new HashSet<>(noTiers);
			}
			else {
				idsTiers = Collections.emptySet();
			}

			if (!idsTiers.isEmpty()) {
				final Set<Long> nosIndividus = tiersDAO.getNumerosIndividu(idsTiers, includesComposantsMenage);
				warmIndividus(nosIndividus, date, parties);
			}
		}
	}

	public void warmIndividus(Collection<Long> nosIndividus, @Nullable RegDate date, AttributeIndividu... parties) {
		if (serviceCivilService.isWarmable() && nosIndividus != null && nosIndividus.size() > 1) { // il est inutile de préchauffer un seul individu à la fois
			final long start = System.nanoTime();
			try {
				serviceCivilService.getIndividus(nosIndividus, date, parties);
				final long end = System.nanoTime();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Récupéré %d individu(s) en %d ms", nosIndividus.size(), (end - start) / 1000000L));
				}
			}
			catch (IndividuConnectorException e) {
				LOGGER.error("Impossible de précharger le lot d'individus [" + nosIndividus + "]. L'erreur est : " + e.getMessage());
			}
		}
	}

	@Override
	public boolean isServiceWarmable() {
		return serviceCivilService.isWarmable();
	}
}
