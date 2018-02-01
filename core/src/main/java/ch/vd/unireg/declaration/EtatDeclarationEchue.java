package ch.vd.unireg.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * Une notification d'échéance (= qui ouvre la porte à la taxation d'office) a été émise pour la déclaration
 */
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
		return TypeEtatDocumentFiscal.ECHU;
	}
}
