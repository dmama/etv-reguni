package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class ContribuableSubIndexable extends TiersSubIndexable {

	//private Logger LOGGER = Logger.getLogger(ContribuableIndividuelSubIndexable.class);
	private final Contribuable ctb;

	public static final String F_MODE_IMPOSITION = "MODE_IMPOSITION";

	public ContribuableSubIndexable(TiersService tiersService, Contribuable ctb) throws IndexerException {
		super(tiersService, ctb);
		this.ctb = ctb;
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {
		super.fillKeyValues(map);

		final ForFiscalPrincipal ffp = ctb.getDernierForFiscalPrincipal();
		if (ffp != null) {
			map.putRawValue(F_MODE_IMPOSITION, ffp.getModeImposition().toString());
		}
		
		final boolean isActif = (ffp != null && ffp.isValidAt(null));
		map.putRawValue(F_TIERS_ACTIF, isActif);
	}

}
