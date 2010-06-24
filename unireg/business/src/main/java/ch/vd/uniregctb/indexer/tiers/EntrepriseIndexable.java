package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.NatureJuridique;

public class EntrepriseIndexable extends ContribuableIndexable {

	public static final String SUB_TYPE = "entreprise";

	public EntrepriseIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, Entreprise entreprise) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, entreprise);
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);
		data.setNatureJuridique(IndexerFormatHelper.objectToString(NatureJuridique.PM));
	}
}
