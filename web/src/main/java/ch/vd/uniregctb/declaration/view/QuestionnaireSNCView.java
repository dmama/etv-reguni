package ch.vd.uniregctb.declaration.view;

import org.springframework.context.MessageSource;

import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class QuestionnaireSNCView extends DeclarationView {

	public QuestionnaireSNCView(QuestionnaireSNC questionnaire, ServiceInfrastructureService infraService, MessageSource messageSource) {
		super(questionnaire, infraService, messageSource);
	}
}
