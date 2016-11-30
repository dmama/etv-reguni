package ch.vd.uniregctb.role;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class RolePMResults<R extends RolePMResults<R>> extends RoleResults<R> {

	public RolePMResults(int annee, int nbThreads, AdresseService adresseService, ServiceInfrastructureService infraService, TiersService tiersService) {
		super(annee, nbThreads, adresseService, infraService, tiersService);
	}

	@Override
	public final TypePopulationRole getTypePopulationRole() {
		return TypePopulationRole.PM;
	}
}
