package ch.vd.uniregctb.documentfiscal;

import javax.persistence.CascadeType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2017-09-20, <raphael.marmier@vd.ch>
 */
public abstract class EtatAutreDocumentFiscal extends EtatDocumentFiscal<EtatAutreDocumentFiscal>{

	public EtatAutreDocumentFiscal() {
		super();
	}

	public EtatAutreDocumentFiscal(RegDate dateObtention) {
		super(dateObtention);
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@ForeignKey(name = "FK_ET_DOCFISC_DOCFISC_ID")
	@Index(name = "IDX_ET_DOCFISC_DOCFISC_ID", columnNames = "DOCUMENT_FISCAL_ID")
	public AutreDocumentFiscal getAutreDocumentFiscal() {
		return (AutreDocumentFiscal) getDocumentFiscal();
	}

	public void setAutreDocumentFiscal(AutreDocumentFiscal autreDocumentFiscal) {
		setDocumentFiscal(autreDocumentFiscal);
	}

}
