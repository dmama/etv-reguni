package ch.vd.uniregctb.decision.aci;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class AddDecisionAciView implements Annulable{
	private Long id;
	private Long tiersId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private String remarque;
	private boolean annule;
	private Integer numeroAutoriteFiscale;
	private String autoriteFiscaleNom;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	public String getAutoriteFiscaleNom() {
		return autoriteFiscaleNom;
	}

	public void setAutoriteFiscaleNom(String autoriteFiscaleNom) {
		this.autoriteFiscaleNom = autoriteFiscaleNom;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public Integer getNumeroAutoriteFiscale() {
		return numeroAutoriteFiscale;
	}

	public void setNumeroAutoriteFiscale(Integer numeroAutoriteFiscale) {
		this.numeroAutoriteFiscale = numeroAutoriteFiscale;
	}

	public AddDecisionAciView(Long tiersId) {
		this.tiersId = tiersId;
		this.typeAutoriteFiscale = TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;

	}


	public AddDecisionAciView() {
	}


	/**
	 * Permet de dire si une entite est annulé ou non
	 *
	 * @return true si l'entite est annulée false sinon
	 */
	@Override
	public boolean isAnnule() {
		return annule;
	}

}
