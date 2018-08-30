package ch.vd.unireg.qsnc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.view.QuestionnaireSNCView;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class QuestionnaireSNCEditView extends QuestionnaireSNCView {

	private final boolean rappelable;       // si on peut envoyer un rappel
	private final boolean duplicable;       // si on peut en faire un duplicata
	private final boolean liberable;        // si on peut liberer le questionnaire

	public QuestionnaireSNCEditView(QuestionnaireSNC questionnaire, ServiceInfrastructureService infraService, boolean allowedRappel, boolean allowedDuplicata,
	                                boolean allowedLiberable, MessageHelper messageHelper) {
		super(questionnaire, infraService, messageHelper);
		this.liberable = allowedLiberable;
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

	public boolean isLiberable() {
		return liberable;
	}
}
