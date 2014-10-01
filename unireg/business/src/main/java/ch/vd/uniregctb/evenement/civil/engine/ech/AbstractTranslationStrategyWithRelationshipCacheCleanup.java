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
 * Classe de base des stratégies qui commencent par vider les caches civils des individus concernés
 */
public abstract class AbstractTranslationStrategyWithRelationshipCacheCleanup implements EvenementCivilEchTranslationStrategy {

	private final ServiceCivilService serviceCivil;
	private final DataEventService dataEventService;
	private final TiersService tiersService;

	protected AbstractTranslationStrategyWithRelationshipCacheCleanup(ServiceCivilService serviceCivil, DataEventService dataEventService, TiersService tiersService) {
		this.serviceCivil = serviceCivil;
		this.dataEventService = dataEventService;
		this.tiersService = tiersService;
	}

	@Override
	public final EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		// invalidation du cache de tous les individus en relation avec celui de l'événement
		final long noIndividu = event.getNumeroIndividu();
		final Individu individu = serviceCivil.getIndividu(noIndividu, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);
		final Set<Long> individusLies = new HashSet<>();
		if (individu != null) {
			individusLies.addAll(extractAutresIndividus(individu.getParents()));
			individusLies.addAll(extractAutresIndividus(individu.getConjoints()));
		}
		individusLies.addAll(extractRelatedIndividus(noIndividu));
		invalidateCache(individusLies);

		// création de l'événement interne de traitement de l'événement
		return doCreate(event, context, options);
	}

	protected abstract EvenementCivilInterne doCreate(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException;

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
