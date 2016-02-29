package ch.vd.uniregctb.migration.pm.communes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class FractionsCommuneProviderImpl implements FractionsCommuneProvider, InitializingBean {

	private ServiceInfrastructureService infraService;

	/**
	 * Map des fractions associées aux communes faitières (clé = OFS de la commune faîtière)
	 */
	private final Map<Integer, SortedSet<Commune>> fractions = new HashMap<>();

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final List<Commune> communesVaudoises = infraService.getCommunesDeVaud();
		for (Commune commune : communesVaudoises) {
			if (commune.isFraction()) {
				final SortedSet<Commune> set = fractions.computeIfAbsent(commune.getOfsCommuneMere(),
				                                                         ofs -> new TreeSet<>(Comparator.comparing(Commune::getNoOFS)));
				set.add(commune);
			}
		}
	}

	@Override
	public List<Commune> getFractions(Commune faitiere) {
		if (!faitiere.isPrincipale()) {
			throw new IllegalArgumentException(String.format("La commune %d (%s) n'est pas une commune faitière de fractions", faitiere.getNoOFS(), faitiere.getNomOfficielAvecCanton()));
		}

		return new ArrayList<>(fractions.get(faitiere.getNoOFS()));
	}
}
