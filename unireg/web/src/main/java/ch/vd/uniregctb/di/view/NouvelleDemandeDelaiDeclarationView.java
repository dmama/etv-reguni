package ch.vd.uniregctb.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

@SuppressWarnings("UnusedDeclaration")
public class NouvelleDemandeDelaiDeclarationView extends AbstractEditionDelaiDeclarationPMView {

	private boolean sursis;

	public NouvelleDemandeDelaiDeclarationView() {
	}

	public NouvelleDemandeDelaiDeclarationView(DeclarationImpotOrdinaire di, RegDate delaiAccordeAu, boolean sursis) {
		super(di, RegDate.get(), delaiAccordeAu);
		this.sursis = sursis;
	}

	public boolean isSursis() {
		return sursis;
	}

	public void setSursis(boolean sursis) {
		this.sursis = sursis;
	}
}
