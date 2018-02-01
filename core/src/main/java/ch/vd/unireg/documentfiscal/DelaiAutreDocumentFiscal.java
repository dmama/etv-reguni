package ch.vd.unireg.documentfiscal;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

/**
 * @author Raphaël Marmier, 2017-09-21, <raphael.marmier@vd.ch>
 */
@Entity
@DiscriminatorValue("DELAI_AUTRE_DOCUMENT_FISCAL")
public class DelaiAutreDocumentFiscal extends DelaiDocumentFiscal {

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@ForeignKey(name = "FK_DEL_DOCFISC_DOCFISC_ID")
	@Index(name = "IDX_DEL_DOCFISC_DOCFISC_ID", columnNames = "DOCUMENT_FISCAL_ID")
	public AutreDocumentFiscal getAutreDocumentFiscal() {
		return (AutreDocumentFiscal) getDocumentFiscal();
	}

	public void setAutreDocumentFiscal(AutreDocumentFiscal autreDocumentFiscal) {
		setDocumentFiscal(autreDocumentFiscal);
	}
}
