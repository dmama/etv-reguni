package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.LongZeroIsNullUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "ENTREPRISE")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "LongZeroIsNull", typeClass = LongZeroIsNullUserType.class)
})
public class RegpmEntreprise extends RegpmEntity implements WithLongId {

	private Long id;
	private String raisonSociale1;
	private String raisonSociale2;
	private String raisonSociale3;
	private String raisonSocialeProv1;      // ?
	private String raisonSocialeProv2;      // ?
	private String raisonSocialeProv3;      // ?
	private String designationAbregee;
	private RegDate dateInscriptionRC;
	private RegDate dateRadiationRC;
	private RegDate dateFinFiscale;         // ?
	private RegDate dateStatuts;
	private Long noAfc;
	private String noIpmro;
	private String noTelephone;
	private String noFax;
	private String noCCP;
	private String noCompteBancaire;
	private String iban;
	private String bicSwift;
	private String nomInstitutionFinanciere;
	private ContactEntreprise contact1;
	private ContactEntreprise contact2;
	private RegDate dateConstitution;
	private RegDate dateDissolution;
	private String natureDroit;
	private String typeFondation;
	private RegDate dateDebutSocietePersonnes;
	private RegDate dateFinSocietePersonnes;
	private String enseigne;
	private RegDate dateRequisitionRadiation;
	private RegDate dateAutRadiation;       // ?
	private RegDate dateBouclementFutur;
	private NumeroIDE numeroIDE;
	private Long numeroCantonal;
	private SortedSet<RaisonSociale> raisonsSociales;
	private SortedSet<InscriptionRC> inscriptionsRC;
	private SortedSet<RadiationRC> radiationsRC;
	private SortedSet<RegpmFormeJuridique> formesJuridiques;
	private Set<RegpmEtablissement> etablissements;
	private SortedSet<RegpmSiegeEntreprise> sieges;
	private SortedSet<RegpmRegimeFiscalVD> regimesFiscauxVD;
	private SortedSet<RegpmRegimeFiscalCH> regimesFiscauxCH;
	private SortedSet<RegpmDossierFiscal> dossiersFiscaux;
	private SortedSet<RegpmExerciceCommercial> exercicesCommerciaux;
	private Set<RegpmAdresseEntreprise> adresses;
	private Set<RegpmAssocieSC> associesSC;
	private SortedSet<RegpmAssujettissement> assujettissements;
	private SortedSet<RegpmForPrincipal> forsPrincipaux;
	private Set<RegpmForSecondaire> forsSecondaires;
	private Set<RegpmAllegementFiscal> allegementsFiscaux;
	private Set<RegpmFusion> fusionsAvant;
	private Set<RegpmFusion> fusionsApres;
	private SortedSet<RegpmEtatEntreprise> etatsEntreprise;
	private Set<RegpmMandat> mandataires;
	private SortedSet<RegpmQuestionnaireSNC> questionnairesSNC;
	private SortedSet<RegpmCapital> capitaux;

	@Id
	@Column(name = "NO_ENTREPRISE")
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

	@Column(name = "RAIS_SOC_PROV_LGN1")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getRaisonSocialeProv1() {
		return raisonSocialeProv1;
	}

	public void setRaisonSocialeProv1(String raisonSocialeProv1) {
		this.raisonSocialeProv1 = raisonSocialeProv1;
	}

	@Column(name = "RAIS_SOC_PROV_LGN2")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getRaisonSocialeProv2() {
		return raisonSocialeProv2;
	}

	public void setRaisonSocialeProv2(String raisonSocialeProv2) {
		this.raisonSocialeProv2 = raisonSocialeProv2;
	}

	@Column(name = "RAIS_SOC_PROV_LGN3")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getRaisonSocialeProv3() {
		return raisonSocialeProv3;
	}

	public void setRaisonSocialeProv3(String raisonSocialeProv3) {
		this.raisonSocialeProv3 = raisonSocialeProv3;
	}

	@Column(name = "DESIGN_ABREGEE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getDesignationAbregee() {
		return designationAbregee;
	}

	public void setDesignationAbregee(String designationAbregee) {
		this.designationAbregee = designationAbregee;
	}

	@Column(name = "DA_INSC_RC_VD")
	@Type(type = "RegDate")
	public RegDate getDateInscriptionRC() {
		return dateInscriptionRC;
	}

	public void setDateInscriptionRC(RegDate dateInscriptionRC) {
		this.dateInscriptionRC = dateInscriptionRC;
	}

	@Column(name = "DA_RAD_RC_VD")
	@Type(type = "RegDate")
	public RegDate getDateRadiationRC() {
		return dateRadiationRC;
	}

	public void setDateRadiationRC(RegDate dateRadiationRC) {
		this.dateRadiationRC = dateRadiationRC;
	}

	@Column(name = "DAF_FISCALE")
	@Type(type = "RegDate")
	public RegDate getDateFinFiscale() {
		return dateFinFiscale;
	}

	public void setDateFinFiscale(RegDate dateFinFiscale) {
		this.dateFinFiscale = dateFinFiscale;
	}

	@Column(name = "DA_STATUTS")
	@Type(type = "RegDate")
	public RegDate getDateStatuts() {
		return dateStatuts;
	}

	public void setDateStatuts(RegDate dateStatuts) {
		this.dateStatuts = dateStatuts;
	}

	@Column(name = "NO_AFC")
	@Type(type = "LongZeroIsNull")
	public Long getNoAfc() {
		return noAfc;
	}

	public void setNoAfc(Long noAfc) {
		this.noAfc = noAfc;
	}

	@Column(name = "NO_IPMRO")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "5"))
	public String getNoIpmro() {
		return noIpmro;
	}

	public void setNoIpmro(String noIpmro) {
		this.noIpmro = noIpmro;
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

	@Column(name = "NO_CCP")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "15"))
	public String getNoCCP() {
		return noCCP;
	}

	public void setNoCCP(String noCCP) {
		this.noCCP = noCCP;
	}

	@Column(name = "NO_COMPTE_BANCAIRE")
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
	@AttributeOverrides({
			@AttributeOverride(name = "nom", column = @Column(name = "NOM_CONTACT_1")),
			@AttributeOverride(name = "prenom", column = @Column(name = "PRENOM_CONTACT_1")),
			@AttributeOverride(name = "noTelephone", column = @Column(name = "NO_TEL_CONTACT_1")),
			@AttributeOverride(name = "noFax", column = @Column(name = "NO_FAX_CONTACT_1"))
	})
	public ContactEntreprise getContact1() {
		return contact1;
	}

	public void setContact1(ContactEntreprise contact1) {
		this.contact1 = contact1;
	}

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "nom", column = @Column(name = "NOM_CONTACT_2")),
			@AttributeOverride(name = "prenom", column = @Column(name = "PRENOM_CONTACT_2")),
			@AttributeOverride(name = "noTelephone", column = @Column(name = "NO_TEL_CONTACT_2")),
			@AttributeOverride(name = "noFax", column = @Column(name = "NO_FAX_CONTACT_2"))
	})
	public ContactEntreprise getContact2() {
		return contact2;
	}

	public void setContact2(ContactEntreprise contact2) {
		this.contact2 = contact2;
	}

	@Column(name = "DA_CONSTITUTION")
	@Type(type = "RegDate")
	public RegDate getDateConstitution() {
		return dateConstitution;
	}

	public void setDateConstitution(RegDate dateConstitution) {
		this.dateConstitution = dateConstitution;
	}

	@Column(name = "DA_DISSOLUTION")
	@Type(type = "RegDate")
	public RegDate getDateDissolution() {
		return dateDissolution;
	}

	public void setDateDissolution(RegDate dateDissolution) {
		this.dateDissolution = dateDissolution;
	}

	@Column(name = "NATURE_DROIT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getNatureDroit() {
		return natureDroit;
	}

	public void setNatureDroit(String natureDroit) {
		this.natureDroit = natureDroit;
	}

	@Column(name = "TY_FONDATION")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "15"))
	public String getTypeFondation() {
		return typeFondation;
	}

	public void setTypeFondation(String typeFondation) {
		this.typeFondation = typeFondation;
	}

	@Column(name = "DAD_STE_PERSONNES")
	@Type(type = "RegDate")
	public RegDate getDateDebutSocietePersonnes() {
		return dateDebutSocietePersonnes;
	}

	public void setDateDebutSocietePersonnes(RegDate dateDebutSocietePersonnes) {
		this.dateDebutSocietePersonnes = dateDebutSocietePersonnes;
	}

	@Column(name = "DAF_STE_PERSONNES")
	@Type(type = "RegDate")
	public RegDate getDateFinSocietePersonnes() {
		return dateFinSocietePersonnes;
	}

	public void setDateFinSocietePersonnes(RegDate dateFinSocietePersonnes) {
		this.dateFinSocietePersonnes = dateFinSocietePersonnes;
	}

	@Column(name = "ENSEIGNE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getEnseigne() {
		return enseigne;
	}

	public void setEnseigne(String enseigne) {
		this.enseigne = enseigne;
	}

	@Column(name = "DA_REQ_RADIATION")
	@Type(type = "RegDate")
	public RegDate getDateRequisitionRadiation() {
		return dateRequisitionRadiation;
	}

	public void setDateRequisitionRadiation(RegDate dateRequisitionRadiation) {
		this.dateRequisitionRadiation = dateRequisitionRadiation;
	}

	@Column(name = "DA_AUT_RADIATION")
	@Type(type = "RegDate")
	public RegDate getDateAutRadiation() {
		return dateAutRadiation;
	}

	public void setDateAutRadiation(RegDate dateAutRadiation) {
		this.dateAutRadiation = dateAutRadiation;
	}

	@Column(name = "DA_BOUCL_FUTUR")
	@Type(type = "RegDate")
	public RegDate getDateBouclementFutur() {
		return dateBouclementFutur;
	}

	public void setDateBouclementFutur(RegDate dateBouclementFutur) {
		this.dateBouclementFutur = dateBouclementFutur;
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

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RaisonSociale> getRaisonsSociales() {
		return raisonsSociales;
	}

	public void setRaisonsSociales(SortedSet<RaisonSociale> raisonsSociales) {
		this.raisonsSociales = raisonsSociales;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<InscriptionRC> getInscriptionsRC() {
		return inscriptionsRC;
	}

	public void setInscriptionsRC(SortedSet<InscriptionRC> inscriptionsRC) {
		this.inscriptionsRC = inscriptionsRC;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RadiationRC> getRadiationsRC() {
		return radiationsRC;
	}

	public void setRadiationsRC(SortedSet<RadiationRC> radiationsRC) {
		this.radiationsRC = radiationsRC;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmFormeJuridique> getFormesJuridiques() {
		return formesJuridiques;
	}

	public void setFormesJuridiques(SortedSet<RegpmFormeJuridique> formesJuridiques) {
		this.formesJuridiques = formesJuridiques;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	public Set<RegpmEtablissement> getEtablissements() {
		return etablissements;
	}

	public void setEtablissements(Set<RegpmEtablissement> etablissements) {
		this.etablissements = etablissements;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmSiegeEntreprise> getSieges() {
		return sieges;
	}

	public void setSieges(SortedSet<RegpmSiegeEntreprise> sieges) {
		this.sieges = sieges;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmRegimeFiscalVD> getRegimesFiscauxVD() {
		return regimesFiscauxVD;
	}

	public void setRegimesFiscauxVD(SortedSet<RegpmRegimeFiscalVD> regimesFiscauxVD) {
		this.regimesFiscauxVD = regimesFiscauxVD;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmRegimeFiscalCH> getRegimesFiscauxCH() {
		return regimesFiscauxCH;
	}

	public void setRegimesFiscauxCH(SortedSet<RegpmRegimeFiscalCH> regimesFiscauxCH) {
		this.regimesFiscauxCH = regimesFiscauxCH;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmDossierFiscal> getDossiersFiscaux() {
		return dossiersFiscaux;
	}

	public void setDossiersFiscaux(SortedSet<RegpmDossierFiscal> dossiersFiscaux) {
		this.dossiersFiscaux = dossiersFiscaux;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmExerciceCommercial> getExercicesCommerciaux() {
		return exercicesCommerciaux;
	}

	public void setExercicesCommerciaux(SortedSet<RegpmExerciceCommercial> exercicesCommerciaux) {
		this.exercicesCommerciaux = exercicesCommerciaux;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	public Set<RegpmAdresseEntreprise> getAdresses() {
		return adresses;
	}

	public void setAdresses(Set<RegpmAdresseEntreprise> adresses) {
		this.adresses = adresses;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_A_ENTPRNO")
	public Set<RegpmAssocieSC> getAssociesSC() {
		return associesSC;
	}

	public void setAssociesSC(Set<RegpmAssocieSC> associesSC) {
		this.associesSC = associesSC;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmAssujettissement> getAssujettissements() {
		return assujettissements;
	}

	public void setAssujettissements(SortedSet<RegpmAssujettissement> assujettissements) {
		this.assujettissements = assujettissements;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmForPrincipal> getForsPrincipaux() {
		return forsPrincipaux;
	}

	public void setForsPrincipaux(SortedSet<RegpmForPrincipal> forsPrincipaux) {
		this.forsPrincipaux = forsPrincipaux;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	public Set<RegpmForSecondaire> getForsSecondaires() {
		return forsSecondaires;
	}

	public void setForsSecondaires(Set<RegpmForSecondaire> forsSecondaires) {
		this.forsSecondaires = forsSecondaires;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTNO")
	public Set<RegpmAllegementFiscal> getAllegementsFiscaux() {
		return allegementsFiscaux;
	}

	public void setAllegementsFiscaux(Set<RegpmAllegementFiscal> allegementsFiscaux) {
		this.allegementsFiscaux = allegementsFiscaux;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_D_ENTPRNO")
	public Set<RegpmFusion> getFusionsAvant() {
		return fusionsAvant;
	}

	public void setFusionsAvant(Set<RegpmFusion> fusionsAvant) {
		this.fusionsAvant = fusionsAvant;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_P_ENTPRNO")
	public Set<RegpmFusion> getFusionsApres() {
		return fusionsApres;
	}

	public void setFusionsApres(Set<RegpmFusion> fusionsApres) {
		this.fusionsApres = fusionsApres;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmEtatEntreprise> getEtatsEntreprise() {
		return etatsEntreprise;
	}

	public void setEtatsEntreprise(SortedSet<RegpmEtatEntreprise> etatsEntreprise) {
		this.etatsEntreprise = etatsEntreprise;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_P_ENTPRNO")
	public Set<RegpmMandat> getMandataires() {
		return mandataires;
	}

	public void setMandataires(Set<RegpmMandat> mandataires) {
		this.mandataires = mandataires;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTRNO_ENTR")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmQuestionnaireSNC> getQuestionnairesSNC() {
		return questionnairesSNC;
	}

	public void setQuestionnairesSNC(SortedSet<RegpmQuestionnaireSNC> questionnairesSNC) {
		this.questionnairesSNC = questionnairesSNC;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmCapital> getCapitaux() {
		return capitaux;
	}

	public void setCapitaux(SortedSet<RegpmCapital> capitaux) {
		this.capitaux = capitaux;
	}
}
