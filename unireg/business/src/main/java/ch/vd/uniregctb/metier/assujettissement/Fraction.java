package ch.vd.uniregctb.metier.assujettissement;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Contient la date et le motif de fractionnement d'un range de dates.
 */
public class Fraction {
	/**
	 * La date de fractionnement <b>au matin</b>. Au matin, signifie que le fractionnement doit être appliqué à 00:00 le matin du jour spécifié.
	 * <p/>
	 * <b>Exemple:</b> Fractionnement du range 2005.01.01-2005.12.31 à la date 2005.05.12 => deux ranges 2005.01.01-2005.05.11 et 2005.05.12-2005.12.31.
	 */
	public final RegDate date;

	/**
	 * Le motif (= la raison) du fractionnement à l'ouverture. Ce motif peut être nul.
	 */
	public MotifFor motifOuverture;

	/**
	 * Le motif (= la raison) du fractionnement à la fermeture. Ce motif peut être nul.
	 */
	public MotifFor motifFermeture;

	public Fraction(@NotNull RegDate date) {
		this.date = date;
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
		if (motifOuverture == null) {
			return motifFermeture;
		}
		else if (motifFermeture == null) {
			return motifOuverture;
		}
		else if (motifOuverture == motifFermeture) {
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
}
