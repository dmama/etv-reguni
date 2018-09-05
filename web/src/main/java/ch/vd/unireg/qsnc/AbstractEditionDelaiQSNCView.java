package ch.vd.unireg.qsnc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.di.view.AbstractEditionDelaiDeclarationView;
import ch.vd.unireg.documentfiscal.TypeImpression;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

public abstract class AbstractEditionDelaiQSNCView extends AbstractEditionDelaiDeclarationView {

	private EtatDelaiDocumentFiscal decision;
	private TypeImpression typeImpression;

	public AbstractEditionDelaiQSNCView() {
	}

	public AbstractEditionDelaiQSNCView(QuestionnaireSNC qsnc, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal decision) {
		super(qsnc);
		this.dateDemande = dateDemande;
		this.delaiAccordeAu = delaiAccordeAu;
		this.decision = decision;
		this.typeImpression = TypeImpression.BATCH;         // par défaut
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
}
