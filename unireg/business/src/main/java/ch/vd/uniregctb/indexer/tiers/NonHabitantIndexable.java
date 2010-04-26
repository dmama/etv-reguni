package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class NonHabitantIndexable extends PersonnePhysiqueIndexable {

	//private static final Logger LOGGER = Logger.getLogger(NonHabitantIndexable.class);

	public static final String SUB_TYPE = "nonhabitant";

	public NonHabitantIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, PersonnePhysique nonHabitant) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, nonHabitant, new NonHabitantSubIndexable(tiersService, nonHabitant));
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {

		HashMap<String, String> values = super.getKeyValues();

		HashMap<String, String> subValues = tiersSubIndexable.getKeyValues();

		// Search values
		addValueToMap(values, TiersSearchFields.NUMEROS, subValues, NonHabitantSubIndexable.F_ID);
		addValueToMap(values, TiersSearchFields.NOM_RAISON, subValues, NonHabitantSubIndexable.F_NOM);
		addValueToMap(values, TiersSearchFields.AUTRES_NOM, subValues, NonHabitantSubIndexable.F_NOM);
		addValueToMap(values, TiersSearchFields.AUTRES_NOM, subValues, NonHabitantSubIndexable.F_PRENOM);
		addValueToMap(values, TiersSearchFields.DATE_NAISSANCE, subValues, NonHabitantSubIndexable.F_DATE_NAISSANCE);
		addValueToMap(values, TiersSearchFields.NUMERO_ASSURE_SOCIAL, subValues, NonHabitantSubIndexable.F_NO_ASSURE_SOCIAL);
		addValueToMap(values, TiersSearchFields.NUMERO_ASSURE_SOCIAL, subValues, NonHabitantSubIndexable.F_ANCIEN_NUMERO_AVS);

		// Display values
		addValueToMap(values, TiersIndexedData.NOM1, subValues, NonHabitantSubIndexable.F_NOM);
		addValueToMap(values, TiersIndexedData.NOM1, subValues, NonHabitantSubIndexable.F_PRENOM);
		addValueToMap(values, TiersIndexedData.DATE_NAISSANCE, subValues, NonHabitantSubIndexable.F_DATE_NAISSANCE);
		addValueToMap(values, TiersIndexedData.DATE_DECES, subValues, NonHabitantSubIndexable.F_DATE_DECES);

		return values;
	}


}
