package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class ContribuableImpositionPersonnesPhysiquesIndexable<T extends ContribuableImpositionPersonnesPhysiques> extends ContribuableIndexable<T> {

	protected ContribuableImpositionPersonnesPhysiquesIndexable(AdresseService adresseService, TiersService tiersService,
	                                                            ServiceInfrastructureService serviceInfra, AvatarService avatarService, T contribuable) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, avatarService, contribuable);
	}

	@Override
	protected void fillAssujettissementData(TiersIndexableData data) {
		super.fillAssujettissementData(data);

		final Assujettissement assujettissement = tiersService.getAssujettissement(tiers, null);
		final TypeAssujettissement type = assujettissement != null ? assujettissement.getType() : TypeAssujettissement.NON_ASSUJETTI;
		data.setAssujettissementPP(type);
	}
}
