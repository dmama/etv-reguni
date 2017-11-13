package ch.vd.uniregctb.documentfiscal;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

/**
 * @author Raphaël Marmier, 2017-09-20, <raphael.marmier@vd.ch>
 */
@Entity
@DiscriminatorValue(value = "AUTRE_RETOURNE")
public class EtatAutreDocumentFiscalRetourne extends EtatAutreDocumentFiscal {

	public EtatAutreDocumentFiscalRetourne() {
		super();
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.RETOURNE;
	}

	public EtatAutreDocumentFiscalRetourne(RegDate dateObtention) {
		super(dateObtention);
	}
}
