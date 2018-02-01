package ch.vd.unireg.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

@SuppressWarnings("UnusedDeclaration")
public class NouvelleDemandeDelaiDeclarationView extends AbstractEditionDelaiDeclarationPMView {

	private boolean sursis;

	public NouvelleDemandeDelaiDeclarationView() {
	}

	public NouvelleDemandeDelaiDeclarationView(DeclarationImpotOrdinaire di, RegDate delaiAccordeAu, boolean sursis) {
		super(di, RegDate.get(), delaiAccordeAu, EtatDelaiDocumentFiscal.ACCORDE);     // accordé par défaut
		this.sursis = sursis;
	}

	public boolean isSursis() {
		return sursis;
	}

	public void setSursis(boolean sursis) {
		this.sursis = sursis;
	}
}
