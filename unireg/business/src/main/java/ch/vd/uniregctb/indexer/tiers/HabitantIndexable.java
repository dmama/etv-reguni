package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import org.springframework.util.Assert;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * @author Sean Paul
 *
 */
public class HabitantIndexable extends PersonnePhysiqueIndexable {

	//private static final Logger LOGGER = Logger.getLogger(HabitantIndexable.class);

	/**
	 * La sous entité liée à indexer (Individu)
	 *
	 */
	private final IndividuSubIndexable individuSubIndexable;

	public static final String SUB_TYPE = "habitant";



	/**
	 * @param serviceInfra
	 *@param individu  @throws IndexerException
	 */
	public HabitantIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, PersonnePhysique hab, Individu individu) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, hab, new HabitantSubIndexable(tiersService, hab));
		Assert.notNull(individu);

		individuSubIndexable = new IndividuSubIndexable(individu);
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	/**
	 *
	 * @see ch.vd.uniregctb.indexer.SubIndexable#getKeyValues()
	 */
	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {

		// Récupère les valeurs communes (adresses, fors)
		HashMap<String, String> values = super.getKeyValues();

		HashMap<String, String> subValues = tiersSubIndexable.getKeyValues();
		// Tiers
		addValueToMap(values, TiersSearchFields.NUMEROS, subValues, TiersSubIndexable.F_NUMERO);

		HashMap<String, String> indSubValues = individuSubIndexable.getKeyValues();
		// Individu search
		addValueToMap(values, TiersSearchFields.AUTRES_NOM, indSubValues, IndividuSubIndexable.F_NOM);
		addValueToMap(values, TiersSearchFields.AUTRES_NOM, indSubValues, IndividuSubIndexable.F_PRENOM);
		addValueToMap(values, TiersSearchFields.AUTRES_NOM, indSubValues, IndividuSubIndexable.F_NOM_NAISSANCE);
		addValueToMap(values, TiersSearchFields.DATE_NAISSANCE, indSubValues, IndividuSubIndexable.F_DATE_NAISSANCE);
		addValueToMap(values, TiersSearchFields.NOM_RAISON, indSubValues, IndividuSubIndexable.F_NOM);
		addValueToMap(values, TiersSearchFields.NUMERO_ASSURE_SOCIAL, indSubValues, IndividuSubIndexable.F_NO_ASSURE_SOCIAL);
		addValueToMap(values, TiersSearchFields.NUMERO_ASSURE_SOCIAL, indSubValues, IndividuSubIndexable.F_ANCIEN_NUMERO_AVS);
		addValueToMap(values, TiersSearchFields.NO_SYMIC, indSubValues, IndividuSubIndexable.F_NO_SYMIC);
		// Individu Display
		addValueToMap(values, TiersIndexedData.NOM1, indSubValues, IndividuSubIndexable.F_NOM);
		addValueToMap(values, TiersIndexedData.NOM1, indSubValues, IndividuSubIndexable.F_PRENOM);
		addValueToMap(values, TiersIndexedData.DATE_NAISSANCE, indSubValues, IndividuSubIndexable.F_DATE_NAISSANCE);
		if (subValues.containsKey(HabitantSubIndexable.F_DATE_DECES)) {//surcharge de la date de décès
			addValueToMap(values, TiersIndexedData.DATE_DECES, subValues, HabitantSubIndexable.F_DATE_DECES);
		}
		else {
			addValueToMap(values, TiersIndexedData.DATE_DECES, indSubValues, IndividuSubIndexable.F_DATE_DECES);
		}
		return values;
	}

}
