package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class ContribuableImpositionPersonnesMoralesIndexable<T extends ContribuableImpositionPersonnesMorales> extends ContribuableIndexable<T> {

	protected ContribuableImpositionPersonnesMoralesIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService,
	                                                          ServiceInfrastructureService serviceInfra, AvatarService avatarService, T contribuable) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, contribuable);
	}

	@Override
	protected void fillAssujettissementData(TiersIndexableData data) {
		super.fillAssujettissementData(data);

		final Assujettissement assujettissement = tiersService.getAssujettissement(tiers, null);
		final TypeAssujettissement type = assujettissement != null ? assujettissement.getType() : TypeAssujettissement.NON_ASSUJETTI;
		data.setAssujettissementPM(type);
	}
}
