package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.AssociationOverride;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import ch.vd.uniregctb.migration.pm.regpm.usertype.NatureDroitUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeFondationUserType;

@Entity
@Table(name = "ENTREPRISE")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "NatureDroit", typeClass = NatureDroitUserType.class),
		@TypeDef(name = "TypeFondation", typeClass = TypeFondationUserType.class),
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class),
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
	private RegpmCoordonneesFinancieres coordonneesFinancieres;
	private ContactEntreprise contact1;
	private ContactEntreprise contact2;
	private RegDate dateConstitution;
	private RegDate dateDissolution;
	private RegpmNatureDroit natureDroit;
	private RegpmTypeFondation typeFondation;
	private String loiReglement;
	private RegDate dateDebutSocietePersonnes;
	private RegDate dateFinSocietePersonnes;
	private String enseigne;
	private RegDate dateRequisitionRadiation;
	private RegDate dateAutRadiation;       // Date à laquelle l'ACI signale au Registre du Commerce qu'elle ne formule pas d'opposition à ce qu'il radie une entreprise de son registre.
	private boolean etablissementHS;        // indique qu'il y aura une répartition internationale et que le dossier devra être traité manuellement (taxation)
	private RegDate dateBouclementFutur;
	private RegDate dateDebutLivrRecette;   // Date à partir de laquelle tout document émis en différé doit transiter par la Recette (Perception).
	private RegDate dateFinLivrRecette;     // Date à partir de laquelle tout document émis en différé ne doit plus transiter par la Recette (Perception).
	private String commentaireLivrRecette;  // Permet de commenter la raison pour laquelle tout document émis en différé doit transiter par la Recette (Perception).
	private boolean etablissementHC;        // indique qu'il y aura une répartition intercantonale et que le dossier devra être traité manuellement (taxation)
	private String numeroREE;
	private NumeroIDE numeroIDE;
	private Long numeroCantonal;
	private RegpmCommune commune;
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
	private Set<RegpmMandat> mandants;
	private SortedSet<RegpmQuestionnaireSNC> questionnairesSNC;
	private SortedSet<RegpmCapital> capitaux;
	private Set<RegpmRattachementProprietaire> rattachementsProprietaires;
	private Set<RegpmAppartenanceGroupeProprietaire> appartenancesGroupeProprietaire;
	private SortedSet<RegpmBlocNotesEntreprise> notes;
	private Set<RegpmCritereSegmentation> criteresSegmentation;

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

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "noCCP", column = @Column(name = "NO_CCP")),
			@AttributeOverride(name = "noCompteBancaire", column = @Column(name = "NO_COMPTE_BANCAIRE")),
			@AttributeOverride(name = "iban", column = @Column(name = "IBAN")),
			@AttributeOverride(name = "bicSwift", column = @Column(name = "BIC_SWIFT")),
			@AttributeOverride(name = "nomInstitutionFinanciere", column = @Column(name = "NOM_INSTIT_FIN"))
	})
	@AssociationOverride(name = "institutionFinanciere", joinColumns = @JoinColumn(name = "FK_INSTIT_FINNO"))
	public RegpmCoordonneesFinancieres getCoordonneesFinancieres() {
		return coordonneesFinancieres;
	}

	public void setCoordonneesFinancieres(RegpmCoordonneesFinancieres coordonneesFinancieres) {
		this.coordonneesFinancieres = coordonneesFinancieres;
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
	@Type(type = "NatureDroit")
	public RegpmNatureDroit getNatureDroit() {
		return natureDroit;
	}

	public void setNatureDroit(RegpmNatureDroit natureDroit) {
		this.natureDroit = natureDroit;
	}

	@Column(name = "TY_FONDATION")
	@Type(type = "TypeFondation")
	public RegpmTypeFondation getTypeFondation() {
		return typeFondation;
	}

	public void setTypeFondation(RegpmTypeFondation typeFondation) {
		this.typeFondation = typeFondation;
	}

	@Column(name = "LOI_REGLEMENT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getLoiReglement() {
		return loiReglement;
	}

	public void setLoiReglement(String loiReglement) {
		this.loiReglement = loiReglement;
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

	@Column(name = "ETABLISSEMENT_HS")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isEtablissementHS() {
		return etablissementHS;
	}

	public void setEtablissementHS(boolean etablissementHS) {
		this.etablissementHS = etablissementHS;
	}

	@Column(name = "DA_BOUCL_FUTUR")
	@Type(type = "RegDate")
	public RegDate getDateBouclementFutur() {
		return dateBouclementFutur;
	}

	public void setDateBouclementFutur(RegDate dateBouclementFutur) {
		this.dateBouclementFutur = dateBouclementFutur;
	}

	@Column(name = "DAD_LIVR_RECETTE")
	@Type(type = "RegDate")
	public RegDate getDateDebutLivrRecette() {
		return dateDebutLivrRecette;
	}

	public void setDateDebutLivrRecette(RegDate dateDebutLivrRecette) {
		this.dateDebutLivrRecette = dateDebutLivrRecette;
	}

	@Column(name = "DAF_LIVR_RECETTE")
	@Type(type = "RegDate")
	public RegDate getDateFinLivrRecette() {
		return dateFinLivrRecette;
	}

	public void setDateFinLivrRecette(RegDate dateFinLivrRecette) {
		this.dateFinLivrRecette = dateFinLivrRecette;
	}

	@Column(name = "COMMENT_LIVR_RECET")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getCommentaireLivrRecette() {
		return commentaireLivrRecette;
	}

	public void setCommentaireLivrRecette(String commentaireLivrRecette) {
		this.commentaireLivrRecette = commentaireLivrRecette;
	}

	@Column(name = "ETABLISSEMENT_HC")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isEtablissementHC() {
		return etablissementHC;
	}

	public void setEtablissementHC(boolean etablissementHC) {
		this.etablissementHC = etablissementHC;
	}

	@Column(name = "NO_REE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "8"))
	public String getNumeroREE() {
		return numeroREE;
	}

	public void setNumeroREE(String numeroREE) {
		this.numeroREE = numeroREE;
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
	@JoinColumn(name = "FK_COMMUNENO")
	public RegpmCommune getCommune() {
		return commune;
	}

	public void setCommune(RegpmCommune commune) {
		this.commune = commune;
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

	/**
	 * Il n'y a qu'une seule adresse par entreprise et par type dans RegPM
	 * @return une map indexée par le type de l'adresse en regard
	 */
	@Transient
	public Map<RegpmTypeAdresseEntreprise, RegpmAdresseEntreprise> getAdressesTypees() {
		return adresses.stream()
				.collect(Collectors.toMap(RegpmAdresseEntreprise::getTypeAdresse,
				                          Function.identity(),
				                          (u,v) -> { throw new IllegalArgumentException("Plusieurs adresses pour un même type (" + u.getTypeAdresse() + ")"); },
				                          () -> new EnumMap<>(RegpmTypeAdresseEntreprise.class)));
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
	@JoinColumn(name = "FK_A_ENTPRNO")
	public Set<RegpmMandat> getMandants() {
		return mandants;
	}

	public void setMandants(Set<RegpmMandat> mandants) {
		this.mandants = mandants;
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

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	public Set<RegpmRattachementProprietaire> getRattachementsProprietaires() {
		return rattachementsProprietaires;
	}

	public void setRattachementsProprietaires(Set<RegpmRattachementProprietaire> rattachementsProprietaires) {
		this.rattachementsProprietaires = rattachementsProprietaires;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTPRNO")
	public Set<RegpmAppartenanceGroupeProprietaire> getAppartenancesGroupeProprietaire() {
		return appartenancesGroupeProprietaire;
	}

	public void setAppartenancesGroupeProprietaire(Set<RegpmAppartenanceGroupeProprietaire> appartenancesGroupeProprietaire) {
		this.appartenancesGroupeProprietaire = appartenancesGroupeProprietaire;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTREPRISENO_EN")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmBlocNotesEntreprise> getNotes() {
		return notes;
	}

	public void setNotes(SortedSet<RegpmBlocNotesEntreprise> notes) {
		this.notes = notes;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ENTREPRISE_NO")
	public Set<RegpmCritereSegmentation> getCriteresSegmentation() {
		return criteresSegmentation;
	}

	public void setCriteresSegmentation(Set<RegpmCritereSegmentation> criteresSegmentation) {
		this.criteresSegmentation = criteresSegmentation;
	}
}
