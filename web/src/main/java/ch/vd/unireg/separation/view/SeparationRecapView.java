package ch.vd.unireg.separation.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.EtatCivil;

public class SeparationRecapView {

	private Long idMenage;
	private RegDate dateSeparation;
	private EtatCivil etatCivil;
	private String remarque;

	public SeparationRecapView(long idMenage) {
		this.idMenage = idMenage;
		this.etatCivil = EtatCivil.DIVORCE;
	}

	public SeparationRecapView() {
	}

	public Long getIdMenage() {
		return idMenage;
	}

	public void setIdMenage(Long idMenage) {
		this.idMenage = idMenage;
	}

	public RegDate getDateSeparation() {
		return dateSeparation;
	}

	public void setDateSeparation(RegDate dateSeparation) {
		this.dateSeparation = dateSeparation;
	}

	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public void setEtatCivil(EtatCivil etatCivil) {
		this.etatCivil = etatCivil;
	}

	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}



}
