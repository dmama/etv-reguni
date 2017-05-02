package ch.vd.uniregctb.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.LinkedEntity;

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

		return result;
	}
}
