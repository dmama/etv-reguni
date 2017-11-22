package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

/**
 * @author Raphaël Marmier, 2017-09-20, <raphael.marmier@vd.ch>
 */
@Entity
@DiscriminatorValue(value = "AUTRE_RAPPELE")
public class EtatAutreDocumentFiscalRappele extends EtatAutreDocumentFiscal implements EtatDocumentFiscalAvecDocumentArchive {

	private String cleArchivage;
	private String cleDocument;

	public EtatAutreDocumentFiscalRappele() {
		super();
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.RAPPELE;
	}

	public EtatAutreDocumentFiscalRappele(RegDate dateObtention) {
		super(dateObtention);
	}

	@Override
	@Column(name = "CLE_ARCHIVAGE", length = LengthConstants.CLE_ARCHIVAGE_FOLDERS)
	public String getCleArchivage() {
		return cleArchivage;
	}

	@Override
	public void setCleArchivage(String cleArchivage) {
		this.cleArchivage = cleArchivage;
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

	@Override
	public String toString() {
		final String desc = super.toString();
		return String.format("%s, (courrier envoyé à cette date)", desc);
	}
}
