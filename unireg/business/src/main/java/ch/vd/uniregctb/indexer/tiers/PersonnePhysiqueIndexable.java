package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.NatureJuridique;

public abstract class PersonnePhysiqueIndexable extends ContribuableImpositionPersonnesPhysiquesIndexable<PersonnePhysique> {

	public PersonnePhysiqueIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, AvatarService avatarService, PersonnePhysique pp) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, avatarService, pp);
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);
		data.setNatureJuridique(IndexerFormatHelper.enumToString(NatureJuridique.PP));
		data.setAncienNumeroSourcier(IndexerFormatHelper.numberToString(tiers.getAncienNumeroSourcier()));
	}
}
