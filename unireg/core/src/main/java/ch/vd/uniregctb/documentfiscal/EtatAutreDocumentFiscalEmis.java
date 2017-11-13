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
@DiscriminatorValue(value = "AUTRE_EMIS")
public class EtatAutreDocumentFiscalEmis extends EtatAutreDocumentFiscal implements EtatAutreDocumentFiscalAvecDocumentArchive {

	private String cleArchivage;
	private String cleDocument;

	public EtatAutreDocumentFiscalEmis() {
		super();
	}

	public EtatAutreDocumentFiscalEmis(RegDate dateObtention) {
		super(dateObtention);
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.EMIS;
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
}
