package ch.vd.unireg.documentfiscal;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Raphaël Marmier, 2017-09-21, <raphael.marmier@vd.ch>
 */
@Entity
@DiscriminatorValue("DELAI_AUTRE_DOCUMENT_FISCAL")
public class DelaiAutreDocumentFiscal extends DelaiDocumentFiscal {

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	public AutreDocumentFiscal getAutreDocumentFiscal() {
		return (AutreDocumentFiscal) getDocumentFiscal();
	}

	public void setAutreDocumentFiscal(AutreDocumentFiscal autreDocumentFiscal) {
		setDocumentFiscal(autreDocumentFiscal);
	}
}
