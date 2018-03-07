package ch.vd.unireg.documentfiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * @author Raphaël Marmier, 2017-09-20, <raphael.marmier@vd.ch>
 */
@Entity
@DiscriminatorValue(value = "AUTRE_RAPPELE")
public class EtatAutreDocumentFiscalRappele extends EtatAutreDocumentFiscal implements EtatDocumentFiscalAvecDocumentArchive, EtatDocumentFiscalAvecDateEnvoiCourrier {

	private RegDate dateEnvoiCourrier;
	private String cleArchivage;
	private String cleDocument;

	@SuppressWarnings("unused")
	public EtatAutreDocumentFiscalRappele() {
		super();
	}

	public EtatAutreDocumentFiscalRappele(RegDate dateObtention, RegDate dateEnvoiCourrier) {
		super(dateObtention);
		this.dateEnvoiCourrier = dateEnvoiCourrier;
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.RAPPELE;
	}

	@Override
	@Column(name = "DATE_ENVOI_COURRIER")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateEnvoiCourrier() {
		return dateEnvoiCourrier;
	}

	@Override
	public void setDateEnvoiCourrier(RegDate dateEnvoiCourrier) {
		this.dateEnvoiCourrier = dateEnvoiCourrier;
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
