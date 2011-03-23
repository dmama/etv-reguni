package ch.vd.uniregctb.rt.view;

import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BaseComparator;
import ch.vd.uniregctb.common.NomCourrierViewPart;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class RapportPrestationView implements Comparable<RapportPrestationView> {

	private static BaseComparator<RapportPrestationView> comparator = new BaseComparator<RapportPrestationView>(new String[] {
			"annule", "dateDebut"
	}, new Boolean[] {
			true, true
	});

	private Long id;

	private String provenance;

	private RegDate dateDebut;

	private RegDate dateFin;

	private TypeActivite typeActivite;

	private Integer tauxActivite;

	private TiersGeneralView debiteur;

	private TiersGeneralView sourcier;

	private SensRapportEntreTiers sensRapportEntreTiers;

	private TypeRapportEntreTiers typeRapportEntreTiers;

	private Long numero;

	private final NomCourrierViewPart nomCourrier = new NomCourrierViewPart();

	private String natureRapportEntreTiers;

	private String numeroAVS;

	private boolean annule;

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

	public RegDate getRegDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getRegDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public Date getDateDebut() {
		return RegDate.asJavaDate(dateDebut);
	}

	public void setDateDebut(Date dateDebut) {
		this.dateDebut = RegDate.get(dateDebut);
	}

	public Date getDateFin() {
		return RegDate.asJavaDate(dateFin);
	}

	public void setDateFin(Date dateFin) {
		this.dateFin = RegDate.get(dateFin);
	}

	public TypeActivite getTypeActivite() {
		return typeActivite;
	}

	public void setTypeActivite(TypeActivite typeActivite) {
		this.typeActivite = typeActivite;
	}

	public Integer getTauxActivite() {
		return tauxActivite;
	}

	public void setTauxActivite(Integer tauxActivite) {
		this.tauxActivite = tauxActivite;
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

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier.setNomCourrier(nomCourrier);
	}

	public String getNomCourrier1() {
		return this.nomCourrier.getNomCourrier1();
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier.setNomCourrier1(nomCourrier1);
	}

	public String getNomCourrier2() {
		return this.nomCourrier.getNomCourrier2();
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier.setNomCourrier2(nomCourrier2);
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

	public int compareTo(RapportPrestationView o) {
		return comparator.compare(this, o);
	}
}
