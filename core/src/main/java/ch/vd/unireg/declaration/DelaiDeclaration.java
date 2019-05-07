package ch.vd.unireg.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.type.TypeDelaiDeclaration;

@Entity
@DiscriminatorValue("DELAI_DECLARATION")
public class DelaiDeclaration extends DelaiDocumentFiscal {

	private TypeDelaiDeclaration typeDelai;

	public DelaiDeclaration() {
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	public Declaration getDeclaration() {
		return (Declaration) getDocumentFiscal();
	}

	public void setDeclaration(Declaration theDeclaration) {
		setDocumentFiscal(theDeclaration);
	}

	@Column(name = "TYPE_DELAI", length = LengthConstants.TYPE_DELAI_DECL)
	@Enumerated(value = EnumType.STRING)
	public TypeDelaiDeclaration getTypeDelai() {
		return typeDelai;
	}

	public void setTypeDelai(TypeDelaiDeclaration typeDelai) {
		this.typeDelai = typeDelai;
	}
}
