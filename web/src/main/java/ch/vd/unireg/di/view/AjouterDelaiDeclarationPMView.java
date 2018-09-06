package ch.vd.unireg.di.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

@SuppressWarnings("unused")
public class AjouterDelaiDeclarationPMView extends AbstractEditionDelaiDeclarationPMView {

	private boolean sursis;

	public AjouterDelaiDeclarationPMView() {
	}

	public AjouterDelaiDeclarationPMView(@NotNull DeclarationImpotOrdinairePM di, RegDate delaiAccordeAu, boolean sursis) {
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
