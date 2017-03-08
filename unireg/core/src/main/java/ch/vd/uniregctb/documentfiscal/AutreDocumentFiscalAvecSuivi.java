package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeEtatAutreDocumentFiscal;

@Entity
public abstract class AutreDocumentFiscalAvecSuivi extends AutreDocumentFiscal {

	private RegDate delaiRetour;
	private RegDate dateRetour;
	private RegDate dateRappel;
	private String cleArchivageRappel;
	private String cleDocumentRappel;

	@Column(name = "DELAI_RETOUR")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDelaiRetour() {
		return delaiRetour;
	}

	public void setDelaiRetour(RegDate delaiRetour) {
		this.delaiRetour = delaiRetour;
	}

	@Column(name = "DATE_RETOUR")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	@Column(name = "DATE_RAPPEL")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateRappel() {
		return dateRappel;
	}

	public void setDateRappel(RegDate dateRappel) {
		this.dateRappel = dateRappel;
	}

	@Column(name = "CLE_ARCHIVAGE_RAPPEL", length = LengthConstants.CLE_ARCHIVAGE_FOLDERS)
	public String getCleArchivageRappel() {
		return cleArchivageRappel;
	}

	public void setCleArchivageRappel(String cleArchivageRappel) {
		this.cleArchivageRappel = cleArchivageRappel;
	}

	@Column(name = "CLE_DOCUMENT_RAPPEL", length = LengthConstants.CLE_DOCUMENT_DPERM)
	public String getCleDocumentRappel() {
		return cleDocumentRappel;
	}

	public void setCleDocumentRappel(String cleDocumentRappel) {
		this.cleDocumentRappel = cleDocumentRappel;
	}

	@Transient
	public TypeEtatAutreDocumentFiscal getEtat() {
		if (dateRetour != null) {
			return TypeEtatAutreDocumentFiscal.RETOURNE;
		}
		else if (dateRappel != null) {
			return TypeEtatAutreDocumentFiscal.RAPPELE;
		}
		else {
			return super.getEtat();
		}
	}
}
