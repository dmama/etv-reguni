package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class PersonnePhysiqueSubIndexable extends ContribuableSubIndexable {

	public PersonnePhysiqueSubIndexable(TiersService tiersService, PersonnePhysique ctb) throws IndexerException {
		super(tiersService, ctb);
	}

}
