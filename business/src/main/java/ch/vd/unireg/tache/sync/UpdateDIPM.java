package ch.vd.unireg.tache.sync;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;

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
