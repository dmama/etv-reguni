package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

@Entity
public abstract class AutreDocumentFiscalAvecSuivi extends AutreDocumentFiscal {

	@Transient
	public RegDate getDelaiRetour() {
		throw new UnsupportedOperationException("TODO: Rechercher la date de délai dans le délai du document.");
	}

	public void setDelaiRetour(RegDate delaiRetour) {
		throw new UnsupportedOperationException("TODO: Stocker la date de délai dans le délai du document.");
	}

	@Transient
	public RegDate getDateRetour() {
		throw new UnsupportedOperationException("TODO: Rechercher la date de retour dans l'état retourné du document.");
	}

	public void setDateRetour(RegDate dateRetour) {
		throw new UnsupportedOperationException("TODO: Stocker la date de retour dans l'état retourné du document.");
	}

	@Transient
	public RegDate getDateRappel() {
		throw new UnsupportedOperationException("TODO: Rechercher la date de rappel dans l'état rappelé du document.");
	}

	public void setDateRappel(RegDate dateRappel) {
		throw new UnsupportedOperationException("TODO: Stocker la date de rappel dans l'état rappelé du document.");
	}

	@Transient
	public String getCleArchivageRappel() {
		throw new UnsupportedOperationException("TODO: Rechercher la clé d'archivage de rappel dans l'état rappelé du document.");
	}

	public void setCleArchivageRappel(String cleArchivageRappel) {
		throw new UnsupportedOperationException("TODO: Stocker la clé d'archivage de rappel dans l'état rappelé du document.");
	}

	@Transient
	public String getCleDocumentRappel() {
		throw new UnsupportedOperationException("TODO: Rechercher la clé de document de rappel dans l'état rappelé du document.");
	}

	public void setCleDocumentRappel(String cleDocumentRappel) {
		throw new UnsupportedOperationException("TODO: Stocker la clé de document de rappel dans l'état rappelé du document.");
	}

	@Transient
	public TypeEtatDocumentFiscal getEtat() {
		throw new UnsupportedOperationException("TODO: Rechercher le dernier état et retourner son type.");
/*
		if (dateRetour != null) {
			return TypeEtatDocumentFiscal.RETOURNE;
		}
		else if (dateRappel != null) {
			return TypeEtatDocumentFiscal.RAPPELE;
		}
		else {
			return super.getEtat();
		}
*/
	}

	@Override
	@Transient
	public boolean isRappelable() {
		return true;
	}
}
