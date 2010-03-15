package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class HabitantSubIndexable extends PersonnePhysiqueSubIndexable {

	public static final String F_NUMERO_INDIVIDU = "NUMERO_INDIVIDU";
	public final static String F_DATE_DECES = "DATE_DECES";

	private final PersonnePhysique hab;

	public HabitantSubIndexable(TiersService tiersService, PersonnePhysique ctb) throws IndexerException {
		super(tiersService, ctb);
		this.hab = ctb;
		//addField("NumeroIndividu", ctb, F_NUMERO_INDIVIDU);
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {
		super.fillKeyValues(map);
		if (hab.getDateDeces() != null)
			map.putRawValue(F_DATE_DECES, hab.getDateDeces());
	}
}
