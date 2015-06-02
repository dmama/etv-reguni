package ch.vd.uniregctb.decision.aci;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EditDecisionAciView implements Annulable {
	private Long id;
	private Long tiersId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private String remarque;
	private boolean annule;
	private boolean dateFinEditable;
	private Integer numeroAutoriteFiscale;

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

	public EditDecisionAciView() {
	}

	public EditDecisionAciView(DecisionAci d) {
		this.id = d.getId();
		this.dateDebut = d.getDateDebut();
		this.dateFin =d.getDateFin();
		this.remarque = d.getRemarque();
		this.annule = d.isAnnule();
		this.dateFinEditable = d.getDateFin() == null || d.getDateFin().isAfter(RegDate.get());
		this.tiersId = d.getContribuable().getNumero();
		this.typeAutoriteFiscale = d.getTypeAutoriteFiscale();
		this.numeroAutoriteFiscale = d.getNumeroOfsAutoriteFiscale();
	}

	public void initReadOnlyData(DecisionAci decision) {
		this.id = decision.getId();
		this.tiersId = decision.getContribuable().getNumero();
		this.dateDebut = decision.getDateDebut();
		this.dateFinEditable = decision.getDateFin() == null || decision.getDateFin().isAfter(RegDate.get());
		if (!this.dateFinEditable) {
			this.dateFin = decision.getDateFin();
		}
		this.typeAutoriteFiscale = decision.getTypeAutoriteFiscale();
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

	public boolean isDateFinEditable() {
		return dateFinEditable;
	}

	public void setDateFinEditable(boolean dateFinEditable) {
		this.dateFinEditable = dateFinEditable;
	}
}
