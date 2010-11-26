package ch.vd.uniregctb.tiers.view;

import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class AdresseView  implements Comparable<AdresseView>, Annulable{

	private Long id;

	private RegDate dateDebut;

	private RegDate dateFin;

	private String numeroMaison;

	private String numeroAppartement;

	private TexteCasePostale texteCasePostale;

	private Integer numeroCasePostale;

	private String rue;

	private String complements;

	private boolean permanente;

	/**
	 * Source de l'adresse (civile ou fiscale)
	 */
	private AdresseGenerique.Source source;

	// Suisse
	private Integer numeroRue;

	private String numeroOrdrePoste;

	private String numCommune;

	// Etranger
	private String numeroPostal;

	private String localite;

	private String localiteSuisse;

	private String paysNpa;

	private Integer paysOFS;

	private String typeLocalite ;

	private TypeAdresseTiers usage;

	private TypeAdresseCivil usageCivil;

	private List<AdresseView> adressesList;

	private Long numCTB;

	private String pays;

	private String complementLocalite;

	private String localiteNpa ;

	private boolean isDefault;

	private boolean active;

	private boolean annule;

	private String mode;

	private List<AdresseDisponibleView> adresseDisponibles ;

	private String index ;

	private NatureTiers nature ;

	private boolean surVaud;

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public List<AdresseDisponibleView> getAdresseDisponibles() {
		return adresseDisponibles;
	}

	public void setAdresseDisponibles(List<AdresseDisponibleView> adresseDisponibles) {
		this.adresseDisponibles = adresseDisponibles;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the dateDebut
	 */
	public RegDate getRegDateDebut() {
		return dateDebut;
	}

	/**
	 * @param dateDebut the dateDebut to set
	 */
	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	/**
	 * @return the dateFin
	 */
	public RegDate getRegDateFin() {
		return dateFin;
	}

	/**
	 * @param dateFin the dateFin to set
	 */
	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	/**
	 * @return the dateDebut
	 */
	public Date getDateDebut() {
		return RegDate.asJavaDate(dateDebut);
	}

	/**
	 * @param dateDebut the dateDebut to set
	 */
	public void setDateDebut(Date dateDebut) {
		this.dateDebut = RegDate.get(dateDebut);
	}

	/**
	 * @return the dateFin
	 */
	public Date getDateFin() {
		return RegDate.asJavaDate(dateFin);
	}

	/**
	 * @param dateFin the dateFin to set
	 */
	public void setDateFin(Date dateFin) {
		this.dateFin = RegDate.get(dateFin);
	}

	/**
	 * @return the numeroMaison
	 */
	public String getNumeroMaison() {
		return numeroMaison;
	}

	/**
	 * @param numeroMaison the numeroMaison to set
	 */
	public void setNumeroMaison(String numeroMaison) {
		this.numeroMaison = numeroMaison;
	}

	/**
	 * @return the numeroAppartement
	 */
	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	/**
	 * @param numeroAppartement the numeroAppartement to set
	 */
	public void setNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
	}

	/**
	 * @return the texteCasePostale
	 */
	public TexteCasePostale getTexteCasePostale() {
		return texteCasePostale;
	}

	/**
	 * @param texteCasePostale the texteCasePostale to set
	 */
	public void setTexteCasePostale(TexteCasePostale texteCasePostale) {
		this.texteCasePostale = texteCasePostale;
	}

	/**
	 * @return the numeroCasePostale
	 */
	public Integer getNumeroCasePostale() {
		return numeroCasePostale;
	}

	/**
	 * @param numeroCasePostale the numeroCasePostale to set
	 */
	public void setNumeroCasePostale(Integer numeroCasePostale) {
		this.numeroCasePostale = numeroCasePostale;
	}

	/**
	 * @return the rue
	 */
	public String getRue() {
		return rue;
	}

	/**
	 * @param rue the rue to set
	 */
	public void setRue(String rue) {
		this.rue = rue;
	}

	/**
	 * @return the numeroRue
	 */
	public Integer getNumeroRue() {
		return numeroRue;
	}

	/**
	 * @param numeroRue the numeroRue to set
	 */
	public void setNumeroRue(Integer numeroRue) {
		this.numeroRue = numeroRue;
	}

	/**
	 * @return the numeroOrdrePoste
	 */
	public String getNumeroOrdrePoste() {
		return numeroOrdrePoste;
	}

	/**
	 * @param numeroOrdrePoste the numeroOrdrePoste to set
	 */
	public void setNumeroOrdrePoste(String numeroOrdrePoste) {
		this.numeroOrdrePoste = numeroOrdrePoste;
	}

	/**
	 * @return the numeroPostal
	 */
	public String getNumeroPostal() {
		return numeroPostal;
	}

	/**
	 * @param numeroPostal the numeroPostal to set
	 */
	public void setNumeroPostal(String numeroPostal) {
		this.numeroPostal = numeroPostal;
	}

	/**
	 * @return the localite
	 */
	public String getLocalite() {
		return localite;
	}

	/**
	 * @param localite the localite to set
	 */
	public void setLocalite(String localite) {
		this.localite = localite;
	}

	/**
	 * @return the paysOFS
	 */
	public Integer getPaysOFS() {
		return paysOFS;
	}

	/**
	 * @param paysOFS the paysOFS to set
	 */
	public void setPaysOFS(Integer paysOFS) {
		this.paysOFS = paysOFS;
	}

	public TypeAdresseTiers getUsage() {
		return usage;
	}

	public void setUsage(TypeAdresseTiers usage) {
		this.usage = usage;
	}

	public AdresseGenerique.Source getSource() {
		return source;
	}

	public void setSource(AdresseGenerique.Source source) {
		this.source = source;
	}

	public String getTypeLocalite() {
		return typeLocalite;
	}

	public void setTypeLocalite(String typeLocalite) {
		this.typeLocalite = typeLocalite;
	}


	public String getPays() {
		return pays;
	}

	public void setPays(String pays) {
		this.pays = pays;
	}

	public List<AdresseView> getAdressesList() {
		return adressesList;
	}

	public void setAdressesList(List<AdresseView> adressesList) {
		this.adressesList = adressesList;
	}

	public String getComplements() {
		return complements;
	}

	public void setComplements(String complements) {
		this.complements = complements;
	}

	public boolean isPermanente() {
		return permanente;
	}

	public void setPermanente(boolean permanente) {
		this.permanente = permanente;
	}

	public Long getNumCTB() {
		return numCTB;
	}

	public void setNumCTB(Long numCTB) {
		this.numCTB = numCTB;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getLocaliteSuisse() {
		return localiteSuisse;
	}

	public void setLocaliteSuisse(String localiteSuisse) {
		this.localiteSuisse = localiteSuisse;
	}

	public String getPaysNpa() {
		return paysNpa;
	}

	public void setPaysNpa(String paysNpa) {
		this.paysNpa = paysNpa;
	}

	public String getLocaliteNpa() {
		return localiteNpa;
	}

	public void setLocaliteNpa(String localiteNpa) {
		this.localiteNpa = localiteNpa;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public NatureTiers getNature() {
		return nature;
	}

	public void setNature(NatureTiers nature) {
		this.nature = nature;
	}

	public int compareTo(AdresseView o) {
		int value = -  getDateDebut().compareTo(o.getDateDebut());
		return value;
	}

	/**
	 * @return the numCommune
	 */
	public String getNumCommune() {
		return numCommune;
	}

	/**
	 * @param numCommune the numCommune to set
	 */
	public void setNumCommune(String numCommune) {
		this.numCommune = numCommune;
	}

	public String getComplementLocalite() {
		return complementLocalite;
	}

	public void setComplementLocalite(String complementLocalite) {
		this.complementLocalite = complementLocalite;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public TypeAdresseCivil getUsageCivil() {
		return usageCivil;
	}

	public void setUsageCivil(TypeAdresseCivil usageCivil) {
		this.usageCivil = usageCivil;
	}

	public boolean isSurVaud() {
		return surVaud;
	}

	public void setSurVaud(boolean surVaud) {
		this.surVaud = surVaud;
	}
}
