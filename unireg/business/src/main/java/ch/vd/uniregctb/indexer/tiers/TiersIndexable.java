package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import org.springframework.util.Assert;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.AbstractIndexable;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class TiersIndexable extends AbstractIndexable {

	//private Logger LOGGER = Logger.getLogger(TiersIndexable.class);

	public static final String TYPE = "tiers";

	private final Tiers tiers;

	/** Les adresses à indexer */
	protected final TiersSubIndexable tiersSubIndexable;
	private final AdressesTiersSubIndexable adressesSubIndexable;
	private final ForFiscalSubIndexable forsIndexable;

	public TiersIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, Tiers tiers, TiersSubIndexable tiersSubIndexable) throws IndexerException {
		Assert.notNull(tiers);
		Assert.notNull(adresseService);

		this.tiers = tiers;
		this.tiersSubIndexable = tiersSubIndexable;
		this.forsIndexable = new ForFiscalSubIndexable(serviceInfra, tiers);
		this.adressesSubIndexable = new AdressesTiersSubIndexable(adresseService, serviceInfra, tiers);
	}

	public String getType() {
		return TYPE;
	}

	public Long getID() {
		return tiers.getId();
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {

		HashMap<String, String> values = new HashMap<String, String>();

		// Tiers
		{
			HashMap<String, String> subValues = tiersSubIndexable.getKeyValues();

			//Search
			addValueToMap(values, TiersIndexableData.ANNULE, subValues, TiersSubIndexable.F_ANNULE);
			addValueToMap(values, TiersIndexableData.DEBITEUR_INACTIF, subValues, TiersSubIndexable.F_DEBITEUR_INACTIF);
			addValueToMap(values, TiersIndexableData.TIERS_ACTIF, subValues, TiersSubIndexable.F_TIERS_ACTIF);
			// Display
			addValueToMap(values, TiersIndexableData.ROLE_LIGNE1, subValues, TiersSubIndexable.F_ROLE_LIGNE1);
			addValueToMap(values, TiersIndexableData.ROLE_LIGNE2, subValues, TiersSubIndexable.F_ROLE_LIGNE2);
			addValueToMap(values, TiersIndexableData.INDEXATION_DATE, subValues, TiersSubIndexable.F_INDEXATION_DATE);
		}

		// Adresses
		{
			HashMap<String, String> subValues = adressesSubIndexable.getKeyValues();

			// Search fields
			addValueToMap(values, TiersIndexableData.LOCALITE_PAYS, subValues, AdressesTiersSubIndexable.F_LOCALITE);
			addValueToMap(values, TiersIndexableData.LOCALITE_PAYS, subValues, AdressesTiersSubIndexable.F_PAYS);
			addValueToMap(values, TiersIndexableData.NPA, subValues, AdressesTiersSubIndexable.F_NPA);

			// Display fields
			addValueToMap(values, TiersIndexableData.RUE, subValues, AdressesTiersSubIndexable.F_RUE);
			addValueToMap(values, TiersIndexableData.LOCALITE, subValues, AdressesTiersSubIndexable.F_LOCALITE);
			addValueToMap(values, TiersIndexableData.PAYS, subValues, AdressesTiersSubIndexable.F_PAYS);
			addValueToMap(values, TiersIndexableData.DOMICILE_VD, subValues, AdressesTiersSubIndexable.F_DOMICILE_VD);
			addValueToMap(values, TiersIndexableData.NO_OFS_DOMICILE_VD, subValues, AdressesTiersSubIndexable.F_NO_OFS_DOMICILE_VD);
		}

		// Fors
		{
			HashMap<String, String> subValues = forsIndexable.getKeyValues();

			// Search
			addValueToMap(values, TiersIndexableData.NO_OFS_FOR_PRINCIPAL, subValues, ForFiscalSubIndexable.F_NO_OFS_FOR_PRINCIPAL_ACTIF);
			addValueToMap(values, TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL, subValues, ForFiscalSubIndexable.F_TYPE_OFS_FOR_PRINCIPAL_ACTIF);
			addValueToMap(values, TiersIndexableData.NOS_OFS_AUTRES_FORS, subValues, ForFiscalSubIndexable.F_NOS_OFS_AUTRES_FORS);

			// Display
			addValueToMap(values, TiersIndexableData.FOR_PRINCIPAL, subValues, ForFiscalSubIndexable.F_DERNIER_FOR_PRINCIPAL);
		}

		return values;
	}

	/**
	 * Helper method
	 * Ajoute une valeur a la map passée en paramètre
	 * Si la valeur est déja présente dans la map, la valeur courante est cooncaténée acelle existante
	 *
	 * @param values
	 * @param tiersField
	 * @param subValues
	 * @param subField
	 */
	protected static void addValueToMap(HashMap<String, String> values, String tiersField, HashMap<String, String> subValues, String subField) {

		// La valeur du Tiers courant
		String v = subValues.get(subField);

		// La valeur existante?
		String value = values.get(tiersField);
		if (value != null) {
			// Concaténé
			if (!"".equals(v)) {
				value += " " + v;
			}
		}
		else {
			// Nouvelle valeur
			value = v;
		}
		values.put(tiersField, value);
	}
}
