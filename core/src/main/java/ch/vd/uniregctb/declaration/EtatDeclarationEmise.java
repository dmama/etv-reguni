package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.documentfiscal.EtatDocumentFiscalAvecDocumentArchive;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

/**
 * Déclaration émise, en attente de retour depuis le tiers. C'est le premier état de toute déclaration.
 */
@Entity
@DiscriminatorValue("DI_EMISE")
public class EtatDeclarationEmise extends EtatDeclaration implements EtatDocumentFiscalAvecDocumentArchive {

	private String cleArchivage;
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
