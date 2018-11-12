package ch.vd.unireg.declaration.ordinaire;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Structure de données qui contient les deux dates importantes dans le délai initial des DIs PM :
 * la date imprimée sur le document et la date effective, qui peuvent être (ou pas) différentes
 */
public class DatesDelaiInitialDI {

	private final RegDate dateImprimee;
	private final RegDate dateEffective;

	public DatesDelaiInitialDI(@NotNull RegDate dateImprimee, @NotNull RegDate dateEffective) {
		this.dateImprimee = dateImprimee;
		this.dateEffective = dateEffective;
	}

	/**
	 * @param seuil date minimale acceptable
	 * @return une nouvelle instance (ou pas, selon nécessité) qui prend en compte le seuil
	 */
	public DatesDelaiInitialDI auPlusTot(@NotNull RegDate seuil) {
		if (seuil.isAfter(dateImprimee) || seuil.isAfter(dateEffective)) {
			return new DatesDelaiInitialDI(RegDateHelper.maximum(dateImprimee, seuil, NullDateBehavior.EARLIEST),
			                               RegDateHelper.maximum(dateEffective, seuil, NullDateBehavior.EARLIEST));
		}
		else {
			return this;
		}
	}

	/**
	 * @return la date imprimée sur le document.
	 */
	public RegDate getDateImprimee() {
		return dateImprimee;
	}

	/**
	 * @return la date du délai inséré en base.
	 */
	public RegDate getDateEffective() {
		return dateEffective;
	}
}
