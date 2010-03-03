package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import org.apache.log4j.Logger;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * @author Sean Paul
 *
 */
public class CollectiviteAdministrativeIndexable extends ContribuableIndexable {

	/** LOGGER */
	private static final Logger LOGGER = Logger.getLogger(CollectiviteAdministrativeIndexable.class);

	public static final String SUB_TYPE = "collectiviteadministrative";

	/**
	 * @param serviceInfra
	 * @throws IndexerException
	 */
	public CollectiviteAdministrativeIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, CollectiviteAdministrative collectivite) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, collectivite, new CollectiviteAdministrativeSubIndexable(adresseService, tiersService, serviceInfra, collectivite));
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {

		HashMap<String, String> values = super.getKeyValues();

		HashMap<String, String> subValues = tiersSubIndexable.getKeyValues();

		// Search
		addValueToMap(values, TiersSearchFields.NOM_RAISON, subValues, CollectiviteAdministrativeSubIndexable.F_NOM);
		addValueToMap(values, TiersSearchFields.NUMEROS, subValues, CollectiviteAdministrativeSubIndexable.F_ID);

		// Display
		addValueToMap(values, TiersIndexedData.NOM1, subValues, CollectiviteAdministrativeSubIndexable.F_NOM);

		return values;
	}

}
