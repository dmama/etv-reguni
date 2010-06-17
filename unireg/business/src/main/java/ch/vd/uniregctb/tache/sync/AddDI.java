package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * Action permettant d'ajouter une tâche d'envoi de déclaration d'impôt.
 */
public class AddDI extends SynchronizeAction {
	public final PeriodeImposition periodeImposition;

	public AddDI(PeriodeImposition periodeImposition) {
		this.periodeImposition = periodeImposition;
	}

	@Override
	public void execute(Context context) {
		final TacheEnvoiDeclarationImpot tache =
				new TacheEnvoiDeclarationImpot(TypeEtatTache.EN_INSTANCE, context.dateEcheance, context.contribuable, periodeImposition.getDateDebut(), periodeImposition.getDateFin(),
						periodeImposition.getTypeContribuable(), periodeImposition.getTypeDocument(), periodeImposition.getQualification(), periodeImposition.getAdresseRetour(), context.collectivite);
		context.tacheDAO.save(tache);
	}

	@Override
	public String toString() {
		return String.format("création d'une tâche d'émission de déclaration d'impôt %s couvrant la période du %s au %s", periodeImposition.getTypeContribuable().description(),
				RegDateHelper.dateToDisplayString(periodeImposition.getDateDebut()), RegDateHelper.dateToDisplayString(periodeImposition.getDateFin()));
	}
}
