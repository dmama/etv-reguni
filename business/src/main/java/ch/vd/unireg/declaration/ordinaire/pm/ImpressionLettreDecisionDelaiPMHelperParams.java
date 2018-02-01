package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class ImpressionLettreDecisionDelaiPMHelperParams {

	public enum TypeLettre {
		ACCORD,
		ACCORD_SURSIS,
		REFUS
	}

	private final DeclarationImpotOrdinairePM di;
	private final RegDate dateDemande;
	private final RegDate dateDelaiAccorde;
	private final Long idDelai;
	private final Date logCreationDateDelai;
	private final TypeLettre typeLettre;
	private final RegDate dateSommation;
	private final RegDate dateTraitement;

	public ImpressionLettreDecisionDelaiPMHelperParams(DeclarationImpotOrdinairePM di, DelaiDeclaration delai) throws EditiqueException {
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
			this.typeLettre = delai.isSursis() ? TypeLettre.ACCORD_SURSIS : TypeLettre.ACCORD;
			break;
		default:
			throw new IllegalArgumentException("Impossible d'imprimer un courrier sur une demande de délai ni accordée ni refusée...");
		}

		if (this.typeLettre == TypeLettre.ACCORD_SURSIS) {
			final EtatDeclarationSommee sommation = (EtatDeclarationSommee) di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.SOMME);
			if (sommation == null) {
				throw new EditiqueException("Tentative (vouée à l'échec) de génération d'un document de sursis alors que la déclaration n'est même pas sommée !");
			}
			this.dateSommation = sommation.getDateEnvoiCourrier();
		}
		else {
			this.dateSommation = null;
		}

		if (this.typeLettre == TypeLettre.ACCORD || this.typeLettre == TypeLettre.ACCORD_SURSIS) {
			this.dateDelaiAccorde = delai.getDelaiAccordeAu();
			if (this.dateDelaiAccorde == null) {
				throw new EditiqueException("Tentative (vouée à l'échec) de génération d'un document d'acord/sursis dans date de délai accordé !");
			}
		}
		else {
			this.dateDelaiAccorde = null;
		}
	}

	public DeclarationImpotOrdinairePM getDi() {
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

	public RegDate getDateSommation() {
		return dateSommation;
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
		case ACCORD_SURSIS:
			description = String.format("Confirmation de sursis accordé au %s", RegDateHelper.dateToDisplayString(dateDelaiAccorde));
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
