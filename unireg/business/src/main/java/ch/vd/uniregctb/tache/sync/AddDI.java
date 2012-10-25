package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
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

		final RegDate dateEcheance;
		final CollectiviteAdministrative collectivite;
		if (periodeImposition.isFermetureCauseDeces()) {
			// [UNIREG-2305] En cas de décès, l'échéance de la tâche est poussée 30 jours plus tard et on assigne la tâche à l'office des successions
			dateEcheance = periodeImposition.getDateFin().addDays(30);
			collectivite = context.officeSuccessions;
		}
		else {
			// autrement, on prend les valeurs par défaut
			dateEcheance = null;
			collectivite = context.collectivite;
		}

		final TacheEnvoiDeclarationImpot tache =
				new TacheEnvoiDeclarationImpot(TypeEtatTache.EN_INSTANCE, dateEcheance, context.contribuable, periodeImposition.getDateDebut(), periodeImposition.getDateFin(),
						periodeImposition.getTypeContribuable(), periodeImposition.getTypeDocument(), periodeImposition.getQualification(), periodeImposition.getCodeSegment(),
						periodeImposition.getAdresseRetour(), collectivite);
		context.tacheDAO.save(tache);
	}

	@Override
	public boolean willChangeEntity() {
		return false;
	}

	@Override
	public String toString() {
		return String.format("création d'une tâche d'émission de déclaration d'impôt %s couvrant la période du %s au %s", periodeImposition.getTypeContribuable().description(),
				RegDateHelper.dateToDisplayString(periodeImposition.getDateDebut()), RegDateHelper.dateToDisplayString(periodeImposition.getDateFin()));
	}
}
