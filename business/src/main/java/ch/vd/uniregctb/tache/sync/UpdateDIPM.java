package ch.vd.uniregctb.tache.sync;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;

public class UpdateDIPM extends UpdateDI<PeriodeImpositionPersonnesMorales, DeclarationImpotOrdinairePM> {

	public UpdateDIPM(PeriodeImpositionPersonnesMorales periodeImposition, DeclarationImpotOrdinairePM declaration) {
		super(periodeImposition, declaration);
	}

	@Override
	protected void miseAJourDeclaration(@NotNull DeclarationImpotOrdinairePM declaration) {
		super.miseAJourDeclaration(declaration);

		final ExerciceCommercial exercice = periodeImposition.getExerciceCommercial();
		declaration.setDateDebutExerciceCommercial(exercice.getDateDebut());
		declaration.setDateFinExerciceCommercial(exercice.getDateFin());
	}
}
