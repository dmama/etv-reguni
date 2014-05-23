package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.metier.common.Fractionnements;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;

public abstract class FractionnementsAssujettissement extends Fractionnements {

	protected FractionnementsAssujettissement(List<ForFiscalPrincipal> principaux) {
		super(principaux);
	}

	protected static void checkMotifSurFractionOuverture(ForFiscalPrincipalContext forPrincipal, Fraction fraction) {
		if (fraction != null) {
			final ForFiscalPrincipal current = forPrincipal.getCurrent();
			final ForFiscalPrincipal next = forPrincipal.getNext();
			if (next != null && AssujettissementServiceImpl.isArriveeHCApresDepartHSMemeAnnee(current) && !AssujettissementServiceImpl.roleSourcierPur(current)) {
				// dans ce cas précis, on veut utiliser le motif d'ouverture du for suivant comme motif de fractionnement
				fraction.setMotifOuverture(next.getMotifOuverture());
			}
		}
	}
}
