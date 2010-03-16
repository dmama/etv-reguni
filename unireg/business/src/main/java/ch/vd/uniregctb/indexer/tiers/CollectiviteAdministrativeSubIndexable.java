package ch.vd.uniregctb.indexer.tiers;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.TiersService;

public class CollectiviteAdministrativeSubIndexable extends ContribuableSubIndexable {

	public static final String F_NOM = "NOM_COLLECTIVITE";
	public static final String F_ID = "ID_COLLECTIVITE";

	AdresseService tiersService = null;
	CollectiviteAdministrative collectivite = null;

	public CollectiviteAdministrativeSubIndexable(AdresseService adresseService, TiersService tiersService, CollectiviteAdministrative collectivite) throws IndexerException {
		super(tiersService, collectivite);

		this.tiersService = adresseService;
		this.collectivite = collectivite;
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {
		super.fillKeyValues(map);
		try {
			long noColAdm = collectivite.getNumeroCollectiviteAdministrative();
			ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative collectiviteCivile = tiersService.getServiceInfra()
					.getCollectivite((int) noColAdm);
			Assert.notNull(collectiviteCivile);

			map.putRawValue(F_NOM, collectiviteCivile.getNomComplet1());
			map.putRawValue(F_ID, String.valueOf(noColAdm));
		}
		catch (Exception e) {
			throw new IndexerException(collectivite, "Problème lors de l'indexation de la collectivite administrative", e);
		}
	}
}
