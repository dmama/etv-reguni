package ch.vd.uniregctb.role;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;

public class RolePMOfficeResults extends RolePMResults<RolePMOfficeResults> {

	public final List<RolePMData> extraction = new LinkedList<>();

	public RolePMOfficeResults(int annee, int nbThreads, AdresseService adresseService, ServiceInfrastructureService infraService, TiersService tiersService) {
		super(annee, nbThreads, adresseService, infraService, tiersService);
	}

	@Override
	public void addAll(RolePMOfficeResults right) {
		this.extraction.addAll(right.extraction);
		super.addAll(right);
	}

	@Override
	public void end() {
		Collections.sort(this.extraction, Comparator.comparingLong(data -> data.noContribuable));
		super.end();
	}

	public void addToRole(Entreprise entreprise, int ofsCommune) {
		this.extraction.add(new RolePMData(entreprise, ofsCommune, annee, adresseService, infraService, tiersService));
		addContribuableAuDecompte();
	}
}
