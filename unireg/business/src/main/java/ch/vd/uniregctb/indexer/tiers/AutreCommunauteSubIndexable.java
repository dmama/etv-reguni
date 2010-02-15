package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.TiersService;

public class AutreCommunauteSubIndexable extends ContribuableSubIndexable {

	public static final String F_NOM = "NOM";
	public static final String F_COMPLEMENT_NOM = "COMPLEMENT_NOM";
	public static final String F_FORME_JURIDIQUE = "FORME_JURIDIQUE";

	private final AutreCommunaute communaute;

	public AutreCommunauteSubIndexable(TiersService tiersService, AutreCommunaute communaute) throws IndexerException {
		super(tiersService, communaute);
		this.communaute = communaute;
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {
		super.fillKeyValues(map);
		map.putRawValue(F_NOM, communaute.getNom());
		map.putRawValue(F_COMPLEMENT_NOM, communaute.getComplementNom());
	}
}
