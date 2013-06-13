package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collection;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

/**
 * Façade d'une stratégie de translation d'événement civil ECH qui invalide le cache des individus qui
 * possèdent ou ont possédé une relation civile (parents, enfants, conjoints) avec l'individu de l'événement à traiter
 */
public class TranslationStrategyWithRelationshipCacheCleanup implements EvenementCivilEchTranslationStrategy {

	private final EvenementCivilEchTranslationStrategy target;
	private final ServiceCivilService serviceCivil;
	private final DataEventService dataEventService;

	public TranslationStrategyWithRelationshipCacheCleanup(EvenementCivilEchTranslationStrategy target, ServiceCivilService serviceCivil, DataEventService dataEventService) {
		this.target = target;
		this.serviceCivil = serviceCivil;
		this.dataEventService = dataEventService;
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		// invalidation du cache de tous les individus en relation avec celui de l'événement
		final long noIndividu = event.getNumeroIndividu();
		final Individu individu = serviceCivil.getIndividu(noIndividu, null, AttributeIndividu.ENFANTS, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);
		invalidateCache(individu.getParents());
		invalidateCache(individu.getEnfants());
		invalidateCache(individu.getConjoints());

		// création de l'événement interne de traitement de l'événement
		return target.create(event, context, options);
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return target.isPrincipalementIndexation(event, context);
	}

	private void invalidateCache(Collection<RelationVersIndividu> relations) {
		if (relations != null && relations.size() > 0) {
			for (RelationVersIndividu relation : relations) {
				dataEventService.onIndividuChange(relation.getNumeroAutreIndividu());
			}
		}
	}
}
