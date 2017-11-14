package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.tiers.LinkedEntity;

/**
 * Classe abstraite qui représentent un droit de virtuel généré à la volée pour un tiers RF donné.
 */
public abstract class DroitVirtuelRF extends DroitRF {

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntity.@NotNull Context context, boolean includeAnnuled) {
		throw new ProgrammingException("On ne devrait jamais tomber ici car les droits virtuels ne sont pas persistés.");
	}
}
