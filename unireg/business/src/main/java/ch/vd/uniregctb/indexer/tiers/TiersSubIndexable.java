package ch.vd.uniregctb.indexer.tiers;

import java.util.Date;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.indexer.AbstractSubIndexable;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class TiersSubIndexable extends AbstractSubIndexable {

	//private Logger LOGGER = Logger.getLogger(TiersSubIndexable.class);

	public static final String F_NUMERO = "NUMERO";
	public static final String F_ROLE_LIGNE1 = "ROLE_LIGNE1";
	public static final String F_ROLE_LIGNE2 = "ROLE_LIGNE2";
	public static final String F_REMARQUE = "REMARQUE";
	public static final String F_ANNULE = "ANNULE";
	public static final String F_DEBITEUR_INACTIF = "DEBITEUR_INACTIF";
	public static final String F_INDEXATION_DATE = "INDEXATION_DATE";

	private final Tiers tiers;

	private final TiersService tiersService;

	public TiersSubIndexable(TiersService tiersService, Tiers tiers) throws IndexerException {
		Assert.notNull(tiers);
		Assert.notNull(tiersService);

		this.tiers = tiers;
		this.tiersService = tiersService;
	}

	public Tiers getTiers() {
		return tiers;
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {
		map.putRawValue(F_NUMERO, tiers.getId());
		map.putRawValue(F_REMARQUE, tiers.getRemarque());
		map.putRawValue(F_ROLE_LIGNE1, tiers.getRoleLigne1());
		map.putRawValue(F_ROLE_LIGNE2, tiersService.getRoleAssujettissement(tiers, RegDate.get()));
		map.putRawValue(F_ANNULE, tiers.isAnnule());
		map.putRawValue(F_DEBITEUR_INACTIF, tiers.isDebiteurInactif());

		Long millisecondes = new Date().getTime();
		map.putRawValue(F_INDEXATION_DATE, millisecondes);
	}

}
