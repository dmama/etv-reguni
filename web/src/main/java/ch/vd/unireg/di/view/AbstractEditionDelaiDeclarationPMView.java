package ch.vd.unireg.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.documentfiscal.TypeImpression;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

public abstract class AbstractEditionDelaiDeclarationPMView extends AbstractEditionDelaiDeclarationView {

	private EtatDelaiDocumentFiscal decision;
	private TypeImpression typeImpression;

	public AbstractEditionDelaiDeclarationPMView() {
	}

	public AbstractEditionDelaiDeclarationPMView(DeclarationImpotOrdinaire di, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal decision) {
		super(di, dateDemande, delaiAccordeAu);
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
