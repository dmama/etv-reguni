package ch.vd.unireg.foncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

/**
 * Classe abstraite qui représentent un allégement foncier de virtuel généré à la volée pour un entreprise donnée.
 */
public class AllegementFoncierVirtuel extends AllegementFoncier {

	/**
	 * L'id de l'entreprise absorbée duquel provient l'allégement de référence.
	 */
	private long absorbeeId;

	/**
	 * L'allégement de référence tel qu'il est défini sur l'entreprise absorbée.
	 */
	private AllegementFoncier reference;

	public long getAbsorbeeId() {
		return absorbeeId;
	}

	public void setAbsorbeeId(long absorbeeId) {
		this.absorbeeId = absorbeeId;
	}

	public AllegementFoncier getReference() {
		return reference;
	}

	public void setReference(AllegementFoncier reference) {
		this.reference = reference;
	}

	@Override
	public TypeImpot getTypeImpot() {
		return TypeImpot.ICI;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		throw new ProgrammingException("On ne devrait jamais tomber ici car les allégements fonciers virtuels ne sont pas persistés.");
	}

}
