package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.NatureJuridique;

public abstract class PersonnePhysiqueIndexable extends ContribuableIndexable {

	public PersonnePhysiqueIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, PersonnePhysique pp,
	                                 PersonnePhysiqueSubIndexable ppSubIndexable) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, pp, ppSubIndexable);
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {
		final HashMap<String, String> values = super.getKeyValues();

		String s = NatureJuridique.PP.toString();
		values.put(TiersIndexableData.NATURE_JURIDIQUE, s);

		return values;
	}
}
