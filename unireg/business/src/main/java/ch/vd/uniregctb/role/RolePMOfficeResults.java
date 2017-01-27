package ch.vd.uniregctb.role;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;

public class RolePMOfficeResults extends RolePMResults<RolePMOfficeResults> {

	public final List<RolePMData> extraction = new LinkedList<>();

	public RolePMOfficeResults(int annee, int nbThreads, AdresseService adresseService, ServiceInfrastructureService infraService, TiersService tiersService, AssujettissementService assujettissementService) {
		super(annee, nbThreads, adresseService, infraService, tiersService, assujettissementService);
	}

	@Override
	public void addAll(RolePMOfficeResults right) {
		this.extraction.addAll(right.extraction);
		super.addAll(right);
	}

	@Override
	public void end() {
		this.extraction.sort(Comparator.comparingLong(data -> data.noContribuable));
		super.end();
	}

	public void addToRole(Entreprise entreprise, int ofsCommune) throws CalculRoleException {
		this.extraction.add(new RolePMData(entreprise, ofsCommune, annee, adresseService, infraService, tiersService, assujettissementService));
		addContribuableAuDecompte();
	}
}
