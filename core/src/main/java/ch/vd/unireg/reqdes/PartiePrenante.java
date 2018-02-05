package ch.vd.unireg.reqdes;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.OriginePersonnePhysique;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.Sexe;

@Entity
@Table(name = "REQDES_PARTIE_PRENANTE")
public class PartiePrenante extends HibernateEntity {

	private Long id;
	private UniteTraitement uniteTraitement;

	private String nom;
	private String nomNaissance;
	private String prenoms;
	private RegDate dateNaissance;
	private Sexe sexe;
	private RegDate dateDeces;
	private boolean sourceCivile;
	private Long numeroContribuable;
	private String avs;
	private OriginePersonnePhysique origine;
	private String nomMere;
	private String prenomsMere;
	private String nomPere;
	private String prenomsPere;
	private EtatCivil etatCivil;
	private RegDate dateEtatCivil;
	private RegDate dateSeparation;
	private Integer ofsPaysNationalite;
	private CategorieEtranger categorieEtranger;

	private String nomConjoint;
	private String prenomConjoint;
	private PartiePrenante conjointPartiePrenante;

	private String texteCasePostale;
	private Integer casePostale;
	private String localite;
	private Integer numeroOrdrePostal;
	private String numeroPostal;
	private Integer numeroPostalComplementaire;
	private Integer ofsPays;
	private String rue;
	private String numeroMaison;
	private String numeroAppartement;
	private String titre;
	private Integer ofsCommune;

	private Set<RolePartiePrenante> roles;

	private Long numeroContribuableCree;

	@Transient
	@Override
	public Object getKey() {
		return getId();
	}

	@Id
	@Column(name = "ID", nullable = false, updatable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "NOM", length = LengthConstants.ADRESSE_NOM, nullable = false)
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "NOM_NAISSANCE", length = LengthConstants.ADRESSE_NOM)
	public String getNomNaissance() {
		return nomNaissance;
	}

	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
	}

	@Column(name = "PRENOMS", length = LengthConstants.TIERS_TOUS_PRENOMS)
	public String getPrenoms() {
		return prenoms;
	}

	public void setPrenoms(String prenoms) {
		this.prenoms = prenoms;
	}

	@Column(name = "DATE_NAISSANCE")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType", parameters = { @Parameter(name = "allowPartial", value = "true") })
	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	@Column(name = "SEXE", length = LengthConstants.TIERS_SEXE)
	@Enumerated(value = EnumType.STRING)
	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	@Column(name = "DATE_DECES")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(RegDate dateDeces) {
		this.dateDeces = dateDeces;
	}

	@Column(name = "SOURCE_CIVILE", nullable = false)
	public boolean isSourceCivile() {
		return sourceCivile;
	}

	public void setSourceCivile(boolean sourceCivile) {
		this.sourceCivile = sourceCivile;
	}

	@Column(name = "NO_CTB")
	public Long getNumeroContribuable() {
		return numeroContribuable;
	}

	public void setNumeroContribuable(Long numeroContribuable) {
		this.numeroContribuable = numeroContribuable;
	}

	@Column(name = "NAVS", length = LengthConstants.TIERS_NUMAVS)
	public String getAvs() {
		return avs;
	}

	public void setAvs(String avs) {
		this.avs = avs;
	}

	@Embedded
	@AttributeOverrides({
	        @AttributeOverride(name = "libelle", column = @Column(name = "LIBELLE_ORIGINE", nullable = true, length = LengthConstants.REQDES_LIBELLE_ORIGINE)),
	        @AttributeOverride(name = "sigleCanton", column = @Column(name = "CANTON_ORIGINE", nullable = true, length = LengthConstants.TIERS_CANTON_ORIGINE))})
	public OriginePersonnePhysique getOrigine() {
		return origine;
	}

	public void setOrigine(OriginePersonnePhysique origine) {
		this.origine = origine;
	}

	@Column(name = "NOM_MERE", length = LengthConstants.TIERS_NOM_PRENOMS_PARENT)
	public String getNomMere() {
		return nomMere;
	}

	public void setNomMere(String nomMere) {
		this.nomMere = nomMere;
	}

	@Column(name = "PRENOMS_MERE", length = LengthConstants.TIERS_NOM_PRENOMS_PARENT)
	public String getPrenomsMere() {
		return prenomsMere;
	}

	public void setPrenomsMere(String prenomsMere) {
		this.prenomsMere = prenomsMere;
	}

	@Column(name = "NOM_PERE", length = LengthConstants.TIERS_NOM_PRENOMS_PARENT)
	public String getNomPere() {
		return nomPere;
	}

	public void setNomPere(String nomPere) {
		this.nomPere = nomPere;
	}

	@Column(name = "PRENOMS_PERE", length = LengthConstants.TIERS_NOM_PRENOMS_PARENT)
	public String getPrenomsPere() {
		return prenomsPere;
	}

	public void setPrenomsPere(String prenomsPere) {
		this.prenomsPere = prenomsPere;
	}

	@Column(name = "ETAT_CIVIL", length = LengthConstants.SITUATIONFAMILLE_ETATCIVIL)
	@Enumerated(value = EnumType.STRING)
	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public void setEtatCivil(EtatCivil etatCivil) {
		this.etatCivil = etatCivil;
	}

	@Column(name = "DATE_ETAT_CIVIL")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateEtatCivil() {
		return dateEtatCivil;
	}

	public void setDateEtatCivil(RegDate dateEtatCivil) {
		this.dateEtatCivil = dateEtatCivil;
	}

	@Column(name = "DATE_SEPARATION")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateSeparation() {
		return dateSeparation;
	}

	public void setDateSeparation(RegDate dateSeparation) {
		this.dateSeparation = dateSeparation;
	}

	@Column(name = "OFS_PAYS_NATIONALITE")
	public Integer getOfsPaysNationalite() {
		return ofsPaysNationalite;
	}

	public void setOfsPaysNationalite(Integer ofsPaysNationalite) {
		this.ofsPaysNationalite = ofsPaysNationalite;
	}

	@Column(name = "CATEGORIE_ETRANGER", length = LengthConstants.TIERS_CATETRANGER)
	@Enumerated(value = EnumType.STRING)
	public CategorieEtranger getCategorieEtranger() {
		return categorieEtranger;
	}

	public void setCategorieEtranger(CategorieEtranger categorieEtranger) {
		this.categorieEtranger = categorieEtranger;
	}

	@Column(name = "TEXTE_CASE_POSTALE", length = LengthConstants.ADRESSE_TYPESUPPLEM)
	public String getTexteCasePostale() {
		return texteCasePostale;
	}

	public void setTexteCasePostale(String texteCasePostale) {
		this.texteCasePostale = texteCasePostale;
	}

	@Column(name = "NUMERO_CASE_POSTALE")
	public Integer getCasePostale() {
		return casePostale;
	}

	public void setCasePostale(Integer casePostale) {
		this.casePostale = casePostale;
	}

	@Column(name = "LOCALITE", length = LengthConstants.ADRESSE_NOM)
	public String getLocalite() {
		return localite;
	}

	public void setLocalite(String localite) {
		this.localite = localite;
	}

	@Column(name = "NO_ORDRE_POSTAL")
	public Integer getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	public void setNumeroOrdrePostal(Integer numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
	}

	@Column(name = "NPA", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroPostal() {
		return numeroPostal;
	}

	public void setNumeroPostal(String numeroPostal) {
		this.numeroPostal = numeroPostal;
	}

	@Column(name = "NPA_CPLT")
	public Integer getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	public void setNumeroPostalComplementaire(Integer numeroPostalComplementaire) {
		this.numeroPostalComplementaire = numeroPostalComplementaire;
	}

	@Column(name = "OFS_PAYS_RESIDENCE")
	public Integer getOfsPays() {
		return ofsPays;
	}

	public void setOfsPays(Integer ofsPays) {
		this.ofsPays = ofsPays;
	}

	@Column(name = "RUE", length = LengthConstants.ADRESSE_NOM)
	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	@Column(name = "NUMERO_MAISON", length = LengthConstants.ADRESSE_NUM_MAISON)
	public String getNumeroMaison() {
		return numeroMaison;
	}

	public void setNumeroMaison(String numeroMaison) {
		this.numeroMaison = numeroMaison;
	}

	@Column(name = "NUMERO_APPARTEMENT", length = LengthConstants.ADRESSE_NUM)
	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public void setNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
	}

	@Column(name = "COMPLEMENT_ADRESSE", length = LengthConstants.ADRESSE_NOM)
	public String getTitre() {
		return titre;
	}

	public void setTitre(String titre) {
		this.titre = titre;
	}

	@Column(name = "OFS_COMMUNE_RESIDENCE")
	public Integer getOfsCommune() {
		return ofsCommune;
	}

	public void setOfsCommune(Integer ofsCommune) {
		this.ofsCommune = ofsCommune;
	}

	@Column(name = "NOM_CONJOINT", length = LengthConstants.ADRESSE_NOM)
	public String getNomConjoint() {
		return nomConjoint;
	}

	public void setNomConjoint(String nomConjoint) {
		this.nomConjoint = nomConjoint;
	}

	@Column(name = "PRENOMS_CONJOINT", length = LengthConstants.ADRESSE_NOM)
	public String getPrenomConjoint() {
		return prenomConjoint;
	}

	public void setPrenomConjoint(String prenomConjoint) {
		this.prenomConjoint = prenomConjoint;
	}

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
	@JoinColumn(name = "CONJOINT_ID")
	@ForeignKey(name = "FK_REQDES_PP_CONJOINT_ID")
	public PartiePrenante getConjointPartiePrenante() {
		return conjointPartiePrenante;
	}

	public void setConjointPartiePrenante(PartiePrenante conjointPartiePrenante) {
		this.conjointPartiePrenante = conjointPartiePrenante;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "PARTIE_PRENANTE_ID", nullable = false)
	@ForeignKey(name = "FK_REQDES_RPP_PP_ID")
	public Set<RolePartiePrenante> getRoles() {
		return roles;
	}

	public void setRoles(Set<RolePartiePrenante> roles) {
		this.roles = roles;
	}

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
	@JoinColumn(name = "UNITE_TRAITEMENT_ID", nullable = false)
	@ForeignKey(name = "FK_REQDES_PP_UT_ID")
	public UniteTraitement getUniteTraitement() {
		return uniteTraitement;
	}

	public void setUniteTraitement(UniteTraitement uniteTraitement) {
		this.uniteTraitement = uniteTraitement;
	}

	@Column(name = "NO_CTB_CREE", nullable = true)
	@ForeignKey(name = "FK_REQDES_PP_CTB_CREE")
	public Long getNumeroContribuableCree() {
		return numeroContribuableCree;
	}

	public void setNumeroContribuableCree(Long numeroContribuableCree) {
		this.numeroContribuableCree = numeroContribuableCree;
	}
}
