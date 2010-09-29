package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.NatureJuridique;

public class EntrepriseIndexable extends ContribuableIndexable {

	public static final String SUB_TYPE = "entreprise";

	private final PersonneMorale pm;

	public EntrepriseIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, Entreprise entreprise, PersonneMorale pm) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, entreprise);
		this.pm = pm;
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);
		data.setNatureJuridique(IndexerFormatHelper.objectToString(NatureJuridique.PM));
		data.addNomRaison(pm.getRaisonSociale1());
		data.addNomRaison(pm.getRaisonSociale2());
		data.addNomRaison(pm.getRaisonSociale3());
		data.setNom1(pm.getRaisonSociale());
	}
}
