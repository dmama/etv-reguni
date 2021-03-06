package ch.vd.unireg.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAvecDateEnvoiCourrier;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAvecDocumentArchive;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * Un rappel a été émis pour la déclaration.
 */
@Entity
@DiscriminatorValue("DI_RAPPELEE")
public class EtatDeclarationRappelee extends EtatDeclaration implements EtatDocumentFiscalAvecDocumentArchive, EtatDocumentFiscalAvecDateEnvoiCourrier {

	private RegDate dateEnvoiCourrier;
	private String cleArchivage;
	private String cleDocument;

	public EtatDeclarationRappelee() {
		super();
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.RAPPELE;
	}

	public EtatDeclarationRappelee(RegDate dateObtention, RegDate dateEnvoiCourrier) {
		super(dateObtention);
		this.dateEnvoiCourrier = dateEnvoiCourrier;
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
		final String dateEnvoiStr = dateEnvoiCourrier != null ? RegDateHelper.dateToDisplayString(dateEnvoiCourrier) : "?";
		return String.format("%s, (courrier envoyé le %s)", desc, dateEnvoiStr);
	}
}
