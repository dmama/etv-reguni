package ch.vd.unireg.etiquette;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public class AddEtiquetteTiersView implements DateRange {

	private Long tiersId;
	private String codeEtiquette;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String commentaire;

	public AddEtiquetteTiersView() {
	}

	public AddEtiquetteTiersView(Long tiersId) {
		this.tiersId = tiersId;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public String getCodeEtiquette() {
		return codeEtiquette;
	}

	public void setCodeEtiquette(String codeEtiquette) {
		this.codeEtiquette = codeEtiquette;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}
}
