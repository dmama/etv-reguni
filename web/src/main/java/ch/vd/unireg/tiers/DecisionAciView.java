package ch.vd.unireg.tiers;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class DecisionAciView implements Annulable{
	private Long id;
	private Long tiersId;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private String remarque;
	private boolean annule;
	private Integer numeroForFiscalCommune;
	private Integer numeroForFiscalCommuneHorsCanton;
	private Integer numeroForFiscalPays;


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

	public Date getDebutInFormatDate(){
		return RegDate.asJavaDate(dateDebut);
	}

	public Date getFinInFormatDate(){
		return RegDate.asJavaDate(dateFin);
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

	public DecisionAciView(Long tiersId) {
		this.tiersId = tiersId;
	}

	public DecisionAciView(DecisionAci d) {
		this.id = d.getId();
		this.tiersId = d.getContribuable().getNumero();
		this.dateDebut = d.getDateDebut();
		this.dateFin =d.getDateFin();
		this.remarque = d.getRemarque();
		this.annule = d.isAnnule();
		setTypeEtNumeroForFiscal(d.getTypeAutoriteFiscale(),d.getNumeroOfsAutoriteFiscale());
	}

	private void setTypeEtNumeroForFiscal(TypeAutoriteFiscale taf, int noOfs) {
		this.typeAutoriteFiscale = taf;
		switch (taf) {
		case COMMUNE_OU_FRACTION_VD:
			this.numeroForFiscalCommune = noOfs;
			break;
		case COMMUNE_HC:
			this.numeroForFiscalCommuneHorsCanton = noOfs;
			break;
		case PAYS_HS:
			this.numeroForFiscalPays = noOfs;
			break;
		}
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

	public Integer getNumeroForFiscalCommune() {
		return numeroForFiscalCommune;
	}

	public void setNumeroForFiscalCommune(Integer numeroForFiscalCommune) {
		this.numeroForFiscalCommune = numeroForFiscalCommune;
	}

	public Integer getNumeroForFiscalCommuneHorsCanton() {
		return numeroForFiscalCommuneHorsCanton;
	}

	public void setNumeroForFiscalCommuneHorsCanton(Integer numeroForFiscalCommuneHorsCanton) {
		this.numeroForFiscalCommuneHorsCanton = numeroForFiscalCommuneHorsCanton;
	}

	public Integer getNumeroForFiscalPays() {
		return numeroForFiscalPays;
	}

	public void setNumeroForFiscalPays(Integer numeroForFiscalPays) {
		this.numeroForFiscalPays = numeroForFiscalPays;
	}


}
