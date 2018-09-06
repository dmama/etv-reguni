package ch.vd.unireg.di.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

@SuppressWarnings("unused")
public class AbstractEditionDelaiDeclarationPPView extends AbstractEditionDelaiDeclarationView {

	// champs du formulaire
	private boolean confirmationEcrite;

	public AbstractEditionDelaiDeclarationPPView() {
	}

	public AbstractEditionDelaiDeclarationPPView(@NotNull DeclarationImpotOrdinairePP declaration, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal decision) {
		super(declaration, dateDemande, delaiAccordeAu, decision);
		this.confirmationEcrite = false;
	}

	public AbstractEditionDelaiDeclarationPPView(@NotNull DeclarationImpotOrdinairePP declaration) {
		super(declaration);
	}

	public boolean isConfirmationEcrite() {
		return confirmationEcrite;
	}

	public void setConfirmationEcrite(boolean confirmationEcrite) {
		this.confirmationEcrite = confirmationEcrite;
	}
}
