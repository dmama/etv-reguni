package ch.vd.unireg.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.EntityKey;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.common.linkedentity.LinkedEntityPhase;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;

/**
 * Droit de propriété entre un immeuble bénéficiaire et un immeuble grevé.
 */
@Entity
@DiscriminatorValue("DroitProprieteImmeuble")
public class DroitProprieteImmeubleRF extends DroitProprieteRF {

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		final List<?> entites = super.getLinkedEntities(context, includeAnnuled);

		// [SIFISC-24600] on ajoute l'immeuble bénéficiaire du droit
		final ImmeubleBeneficiaireRF beneficiaire = (ImmeubleBeneficiaireRF) getAyantDroit();
		final ArrayList<Object> result = new ArrayList<>(entites);
		result.add(beneficiaire.getImmeuble());

		// on ne veut pas retourner les tiers Unireg dans le cas de la validation/indexation/parentés, car ils ne sont pas influencés par les données RF
		if (context.getPhase() == LinkedEntityPhase.TACHES || context.getPhase() == LinkedEntityPhase.DATA_EVENT) {
			// on va chercher tous les contribuables propriétaires virtuels
			final Collection<Contribuable> contribuables = findLinkedContribuables(beneficiaire, new HashSet<>());
			result.addAll(contribuables);

			// [SIFISC-24999] on ajoute les héritiers des contribuables trouvés (car les droits des décédés sont exposés sur les héritiers)
			final List<EntityKey> keysHeritage = findHeirsKeys(contribuables);
			result.addAll(keysHeritage);

			// [SIFISC-24999] on ajoute les entreprises absorbantes en cas de fusion (car les droits des entreprises absorbées sont exposés sur les entreprises absorbantes)
			final List<EntityKey> keysAcquiringCompany = findAcquiringOrganisationKeys(contribuables);
			result.addAll(keysAcquiringCompany);
		}

		return result;
	}

	/**
	 * @param immeubleBeneficiaire un immeuble bénéficiaire
	 * @param visited              les ids d'immeubles déjà visités
	 * @return tous les contribuables liés à un immeuble, y compris les propriétaires virtuels, c'est-à-dire les contribuables propriétaires d'autres immeubles eux-mêmes propriétaires de cet immeuble.
	 */
	private static Collection<Contribuable> findLinkedContribuables(@NotNull ImmeubleBeneficiaireRF immeubleBeneficiaire, @NotNull Set<Long> visited) {

		final ImmeubleRF immeuble = immeubleBeneficiaire.getImmeuble();

		if (visited.contains(immeuble.getId())) {
			// on sort si l'immeuble a déjà été visité (détection des cycles)
			return Collections.emptyList();
		}
		visited.add(immeuble.getId());

		final Set<DroitProprieteRF> droitsPropriete = immeuble.getDroitsPropriete();
		if (droitsPropriete == null || droitsPropriete.isEmpty()) {
			// pas de droits, pas de chocolat
			return Collections.emptyList();
		}

		// les contribuables qui sont directement propriétaires de l'immeuble
		final Map<Long, Contribuable> contribuables = droitsPropriete.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(DroitProprietePersonneRF.class::isInstance)         // tous les droits de propriété de tiers vers cet immeuble
				.map(DroitProprieteRF.class::cast)
				.map(DroitProprieteRF::getAyantDroit)                       // on récupère l'ayant-droit (qui doit être un tiers)
				.map(TiersRF.class::cast)
				.map(DroitProprieteRF::findLinkedContribuables)             // on chercher les contribuables liés
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Tiers::getId, Function.identity(), (l, r) -> l));

		// on ajoute les contribuable qui sont propriétaires indirects de l'immeuble (à travers d'autres immeubles)
		final Map<Long, Contribuable> contribuablesIndirects = droitsPropriete.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(DroitProprieteImmeubleRF.class::isInstance)         // tous les droits de propriété entre immeubles vers cet immeuble
				.map(DroitProprieteImmeubleRF.class::cast)
				.map(DroitProprieteRF::getAyantDroit)                       // on récupère les ayants-droits (qui doivent être des immeubles)
				.map(ImmeubleBeneficiaireRF.class::cast)
				.map(immeubleBeneficiaire1 -> findLinkedContribuables(immeubleBeneficiaire1, visited))     // on continue en récursion
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Tiers::getId, Function.identity(), (l, r) -> l));

		if (!contribuablesIndirects.isEmpty()) {
			contribuables.putAll(contribuablesIndirects);
		}

		return contribuables.values();
	}

}
