package ch.vd.uniregctb.evenement.civil.interne.correction.relation;

import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.engine.ech.AbstractTranslationStrategyWithRelationshipCacheCleanup;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.TiersService;

public class CorrectionRelationTranslationStrategy extends AbstractTranslationStrategyWithRelationshipCacheCleanup {

	public CorrectionRelationTranslationStrategy(ServiceCivilService serviceCivil, DataEventService dataEventService, TiersService tiersService) {
		super(serviceCivil, dataEventService, tiersService);
	}

	@Override
	protected EvenementCivilInterne doCreate(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return new CorrectionRelation(event, context, options);
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return false;
	}
}
