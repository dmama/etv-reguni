package ch.vd.uniregctb.annulation.deces.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.general.view.TiersGeneralView;

public class AnnulationDecesRecapView {

	private TiersGeneralView personne;

	private RegDate dateDeces;
	private RegDate dateDecesDepuisDernierForPrincipal;

	private RegDate dateVeuvage;

	private boolean marieSeulAndVeuf;

	private boolean warningDateDecesModifie;



	public TiersGeneralView getPersonne() {
		return personne;
	}

	public void setPersonne(TiersGeneralView personne) {
		this.personne = personne;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(RegDate dateDeces) {
		this.dateDeces = dateDeces;
	}

	public RegDate getDateVeuvage() {
		return dateVeuvage;
	}

	public void setDateVeuvage(RegDate dateVeuvage) {
		this.dateVeuvage = dateVeuvage;
	}

	public boolean isMarieSeulAndVeuf() {
		return marieSeulAndVeuf;
	}

	public void setMarieSeulAndVeuf(boolean marieSeulAndVeuf) {
		this.marieSeulAndVeuf = marieSeulAndVeuf;
	}

	public boolean isWarningDateDecesModifie() {
		return warningDateDecesModifie;
	}

	public void setWarningDateDecesModifie(boolean warningDateDecesModifie) {
		this.warningDateDecesModifie = warningDateDecesModifie;
	}

	public RegDate getDateDecesDepuisDernierForPrincipal() {
		return dateDecesDepuisDernierForPrincipal;
	}

	public void setDateDecesDepuisDernierForPrincipal(RegDate dateDecesDepuisDernierForPrincipal) {
		this.dateDecesDepuisDernierForPrincipal = dateDecesDepuisDernierForPrincipal;
	}
}
