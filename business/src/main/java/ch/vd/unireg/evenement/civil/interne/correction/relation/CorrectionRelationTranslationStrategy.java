package ch.vd.unireg.evenement.civil.interne.correction.relation;

import ch.vd.unireg.data.CivilDataEventService;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.engine.ech.AbstractTranslationStrategyWithRelationshipCacheCleanup;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.tiers.TiersService;

public class CorrectionRelationTranslationStrategy extends AbstractTranslationStrategyWithRelationshipCacheCleanup {

	public CorrectionRelationTranslationStrategy(ServiceCivilService serviceCivil, CivilDataEventService dataEventService, TiersService tiersService) {
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
