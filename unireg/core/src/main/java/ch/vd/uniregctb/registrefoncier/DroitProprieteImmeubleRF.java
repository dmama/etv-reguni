package ch.vd.uniregctb.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Droit de propriété entre un immeuble bénéficiaire et un immeuble grevé.
 */
@Entity
@DiscriminatorValue("DroitProprieteImmeuble")
public class DroitProprieteImmeubleRF extends DroitProprieteRF {

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntity.@NotNull Context context, boolean includeAnnuled) {
		final List<?> entites = super.getLinkedEntities(context, includeAnnuled);

		// [SIFISC-24600] on ajoute l'immeuble bénéficiaire du droit
		final ImmeubleBeneficiaireRF beneficiaire = (ImmeubleBeneficiaireRF) getAyantDroit();
		final ArrayList<Object> result = new ArrayList<>(entites);
		result.add(beneficiaire.getImmeuble());

		// on ne veut pas retourner les tiers Unireg dans le cas de la validation/indexation/parentés, car ils ne sont pas influencés par les données RF
		if (context == Context.TACHES || context == Context.DATA_EVENT) {
			// on va chercher tous les contribuables propriétaires virtuels
			result.addAll(findLinkedContribuables(beneficiaire));
		}

		return result;
	}

	/**
	 * @param immeubleBeneficiaire un immeuble bénéficiaire
	 * @return tous les contribuables liés à un immeuble, y compris les propriétaires virtuels, c'est-à-dire les contribuables propriétaires d'autres immeubles eux-mêmes propriétaires de cet immeuble.
	 */
	@NotNull
	public static Collection<Contribuable> findLinkedContribuables(@NotNull ImmeubleBeneficiaireRF immeubleBeneficiaire) {

		final ImmeubleRF immeuble = immeubleBeneficiaire.getImmeuble();

		final Set<DroitProprieteRF> droitsPropriete = immeuble.getDroitsPropriete();
		if (droitsPropriete == null || droitsPropriete.isEmpty()) {
			return Collections.emptyList();
		}

		// les contribuables qui sont directement propriétaires de l'immeuble
		final Map<Long, Contribuable> contribuables = droitsPropriete.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(DroitProprieteRF.class::isInstance)                 // tous les droits de propriété vers cet immeuble
				.filter(d -> !(d instanceof DroitProprieteImmeubleRF))      // sauf les droits entre immeubles
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
				.map(DroitProprieteImmeubleRF::findLinkedContribuables)     // on continue en récursion
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Tiers::getId, Function.identity(), (l, r) -> l));

		if (!contribuablesIndirects.isEmpty()) {
			contribuables.putAll(contribuablesIndirects);
		}

		return contribuables.values();
	}

}
