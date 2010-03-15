package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Adapter retournant la liste des champs à indexer pour le ménage commun.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class MenageCommunSubIndexable extends ContribuableSubIndexable {

	//private Logger LOGGER = Logger.getLogger(MenageCommunSubIndexable.class);

	public MenageCommunSubIndexable(TiersService tiersService, MenageCommun menageCommun) throws IndexerException {
		super(tiersService, menageCommun);
	}
}
