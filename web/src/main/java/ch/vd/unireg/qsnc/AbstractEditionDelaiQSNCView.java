package ch.vd.unireg.qsnc;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.di.view.AbstractEditionDelaiDeclarationView;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

@SuppressWarnings("unused")
public abstract class AbstractEditionDelaiQSNCView extends AbstractEditionDelaiDeclarationView {

	public AbstractEditionDelaiQSNCView() {
	}

	public AbstractEditionDelaiQSNCView(@NotNull QuestionnaireSNC qsnc, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal decision) {
		super(qsnc, dateDemande, delaiAccordeAu, decision);
	}
}
