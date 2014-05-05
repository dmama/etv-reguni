package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.NatureJuridique;

/**
 * @author Sean Paul
 *
 */
public class AutreCommunauteIndexable extends ContribuableIndexable<AutreCommunaute> {

	public static final String SUB_TYPE = "autrecommunaute";

	public AutreCommunauteIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, AutreCommunaute autreCommunaute) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, autreCommunaute);
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		data.addNomRaison(tiers.getNom());
		data.addNomRaison(tiers.getComplementNom());
		data.setNom1(tiers.getNom());
		data.setNom2(tiers.getComplementNom());
		data.setNatureJuridique(IndexerFormatHelper.enumToString(NatureJuridique.PM));

		// TODO IDE : ajouter l'IDE
//		data.addIde(ac.getIde());
	}
}
