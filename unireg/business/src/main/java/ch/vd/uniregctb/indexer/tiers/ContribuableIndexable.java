package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class ContribuableIndexable extends TiersIndexable {

	// private static final Logger LOGGER = Logger.getLogger(ContribuableIndexable.class);

	public ContribuableIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, Contribuable contribuable,
	                             ContribuableSubIndexable ctbSubIndexable) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, contribuable, ctbSubIndexable);
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {
		HashMap<String, String> values = super.getKeyValues();

		// CTB
		HashMap<String, String> subValues = tiersSubIndexable.getKeyValues();
		// Search
		addValueToMap(values, TiersIndexableData.MODE_IMPOSITION, subValues, ContribuableSubIndexable.F_MODE_IMPOSITION);

		return values;
	}

}
