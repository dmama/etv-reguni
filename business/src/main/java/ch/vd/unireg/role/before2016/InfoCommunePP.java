package ch.vd.uniregctb.role.before2016;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe concrète des informations pour les rôles PM d'une commune
 */
public class InfoCommunePP extends InfoCommune<InfoContribuablePP, InfoCommunePP> {

	private final Map<Long, InfoContribuablePP> infosContribuables = new HashMap<>();

	public InfoCommunePP(int noOfs) {
		super(noOfs);
	}

	public InfoContribuablePP getOrCreateInfoPourContribuable(ContribuableImpositionPersonnesPhysiques ctb, final int annee, final AdresseService adresseService, final TiersService tiersService) {
		final Long key = ctb.getNumero();
		return infosContribuables.computeIfAbsent(key, k -> new InfoContribuablePP(ctb, annee, adresseService, tiersService));
	}

	@Override
	public Collection<InfoContribuablePP> getInfosContribuables() {
		return infosContribuables.values();
	}

	@Override
	public void addAll(InfoCommunePP value) {
		infosContribuables.putAll(value.infosContribuables);
	}

	public InfoContribuablePP getInfoPourContribuable(Long noContribuable) {
		return infosContribuables.get(noContribuable);
	}
}
