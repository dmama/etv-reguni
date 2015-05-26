package ch.vd.uniregctb.migration.pm.rcent.service;

import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.uniregctb.migration.pm.rcent.model.main.RCEntOrganisation;
import ch.vd.uniregctb.migration.pm.rcent.model.main.RCEntOrganisationLocation;

/**
 * Adapteur / abstraction de service pour RC-ENT. Expose les requêtes dont nous avons besoin.
 */
public class RCEntService {

	// TODO: Acceder au client, faire faire les conversions nécessaires par l'historizer et générer les instances RCEnt correspondantes.

	private RcEntClient rcentClient;

	public RCEntService(RcEntClient rcentClient) {
		this.rcentClient = rcentClient;
	}

	public RCEntOrganisation getEntreprise(Long id) {
        return null;
    }

    public RCEntOrganisationLocation getEtablissement(Long id) {
        return null;
    }
}
