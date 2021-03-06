package ch.vd.unireg.metier.assujettissement;

import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.ForsList;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalSecondaire;

/**
 * Décomposition des fors d'un contribuable par type sur une année complète.
 */
public class DecompositionForsAnneeComplete extends DecompositionFors {

	/** Année considérée */
	public final int annee;

	/** Liste des fors principaux existant dans la période suivant celle considérée */
	public final ForsList<ForFiscalPrincipal> principauxDansPeriodeSuivante = new ForsList<>();

	/** Liste des fors secondaires existant dans la période suivant celle considérée */
	public final ForsList<ForFiscalSecondaire> secondairesDansPeriodeSuivante = new ForsList<>();

	public DecompositionForsAnneeComplete(Contribuable contribuable, int annee) {
		super(contribuable, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));
		this.annee = annee;

		final DateRange periodeSuivante = new DateRangeHelper.Range(RegDate.get(annee + 1, 1, 1), RegDate.get(annee + 1, 12, 31));

		final List<ForFiscal> fors = contribuable.getForsFiscauxSorted();
		for (ForFiscal f : fors) {
			if (f.isAnnule()) {
				continue;
			}
			if (f.isPrincipal()) {
				if (DateRangeHelper.intersect(f, periodeSuivante)) {
					principauxDansPeriodeSuivante.add((ForFiscalPrincipal) f);
				}
			}
			else if (f instanceof ForFiscalSecondaire) {
				if (DateRangeHelper.intersect(f, periodeSuivante)) {
					secondairesDansPeriodeSuivante.add((ForFiscalSecondaire) f);
				}
			}
		}
	}
}
