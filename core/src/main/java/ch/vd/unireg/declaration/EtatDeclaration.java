package ch.vd.unireg.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;

/**
 * @author jec
 */
@Entity
public abstract class EtatDeclaration extends EtatDocumentFiscal<EtatDeclaration> implements LinkedEntity {

	public EtatDeclaration() {
		super();
	}

	public EtatDeclaration(RegDate dateObtention) {
		super(dateObtention);
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	public Declaration getDeclaration() {
		return (Declaration) getDocumentFiscal();
	}

	public void setDeclaration(Declaration theDeclaration) {
		setDocumentFiscal(theDeclaration);
	}

}
