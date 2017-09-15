package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

@Entity
@DiscriminatorValue("DI_EMISE")
public class EtatDeclarationEmise extends EtatDeclaration implements EtatDeclarationAvecDocumentArchive {

	private String cleDocument;

	public EtatDeclarationEmise() {
		super();
	}

	public EtatDeclarationEmise(RegDate dateObtention) {
		super(dateObtention);
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.EMISE;
	}

	@Override
	@Column(name = "CLE_DOCUMENT", length = LengthConstants.CLE_DOCUMENT_DPERM)
	public String getCleDocument() {
		return cleDocument;
	}

	@Override
	public void setCleDocument(String cleDocument) {
		this.cleDocument = cleDocument;
	}

}
