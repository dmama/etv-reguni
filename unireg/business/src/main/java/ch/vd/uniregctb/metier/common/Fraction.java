package ch.vd.uniregctb.metier.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Contient la date et le motif d'un fractionnement. Une fraction est une coupure obligatoire des assujettissements à une date déterminée (par exemple, en cas de départ ou arrivée hors-Suiss, de
 * décès, ...).
 */
public abstract class Fraction {

	private final RegDate date;
	private MotifFor motifOuverture;
	private MotifFor motifFermeture;

	public Fraction(@NotNull RegDate date, MotifFor motifOuverture, MotifFor motifFermeture) {
		this.date = date;
		this.motifOuverture = motifOuverture;
		this.motifFermeture = motifFermeture;
	}

	public void setMotifOuverture(MotifFor motifOuverture) {
		this.motifOuverture = motifOuverture;
	}

	public void setMotifFermeture(MotifFor motifFermeture) {
		this.motifFermeture = motifFermeture;
	}

	/**
	 * @return le motif de fractionnement général et accepté comme tel. L'utilisation des motifs différenciés <i>ouverture</i> et <i>fermeture</i> est réservée pour des cas particuliers.
	 */
	public MotifFor getMotif() {
		return getMotifEffectif(motifFermeture, motifOuverture);
	}

	/**
	 * Règle de choix du motif effectif quand deux fors se suivent avec les motifs de fermeture et d'ouverture donnés
	 * @param motifFermeture motif de fermeture du premier for
	 * @param motifOuverture motif d'ouverture de second for
	 * @return motif à prendre en compte
	 */
	@Nullable
	public static MotifFor getMotifEffectif(@Nullable MotifFor motifFermeture, @Nullable MotifFor motifOuverture) {
		if (motifOuverture == null || motifOuverture == motifFermeture) {
			return motifFermeture;
		}
		else if (motifFermeture == null) {
			return motifOuverture;
		}
		else {
			// on doit choisir entre les deux motifs le plus pertinent
			if (motifOuverture == MotifFor.VEUVAGE_DECES || motifFermeture == MotifFor.VEUVAGE_DECES) {
				// le veuvage est toujours prioritaire
				return MotifFor.VEUVAGE_DECES;
			}
			// en l'absence d'autre règle, on prend le premier motif, c'est-à-dire le motif de fermeture...
			return motifFermeture;
		}
	}

	/**
	 * La date de fractionnement <b>au matin</b>. Au matin, signifie que le fractionnement doit être appliqué à 00:00 le matin du jour spécifié.
	 * <p/>
	 * <b>Exemple:</b> Fractionnement du range 2005.01.01-2005.12.31 à la date 2005.05.12 => deux ranges 2005.01.01-2005.05.11 et 2005.05.12-2005.12.31.
	 */
	public RegDate getDate() {
		return date;
	}

	/**
	 * Si un for fiscal s'ouvre ou se ferme durant la plage de dates retournée, la fraction courante s'applique et la date d'effet est celle retournée par {@link #getDate()}.
	 *
	 * @return la plage de dates impactées par le fractionnement.
	 */
	public abstract DateRange getPeriodeImpact();

	/**
	 * Le motif (= la raison) du fractionnement à l'ouverture. Ce motif peut être nul.
	 */
	public MotifFor getMotifOuverture() {
		return motifOuverture;
	}

	/**
	 * Le motif (= la raison) du fractionnement à la fermeture. Ce motif peut être nul.
	 */
	public MotifFor getMotifFermeture() {
		return motifFermeture;
	}

	@Override
	public String toString() {
		final DateRange impact = getPeriodeImpact();
		return String.format("%s au %s (impact du %s au %s) : %s / %s",
		                     getClass().getSimpleName(),
		                     RegDateHelper.dateToDisplayString(date),
		                     RegDateHelper.dateToDisplayString(impact.getDateDebut()),
		                     RegDateHelper.dateToDisplayString(impact.getDateFin()),
		                     motifOuverture,
		                     motifFermeture);
	}
}
