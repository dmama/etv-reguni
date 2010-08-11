package ch.vd.uniregctb.rapport.view;

import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BaseComparator;
import ch.vd.uniregctb.common.NomCourrierViewPart;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.type.SensRapportEntreTiers;
import ch.vd.uniregctb.type.TypeActivite;

/**
 * Classe utilisée pour le fromBackingObject du RapportEditController
 *
 * @author xcifde
 *
 */
public class RapportView implements Comparable<RapportView> {

	private static BaseComparator<RapportView> comparator = new BaseComparator<RapportView>(new String[] {
			"annule", "dateDebut"
	}, new Boolean[] {
			true, true
	});

	private RegDate dateDebut;

	private RegDate dateFin;

	private SensRapportEntreTiers sensRapportEntreTiers;

	private TiersGeneralView tiers;

	private TiersGeneralView tiersLie;

	private Long id;

	private boolean annule;

	private TypeRapportEntreTiersWeb typeRapportEntreTiers;

	private Long numero;

	/**
	 * Message à afficher en lieu et place du numéro de contribuable si celui-ci est absent
	 */
	private String messageNumeroAbsent;

	private final NomCourrierViewPart nomCourrier = new NomCourrierViewPart();

	private String natureRapportEntreTiers;

	private boolean isAllowed;

	// --- uniquement pour RapportPrestationImposable ----

	private TypeActivite typeActivite;

	private Integer tauxActivite;

	// -- uniquement pour RepresentationConventionnelle --

	private boolean extensionExecutionForcee;

	// ---------------------------------------------------

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
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

	public TypeRapportEntreTiersWeb getTypeRapportEntreTiers() {
		return typeRapportEntreTiers;
	}

	public void setTypeRapportEntreTiers(TypeRapportEntreTiersWeb typeRapportEntreTiers) {
		this.typeRapportEntreTiers = typeRapportEntreTiers;
	}

	public RegDate getRegDateDebut() {
		return dateDebut;
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

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public String getMessageNumeroAbsent() {
		return messageNumeroAbsent;
	}

	public void setMessageNumeroAbsent(String messageNumeroAbsent) {
		this.messageNumeroAbsent = messageNumeroAbsent;
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

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public TiersGeneralView getTiers() {
		return tiers;
	}

	public void setTiers(TiersGeneralView tiers) {
		this.tiers = tiers;
	}

	public TiersGeneralView getTiersLie() {
		return tiersLie;
	}

	public void setTiersLie(TiersGeneralView tiersLie) {
		this.tiersLie = tiersLie;
	}

	public SensRapportEntreTiers getSensRapportEntreTiers() {
		return sensRapportEntreTiers;
	}

	public void setSensRapportEntreTiers(SensRapportEntreTiers sensRapportEntreTiers) {
		this.sensRapportEntreTiers = sensRapportEntreTiers;
	}

	public String getNatureRapportEntreTiers() {
		return natureRapportEntreTiers;
	}

	public void setNatureRapportEntreTiers(String natureRapportEntreTiers) {
		this.natureRapportEntreTiers = natureRapportEntreTiers;
	}

	public int compareTo(RapportView o) {
		return comparator.compare(this, o);
	}

	public boolean isAllowed() {
		return isAllowed;
	}

	public void setAllowed(boolean isAllowed) {
		this.isAllowed = isAllowed;
	}

	public boolean isExtensionExecutionForcee() {
		return extensionExecutionForcee;
	}

	public void setExtensionExecutionForcee(boolean extensionExecutionForcee) {
		this.extensionExecutionForcee = extensionExecutionForcee;
	}
}
