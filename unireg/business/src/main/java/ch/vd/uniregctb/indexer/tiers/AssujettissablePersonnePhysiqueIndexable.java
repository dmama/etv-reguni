package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class AssujettissablePersonnePhysiqueIndexable extends ContribuableIndexable {

	protected AssujettissablePersonnePhysiqueIndexable(AdresseService adresseService, TiersService tiersService,
	                                                   ServiceInfrastructureService serviceInfra, MenageCommun mc) throws IndexerException {
		this(adresseService, tiersService, serviceInfra, (Contribuable) mc);
	}

	protected AssujettissablePersonnePhysiqueIndexable(AdresseService adresseService, TiersService tiersService,
	                                                   ServiceInfrastructureService serviceInfra, PersonnePhysique pp) throws IndexerException {
		this(adresseService, tiersService, serviceInfra, (Contribuable) pp);
	}

	private AssujettissablePersonnePhysiqueIndexable(AdresseService adresseService, TiersService tiersService,
	                                                 ServiceInfrastructureService serviceInfra, Contribuable contribuable) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, contribuable);
	}

	@Override
	protected void fillAssujettissementData(TiersIndexableData data) {
		super.fillAssujettissementData(data);

		final Assujettissement assujettissement = tiersService.getAssujettissement((Contribuable) tiers, null);
		final TypeAssujettissement type = assujettissement != null ? assujettissement.getType() : TypeAssujettissement.NON_ASSUJETTI;
		data.setAssujettissementPP(type);
	}
}
