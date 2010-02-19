package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.ordinaire.ForsList;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;

import java.util.List;

/**
 * Décomposition des fors d'un contribuable par type sur une année complète.
 */
public class DecompositionForsAnneeComplete extends DecompositionFors {

	/** Année considérée */
	public final int annee;

	/** Liste des fors principaux existant dans la période suivant celle considérée */
	public final ForsList<ForFiscalPrincipal> principauxDansPeriodeSuivante = new ForsList<ForFiscalPrincipal>();

	/** Liste des fors secondaires existant dans la période suivant celle considérée */
	public final ForsList<ForFiscalSecondaire> secondairesDansPeriodeSuivante = new ForsList<ForFiscalSecondaire>();

	public DecompositionForsAnneeComplete(Contribuable contribuable, int annee) {
		super(contribuable, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));
		this.annee = annee;

		final DateRange periodeSuivante = new DateRangeHelper.Range(RegDate.get(annee + 1, 1, 1), RegDate.get(annee + 1, 12, 31));

		final List<ForFiscal> fors = contribuable.getForsFiscauxSorted();
		if (fors != null) {
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
}
