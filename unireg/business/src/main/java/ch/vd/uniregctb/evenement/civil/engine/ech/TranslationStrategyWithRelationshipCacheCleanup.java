package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Façade d'une stratégie de translation d'événement civil ECH qui invalide le cache des individus qui
 * possèdent ou ont possédé une relation civile (parents, enfants, conjoints) avec l'individu de l'événement à traiter
 */
public class TranslationStrategyWithRelationshipCacheCleanup implements EvenementCivilEchTranslationStrategy {

	private final EvenementCivilEchTranslationStrategy target;
	private final ServiceCivilService serviceCivil;
	private final DataEventService dataEventService;
	private final TiersService tiersService;

	public TranslationStrategyWithRelationshipCacheCleanup(EvenementCivilEchTranslationStrategy target, ServiceCivilService serviceCivil, DataEventService dataEventService, TiersService tiersService) {
		this.target = target;
		this.serviceCivil = serviceCivil;
		this.dataEventService = dataEventService;
		this.tiersService = tiersService;
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		// invalidation du cache de tous les individus en relation avec celui de l'événement
		final long noIndividu = event.getNumeroIndividu();
		final Individu individu = serviceCivil.getIndividu(noIndividu, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);
		final Set<Long> individusLies = new HashSet<>();
		individusLies.addAll(extractAutresIndividus(individu.getParents()));
		individusLies.addAll(extractAutresIndividus(individu.getConjoints()));
		individusLies.addAll(extractRelatedIndividus(individu.getNoTechnique()));
		invalidateCache(individusLies);

		// création de l'événement interne de traitement de l'événement
		return target.create(event, context, options);
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
		return target.isPrincipalementIndexation(event, context);
	}

	private void invalidateCache(Set<Long> individus) {
		if (individus != null && individus.size() > 0) {
			for (Long noIndividu : individus) {
				dataEventService.onIndividuChange(noIndividu);
			}
		}
	}

	private static Collection<Long> extractAutresIndividus(List<RelationVersIndividu> relations) {
		if (relations != null && relations.size() > 0) {
			final List<Long> list = new ArrayList<>(relations.size());
			for (RelationVersIndividu relation : relations) {
				list.add(relation.getNumeroAutreIndividu());
			}
			return list;
		}
		else {
			return Collections.emptyList();
		}
	}

	private Collection<Long> extractRelatedIndividus(long noIndividu) {
		return tiersService.getNumerosIndividusLiesParParente(noIndividu);
	}
}
