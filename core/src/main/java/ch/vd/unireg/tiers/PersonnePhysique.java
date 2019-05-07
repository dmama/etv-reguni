package ch.vd.unireg.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ComparisonHelper;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.Sexe;

/**
 * Être humain sous l'angle du droit, individualisée par ses caractéristiques, telles que son
 * nom et prénom, sa date de naissance, son sexe, son numéro AVS?
 */
@Entity
@DiscriminatorValue("PersonnePhysique")
public class PersonnePhysique extends ContribuableImpositionPersonnesPhysiques {

	public PersonnePhysique(){
		super();
	}
	public PersonnePhysique(Boolean habitant) {
		this.habitant = habitant;
	}
	public PersonnePhysique(long numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
		this.habitant = true;
	}

	private Long ancienNumeroSourcier;

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


	/**
	 *
	 * Nouveau numéro d'assuré AVS au sens de l'art. 50c LAVS.
	 * Le numéro d'assuré AVS est numérique (13 positions) et non signifiant.
	 *
	 * attributs d'un non habitant (ie personne physique non résident dans le canton)
	 */
	private String numeroAssureSocial;
	private String nom;
	private String prenomUsuel;
	private String tousPrenoms;
	private RegDate dateNaissance;
	private Sexe sexe;

	/**
	 * Origine du non-habitant
	 */
	private OriginePersonnePhysique origine;

	/**
	 * Nom de naissance du non-habitant
	 */
	private String nomNaissance;

	/**
	 * Code ISO-2 du pays selon la norme ISO-3166
	 */
	private Integer numeroOfsNationalite;

	/**
	 * Genre de permis réglementant le séjour d'une personne étrangère en Suisse.
	 * Voir eCH-0006 pour les valeurs possibles
	 */
	private CategorieEtranger categorieEtranger;

	/**
	 * Date à partir de laquelle l'autorisation pour étrangers est valable
	 */
	private RegDate dateDebutValiditeAutorisation;

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

	/**
	 * [SIFISC-8177] Booléen utilisé par le batch de passage au rôle des sourcier rentiers, afin de ne pas traiter le même contribuable
	 * lors de deux exécutions différentes du batch, dans le cas où par exemple une décision métier a conservé l'assujettissement
	 * "source pure" sur un contribuable précédemment passé au rôle par ce batch
	 */
	private Boolean rentierSourcierPasseAuRole;

	/**
	 * [SIFISC-9096] Booléen utilisé pour déterminer s'il faut tenter un recalcul des relations de parentés sur la personne physique
	 * (par exemple utilisé pour les mineurs dont l'arrivée est signalée avant celle de leurs parents, et pour lesquels les parentés
	 * ne peuvent être insérées dans Unireg tant que les parents ne sont pas créés également)
	 */
	private Boolean parenteDirty;

	/**
	 * [SIFISC-12136] Noms officiels des parents
	 */
	private String nomPere;
	private String prenomsPere;
	private String nomMere;
	private String prenomsMere;

	@Transient
	@Override
	public NatureTiers getNatureTiers() {
		if (habitant) {
			return NatureTiers.Habitant;
		}
		else {
			return NatureTiers.NonHabitant;
		}
	}

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.PERSONNE_PHYSIQUE;
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		if (habitant || !getForsFiscauxNonAnnules(false).isEmpty()) {
			return super.getRoleLigne1();
		}
		return "Autre tiers";
	}

	@Transient
	public boolean isConnuAuCivil() {
		return numeroIndividu != null && numeroIndividu > 0L;
	}

	@Column(name = "PP_HABITANT")
	public Boolean getHabitant() { // le préfixe 'is' sur le type Boolean (avec une grand B) n'est pas compris par la classe java.beans.Introspector
		return habitant;
	}

	public void setHabitant(Boolean theHabitant) {
		habitant = theHabitant;
	}

	/**
	 * @return <b>true</b> si la personne habite le canton; <b>false</b> autrement.
	 */
	@Transient
	public boolean isHabitantVD() {
		return habitant;
	}

	/**
	 * @return Returns the numeroIndividu.
	 * Peut ne pas etre unique!
	 */
	@Column(name = "NUMERO_INDIVIDU")
	@Index(name = "IDX_NUMERO_INDIVIDU")
	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	public void setNumeroIndividu(@Nullable Long theNumeroIndividu) {
		numeroIndividu = theNumeroIndividu;
	}

	@Transient
	public String getNumeroIndividuFormatte() {
		String s = "";

		if (numeroIndividu != null) {
			s = String.valueOf(numeroIndividu);
		}

		return s;
	}

	@Column(name = "ANCIEN_NUMERO_SOURCIER")
	@Index(name = "IDX_ANC_NO_SRC", columnNames = "ANCIEN_NUMERO_SOURCIER")
	public Long getAncienNumeroSourcier() {
		return ancienNumeroSourcier;
	}

	public void setAncienNumeroSourcier(Long theAncienNumeroSourcier) {
		ancienNumeroSourcier = theAncienNumeroSourcier;
	}

	@Transient
	public Object getIndividuCache() {
		return individuCache;
	}

	@Transient
	public void setIndividuCache(Object individuCache) {
		this.individuCache = individuCache;
	}

	@Column(name = "NH_NUMERO_ASSURE_SOCIAL", length = LengthConstants.TIERS_NUMAVS)
	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public void setNumeroAssureSocial(@Nullable String theNumeroAssureSocial) {
		numeroAssureSocial = theNumeroAssureSocial;
	}

	@Column(name = "NH_NOM", length = LengthConstants.TIERS_NOM)
	public String getNom() {
		return nom;
	}

	public void setNom(@Nullable String theNom) {
		nom = theNom;
	}

	@Column(name = "NH_PRENOM", length = LengthConstants.TIERS_NOM)
	public String getPrenomUsuel() {
		return prenomUsuel;
	}

	public void setPrenomUsuel(@Nullable String thePrenom) {
		prenomUsuel = thePrenom;
	}

	@Column(name = "NH_TOUS_PRENOMS", length = LengthConstants.TIERS_TOUS_PRENOMS)
	public String getTousPrenoms() {
		return tousPrenoms;
	}

	public void setTousPrenoms(@Nullable String tousPrenoms) {
		this.tousPrenoms = tousPrenoms;
	}

	@Column(name = "NH_NOM_NAISSANCE", length = LengthConstants.TIERS_NOM)
	public String getNomNaissance() {
		return nomNaissance;
	}

	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
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
	 */
	@Column(name = "NH_DATE_NAISSANCE")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType", parameters = { @Parameter(name = "allowPartial", value = "true") })
	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(@Nullable RegDate theDateNaissance) {
		dateNaissance = theDateNaissance;
	}

	@Column(name = "NH_SEXE", length = LengthConstants.TIERS_SEXE)
	@Type(type = "ch.vd.unireg.hibernate.SexeUserType")
	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(@Nullable Sexe theSexe) {
		sexe = theSexe;
	}

	@Column(name = "NH_NO_OFS_NATIONALITE")
	public Integer getNumeroOfsNationalite() {
		return numeroOfsNationalite;
	}

	public void setNumeroOfsNationalite(@Nullable Integer theNumeroOfsNationalite) {
		numeroOfsNationalite = theNumeroOfsNationalite;
	}

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "libelle", column = @Column(name = "NH_LIBELLE_ORIGINE", nullable = true, length = LengthConstants.TIERS_LIBELLE_ORIGINE)),
			@AttributeOverride(name = "sigleCanton", column = @Column(name = "NH_CANTON_ORIGINE", nullable = true, length = LengthConstants.TIERS_CANTON_ORIGINE))})
	public OriginePersonnePhysique getOrigine() {
		return origine;
	}

	public void setOrigine(OriginePersonnePhysique origine) {
		this.origine = origine;
	}

	@Column(name = "NH_CAT_ETRANGER", length = LengthConstants.TIERS_CATETRANGER)
	@Type(type = "ch.vd.unireg.hibernate.CategorieEtrangerUserType")
	public CategorieEtranger getCategorieEtranger() {
		return categorieEtranger;
	}

	public void setCategorieEtranger(@Nullable CategorieEtranger theCategorieEtranger) {
		categorieEtranger = theCategorieEtranger;
	}

	@Column(name = "NH_DATE_DEBUT_VALID_AUTORIS")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebutValiditeAutorisation() {
		return dateDebutValiditeAutorisation;
	}

	public void setDateDebutValiditeAutorisation(@Nullable RegDate theDateDebutValiditeAutorisation) {
		dateDebutValiditeAutorisation = theDateDebutValiditeAutorisation;
	}

	@Column(name = "DATE_DECES")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(@Nullable RegDate date) {
		this.dateDeces = date;
	}

	/**
	 * ne doit être appelée que sur un non habitant
	 * @return <b>vrai</b> si le non-habitant est décédé; <b>false</b> s'il est toujours en vie.
	 */
	@Transient
	public boolean isDecede() {
		if (habitant) {
			throw new IllegalArgumentException("PersonnePhysique.isDecede ne doit être exécutée que sur un non-habitant");
		}
		return dateDeces != null;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "NON_HABITANT_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_ID_PERS_TRS_ID"))
	public Set<IdentificationPersonne> getIdentificationsPersonnes() {
		return identificationsPersonnes;
	}

	public void setIdentificationsPersonnes(
			@Nullable Set<IdentificationPersonne> theIdentificationsPersonnes) {
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
		if (identificationsPersonnes == null || identificationsPersonnes instanceof HashSet) {
			identificationsPersonnes = theIdentificationsPersonnes;
		}
		else if (identificationsPersonnes == theIdentificationsPersonnes) {
			// rien à faire
		}
		else {
			identificationsPersonnes.clear();
			if (theIdentificationsPersonnes != null) {
				identificationsPersonnes.addAll(theIdentificationsPersonnes);
			}
		}
	}

	/**
	 * Version simple du setter pour la méthode getBatch() du TiersDAO.
	 *
	 * @param set un ensemble
	 */
	public void setIdentificationsPersonnesForGetBatch(Set<IdentificationPersonne> set) {
		this.identificationsPersonnes = set;
	}

	public void addIdentificationPersonne(IdentificationPersonne ident) {
		if (identificationsPersonnes == null) {
			identificationsPersonnes = new HashSet<>();
		}
		ident.setPersonnePhysique(this);
		identificationsPersonnes.add(ident);
	}

	@Column(name = "MAJORITE_TRAITEE")
	public Boolean getMajoriteTraitee() {
		return majoriteTraitee;
	}

	public void setMajoriteTraitee(Boolean value) {
		this.majoriteTraitee = value;
	}

	@Column(name = "RENTIER_SRC_ROLE")
	public Boolean getRentierSourcierPasseAuRole() {
		return rentierSourcierPasseAuRole;
	}

	public void setRentierSourcierPasseAuRole(Boolean rentierSourcierPasseAuRole) {
		this.rentierSourcierPasseAuRole = rentierSourcierPasseAuRole;
	}

	// Updatable = false -> hibernate ne modifiera pas cette valeur (qui sera modifiée par une requête SQL ad'hoc)
	@Column(name = "PP_PARENTE_DIRTY", updatable = false)
	public Boolean getParenteDirty() {
		return parenteDirty;
	}

	protected void setParenteDirty(Boolean parenteDirty) {
		this.parenteDirty = parenteDirty;
	}

	@Transient
	public boolean parenteDirty() {
		return parenteDirty != null && parenteDirty;
	}

	@Column(name = "NH_NOM_PERE", length = LengthConstants.TIERS_NOM_PRENOMS_PARENT)
	public String getNomPere() {
		return nomPere;
	}

	public void setNomPere(String nomPere) {
		this.nomPere = StringUtils.trimToNull(nomPere);
	}

	@Column(name = "NH_PRENOMS_PERE", length = LengthConstants.TIERS_NOM_PRENOMS_PARENT)
	public String getPrenomsPere() {
		return prenomsPere;
	}

	public void setPrenomsPere(String prenomsPere) {
		this.prenomsPere = StringUtils.trimToNull(prenomsPere);
	}

	@Column(name = "NH_NOM_MERE", length = LengthConstants.TIERS_NOM_PRENOMS_PARENT)
	public String getNomMere() {
		return nomMere;
	}

	public void setNomMere(String nomMere) {
		this.nomMere = StringUtils.trimToNull(nomMere);
	}

	@Column(name = "NH_PRENOMS_MERE", length = LengthConstants.TIERS_NOM_PRENOMS_PARENT)
	public String getPrenomsMere() {
		return prenomsMere;
	}

	public void setPrenomsMere(String prenomsMere) {
		this.prenomsMere = StringUtils.trimToNull(prenomsMere);
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

		final PersonnePhysique other = (PersonnePhysique) obj;
		return ComparisonHelper.areEqual(ancienNumeroSourcier, other.ancienNumeroSourcier)
				&& ComparisonHelper.areEqual(categorieEtranger, other.categorieEtranger)
				&& ComparisonHelper.areEqual(dateDebutValiditeAutorisation, other.dateDebutValiditeAutorisation)
				&& ComparisonHelper.areEqual(dateDeces, other.dateDeces)
				&& ComparisonHelper.areEqual(dateNaissance, other.dateNaissance)
				&& ComparisonHelper.areEqual(habitant, other.habitant)
				&& ComparisonHelper.areEqual(identificationsPersonnes, other.identificationsPersonnes)
				&& ComparisonHelper.areEqual(majoriteTraitee, other.majoriteTraitee)
				&& ComparisonHelper.areEqual(nom, other.nom)
				&& ComparisonHelper.areEqual(numeroAssureSocial, other.numeroAssureSocial)
				&& ComparisonHelper.areEqual(numeroIndividu, other.numeroIndividu)
				&& ComparisonHelper.areEqual(numeroOfsNationalite, other.numeroOfsNationalite)
				&& ComparisonHelper.areEqual(prenomUsuel, other.prenomUsuel)
				&& ComparisonHelper.areEqual(tousPrenoms, other.tousPrenoms)
				&& ComparisonHelper.areEqual(sexe, other.sexe);
	}
}
