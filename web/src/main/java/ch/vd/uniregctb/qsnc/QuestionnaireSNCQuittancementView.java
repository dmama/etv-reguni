package ch.vd.uniregctb.qsnc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;

public class QuestionnaireSNCQuittancementView {

	private long tiersId;
	private long questionnaireId;
	private int periodeFiscale;
	private RegDate dateEmission;
	private RegDate dateRetour;

	public QuestionnaireSNCQuittancementView() {
	}

	public QuestionnaireSNCQuittancementView(QuestionnaireSNC questionnaire) {
		this.tiersId = questionnaire.getTiers().getNumero();
		this.questionnaireId = questionnaire.getId();
		this.periodeFiscale = questionnaire.getPeriode().getAnnee();
		this.dateEmission = questionnaire.getDateExpedition();
	}

	public long getTiersId() {
		return tiersId;
	}

	public void setTiersId(long tiersId) {
		this.tiersId = tiersId;
	}

	public long getQuestionnaireId() {
		return questionnaireId;
	}

	public void setQuestionnaireId(long questionnaireId) {
		this.questionnaireId = questionnaireId;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(int periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public RegDate getDateEmission() {
		return dateEmission;
	}

	public void setDateEmission(RegDate dateEmission) {
		this.dateEmission = dateEmission;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}
}
