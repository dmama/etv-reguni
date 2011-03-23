package ch.vd.uniregctb.deces.view;

import java.util.Date;

import ch.vd.uniregctb.general.view.TiersGeneralView;

public class DecesRecapView {

	private Date dateDeces;

	private TiersGeneralView personne;
	
	private boolean marieSeul;
	
	private boolean veuf;

	private String remarque;
	
	public Date getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(Date dateDeces) {
		this.dateDeces = dateDeces;
	}

	public TiersGeneralView getPersonne() {
		return personne;
	}

	public void setPersonne(TiersGeneralView personne) {
		this.personne = personne;
	}

	public boolean isMarieSeul() {
		return marieSeul;
	}

	public void setMarieSeul(boolean marieSeul) {
		this.marieSeul = marieSeul;
	}

	public boolean isVeuf() {
		return veuf;
	}

	public void setVeuf(boolean veuf) {
		this.veuf = veuf;
	}

	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}

}
