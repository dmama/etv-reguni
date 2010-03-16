package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.NatureJuridique;

/**
 * @author Sean Paul
 *
 */
public class EntrepriseIndexable extends ContribuableIndexable {

	/** LOGGER */
	//private static final Logger LOGGER = Logger.getLogger(EntrepriseIndexable.class);

	public static final String SUB_TYPE = "entreprise";

	//private ForFiscalSubIndexable forsIndexable = null;

	/**
	 * @param serviceInfrastructure
	 * @param contribuable
	 * @param individu
	 * @throws IndexerException
	 */
	public EntrepriseIndexable(AdresseService adresseService, TiersService tiersService, Entreprise entreprise) throws IndexerException {
		super(adresseService, tiersService, entreprise, new EntrepriseSubIndexable(tiersService, entreprise));

		// TODO(MSI) : A corriger quand les Entreprise seront liées par HI
		//forsIndexable = new ForFiscalSubIndexable(serviceInfrastructure, entreprise);
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

		return values;
	}

}
