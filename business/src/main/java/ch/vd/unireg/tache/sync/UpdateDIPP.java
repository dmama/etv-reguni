package ch.vd.unireg.tache.sync;

import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;

public class UpdateDIPP extends UpdateDI<PeriodeImpositionPersonnesPhysiques, DeclarationImpotOrdinairePP> {

	public UpdateDIPP(PeriodeImpositionPersonnesPhysiques periodeImposition, DeclarationImpotOrdinairePP declaration) {
		super(periodeImposition, declaration);
	}
}
