package ch.vd.unireg.documentfiscal;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2017-09-20, <raphael.marmier@vd.ch>
 */
@Entity
public abstract class EtatAutreDocumentFiscal extends EtatDocumentFiscal<EtatAutreDocumentFiscal>{

	public EtatAutreDocumentFiscal() {
		super();
	}

	public EtatAutreDocumentFiscal(RegDate dateObtention) {
		super(dateObtention);
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	public AutreDocumentFiscal getAutreDocumentFiscal() {
		return (AutreDocumentFiscal) getDocumentFiscal();
	}

	public void setAutreDocumentFiscal(AutreDocumentFiscal autreDocumentFiscal) {
		setDocumentFiscal(autreDocumentFiscal);
	}

}
