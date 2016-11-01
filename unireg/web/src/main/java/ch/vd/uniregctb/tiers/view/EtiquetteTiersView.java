package ch.vd.uniregctb.tiers.view;

import java.util.Optional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;

public class EtiquetteTiersView implements Annulable, DateRange {

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String commentaire;
	private boolean annule;
	private String code;
	private String libelle;
	private Long idCollectiviteAdministrative;

	public EtiquetteTiersView(EtiquetteTiers etiquetteTiers) {
		this.id = etiquetteTiers.getId();
		this.dateDebut = etiquetteTiers.getDateDebut();
		this.dateFin = etiquetteTiers.getDateFin();
		this.annule = etiquetteTiers.isAnnule();
		this.commentaire = etiquetteTiers.getCommentaire();

		final Etiquette etiquette = etiquetteTiers.getEtiquette();
		this.code = etiquette.getCode();
		this.libelle = etiquette.getLibelle();
		this.idCollectiviteAdministrative = Optional.ofNullable(etiquette.getCollectiviteAdministrative())
				.map(CollectiviteAdministrative::getNumero)
				.orElse(null);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	public Long getIdCollectiviteAdministrative() {
		return idCollectiviteAdministrative;
	}

	public void setIdCollectiviteAdministrative(Long idCollectiviteAdministrative) {
		this.idCollectiviteAdministrative = idCollectiviteAdministrative;
	}
}
