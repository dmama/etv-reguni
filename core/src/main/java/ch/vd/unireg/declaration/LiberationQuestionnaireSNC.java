package ch.vd.unireg.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.documentfiscal.LiberationDocumentFiscal;

@Entity
@DiscriminatorValue("LIBERATION_QSNC")
public class LiberationQuestionnaireSNC extends LiberationDocumentFiscal {

	private String motif;

	public LiberationQuestionnaireSNC() {
	}

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	public Declaration getDeclaration() {
		return (Declaration) getDocumentFiscal();
	}

	public void setDeclaration(Declaration theDeclaration) {
		setDocumentFiscal(theDeclaration);
	}
	@Column(name = "MOTIF_LIBERATION", length = LengthConstants.DI_LIBERATION)
	public String getMotif() {
		return motif;
	}

	public void setMotif(String motif) {
		this.motif = motif;
	}

}
