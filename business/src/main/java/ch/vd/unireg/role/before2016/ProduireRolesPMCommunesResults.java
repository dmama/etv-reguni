package ch.vd.unireg.role.before2016;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;

public class ProduireRolesPMCommunesResults extends ProduireRolesCommunesResults<ProduireRolesPMCommunesResults> {

	private final RolesPM roles = new RolesPM();

	public ProduireRolesPMCommunesResults(int anneePeriode, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(anneePeriode, nbThreads, dateTraitement, tiersService, adresseService);
	}

	public ProduireRolesPMCommunesResults(int anneePeriode, Integer noOfsCommune, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(anneePeriode, noOfsCommune, nbThreads, dateTraitement, tiersService, adresseService);
	}

	@Override
	public TypeRoles getTypeRoles() {
		return TypeRoles.PM;
	}

	@Override
	public void digestInfoFor(InfoFor infoFor, Contribuable ctb, Assujettissement assujettissement, RegDate dateFinAssujettissementPrecedent, int annee, int noOfsCommune, AdresseService adresseService, TiersService tiersService) {
		roles.digestInfoFor(infoFor, (Entreprise) ctb, assujettissement, dateFinAssujettissementPrecedent, annee, noOfsCommune, adresseService, tiersService);
	}

	@Override
	public List<DateRange> getPeriodesFiscales(Contribuable ctb, TiersService tiersService) {
		return roles.getPeriodesFiscales((Entreprise) ctb, tiersService);
	}

	@Override
	public void addAll(ProduireRolesPMCommunesResults rapport) {
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

	public Map<Integer, InfoCommunePM> getInfosCommunes() {
		final Map<Integer, InfoCommunePM> full = roles.getInfosCommunes();
		if (noOfsCommune == null) {
			return full;
		}
		else {
			return Collections.singletonMap(noOfsCommune, full.get(noOfsCommune));
		}
	}
}
