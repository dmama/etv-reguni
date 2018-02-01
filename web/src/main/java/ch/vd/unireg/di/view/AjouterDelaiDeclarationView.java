package ch.vd.unireg.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;

@SuppressWarnings("UnusedDeclaration")
public class AjouterDelaiDeclarationView extends AbstractEditionDelaiDeclarationView {

	// champs du formulaire
	private RegDate dateDemande;
	private RegDate delaiAccordeAu;
	private boolean confirmationEcrite;

	public AjouterDelaiDeclarationView() {
	}

	public AjouterDelaiDeclarationView(DeclarationImpotOrdinaire di, RegDate delaiAccordeAu) {
		super(di);
		this.delaiAccordeAu = delaiAccordeAu;
		this.dateDemande = RegDate.get();
		this.confirmationEcrite = false;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	public RegDate getDelaiAccordeAu() {
		return delaiAccordeAu;
	}

	public void setDelaiAccordeAu(RegDate delaiAccordeAu) {
		this.delaiAccordeAu = delaiAccordeAu;
	}

	public boolean isConfirmationEcrite() {
		return confirmationEcrite;
	}

	public void setConfirmationEcrite(boolean confirmationEcrite) {
		this.confirmationEcrite = confirmationEcrite;
	}
}
