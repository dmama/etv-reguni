package ch.vd.uniregctb.tache.sync;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.tiers.TacheAnnulationQuestionnaireSNC;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

/**
 * Action de synchronisation qui consiste à annuler un questionnaire SNC
 */
public class DeleteQSNC implements TacheSynchronizeAction {

	public final long questionnaireId;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public DeleteQSNC(QuestionnaireSNC questionnaire) {
		this.questionnaireId = questionnaire.getId();
		this.dateDebut = questionnaire.getDateDebut();
		this.dateFin = questionnaire.getDateFin();
	}

	@Override
	public void execute(Context context) {
		final QuestionnaireSNC questionnaire = context.qsncDAO.get(questionnaireId);
		final TacheAnnulationQuestionnaireSNC tache = new TacheAnnulationQuestionnaireSNC(TypeEtatTache.EN_INSTANCE, null, context.contribuable, questionnaire, context.collectivite);
		context.tacheDAO.save(tache);
	}

	@Override
	public boolean willChangeEntity() {
		return false;
	}

	@Override
	public int getPeriodeFiscale() {
		return dateFin.year();
	}

	@Override
	public String toString() {
		return String.format("création d'une tâche d'annulation de %s couvrant la période du %s au %s",
		                     TypeDocument.QUESTIONNAIRE_SNC.getDescription(),
		                     RegDateHelper.dateToDisplayString(dateDebut),
		                     RegDateHelper.dateToDisplayString(dateFin));
	}
}
