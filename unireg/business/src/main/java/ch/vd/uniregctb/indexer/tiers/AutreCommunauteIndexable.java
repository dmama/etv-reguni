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
public class AutreCommunauteIndexable extends ContribuableIndexable {

	//private static final Logger LOGGER = Logger.getLogger(AutreCommunauteIndexable.class);

	public static final String SUB_TYPE = "autrecommunaute";

	public AutreCommunauteIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, AutreCommunaute autreCommunaute) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, autreCommunaute);
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final AutreCommunaute ac = (AutreCommunaute) tiers;

		data.addNomRaison(ac.getNom());
		data.addNomRaison(ac.getComplementNom());
		data.setNom1(ac.getNom());
		data.setNom2(ac.getComplementNom());
		data.setNatureJuridique(IndexerFormatHelper.objectToString(NatureJuridique.PM));
	}
}
