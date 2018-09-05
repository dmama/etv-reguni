package ch.vd.unireg.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;

@SuppressWarnings("UnusedDeclaration")
public class AjouterDelaiDeclarationPPView extends AbstractEditionDelaiDeclarationView {

	// champs du formulaire
	private boolean confirmationEcrite;

	public AjouterDelaiDeclarationPPView() {
	}

	public AjouterDelaiDeclarationPPView(DeclarationImpotOrdinaire di, RegDate delaiAccordeAu) {
		super(di, RegDate.get(), delaiAccordeAu);
		this.confirmationEcrite = false;
	}

	public boolean isConfirmationEcrite() {
		return confirmationEcrite;
	}

	public void setConfirmationEcrite(boolean confirmationEcrite) {
		this.confirmationEcrite = confirmationEcrite;
	}
}
