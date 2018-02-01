package ch.vd.unireg.etiquette;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public class EditEtiquetteTiersView implements DateRange {

	private Long tiersId;
	private Long etiquetteTiersId;
	private String codeEtiquette;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String commentaire;

	public EditEtiquetteTiersView() {
	}

	public EditEtiquetteTiersView(EtiquetteTiers etiquetteTiers) {
		this.tiersId = etiquetteTiers.getTiers().getNumero();
		this.etiquetteTiersId = etiquetteTiers.getId();
		this.codeEtiquette = etiquetteTiers.getEtiquette().getCode();
		this.dateDebut = etiquetteTiers.getDateDebut();
		this.dateFin = etiquetteTiers.getDateFin();
		this.commentaire = etiquetteTiers.getCommentaire();
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public Long getEtiquetteTiersId() {
		return etiquetteTiersId;
	}

	public void setEtiquetteTiersId(Long etiquetteTiersId) {
		this.etiquetteTiersId = etiquetteTiersId;
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
