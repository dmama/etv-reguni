package ch.vd.uniregctb.rt.view;

import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class RapportPrestationView implements Annulable, DateRange {

	private Long id;

	private String provenance;

	private RegDate dateDebut;

	private RegDate dateFin;

	private TiersGeneralView debiteur;

	private TiersGeneralView sourcier;

	private SensRapportEntreTiers sensRapportEntreTiers;

	private TypeRapportEntreTiers typeRapportEntreTiers;

	private Long numero;

	private List<String> nomCourrier;

	private String natureRapportEntreTiers;

	private String numeroAVS;

	private boolean annule;

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
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

	public TiersGeneralView getDebiteur() {
		return debiteur;
	}

	public void setDebiteur(TiersGeneralView debiteur) {
		this.debiteur = debiteur;
	}

	public TiersGeneralView getSourcier() {
		return sourcier;
	}

	public void setSourcier(TiersGeneralView sourcier) {
		this.sourcier = sourcier;
	}

	public String getProvenance() {
		return provenance;
	}

	public void setProvenance(String provenance) {
		this.provenance = provenance;
	}

	public SensRapportEntreTiers getSensRapportEntreTiers() {
		return sensRapportEntreTiers;
	}

	public void setSensRapportEntreTiers(SensRapportEntreTiers sensRapportEntreTiers) {
		this.sensRapportEntreTiers = sensRapportEntreTiers;
	}

	public TypeRapportEntreTiers getTypeRapportEntreTiers() {
		return typeRapportEntreTiers;
	}

	public void setTypeRapportEntreTiers(TypeRapportEntreTiers typeRapportEntreTiers) {
		this.typeRapportEntreTiers = typeRapportEntreTiers;
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public List<String> getNomCourrier() {
		return nomCourrier;
	}

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier = nomCourrier;
	}

	public String getNatureRapportEntreTiers() {
		return natureRapportEntreTiers;
	}

	public void setNatureRapportEntreTiers(String natureRapportEntreTiers) {
		this.natureRapportEntreTiers = natureRapportEntreTiers;
	}

	public String getNumeroAVS() {
		return numeroAVS;
	}

	public void setNumeroAVS(String numeroAVS) {
		this.numeroAVS = numeroAVS;
	}

}
