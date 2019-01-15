package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.editique.EditiqueException;

public class ImpressionLettreDecisionDelaiPPHelperParams {

	public enum TypeLettre {
		ACCORD,
		REFUS
	}

	private final DeclarationImpotOrdinairePP di;
	private final RegDate dateDemande;
	private final RegDate dateDelaiAccorde;
	private final Long idDelai;
	private final Date logCreationDateDelai;
	private final TypeLettre typeLettre;
	private final RegDate dateTraitement;

	public ImpressionLettreDecisionDelaiPPHelperParams(DeclarationImpotOrdinairePP di, DelaiDeclaration delai) throws EditiqueException {
		super();
		this.di = di;
		this.idDelai = delai.getId();
		this.logCreationDateDelai = delai.getLogCreationDate();
		this.dateDemande = delai.getDateDemande();
		this.dateTraitement = delai.getDateTraitement();

		switch (delai.getEtat()) {
		case REFUSE:
			this.typeLettre = TypeLettre.REFUS;
			break;
		case ACCORDE:
			this.typeLettre = TypeLettre.ACCORD;
			break;
		default:
			throw new IllegalArgumentException("Impossible d'imprimer un courrier sur une demande de délai ni accordée ni refusée...");
		}

		if (this.typeLettre == TypeLettre.ACCORD) {
			this.dateDelaiAccorde = delai.getDelaiAccordeAu();
			if (this.dateDelaiAccorde == null) {
				throw new EditiqueException("Tentative (vouée à l'échec) de génération d'un document d'acord sans date de délai accordé !");
			}
		}
		else {
			this.dateDelaiAccorde = null;
		}
	}

	public DeclarationImpotOrdinairePP getDi() {
		return di;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public RegDate getDateDelaiAccorde() {
		return dateDelaiAccorde;
	}

	public Long getIdDelai() {
		return idDelai;
	}

	public Date getLogCreationDateDelai() {
		return logCreationDateDelai;
	}

	public TypeLettre getTypeLettre() {
		return typeLettre;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public String getDescriptionDocument() {
		final String description;
		switch (typeLettre) {
		case ACCORD:
			description = String.format("Confirmation de délai accordé au %s", RegDateHelper.dateToDisplayString(dateDelaiAccorde));
			break;
		case REFUS:
			description = "Refus de délai";
			break;
		default:
			throw new IllegalArgumentException("Type de lettre non-supporté : " + typeLettre);
		}

		return String.format("%s de la déclaration d'impôt %d du contribuable %s",
		                     description,
		                     di.getPeriode().getAnnee(),
		                     FormatNumeroHelper.numeroCTBToDisplay(di.getTiers().getNumero()));
	}
}
