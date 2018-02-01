package ch.vd.uniregctb.role.before2016;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe concrète des informations pour les rôles PM d'une commune
 */
public class InfoCommunePM extends InfoCommune<InfoContribuablePM, InfoCommunePM> {

	private final Map<Pair<Long, RegDate>, InfoContribuablePM> infosContribuables = new HashMap<>();

	public InfoCommunePM(int noOfs) {
		super(noOfs);
	}

	public InfoContribuablePM getOrCreateInfoPourContribuable(Entreprise entreprise, RegDate dateBouclement, AdresseService adresseService, TiersService tiersService) {
		final Pair<Long, RegDate> key = Pair.of(entreprise.getNumero(), dateBouclement);
		return infosContribuables.computeIfAbsent(key, k -> new InfoContribuablePM(entreprise, dateBouclement, adresseService, tiersService));
	}

	@Override
	public Collection<InfoContribuablePM> getInfosContribuables() {
		return infosContribuables.values();
	}

	@Override
	public void addAll(InfoCommunePM value) {
		infosContribuables.putAll(value.infosContribuables);
	}

	public InfoContribuablePM getInfoPourContribuable(Long noContribuable, RegDate dateBouclement) {
		return infosContribuables.get(Pair.of(noContribuable, dateBouclement));
	}
}
