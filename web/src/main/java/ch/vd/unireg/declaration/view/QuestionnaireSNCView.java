package ch.vd.unireg.declaration.view;

import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public class QuestionnaireSNCView extends DeclarationView implements CodeControlable {

	private final String codeControle;

	public QuestionnaireSNCView(QuestionnaireSNC questionnaire, ServiceInfrastructureService infraService) {
		super(questionnaire, infraService);
		codeControle = questionnaire.getCodeControle();
	}

	@Override
	public String getCodeControle() {
		return codeControle;
	}
}
