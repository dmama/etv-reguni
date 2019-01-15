package ch.vd.unireg.di.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

@SuppressWarnings("unused")
public abstract class AbstractEditionDelaiDeclarationPMView extends AbstractEditionDelaiDeclarationView {

	public AbstractEditionDelaiDeclarationPMView() {
	}

	public AbstractEditionDelaiDeclarationPMView(@NotNull DeclarationImpotOrdinairePM di, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal decision) {
		super(di, dateDemande, delaiAccordeAu, decision);
	}
}
