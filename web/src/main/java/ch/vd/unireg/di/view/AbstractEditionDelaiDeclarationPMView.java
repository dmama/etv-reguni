package ch.vd.unireg.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

public abstract class AbstractEditionDelaiDeclarationPMView extends AbstractEditionDelaiDeclarationView {

	public enum TypeImpression {
		LOCAL,
		BATCH
	}

	// champs du formulaire
	private RegDate dateDemande;
	private EtatDelaiDocumentFiscal decision;
	private RegDate delaiAccordeAu;
	private TypeImpression typeImpression;

	public AbstractEditionDelaiDeclarationPMView() {
	}

	public AbstractEditionDelaiDeclarationPMView(DeclarationImpotOrdinaire di, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal decision) {
		super(di);
		this.dateDemande = dateDemande;
		this.delaiAccordeAu = delaiAccordeAu;
		this.decision = decision;
		this.typeImpression = TypeImpression.BATCH;         // par défaut
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	public EtatDelaiDocumentFiscal getDecision() {
		return decision;
	}

	public void setDecision(EtatDelaiDocumentFiscal decision) {
		this.decision = decision;
	}

	public RegDate getDelaiAccordeAu() {
		return delaiAccordeAu;
	}

	public void setDelaiAccordeAu(RegDate delaiAccordeAu) {
		this.delaiAccordeAu = delaiAccordeAu;
	}

	public TypeImpression getTypeImpression() {
		return typeImpression;
	}

	public void setTypeImpression(TypeImpression typeImpression) {
		this.typeImpression = typeImpression;
	}
}
