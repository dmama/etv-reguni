package ch.vd.unireg.indexer.tiers;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.TiersService;

public abstract class ContribuableImpositionPersonnesPhysiquesIndexable<T extends ContribuableImpositionPersonnesPhysiques> extends ContribuableIndexable<T> {

	protected ContribuableImpositionPersonnesPhysiquesIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService,
	                                                            ServiceInfrastructureService serviceInfra, AvatarService avatarService, T contribuable) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, contribuable);
	}

	@Override
	protected void fillAssujettissementData(TiersIndexableData data) {
		super.fillAssujettissementData(data);

		final Assujettissement assujettissement = tiersService.getAssujettissement(tiers, null);
		final TypeAssujettissement type = assujettissement != null ? assujettissement.getType() : TypeAssujettissement.NON_ASSUJETTI;
		data.setAssujettissementPP(type);
	}
}
