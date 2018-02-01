package ch.vd.unireg.metier.assujettissement;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.metier.common.Fraction;
import ch.vd.unireg.metier.common.Fractionnements;
import ch.vd.unireg.tiers.ForFiscalPrincipal;

/**
 * Limites de fractionnement à gauche et à droite d'une range déterminé.
 */
public final class LimitesAssujettissement {

	private final Fraction left;
	private final Fraction right;

	private LimitesAssujettissement(Fraction left, Fraction right) {
		this.left = left;
		this.right = right;
	}

	@Nullable
	public Fraction getLeft() {
		return left;
	}

	@Nullable
	public Fraction getRight() {
		return right;
	}

	/**
	 * Détermine les fractionnements immédiatement à gauche (borne inclue) et droite (borne exclue) du range de dates spécifié. Par principe, les fractions à l'intérieur du range sont ignorées.
	 *
	 * @param range     un range de dates
	 * @param fractions une liste de fractions
	 * @return les fractions gauche et droite déterminées; ou <b>null</b> si aucune limite n'a été trouvée.
	 */
	@Nullable
	public static <T extends ForFiscalPrincipal> LimitesAssujettissement determine(DateRange range, Fractionnements<T> fractions) {
		return determine(range.getDateDebut(), range.getDateFin(), fractions);
	}

	/**
	 * Détermine les fractionnements immédiatement à gauche (borne inclue) et droite (borne exclue) du range de dates spécifié. Par principe, les fractions à l'intérieur du range sont ignorées.
	 *
	 * @param dateDebut la date de début du range
	 * @param dateFin   la date de fin du range
	 * @param fractions une liste de fractions
	 * @return les fractions gauche et droite déterminées; ou <b>null</b> si aucune limite n'a été trouvée.
	 */
	@Nullable
	public static <T extends ForFiscalPrincipal> LimitesAssujettissement determine(RegDate dateDebut, RegDate dateFin, Fractionnements<T> fractions) {

		if (fractions.isEmpty()) {
			return null;
		}

		Fraction left = null;
		Fraction right = null;
		for (Fraction f : fractions) {
			if (dateDebut != null && f.getDate().isBeforeOrEqual(dateDebut)) {
				if (left == null || left.getDate().isBefore(f.getDate())) {
					left = f;
				}
			}
			if (dateFin != null && f.getDate().isAfter(dateFin)) {
				if (right == null || right.getDate().isAfter(f.getDate())) {
					right = f;
				}
			}
		}

		if (left == null && right == null) {
			return null;
		}

		return new LimitesAssujettissement(left, right);
	}
}
