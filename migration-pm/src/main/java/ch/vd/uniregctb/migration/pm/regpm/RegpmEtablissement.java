package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;
import java.util.SortedSet;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.LongZeroIsNullUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "ETABLISSEMENT")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "LongZeroIsNull", typeClass = LongZeroIsNullUserType.class),
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
})
public class RegpmEtablissement extends RegpmEntity implements WithLongId {

	private Long id;
	private String raisonSociale1;
	private String raisonSociale2;
	private String raisonSociale3;
	private RegDate dateInscriptionRC;
	private RegDate dateRadiationRC;
	private String enseigne;
	private RegDate dateInformation;
	private String noTelephone;
	private String noFax;
	private String chez;
	private String nomRue;
	private String noPolice;
	private String noCCP;
	private String noCompteBancaire;
	private String iban;
	private String bicSwift;
	private String nomInstitutionFinanciere;
	private NumeroIDE numeroIDE;
	private Long numeroCantonal;
	private RegpmEntreprise entreprise;
	private RegpmLocalitePostale localitePostale;
	private RegpmRue rue;
	private Set<RegpmEtablissementStable> etablissementsStables;
	private Set<RegpmEtablissement> succursales;
	private SortedSet<RegpmDomicileEtablissement> domicilesEtablissements;

	// no individu, no institution ?, ...

	@Id
	@Column(name = "NO_ETABLISSEMENT")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "RAISON_SOC_LGN1")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getRaisonSociale1() {
		return raisonSociale1;
	}

	public void setRaisonSociale1(String raisonSociale1) {
		this.raisonSociale1 = raisonSociale1;
	}

	@Column(name = "RAISON_SOC_LGN2")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getRaisonSociale2() {
		return raisonSociale2;
	}

	public void setRaisonSociale2(String raisonSociale2) {
		this.raisonSociale2 = raisonSociale2;
	}

	@Column(name = "RAISON_SOC_LGN3")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getRaisonSociale3() {
		return raisonSociale3;
	}

	public void setRaisonSociale3(String raisonSociale3) {
		this.raisonSociale3 = raisonSociale3;
	}

	@Column(name = "DA_INSC_RC_VD")
	@Type(type = "RegDate")
	public RegDate getDateInscriptionRC() {
		return dateInscriptionRC;
	}

	public void setDateInscriptionRC(RegDate dateInscriptionRC) {
		this.dateInscriptionRC = dateInscriptionRC;
	}

	@Column(name = "DA_RADIATION_RC_VD")
	@Type(type = "RegDate")
	public RegDate getDateRadiationRC() {
		return dateRadiationRC;
	}

	public void setDateRadiationRC(RegDate dateRadiationRC) {
		this.dateRadiationRC = dateRadiationRC;
	}

	@Column(name = "ENSEIGNE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getEnseigne() {
		return enseigne;
	}

	public void setEnseigne(String enseigne) {
		this.enseigne = enseigne;
	}

	@Column(name = "DA_INFORMATION")
	@Type(type = "RegDate")
	public RegDate getDateInformation() {
		return dateInformation;
	}

	public void setDateInformation(RegDate dateInformation) {
		this.dateInformation = dateInformation;
	}

	@Column(name = "NO_TELEPHONE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "16"))
	public String getNoTelephone() {
		return noTelephone;
	}

	public void setNoTelephone(String noTelephone) {
		this.noTelephone = noTelephone;
	}

	@Column(name = "NO_FAX")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "16"))
	public String getNoFax() {
		return noFax;
	}

	public void setNoFax(String noFax) {
		this.noFax = noFax;
	}

	@Column(name = "CHEZ")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getChez() {
		return chez;
	}

	public void setChez(String chez) {
		this.chez = chez;
	}

	@Column(name = "RUE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNomRue() {
		return nomRue;
	}

	public void setNomRue(String nomRue) {
		this.nomRue = nomRue;
	}

	@Column(name = "NO_POLICE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getNoPolice() {
		return noPolice;
	}

	public void setNoPolice(String noPolice) {
		this.noPolice = noPolice;
	}

	@Column(name = "NO_CCP")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "15"))
	public String getNoCCP() {
		return noCCP;
	}

	public void setNoCCP(String noCCP) {
		this.noCCP = noCCP;
	}

	@Column(name = "NO_CPTE_BANCAIRE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "20"))
	public String getNoCompteBancaire() {
		return noCompteBancaire;
	}

	public void setNoCompteBancaire(String noCompteBancaire) {
		this.noCompteBancaire = noCompteBancaire;
	}

	@Column(name = "IBAN")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	@Column(name = "BIC_SWIFT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "12"))
	public String getBicSwift() {
		return bicSwift;
	}

	public void setBicSwift(String bicSwift) {
		this.bicSwift = bicSwift;
	}

	@Column(name = "NOM_INSTIT_FIN")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getNomInstitutionFinanciere() {
		return nomInstitutionFinanciere;
	}

	public void setNomInstitutionFinanciere(String nomInstitutionFinanciere) {
		this.nomInstitutionFinanciere = nomInstitutionFinanciere;
	}

	@Embedded
	public NumeroIDE getNumeroIDE() {
		return numeroIDE;
	}

	public void setNumeroIDE(NumeroIDE numeroIDE) {
		this.numeroIDE = numeroIDE;
	}

	@Column(name = "NO_CANTONAL")
	@Type(type = "LongZeroIsNull")
	public Long getNumeroCantonal() {
		return numeroCantonal;
	}

	public void setNumeroCantonal(Long numeroCantonal) {
		this.numeroCantonal = numeroCantonal;
	}

	@ManyToOne
	@JoinColumn(name = "FK_ENTPRNO")
	public RegpmEntreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(RegpmEntreprise entreprise) {
		this.entreprise = entreprise;
	}

	@ManyToOne
	@JoinColumn(name = "FK_LOC_POSTNO")
	public RegpmLocalitePostale getLocalitePostale() {
		return localitePostale;
	}

	public void setLocalitePostale(RegpmLocalitePostale localitePostale) {
		this.localitePostale = localitePostale;
	}

	@ManyToOne
	@JoinColumn(name = "FK_RUENO")
	public RegpmRue getRue() {
		return rue;
	}

	public void setRue(RegpmRue rue) {
		this.rue = rue;
	}

	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumn(name = "FK_ETABNO")
	public Set<RegpmEtablissementStable> getEtablissementsStables() {
		return etablissementsStables;
	}

	public void setEtablissementsStables(Set<RegpmEtablissementStable> etablissementsStables) {
		this.etablissementsStables = etablissementsStables;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ETABLISSEMENNO")
	public Set<RegpmEtablissement> getSuccursales() {
		return succursales;
	}

	public void setSuccursales(Set<RegpmEtablissement> succursales) {
		this.succursales = succursales;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ETABNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmDomicileEtablissement> getDomicilesEtablissements() {
		return domicilesEtablissements;
	}

	public void setDomicilesEtablissements(SortedSet<RegpmDomicileEtablissement> domicilesEtablissements) {
		this.domicilesEtablissements = domicilesEtablissements;
	}
}
