package ch.vd.uniregctb.qsnc;

import java.util.List;

import ch.vd.uniregctb.declaration.view.QuestionnaireSNCView;

public class QuestionnairesSNCEditView {

	private final long ctbId;
	private final List<QuestionnaireSNCView> questionnaires;

	public QuestionnairesSNCEditView(long ctbId, List<QuestionnaireSNCView> questionnaires) {
		this.ctbId = ctbId;
		this.questionnaires = questionnaires;
	}

	public long getCtbId() {
		return ctbId;
	}

	public List<QuestionnaireSNCView> getQuestionnaires() {
		return questionnaires;
	}
}
