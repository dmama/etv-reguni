package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;

/**
 * Action de synchronisation qui consiste à modifier le questionnaire existant (les dates...)
 */
public class UpdateQSNC implements SynchronizeAction {

	private final DateRange newRange;
	public final long questionnaireId;
	private final RegDate oldDateDebut;
	private final RegDate oldDateFin;

	public UpdateQSNC(QuestionnaireSNC questionnaire, DateRange newRange) {
		this.newRange = newRange;
		this.questionnaireId = questionnaire.getId();
		this.oldDateDebut = questionnaire.getDateDebut();
		this.oldDateFin = questionnaire.getDateFin();
	}

	@Override
	public void execute(Context context) {
		final QuestionnaireSNC questionnaire = context.qsncDAO.get(questionnaireId);
		if (questionnaire != null) {
			// [UNIREG-1303] Autant que faire se peut, on évite de créer des tâches d'envoi/annulation de DI et on met-à-jour les DIs existantes. L'idée est d'éviter d'incrémenter le numéro de
			// séquence des DIs parce que cela pose des problèmes lors du quittancement, et de toutes façons la période exacte n'est pas imprimée sur les DIs.
			questionnaire.setDateDebut(newRange.getDateDebut());
			questionnaire.setDateFin(newRange.getDateFin());
		}
	}

	@Override
	public boolean willChangeEntity() {
		return true;
	}

	@Override
	public String toString() {
		return String.format("mise-à-jour du questionnaire SNC existant couvrant la période du %s au %s pour qu'il couvre la période du %s au %s",
		                     RegDateHelper.dateToDisplayString(oldDateDebut), RegDateHelper.dateToDisplayString(oldDateFin),
		                     RegDateHelper.dateToDisplayString(newRange.getDateDebut()), RegDateHelper.dateToDisplayString(newRange.getDateFin()));
	}
}
