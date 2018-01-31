package ch.vd.uniregctb.qsnc;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.view.QuestionnaireSNCView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

public class QuestionnaireSNCEditView extends QuestionnaireSNCView {

	private final boolean rappelable;       // si on peut envoyer un rappel
	private final boolean duplicable;       // si on peut en faire un duplicata

	public QuestionnaireSNCEditView(QuestionnaireSNC questionnaire, ServiceInfrastructureService infraService, MessageSource messageSource, boolean allowedRappel, boolean allowedDuplicata) {
		super(questionnaire, infraService, messageSource);
		this.rappelable = allowedRappel && isRappelable(questionnaire);
		this.duplicable = allowedDuplicata && isDuplicable(questionnaire);
	}

	private static TypeEtatDocumentFiscal getDernierEtat(QuestionnaireSNC q) {
		final EtatDeclaration dernierEtat = q.getDernierEtatDeclaration();
		return dernierEtat == null ? null : dernierEtat.getEtat();
	}

	private static boolean isRappelable(QuestionnaireSNC q) {
		final TypeEtatDocumentFiscal dernierEtat = getDernierEtat(q);
		boolean isRappelable = false;
		if (dernierEtat == TypeEtatDocumentFiscal.EMIS) {
			if (q.getDelaiAccordeAu() == null || RegDate.get().isAfter(q.getDelaiAccordeAu())) {
				isRappelable = true;
			}
		}
		return isRappelable;
	}

	private static boolean isDuplicable(QuestionnaireSNC q) {
		return q.getModeleDocument() != null;
	}

	public boolean isRappelable() {
		return rappelable;
	}

	public boolean isDuplicable() {
		return duplicable;
	}
}
