package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

@Entity
@DiscriminatorValue("DI_ECHUE")
public class EtatDeclarationEchue extends EtatDeclaration {
	public EtatDeclarationEchue() {
		super();
	}

	public EtatDeclarationEchue(RegDate dateObtention) {
		super(dateObtention);
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.ECHUE;
	}
}
