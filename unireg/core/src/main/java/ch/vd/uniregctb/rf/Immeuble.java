package ch.vd.uniregctb.rf;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.net.URL;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.LinkedEntity;

/**
 * Les informations d'un immeuble (= une parcelle + tout ce qu'il y a dessus) telles que disponibles au registre foncier (SIFISC-2337).
 */
@SuppressWarnings({"UnusedDeclaration"})
@Entity
@Table(name = "IMMEUBLE")
public class Immeuble extends HibernateEntity implements DateRange, LinkedEntity {

	private Long id;
	private String idRF;
	private RegDate dateValidRF;
	private RegDate dateDebut;
	private RegDate dateFin;
	private RegDate dateValidation;
	private RegDate dateDerniereMutation;
	private TypeMutation derniereMutation;
	private String numero;
	private String nomCommune;
	private String nature;
	private Integer estimationFiscale;
	private String referenceEstimationFiscale;
	private TypeImmeuble typeImmeuble;
	private GenrePropriete genrePropriete;
	private PartPropriete partPropriete;
	private Proprietaire proprietaire;
	private Contribuable contribuable;
	private URL lienRegistreFoncier;

	/**
	 * @return numéro technique de l'enregistrement dans la base de données.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return l'identifiant technique de l'immeuble du RF (à ne pas confondre avec le numéro d'immeuble)
	 */
	@Column(name = "ID_RF", nullable = false, length = 40)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	/**
	 * @return la date de validation des données dans le registre foncier
	 */
	@Column(name = "DATE_VALID_RF")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateValidRF() {
		return dateValidRF;
	}

	public void setDateValidRF(RegDate date) {
		this.dateValidRF = date;
	}

	/**
	 * @return la date de début de validité de l'inscription au registre foncier.
	 */
	@Override
	@Column(name = "DATE_DEBUT", nullable = true)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	/**
	 * @return la date de fin de validité de l'inscription au registre foncier; ou <b>null</b> si l'inscription est toujours valide.
	 */
	@Override
	@Column(name = "DATE_FIN", nullable = true)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	/**
	 * @return la date de la dernière mutation dans le RF
	 */
	@Column(name = "DATE_DERNIERE_MUTATION")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDerniereMutation() {
		return dateDerniereMutation;
	}

	public void setDateDerniereMutation(RegDate dateDerniereMutation) {
		this.dateDerniereMutation = dateDerniereMutation;
	}

	/**
	 * @return le type de mutation lors du dernier changement.
	 */
	@Column(name = "DERNIERE_MUTATION", length = LengthConstants.DERNIERE_MUTATION)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeMutationUserType")
	public TypeMutation getDerniereMutation() {
		return derniereMutation;
	}

	public void setDerniereMutation(TypeMutation derniereMutation) {
		this.derniereMutation = derniereMutation;
	}

	/**
	 * @return le numéro d'identification de l'immeuble
	 */
	@Column(name = "NUMERO_IMMEUBLE", nullable = false, length = LengthConstants.NUMERO_IMMEUBLE)
	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	/**
	 * @return le nom de la commune où l'immeuble est construit
	 */
	@Column(name = "NOM_COMMUNE", nullable = false, length = LengthConstants.NOM_COMMUNE)
	public String getNomCommune() {
		return nomCommune;
	}

	public void setNomCommune(String nomCommune) {
		this.nomCommune = nomCommune;
	}

	/**
	 * @return la nature de l'immeuble.
	 */
	@Column(name = "NATURE_IMMEUBLE", nullable = true, length = LengthConstants.NATURE_IMMEUBLE)
	public String getNature() {
		return nature;
	}

	public void setNature(String nature) {
		this.nature = nature;
	}

	/**
	 * @return l'estimation fiscale en francs suisses.
	 */
	@Column(name = "ESTIMATION_FISCALE", nullable = true)
	public Integer getEstimationFiscale() {
		return estimationFiscale;
	}

	public void setEstimationFiscale(Integer estimationFiscale) {
		this.estimationFiscale = estimationFiscale;
	}

	/**
	 * @return la référence (année ou code de révision générale) de l'estimation fiscale courante
	 */
	@Column(name = "REF_ESTIM_FISC", nullable = true, length = LengthConstants.REF_ESTIM_FISCALE)
	public String getReferenceEstimationFiscale() {
		return referenceEstimationFiscale;
	}

	public void setReferenceEstimationFiscale(String referenceEstimationFiscale) {
		this.referenceEstimationFiscale = referenceEstimationFiscale;
	}

	/**
	 * @return le type d'immeuble.
	 */
	@Column(name = "TYPE_IMMEUBLE", nullable = false, length = LengthConstants.TYPE_IMMEUBLE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeImmeubleUserType")
	public TypeImmeuble getTypeImmeuble() {
		return typeImmeuble;
	}

	public void setTypeImmeuble(TypeImmeuble typeImmeuble) {
		this.typeImmeuble = typeImmeuble;
	}

	/**
	 * @return le genre de propriété.
	 */
	@Column(name = "GENRE_PROPRIETE", nullable = false, length = LengthConstants.GENRE_PROPRIETE)
	@Type(type = "ch.vd.uniregctb.hibernate.GenreProprieteUserType")
	public GenrePropriete getGenrePropriete() {
		return genrePropriete;
	}

	public void setGenrePropriete(GenrePropriete genrePropriete) {
		this.genrePropriete = genrePropriete;
	}

	/**
	 * @return la part de propriété, sous forme de fraction entière.
	 */
	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "numerateur", column = @Column(name = "PART_PROPRIETE_NUMERATEUR", nullable = false)),
			@AttributeOverride(name = "denominateur", column = @Column(name = "PART_PROPRIETE_DENOMINATEUR", nullable = false))
	})
	public PartPropriete getPartPropriete() {
		return partPropriete;
	}

	public void setPartPropriete(PartPropriete partPropriete) {
		this.partPropriete = partPropriete;
	}

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "id", column = @Column(name = "ID_PROPRIETAIRE_RF", nullable = false, length = 40)),
			@AttributeOverride(name = "numeroIndividu", column = @Column(name = "NUMERO_INDIVIDU_RF", nullable = false))
	})
	public Proprietaire getProprietaire() {
		return proprietaire;
	}

	public void setProprietaire(Proprietaire proprietaire) {
		this.proprietaire = proprietaire;
	}

	/**
	 * @return le propriétaire
	 */
	@ManyToOne
	@JoinColumn(name = "CTB_ID", nullable = false)
	@Index(name = "IDX_IMM_CTB_ID", columnNames = "CTB_ID")
	public Contribuable getContribuable() {
		return contribuable;
	}

	public void setContribuable(Contribuable contribuable) {
		this.contribuable = contribuable;
	}

	@Column(name = "LIEN_RF", nullable = true, length = LengthConstants.LIEN_RF)
	@Type(type = "ch.vd.uniregctb.hibernate.URLUserType")
	public URL getLienRegistreFoncier() {
		return lienRegistreFoncier;
	}

	public void setLienRegistreFoncier(URL lienRegistreFoncier) {
		this.lienRegistreFoncier = lienRegistreFoncier;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		// msi (11.01.2012) l'insertion d'un immeuble ne doit pas provoquer la validation du contribuable : return contribuable == null ? null : Arrays.asList(contribuable);
		return null;
	}
}
