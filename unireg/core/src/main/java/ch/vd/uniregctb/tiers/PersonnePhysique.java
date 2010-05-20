package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.*;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc --> Être humain sous l'angle du droit, individualisée par ses caractéristiques, telles que son
 * nom et prénom, sa date de naissance, son sexe, son numéro AVS?
 *
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8pFx9Edygsbnw9h5bVw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8pFx9Edygsbnw9h5bVw"
 */
@Entity
@DiscriminatorValue("PersonnePhysique")
public class PersonnePhysique extends Contribuable {

	private static final long serialVersionUID = 3641798749348427983L;

	public PersonnePhysique(){
		super();
	}
	public PersonnePhysique(Boolean habitant) {
		this.habitant = habitant;
	}
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc --> Numéro du contribuable imposé à la source dans l'ancienne application SIMPA-IS.
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8mVx9Edygsbnw9h5bVw"
	 */
	private Long ancienNumeroSourcier;

	private Set<DroitAcces> droitsAccesAppliques;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Date de décès de la personne (si habitant surcharge de la date de décès du civil)
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_YWcUYJN-Edy7DqR-SPIh9g"
	 */
	private RegDate dateDeces;

	/**
	 * true si habitant, false si non habitant
	 */
	private Boolean habitant;

	//attribut d'un habitant (ie personne physique connu du registre civil habitant dans le canton)
	/**
	 * Référence unique de la personne physique dans le registre des individus.
	 * Cette référence est renseignée si le tiers est une personne physique connue du contrôle des habitants.
	 */
	private Long numeroIndividu;

	/**
	 * Cache sur l'objet individu. Ce cache est géré par le service tiers, qui se trouve dans business, et c'est pourquoi on utilise le type
	 * Object.
	 */
	private Object individuCache;

	//attributs d'un non habitant (ie personne physique non résident dans le canton)
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Nouveau numéro d?assuré AVS au sens de l?art. 50c LAVS.
	 * Le numéro d?assuré AVS est numérique (13 positions) et non signifiant.
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8PFx9Edygsbnw9h5bVw"
	 */
	private String numeroAssureSocial;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Plx9Edygsbnw9h5bVw"
	 */
	private String nom;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8P1x9Edygsbnw9h5bVw"
	 */
	private String prenom;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Date à laquelle la personne est née. Il arrive que seule l?année, voire l?année et le mois soient connus.
	 * Format yyyymmdd, yyyymm ou yyyy
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8QFx9Edygsbnw9h5bVw"
	 */
	private RegDate dateNaissance;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Qlx9Edygsbnw9h5bVw"
	 */
	private Sexe sexe;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Code ISO-2 du pays selon la norme ISO-3166
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Q1x9Edygsbnw9h5bVw"
	 */
	private Integer numeroOfsNationalite;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Code ISO-2 du pays selon la norme ISO-3166
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Q1x9Edygsbnw9h5bVw"
	 */
	private Integer numeroOfsCommuneOrigine;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Genre de permis réglementant le séjour d'une personne étrangère en Suisse.
	 * Voir eCH-0006 pour les valeurs possibles
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi0jVx9Edygsbnw9h5bVw"
	 */
	private CategorieEtranger categorieEtranger;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Date à partir de laquelle l'autorisation pour étrangers est valable
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_RfBlkFyUEdyz_5BS6IxMlQ"
	 */
	private RegDate dateDebutValiditeAutorisation;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_4Z2GsO9YEdyEV8rfFv3rEg"
	 */
	private Set<IdentificationPersonne> identificationsPersonnes;

	/**
	 * [UNIREG-1114] Ce booléean est utilisé par le batch d'ouverture des contribuables majeurs (mesures d'optimisation du temps d'exécution).
	 * <p>
	 * Il peut prendre les valeurs suivantes:
	 * <ul>
	 * <li><b>null/false</b>: le batch n'est jamais passé sur ce contribuable, ou le batch est passé dessus mais il était mineur</li>
	 * <li><b>true</b>: le batch est passé sur ce contribuable, sa majorité a été traitée et toutes les actions nécessaires ont été
	 * entreprises</li>
	 * </ul>
	 */
	private Boolean majoriteTraitee;

	@Transient
	@Override
	public String getNatureTiers() {
		if (habitant)
			return NATURE_HABITANT;
		else
			return NATURE_NONHABITANT;
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		if (habitant)
			return "Contribuable PP";
		//si non habitant
		if (getForsFiscauxNonAnnules(false).size() > 0) {
			return "Contribuable PP";
		}
		return "Autre tiers";
	}

	private static final Pattern NOM_PRENOM_PATTERN = Pattern.compile("[']?[A-Za-zÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž]['A-Za-zÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž. -]*");

	@Override
	public ValidationResults validate() {
		final ValidationResults results = super.validate();
		if (habitant == null) {
			results.addError("La personne physique doit être habitant ou non habitant");
		}
		else if (habitant && (numeroIndividu == null || numeroIndividu <= 0L)) {
			results.addError("Le numero d'individu du registre civil est un attribut obligatoire pour un habitant");
		}
		else if (!habitant) {

			// nom : obligatoire
			if (StringUtils.isBlank(nom)) {
				results.addError("Le nom est un attribut obligatoire pour un non-habitant");
			}
			else if (!NOM_PRENOM_PATTERN.matcher(nom).matches()) {
				results.addError("Le nom du non-habitant contient au moins un caractère invalide");
			}

			// prénom : facultatif, mais pas avec n'importe quoi
			if (StringUtils.isBlank(prenom)) {
				prenom = null;
			}
			else if (!NOM_PRENOM_PATTERN.matcher(prenom).matches()) {
				results.addError("Le prénom du non-habitant contient au moins un caractère invalide");
			}
		}

		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ValidationResults validateFors() {

		ValidationResults results = super.validateFors();

		/*
		 * On n'autorise pas la présence de fors durant la ou les périodes d'appartenance à un couple
		 */
		// Détermine les périodes de validités ininterrompues du ménage commun
		List<RapportEntreTiers> rapportsMenages = new ArrayList<RapportEntreTiers>();
		Set<RapportEntreTiers> rapports = getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers r : rapports) {
				if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(r.getType())) {
					rapportsMenages.add(r);
				}
			}
		}
		Collections.sort(rapportsMenages, new DateRangeComparator<RapportEntreTiers>());
		final List<DateRange> periodes = DateRangeHelper.collateRange(rapportsMenages);

		// Vérifie que chaque for est entièrement compris à l'extérieur d'une période de validité
		final Set<ForFiscal> fors = getForsFiscaux();
		if (fors != null) {
			for (ForFiscal f : fors) {
				if (f.isAnnule()) {
					continue;
				}
				if (DateRangeHelper.intersect(f, periodes)) {
					results.addError("Le for fiscal [" + f + "] ne peut pas exister alors que le tiers [" + getNumero()
							+ "] appartient à un ménage-commun");
				}
			}
		}

		return results;
	}

	@Override
	protected ValidationResults validateTypeAdresses() {

		ValidationResults results = new ValidationResults();

		final Set<AdresseTiers> adresses = getAdressesTiers();
		if (adresses != null) {
			for (AdresseTiers a : adresses) {
				if (a.isAnnule()) {
					continue;
				}
				if (a instanceof AdressePM) {
					results.addError("L'adresse de type 'personne morale' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur une personne physique.");
				}
				else if (!habitant && a instanceof AdresseCivile && a.getDateFin() == null) {
					results.addError("L'adresse de type 'personne civile' (numéro=" + a.getId() + ", début=" + a.getDateDebut() + ", fin="
							+ a.getDateFin() + ") n'est pas autorisée sur un non-habitant.");
				}
			}
		}

		return results;
	}

	@Override
	protected void dumpForDebug(int nbTabs) {
		super.dumpForDebug(nbTabs);
		if (habitant) {
			ddump(nbTabs, "Individu: "+ numeroIndividu);
		}
	}

	@Transient
	public boolean isConnuAuCivil() {
		return numeroIndividu != null;
	}

	/**
	 * @retrun true si habitant, false si non habitant
	 */
	@Column(name = "PP_HABITANT")
	public Boolean isHabitant() {
		return habitant;
	}

	public void setHabitant(Boolean theHabitant) {
		habitant = theHabitant;
	}
	/**
	 * @return Returns the numeroIndividu.
	 * Peut ne pas etre unique! , unique = true)
	 */
	@Column(name = "NUMERO_INDIVIDU")
	@Index(name = "IDX_NUMERO_INDIVIDU")
//	@Column(name = "NUMERO_INDIVIDU", unique = true)
	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theNumeroIndividu the numeroIndividu to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8V1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroIndividu(Long theNumeroIndividu) {
		numeroIndividu = theNumeroIndividu;
	}

	@Transient
	public String getNumeroIndividuFormatte() {
		String s = "";

		if (numeroIndividu != null) {
			String sNumero = String.valueOf(numeroIndividu);
			s = sNumero;
		}

		return s;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the ancienNumeroSourcier
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8mVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "ANCIEN_NUMERO_SOURCIER")
	@Index(name = "IDX_ANC_NO_SRC", columnNames = "ANCIEN_NUMERO_SOURCIER")
	public Long getAncienNumeroSourcier() {
		return ancienNumeroSourcier;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theAncienNumeroSourcier the ancienNumeroSourcier to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8mVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setAncienNumeroSourcier(Long theAncienNumeroSourcier) {
		ancienNumeroSourcier = theAncienNumeroSourcier;
	}

	@OneToMany(mappedBy = "tiers", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_DA_TRS_ID")
	public Set<DroitAcces> getDroitsAccesAppliques() {
		return droitsAccesAppliques;
	}

	public void setDroitsAccesAppliques(Set<DroitAcces> droitsAccesAppliques) {
		this.droitsAccesAppliques = droitsAccesAppliques;
	}

	@Transient
	public Object getIndividuCache() {
		return individuCache;
	}

	@Transient
	public void setIndividuCache(Object individuCache) {
		this.individuCache = individuCache;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the numeroAssureSocial
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8PFx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NH_NUMERO_ASSURE_SOCIAL", length = LengthConstants.TIERS_NUMAVS)
	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theNumeroAssureSocial the numeroAssureSocial to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8PFx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroAssureSocial(String theNumeroAssureSocial) {
		numeroAssureSocial = theNumeroAssureSocial;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the nom
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Plx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NH_NOM", length = LengthConstants.TIERS_NOM)
	public String getNom() {
		return nom;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theNom the nom to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Plx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNom(String theNom) {
		nom = theNom;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the prenom
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8P1x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NH_PRENOM", length = LengthConstants.TIERS_NOM)
	public String getPrenom() {
		return prenom;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param thePrenom the prenom to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8P1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setPrenom(String thePrenom) {
		prenom = thePrenom;
	}

	/**
	 * Contient la date de naissance de la personne physique. Cette date peut être nulle.
	 * <p>
	 * <b>Note:</b> pour la signification de cette date, il faut distinguer deux cas:
	 * <ul>
	 * <li><b>pour un non-habitant</b>, il s'agit de la date de naissance de <i>référence</i>.</li>
	 * <li><b>pour un habitant</b>, il s'agit d'une <i>copie</i> de la date naissance définie dans le registre civil. Cette copie permet
	 * d'optimiser certains batches (notamment celui qui ouvre les fors fiscaux des habitants majeurs). Elle peut être nulle si
	 * l'information n'a jamais été extraite du registre civil (comportement d'un cache).</li>
	 * </ul>
	 *
	 * @return la date de naissance de la personne physique, ou <b>null</b> si cette information n'est pas connue.
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8QFx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NH_DATE_NAISSANCE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType", parameters = { @Parameter(name = "allowPartial", value = "true") })
	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	/**
	 * Voir la documentation sur le getter ({@link #getDateNaissance()}).
	 *
	 * @param theDateNaissance the dateNaissance to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8QFx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setDateNaissance(RegDate theDateNaissance) {
		dateNaissance = theDateNaissance;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the sexe
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Qlx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NH_SEXE", length = LengthConstants.TIERS_SEXE)
	@Type(type = "ch.vd.uniregctb.hibernate.SexeUserType")
	public Sexe getSexe() {
		return sexe;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theSexe the sexe to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Qlx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setSexe(Sexe theSexe) {
		sexe = theSexe;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the numeroOfsNationalite
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Q1x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NH_NO_OFS_NATIONALITE")
	public Integer getNumeroOfsNationalite() {
		return numeroOfsNationalite;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theNumeroOfsNationalite the numeroOfsNationalite to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Q1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroOfsNationalite(Integer theNumeroOfsNationalite) {
		numeroOfsNationalite = theNumeroOfsNationalite;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the numeroOfsCommuneOrigine
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Q1x9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NH_NO_OFS_COMMUNE_ORIGINE")
	public Integer getNumeroOfsCommuneOrigine() {
		return numeroOfsCommuneOrigine;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theNumeroOfsCommuneOrigine the numeroOfsCommuneOrigine to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi8Q1x9Edygsbnw9h5bVw?SETTER"
	 */
	public void setNumeroOfsCommuneOrigine(Integer numeroOfsCommuneOrigine) {
		this.numeroOfsCommuneOrigine = numeroOfsCommuneOrigine;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the typeAutorisation
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi0jVx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "NH_CAT_ETRANGER", length = LengthConstants.TIERS_CATETRANGER)
	@Type(type = "ch.vd.uniregctb.hibernate.CategorieEtrangerUserType")
	public CategorieEtranger getCategorieEtranger() {
		return categorieEtranger;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theTypeAutorisation the typeAutorisation to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi0jVx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setCategorieEtranger(CategorieEtranger theCategorieEtranger) {
		categorieEtranger = theCategorieEtranger;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the dateDebutValiditeAutorisation
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_RfBlkFyUEdyz_5BS6IxMlQ?GETTER"
	 */
	@Column(name = "NH_DATE_DEBUT_VALID_AUTORIS")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebutValiditeAutorisation() {
		return dateDebutValiditeAutorisation;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theDateDebutValiditeAutorisation the dateDebutValiditeAutorisation to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_RfBlkFyUEdyz_5BS6IxMlQ?SETTER"
	 */
	public void setDateDebutValiditeAutorisation(RegDate theDateDebutValiditeAutorisation) {
		dateDebutValiditeAutorisation = theDateDebutValiditeAutorisation;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the decede
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_YWcUYJN-Edy7DqR-SPIh9g?GETTER"
	 */
	@Column(name = "DATE_DECES")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDeces() {
		return dateDeces;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theDecede the decede to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_YWcUYJN-Edy7DqR-SPIh9g?SETTER"
	 */
	public void setDateDeces(RegDate date) {
		this.dateDeces = date;
	}

	/**
	 * ne doit être appelée que sur un non habitant
	 * @return <b>vrai</b> si le non-habitant est décédé; <b>false</b> s'il est toujours en vie.
	 */
	@Transient
	public boolean isDecede() {
		Assert.isFalse(habitant, "PersonnePhysique.isDecede ne doit être exécutée que sur un non-habitant");
		return dateDeces != null;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the identificationsPersonnes
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_4Z2GsO9YEdyEV8rfFv3rEg?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "NON_HABITANT_ID", nullable = false)
	@ForeignKey(name = "FK_ID_PERS_TRS_ID")
	@Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	public Set<IdentificationPersonne> getIdentificationsPersonnes() {
		return identificationsPersonnes;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @param theIdentificationsPersonnes the identificationsPersonnes to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_4Z2GsO9YEdyEV8rfFv3rEg?SETTER"
	 */
	public void setIdentificationsPersonnes(
			Set<IdentificationPersonne> theIdentificationsPersonnes) {
		// msi (23.11.2009) lorsqu'une collection est définie avec un Cascade=DELETE_ORPHAN *et* qu'on appel session.merge() sur une nouvelle instance de PersonnePhysique (donc avec une
		// collection nulle), hibernate lève une exception avec le message "A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance". Le problème
		// est que le merge fait les étapes suivantes :
		//   1. crée une nouvelle instance de PersonnePhysique avec un id (instance associée à la session)
		//   2. copie toutes les valeurs de l'instance reçue en paramètre sur la nouvelle instance
		//   3. retourne la nouvelle instance
		// Au niveau de la collection 'identificationsPersonnes', cela donne ça :
		//   1. newPP.identificationsPersonnes = new HibernateEntitySet()
		//   2. newPP.identificationsPersonnes = refPP.identificationsPersonnes
		// => le HibernateEntitySet est perdu lors de la copie des valeurs !
		// Résultat: on code un setter 'intelligent' qui évite perdre la collections une fois instanciée.
		if (identificationsPersonnes == null) {
			identificationsPersonnes = theIdentificationsPersonnes;
		}
		else {
			identificationsPersonnes.clear();
			if (theIdentificationsPersonnes != null) {
				identificationsPersonnes.addAll(theIdentificationsPersonnes);
			}
		}
	}

	@Column(name = "MAJORITE_TRAITEE")
	public Boolean getMajoriteTraitee() {
		return majoriteTraitee;
	}

	public void setMajoriteTraitee(Boolean value) {
		this.majoriteTraitee = value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PersonnePhysique other = (PersonnePhysique) obj;
		if (ancienNumeroSourcier == null) {
			if (other.ancienNumeroSourcier != null)
				return false;
		}
		else if (!ancienNumeroSourcier.equals(other.ancienNumeroSourcier))
			return false;
		if (categorieEtranger == null) {
			if (other.categorieEtranger != null)
				return false;
		}
		else if (!categorieEtranger.equals(other.categorieEtranger))
			return false;
		if (dateDebutValiditeAutorisation == null) {
			if (other.dateDebutValiditeAutorisation != null)
				return false;
		}
		else if (!dateDebutValiditeAutorisation.equals(other.dateDebutValiditeAutorisation))
			return false;
		if (dateDeces == null) {
			if (other.dateDeces != null)
				return false;
		}
		else if (!dateDeces.equals(other.dateDeces))
			return false;
		if (dateNaissance == null) {
			if (other.dateNaissance != null)
				return false;
		}
		else if (!dateNaissance.equals(other.dateNaissance))
			return false;
		if (habitant == null) {
			if (other.habitant != null)
				return false;
		}
		else if (!habitant.equals(other.habitant))
			return false;
		if (identificationsPersonnes == null) {
			if (other.identificationsPersonnes != null)
				return false;
		}
		else if (!identificationsPersonnes.equals(other.identificationsPersonnes))
			return false;
		if (majoriteTraitee == null) {
			if (other.majoriteTraitee != null)
				return false;
		}
		else if (!majoriteTraitee.equals(other.majoriteTraitee))
			return false;
		if (nom == null) {
			if (other.nom != null)
				return false;
		}
		else if (!nom.equals(other.nom))
			return false;
		if (numeroAssureSocial == null) {
			if (other.numeroAssureSocial != null)
				return false;
		}
		else if (!numeroAssureSocial.equals(other.numeroAssureSocial))
			return false;
		if (numeroIndividu == null) {
			if (other.numeroIndividu != null)
				return false;
		}
		else if (!numeroIndividu.equals(other.numeroIndividu))
			return false;
		if (numeroOfsNationalite == null) {
			if (other.numeroOfsNationalite != null)
				return false;
		}
		else if (!numeroOfsNationalite.equals(other.numeroOfsNationalite))
			return false;
		if (prenom == null) {
			if (other.prenom != null)
				return false;
		}
		else if (!prenom.equals(other.prenom))
			return false;
		if (sexe == null) {
			if (other.sexe != null)
				return false;
		}
		else if (!sexe.equals(other.sexe))
			return false;
		return true;
	}
}
