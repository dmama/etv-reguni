package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPM;

/**
 * Action qui permet de mettre à jour la catégorie d'entreprise sur une tâche d'envoi de déclaration d'impôt PM
 */
public class UpdateTacheEnvoiDIPM extends UpdateTacheEnvoiDI<TacheEnvoiDeclarationImpotPM, PeriodeImpositionPersonnesMorales> {

	public UpdateTacheEnvoiDIPM(TacheEnvoiDeclarationImpotPM tacheEnvoi, AddDI<PeriodeImpositionPersonnesMorales> addAction) {
		super(tacheEnvoi, addAction);
	}

	@Override
	public void execute(Context context) {
		// c'est en particulier la catégorie d'entreprise qu'il faut mettre à jour
		if (addAction.periodeImposition.getCategorieEntreprise() != null && addAction.periodeImposition.getCategorieEntreprise() != tacheEnvoi.getCategorieEntreprise()) {
			tacheEnvoi.setCategorieEntreprise(addAction.periodeImposition.getCategorieEntreprise());
		}

		tacheEnvoi.setDateDebutExercice(addAction.periodeImposition.getExerciceCommercial().getDateDebut());
		tacheEnvoi.setDateFinExercice(addAction.periodeImposition.getExerciceCommercial().getDateFin());
	}

	@Override
	public String toString() {
		return String.format("mise à jour d'une tâche d'émission de %s %s couvrant la période du %s au %s",
		                     addAction.periodeImposition.getTypeDocumentDeclaration().getDescription(),
		                     addAction.periodeImposition.getTypeContribuable().description(),
		                     RegDateHelper.dateToDisplayString(addAction.periodeImposition.getDateDebut()),
		                     RegDateHelper.dateToDisplayString(addAction.periodeImposition.getDateFin()));
	}
}
