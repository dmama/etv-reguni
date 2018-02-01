package ch.vd.unireg.declaration.view;

import org.springframework.context.MessageSource;

import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

public class QuestionnaireSNCView extends DeclarationView {

	public QuestionnaireSNCView(QuestionnaireSNC questionnaire, ServiceInfrastructureService infraService, MessageSource messageSource) {
		super(questionnaire, infraService, messageSource);
	}
}
