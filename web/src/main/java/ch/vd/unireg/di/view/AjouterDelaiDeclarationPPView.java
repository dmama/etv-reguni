package ch.vd.unireg.di.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

@SuppressWarnings("unused")
public class AjouterDelaiDeclarationPPView extends AbstractEditionDelaiDeclarationPPView {

	public AjouterDelaiDeclarationPPView() {
	}

	public AjouterDelaiDeclarationPPView(@NotNull DeclarationImpotOrdinairePP di, RegDate delaiAccordeAu) {
		super(di, RegDate.get(), delaiAccordeAu, EtatDelaiDocumentFiscal.ACCORDE);  // accordé par défaut
	}

}
