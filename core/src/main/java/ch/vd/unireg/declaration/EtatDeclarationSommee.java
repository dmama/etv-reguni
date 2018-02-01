package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.documentfiscal.EtatDocumentFiscalAvecDocumentArchive;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

/**
 * Une sommation a été émise pour la déclaration.
 */
@Entity
@DiscriminatorValue("DI_SOMMEE")
public class EtatDeclarationSommee extends EtatDeclaration implements EtatDocumentFiscalAvecDocumentArchive {

	private RegDate dateEnvoiCourrier;
	private Integer emolument;
	private String cleArchivage;
	private String cleDocument;

	public EtatDeclarationSommee() {
		super();
	}

	@Transient
	@Override
	public TypeEtatDocumentFiscal getType() {
		return TypeEtatDocumentFiscal.SOMME;
	}

	public EtatDeclarationSommee(RegDate dateObtention, RegDate dateEnvoiCourrier, @Nullable Integer emolument) {
		super(dateObtention);
		this.dateEnvoiCourrier = dateEnvoiCourrier;
		this.emolument = emolument;
	}

	@Column(name = "DATE_ENVOI_COURRIER")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEnvoiCourrier() {
		return dateEnvoiCourrier;
	}

	public void setDateEnvoiCourrier(RegDate dateEnvoiCourrier) {
		this.dateEnvoiCourrier = dateEnvoiCourrier;
	}

	@Nullable
	@Column(name = "EMOLUMENT", nullable = true)
	public Integer getEmolument() {
		return emolument;
	}

	public void setEmolument(Integer emolument) {
		this.emolument = emolument;
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
		final String emolumentStr = emolument == null ? StringUtils.EMPTY : String.format(", émolument de %d %s", emolument, MontantMonetaire.CHF);
		return String.format("%s, (courrier envoyé le %s%s)", desc, dateEnvoiStr, emolumentStr);
	}
}
