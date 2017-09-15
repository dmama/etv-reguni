package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

@Entity
@DiscriminatorValue("DI_SUSPENDUE")
public class EtatDeclarationSuspendue extends EtatDeclaration {

	public EtatDeclarationSuspendue() {
		super();
	}

	public EtatDeclarationSuspendue(RegDate dateObtention) {
		super(dateObtention);
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.SUSPENDUE;
	}
}
