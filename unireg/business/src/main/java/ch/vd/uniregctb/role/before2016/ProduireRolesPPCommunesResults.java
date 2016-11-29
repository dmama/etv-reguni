package ch.vd.uniregctb.role.before2016;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.TiersService;

public class ProduireRolesPPCommunesResults extends ProduireRolesCommunesResults<ProduireRolesPPCommunesResults> {

	private final RolesPP roles = new RolesPP();

	public ProduireRolesPPCommunesResults(int anneePeriode, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(anneePeriode, nbThreads, dateTraitement, tiersService, adresseService);
	}

	public ProduireRolesPPCommunesResults(int anneePeriode, Integer noOfsCommune, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(anneePeriode, noOfsCommune, nbThreads, dateTraitement, tiersService, adresseService);
	}

	@Override
	public TypeRoles getTypeRoles() {
		return TypeRoles.PP;
	}

	@Override
	public void digestInfoFor(InfoFor infoFor, Contribuable ctb, Assujettissement assujettissement, RegDate dateFinAssujettissementPrecedent, int annee, int noOfsCommune, AdresseService adresseService, TiersService tiersService) {
		roles.digestInfoFor(infoFor, (ContribuableImpositionPersonnesPhysiques) ctb, assujettissement, dateFinAssujettissementPrecedent, annee, noOfsCommune, adresseService, tiersService);
	}

	@Override
	public List<DateRange> getPeriodesFiscales(Contribuable ctb, TiersService tiersService) {
		return roles.getPeriodesFiscales((ContribuableImpositionPersonnesPhysiques) ctb, tiersService);
	}

	@Override
	public void addAll(ProduireRolesPPCommunesResults rapport) {
		super.addAll(rapport);
		roles.addAll(rapport.roles);
	}

	@Override
	public Set<Integer> getNoOfsCommunesTraitees() {
		if (noOfsCommune == null) {
			return roles.getNoOfsCommunesTraitees();
		}
		else {
			return Collections.singleton(noOfsCommune);
		}
	}

	public Map<Integer, InfoCommunePP> getInfosCommunes() {
		final Map<Integer, InfoCommunePP> full = roles.getInfosCommunes();
		if (noOfsCommune == null) {
			return full;
		}
		else {
			return Collections.singletonMap(noOfsCommune, full.get(noOfsCommune));
		}
	}
}
