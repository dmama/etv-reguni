package ch.vd.uniregctb.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;

public abstract class AbstractEditionDelaiDeclarationPMView extends AbstractEditionDelaiDeclarationView {

	public enum TypeImpression {
		LOCAL,
		BATCH
	}

	// champs du formulaire
	private RegDate dateDemande;
	private EtatDelaiDeclaration decision;
	private RegDate delaiAccordeAu;
	private TypeImpression typeImpression;

	public AbstractEditionDelaiDeclarationPMView() {
	}

	public AbstractEditionDelaiDeclarationPMView(DeclarationImpotOrdinaire di, RegDate dateDemande, RegDate delaiAccordeAu) {
		super(di);
		this.delaiAccordeAu = delaiAccordeAu;
		this.dateDemande = dateDemande;
		this.typeImpression = TypeImpression.BATCH;         // par défaut
		this.decision = EtatDelaiDeclaration.DEMANDE;       // par défaut
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	public EtatDelaiDeclaration getDecision() {
		return decision;
	}

	public void setDecision(EtatDelaiDeclaration decision) {
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
