package ch.vd.uniregctb.indexer.tiers;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.TiersService;

public class CollectiviteAdministrativeIndexable extends ContribuableIndexable {

//	private static final Logger LOGGER = Logger.getLogger(CollectiviteAdministrativeIndexable.class);

	public static final String SUB_TYPE = "collectiviteadministrative";

	public CollectiviteAdministrativeIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, CollectiviteAdministrative collectivite) throws
			IndexerException {
		super(adresseService, tiersService, serviceInfra, collectivite);
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final CollectiviteAdministrative ca = (CollectiviteAdministrative) tiers;
		final long noColAdm = ca.getNumeroCollectiviteAdministrative();
		final ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative collectiviteCivile;
		try {
			collectiviteCivile = serviceInfra.getCollectivite((int) noColAdm);
		}
		catch (InfrastructureException e) {
			throw new RuntimeException(e);
		}
		Assert.notNull(collectiviteCivile);

		data.setNom1(collectiviteCivile.getNomComplet1());
		data.setNomRaison(collectiviteCivile.getNomComplet1());
	}
}
