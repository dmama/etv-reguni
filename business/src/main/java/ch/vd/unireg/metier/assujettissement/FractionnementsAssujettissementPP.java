package ch.vd.unireg.metier.assujettissement;

import java.util.List;

import ch.vd.unireg.metier.common.ForFiscalPrincipalContext;
import ch.vd.unireg.metier.common.Fraction;
import ch.vd.unireg.metier.common.Fractionnements;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;

public abstract class FractionnementsAssujettissementPP extends Fractionnements<ForFiscalPrincipalPP> {

	protected FractionnementsAssujettissementPP(List<ForFiscalPrincipalPP> principaux) {
		super(principaux);
	}

	protected static void checkMotifSurFractionOuverture(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal, Fraction fraction) {
		if (fraction != null) {
			final ForFiscalPrincipalPP current = forPrincipal.getCurrent();
			final ForFiscalPrincipalPP next = forPrincipal.getNext();
			if (next != null && AssujettissementPersonnesPhysiquesCalculator.isArriveeHCApresDepartHSMemeAnnee(current) && !AssujettissementPersonnesPhysiquesCalculator.roleSourcierPur(current)) {
				// dans ce cas précis, on veut utiliser le motif d'ouverture du for suivant comme motif de fractionnement
				fraction.setMotifOuverture(next.getMotifOuverture());
			}
		}
	}
}
