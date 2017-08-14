package ch.vd.uniregctb.tiers.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.TarifImpotSource;

public class SituationFamilleView implements Comparable<SituationFamilleView>, Annulable {

	private Long numeroCtb;

	private Long numeroTiersRevenuPlusEleve;

	private Long numeroTiers1;

	private Long numeroTiers2;

	private String nomCourrier1TiersRevenuPlusEleve;

	private String nomCourrier1Tiers1;

	private String nomCourrier1Tiers2;

	private Long id;

	private String nomCourrier;

	private RegDate dateDebut;

	private RegDate dateFin;

	private Integer nombreEnfants;

	private EtatCivil etatCivil;

	private TarifImpotSource tarifImpotSource;

	private String natureSituationFamille;

	private boolean annule;
	
	private boolean editable;

	private String source;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNomCourrier() {
		return nomCourrier;
	}

	public void setNomCourrier(String nomCourrier) {
		this.nomCourrier = nomCourrier;
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

	public Integer getNombreEnfants() {
		return nombreEnfants;
	}

	public void setNombreEnfants(Integer nombreEnfants) {
		this.nombreEnfants = nombreEnfants;
	}

	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public void setEtatCivil(EtatCivil etatCivil) {
		this.etatCivil = etatCivil;
	}

	public TarifImpotSource getTarifImpotSource() {
		return tarifImpotSource;
	}

	public void setTarifImpotSource(TarifImpotSource tarifImpotSource) {
		this.tarifImpotSource = tarifImpotSource;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public String getNatureSituationFamille() {
		return natureSituationFamille;
	}

	public void setNatureSituationFamille(String natureSituationFamille) {
		this.natureSituationFamille = natureSituationFamille;
	}

	public Long getNumeroCtb() {
		return numeroCtb;
	}

	public void setNumeroCtb(Long numeroCtb) {
		this.numeroCtb = numeroCtb;
	}

	public Long getNumeroTiersRevenuPlusEleve() {
		return numeroTiersRevenuPlusEleve;
	}

	public void setNumeroTiersRevenuPlusEleve(Long numeroTiersRevenuPlusEleve) {
		this.numeroTiersRevenuPlusEleve = numeroTiersRevenuPlusEleve;
	}

	public String getNomCourrier1TiersRevenuPlusEleve() {
		return nomCourrier1TiersRevenuPlusEleve;
	}

	public void setNomCourrier1TiersRevenuPlusEleve(String nomCourrier1TiersRevenuPlusEleve) {
		this.nomCourrier1TiersRevenuPlusEleve = nomCourrier1TiersRevenuPlusEleve;
	}

	public Long getNumeroTiers1() {
		return numeroTiers1;
	}

	public void setNumeroTiers1(Long numeroTiers1) {
		this.numeroTiers1 = numeroTiers1;
	}

	public Long getNumeroTiers2() {
		return numeroTiers2;
	}

	public void setNumeroTiers2(Long numeroTiers2) {
		this.numeroTiers2 = numeroTiers2;
	}

	public String getNomCourrier1Tiers1() {
		return nomCourrier1Tiers1;
	}

	public void setNomCourrier1Tiers1(String nomCourrier1Tiers1) {
		this.nomCourrier1Tiers1 = nomCourrier1Tiers1;
	}

	public String getNomCourrier1Tiers2() {
		return nomCourrier1Tiers2;
	}

	public void setNomCourrier1Tiers2(String nomCourrier1Tiers2) {
		this.nomCourrier1Tiers2 = nomCourrier1Tiers2;
	}

	@Override
	public int compareTo(@NotNull SituationFamilleView o) {
		return - getDateDebut().compareTo(o.getDateDebut());
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean isAllowed) {
		this.editable = isAllowed;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
