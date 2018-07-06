package ch.vd.unireg.role;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.TiersService;

public abstract class RolePPResults<R extends RolePPResults<R>> extends RoleResults<R> {

	public RolePPResults(int annee, int nbThreads, AdresseService adresseService, ServiceInfrastructureService infraService, TiersService tiersService, AssujettissementService assujettissementService) {
		super(annee, nbThreads, adresseService, infraService, tiersService, assujettissementService);
	}

	@Override
	public final TypePopulationRole getTypePopulationRole() {
		return TypePopulationRole.PP;
	}

	@Override
	public String getTypePopulationRoleName() {
		return getTypePopulationRole().name();
	}
}
