package ch.vd.unireg.qsnc;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.documentfiscal.AjouterDelaiDocumentFiscalView;
import ch.vd.unireg.documentfiscal.TypeImpression;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

/**
 * Données nécessaires à l'ajout d'un délai sur un questionnaire SNC.
 */
public class QuestionnaireSNCAjouterDelaiView extends AjouterDelaiDocumentFiscalView {


	private DateRangeHelper.Range declarationRange;
	private EtatDelaiDocumentFiscal decision;

	private TypeImpression typeImpression;

	// nécessaire pour Spring
	@SuppressWarnings("unused")
	public QuestionnaireSNCAjouterDelaiView() {
	}

	public QuestionnaireSNCAjouterDelaiView(QuestionnaireSNC doc, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal decision) {
		super(doc, delaiAccordeAu);
		this.decision = decision;
		this.typeImpression = TypeImpression.BATCH;
		this.declarationRange = new DateRangeHelper.Range(doc);
	}

	public EtatDelaiDocumentFiscal getDecision() {
		return decision;
	}

	public void setDecision(EtatDelaiDocumentFiscal decision) {
		this.decision = decision;
	}

	public TypeImpression getTypeImpression() {
		return typeImpression;
	}

	public void setTypeImpression(TypeImpression typeImpression) {
		this.typeImpression = typeImpression;
	}

	public DateRangeHelper.Range getDeclarationRange() {
		return declarationRange;
	}

	public void setDeclarationRange(DateRangeHelper.Range declarationRange) {
		this.declarationRange = declarationRange;
	}
}
