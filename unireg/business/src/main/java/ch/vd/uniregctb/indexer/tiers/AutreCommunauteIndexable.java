package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
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

	public static final String SUB_TYPE = "autreCommunaute";

	/**
	 * @param serviceInfra
	 * @throws IndexerException
	 */
	public AutreCommunauteIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, AutreCommunaute autreCommunaute) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, autreCommunaute, new AutreCommunauteSubIndexable(tiersService, autreCommunaute));
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {

		HashMap<String, String> values = super.getKeyValues();
		// Search
		String s = NatureJuridique.PM.toString();
		values.put(TiersSearchFields.NATURE_JURIDIQUE, s);

		// Tiers
		HashMap<String, String> subValues = tiersSubIndexable.getKeyValues();
		// Search
		addValueToMap(values, TiersSearchFields.NUMEROS, subValues, TiersSubIndexable.F_NUMERO);
		addValueToMap(values, TiersSearchFields.NOM_RAISON, subValues, AutreCommunauteSubIndexable.F_NOM);
		addValueToMap(values, TiersSearchFields.NOM_RAISON, subValues, AutreCommunauteSubIndexable.F_COMPLEMENT_NOM);
		// Display
		addValueToMap(values, TiersIndexedData.NOM1, subValues, AutreCommunauteSubIndexable.F_NOM);
		addValueToMap(values, TiersIndexedData.NOM2, subValues, AutreCommunauteSubIndexable.F_COMPLEMENT_NOM);

		return values;
	}

}
