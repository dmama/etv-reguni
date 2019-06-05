package ch.vd.unireg.evenement.civil.engine.ech;

import ch.vd.unireg.data.CivilDataEventNotifier;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.tiers.TiersService;

/**
 * Façade d'une stratégie de translation d'événement civil ECH qui invalide le cache des individus qui
 * possèdent ou ont possédé une relation civile (parents, enfants, conjoints) avec l'individu de l'événement à traiter
 */
public class TranslationStrategyWithRelationshipCacheCleanupFacade extends AbstractTranslationStrategyWithRelationshipCacheCleanup {

	private final EvenementCivilEchTranslationStrategy target;

	public TranslationStrategyWithRelationshipCacheCleanupFacade(EvenementCivilEchTranslationStrategy target, ServiceCivilService serviceCivil, CivilDataEventNotifier civilDataEventNotifier,
	                                                             TiersService tiersService) {
		super(serviceCivil, civilDataEventNotifier, tiersService);
		this.target = target;
	}

	@Override
	protected EvenementCivilInterne doCreate(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		return target.create(event, context, options);
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return target.isPrincipalementIndexation(event, context);
	}
}
