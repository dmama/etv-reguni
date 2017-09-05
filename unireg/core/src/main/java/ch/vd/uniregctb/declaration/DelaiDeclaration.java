package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Index;

@Entity
@DiscriminatorValue("DELAI_DECLARATION")
public class DelaiDeclaration extends DelaiDocumentFiscal {

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_DE_DOCFISC_DOCFISC_ID", columnNames = "DOCUMENT_FISCAL_ID")
	public Declaration getDeclaration() {
		return (Declaration) getDocumentFiscal();
	}

	public void setDeclaration(Declaration theDeclaration) {
		setDocumentFiscal(theDeclaration);
	}
}
