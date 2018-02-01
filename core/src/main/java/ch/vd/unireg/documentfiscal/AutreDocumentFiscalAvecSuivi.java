package ch.vd.unireg.documentfiscal;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Optional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

@Entity
public abstract class AutreDocumentFiscalAvecSuivi extends AutreDocumentFiscal {

	// Compatibilité avec l'ancienne structure de données des autres documents fiscaux.
	@Transient
	public RegDate getDelaiRetour() {
		return Optional.ofNullable(getDernierDelaiAccorde())
				.map(DelaiDocumentFiscal::getDelaiAccordeAu)
				.orElse(null);
	}

	public void setDelaiRetour(RegDate delaiRetour) {
		throw new UnsupportedOperationException("TODO: Stocker la date de délai dans le délai du document.");
	}

	// Compatibilité avec l'ancienne structure de données des autres documents fiscaux.
	@Transient
	public RegDate getDateRetour() {
		return Optional.ofNullable(getEtatRetourne())
				.map(EtatAutreDocumentFiscalRetourne::getDateObtention)
				.orElse(null);
	}

	public void setDateRetour(RegDate dateRetour) {
		final EtatDocumentFiscal etat = getDernierEtatOfType(TypeEtatDocumentFiscal.RETOURNE);
		if (etat == null) {
			addEtat(new EtatAutreDocumentFiscalRetourne(dateRetour));
		}
		else {
			etat.setDateObtention(dateRetour);
		}
	}

	// Compatibilité avec l'ancienne structure de données des autres documents fiscaux.
	@Transient
	public RegDate getDateRappel() {
		return Optional.ofNullable(getEtatRappele())
				.map(EtatAutreDocumentFiscalRappele::getDateObtention)
				.orElse(null);
	}

	public void setDateRappel(RegDate dateRappel) {
		final EtatDocumentFiscal etat = getDernierEtatOfType(TypeEtatDocumentFiscal.RAPPELE);
		if (etat == null) {
			addEtat(new EtatAutreDocumentFiscalRappele(dateRappel));
		}
		else {
			etat.setDateObtention(dateRappel);
		}
	}

	// Compatibilité avec l'ancienne structure de données des autres documents fiscaux.
	@Transient
	public String getCleArchivageRappel() {
		return Optional.ofNullable(getEtatRappele())
				.map(EtatAutreDocumentFiscalRappele::getCleArchivage)
				.orElse(null);
	}

	public void setCleArchivageRappel(String cleArchivageRappel) {
		sauverCleArchivage(TypeEtatDocumentFiscal.RAPPELE, cleArchivageRappel);
	}

	// Compatibilité avec l'ancienne structure de données des autres documents fiscaux.
	@Transient
	public String getCleDocumentRappel() {
		return Optional.ofNullable(getEtatRappele())
				.map(EtatAutreDocumentFiscalRappele::getCleDocument)
				.orElse(null);
	}

	public void setCleDocumentRappel(String cleDocumentRappel) {
		sauverCleDocument(TypeEtatDocumentFiscal.RAPPELE, cleDocumentRappel);
	}

	@Override
	@Transient
	public boolean isRappelable() {
		return true;
	}
}
