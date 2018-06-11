package ch.vd.unireg.qsnc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.documentfiscal.AjouterDelaiDocumentFiscalView;

/**
 * Données nécessaires à l'ajout d'un délai sur un questionnaire SNC.
 */
public class QuestionnaireSNCAjouterDelaiView extends AjouterDelaiDocumentFiscalView {

	// nécessaire pour Spring
	@SuppressWarnings("unused")
	public QuestionnaireSNCAjouterDelaiView() {
	}

	public QuestionnaireSNCAjouterDelaiView(QuestionnaireSNC doc, RegDate delaiAccordeAu) {
		super(doc, delaiAccordeAu);
	}
}
