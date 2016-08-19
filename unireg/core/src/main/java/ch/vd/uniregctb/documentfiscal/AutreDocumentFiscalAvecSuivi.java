package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatAutreDocumentFiscal;

@Entity
public abstract class AutreDocumentFiscalAvecSuivi extends AutreDocumentFiscal {

	private RegDate delaiRetour;
	private RegDate dateRetour;
	private RegDate dateRappel;

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
