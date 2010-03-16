package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;

public class EntrepriseSubIndexable extends ContribuableSubIndexable {

	public EntrepriseSubIndexable(TiersService tiersService, Entreprise entreprise) throws IndexerException {
		super(tiersService, entreprise);
	}
}
