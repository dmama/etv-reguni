package ch.vd.unireg.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

/**
 * Classe abstraite qui représentent un droit de virtuel généré à la volée pour un tiers RF donné.
 */
public abstract class DroitVirtuelRF extends DroitRF {

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		throw new ProgrammingException("On ne devrait jamais tomber ici car les droits virtuels ne sont pas persistés.");
	}
}
