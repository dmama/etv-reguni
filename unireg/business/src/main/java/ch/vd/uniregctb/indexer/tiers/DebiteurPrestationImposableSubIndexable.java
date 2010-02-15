package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersService;

public class DebiteurPrestationImposableSubIndexable extends TiersSubIndexable {

	public static final String F_NOM1 = "NOM1";

	public static final String F_NOM2 = "NOM2";

	public static final String F_CATEGORIE_IS = "CATEGORIE_IS";

	public static final String F_COMPLEMENT_NOM = "COMPLEMENT_NOM";

	private final DebiteurPrestationImposable dis;

	public DebiteurPrestationImposableSubIndexable(TiersService tiersService, DebiteurPrestationImposable dis) throws IndexerException {
		super(tiersService, dis);
		this.dis = dis;
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {
		super.fillKeyValues(map);
		map.putRawValue(F_NOM1, dis.getNom1());
		map.putRawValue(F_NOM2, dis.getNom2());
		map.putRawValue(F_CATEGORIE_IS, dis.getCategorieImpotSource());
		map.putRawValue(F_COMPLEMENT_NOM, dis.getComplementNom());
	}
}
