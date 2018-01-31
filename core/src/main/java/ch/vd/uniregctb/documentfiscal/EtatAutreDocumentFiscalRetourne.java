package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Column;
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
public class EtatAutreDocumentFiscalRetourne extends EtatAutreDocumentFiscal implements SourceQuittancement {

	/**
	 * [SIFISC-1782] La source du quittancement de la déclaration (CEDI, ADDI ou manuel).
	 */
	private String source;

	public EtatAutreDocumentFiscalRetourne() {
		super();
		this.source = SOURCE_WEB;
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.RETOURNE;
	}

	public EtatAutreDocumentFiscalRetourne(RegDate dateObtention) {
		super(dateObtention);
		this.source = SOURCE_WEB;
	}

	@Override
	@Column(name = "SOURCE")
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
