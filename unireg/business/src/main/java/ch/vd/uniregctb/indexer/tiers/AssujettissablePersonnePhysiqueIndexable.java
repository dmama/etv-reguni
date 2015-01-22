package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class AssujettissablePersonnePhysiqueIndexable<T extends Contribuable> extends ContribuableIndexable<T> {

	protected AssujettissablePersonnePhysiqueIndexable(AdresseService adresseService, TiersService tiersService,
	                                                 ServiceInfrastructureService serviceInfra, T contribuable) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, contribuable);
	}

	@Override
	protected void fillAssujettissementData(TiersIndexableData data) {
		super.fillAssujettissementData(data);

		final Assujettissement assujettissement = tiersService.getAssujettissement(tiers, null);
		final TypeAssujettissement type = assujettissement != null ? assujettissement.getType() : TypeAssujettissement.NON_ASSUJETTI;
		data.setAssujettissementPP(type);
	}
}
