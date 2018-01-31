package ch.vd.uniregctb.role.before2016;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;

public class ProduireRolesOIPMResults extends ProduireRolesResults<ProduireRolesOIPMResults> {

	private final RolesPM roles = new RolesPM();

	public ProduireRolesOIPMResults(int anneePeriode, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(anneePeriode, nbThreads, dateTraitement, tiersService, adresseService);
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
	public void addAll(ProduireRolesOIPMResults rapport) {
		super.addAll(rapport);
		roles.addAll(rapport.roles);
	}

	public List<InfoContribuablePM> buildInfoPourRegroupementCommunes(Collection<Integer> noOfsCommunes) {
		return roles.buildInfosPourRegroupementCommunes(noOfsCommunes);
	}

	@Override
	public Set<Integer> getNoOfsCommunesTraitees() {
		return roles.getNoOfsCommunesTraitees();
	}
}