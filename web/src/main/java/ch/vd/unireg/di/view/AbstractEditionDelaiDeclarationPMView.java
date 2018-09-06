package ch.vd.unireg.di.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.documentfiscal.TypeImpression;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

@SuppressWarnings("unused")
public abstract class AbstractEditionDelaiDeclarationPMView extends AbstractEditionDelaiDeclarationView {

	private TypeImpression typeImpression;

	public AbstractEditionDelaiDeclarationPMView() {
	}

	public AbstractEditionDelaiDeclarationPMView(@NotNull DeclarationImpotOrdinairePM di, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal decision) {
		super(di, dateDemande, delaiAccordeAu, decision);
		this.typeImpression = TypeImpression.BATCH;         // par défaut
	}

	public TypeImpression getTypeImpression() {
		return typeImpression;
	}

	public void setTypeImpression(TypeImpression typeImpression) {
		this.typeImpression = typeImpression;
	}
}
