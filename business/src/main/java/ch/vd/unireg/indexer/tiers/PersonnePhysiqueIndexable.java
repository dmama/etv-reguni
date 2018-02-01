package ch.vd.unireg.indexer.tiers;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.IndexerFormatHelper;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.NatureJuridique;

public abstract class PersonnePhysiqueIndexable extends ContribuableImpositionPersonnesPhysiquesIndexable<PersonnePhysique> {

	public PersonnePhysiqueIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService,
	                                 ServiceInfrastructureService serviceInfra, AvatarService avatarService, PersonnePhysique pp) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp);
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);
		data.setNatureJuridique(IndexerFormatHelper.enumToString(NatureJuridique.PP));
		data.setAncienNumeroSourcier(IndexerFormatHelper.numberToString(tiers.getAncienNumeroSourcier()));
		data.setConnuAuCivil(tiers.isConnuAuCivil());
	}
}
