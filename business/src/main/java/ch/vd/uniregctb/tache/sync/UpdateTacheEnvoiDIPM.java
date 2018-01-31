package ch.vd.uniregctb.tache.sync;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPM;

/**
 * Action qui permet de mettre à jour la catégorie d'entreprise sur une tâche d'envoi de déclaration d'impôt PM
 */
public class UpdateTacheEnvoiDIPM extends UpdateTacheEnvoiDI<TacheEnvoiDeclarationImpotPM, PeriodeImpositionPersonnesMorales> {

	private UpdateTacheEnvoiDIPM(TacheEnvoiDeclarationImpotPM tacheEnvoi, AddDI<PeriodeImpositionPersonnesMorales> addAction) {
		super(tacheEnvoi, addAction);
	}

	/**
	 * [SIFISC-20682] Pas la peine de créer l'action d'update si rien ne change...
	 * @param tacheEnvoi tâche d'envoi existante pouvant nécessiter une mise-à-jour
	 * @param addAction action détectée d'envoi de DI (qui peut causer, en cas de différence, une mise-à-jour de la tâche)
	 * @return l'action instanciée s'il y a des différences nécessitant la mise à jour de la tâche d'envoi
	 */
	@Nullable
	public static UpdateTacheEnvoiDIPM createIfNecessary(TacheEnvoiDeclarationImpotPM tacheEnvoi, AddDI<PeriodeImpositionPersonnesMorales> addAction) {
		if ((addAction.periodeImposition.getCategorieEntreprise() != null && addAction.periodeImposition.getCategorieEntreprise() != tacheEnvoi.getCategorieEntreprise())
				|| tacheEnvoi.getDateDebutExercice() != addAction.periodeImposition.getExerciceCommercial().getDateDebut()
				|| tacheEnvoi.getDateFinExercice() != addAction.periodeImposition.getExerciceCommercial().getDateFin()) {

			return new UpdateTacheEnvoiDIPM(tacheEnvoi, addAction);
		}
		else {
			return null;
		}
	}

	@Override
	public void execute(Context context) {
		if (addAction.periodeImposition.getCategorieEntreprise() != null) {
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
