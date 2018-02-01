package ch.vd.unireg.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.documentfiscal.SourceQuittancement;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

@Entity
@DiscriminatorValue("DI_RETOURNEE")
public class EtatDeclarationRetournee extends EtatDeclaration implements SourceQuittancement {

	/**
	 * [SIFISC-1782] La source du quittancement de la déclaration (CEDI, ADDI ou manuel).
	 */
	private String source;

	public EtatDeclarationRetournee() {
		super();
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.RETOURNE;
	}

	public EtatDeclarationRetournee(RegDate dateObtention, String source) {
		super(dateObtention);
		this.source = source;
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
