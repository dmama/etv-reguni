package ch.vd.uniregctb.tache.sync;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;

public class UpdateDIPP extends UpdateDI<PeriodeImpositionPersonnesPhysiques, DeclarationImpotOrdinairePP> {

	public UpdateDIPP(PeriodeImpositionPersonnesPhysiques periodeImposition, DeclarationImpotOrdinairePP declaration) {
		super(periodeImposition, declaration);
	}
}
