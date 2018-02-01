package ch.vd.unireg.supergra.view;

import ch.vd.registre.base.date.RegDate;

public class Pp2McView {
	private long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private Long idPrincipal;
	private Long idSecondaire;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public Long getIdPrincipal() {
		return idPrincipal;
	}

	public void setIdPrincipal(Long idPrincipal) {
		this.idPrincipal = idPrincipal;
	}

	public Long getIdSecondaire() {
		return idSecondaire;
	}

	public void setIdSecondaire(Long idSecondaire) {
		this.idSecondaire = idSecondaire;
	}
}
