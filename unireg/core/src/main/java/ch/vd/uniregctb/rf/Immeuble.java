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

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Les informations d'un immeuble (= une parcelle + tout ce qu'il y a dessus) telles que disponibles au registre foncier (SIFISC-2337).
 */
@SuppressWarnings({"UnusedDeclaration"})
@Entity
@Table(name = "IMMEUBLE")
public class Immeuble extends HibernateEntity implements DateRange {

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String numero;
	private String nature;
	private int estimationFiscale;
	private RegDate dateEstimationFiscale;
	private Integer ancienneEstimationFiscale;
	private GenrePropriete genrePropriete;
	private PartPropriete partPropriete;
	private Contribuable proprietaire;
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
	 * @return la nature de l'immeuble.
	 */
	@Column(name = "NATURE_IMMEUBLE", nullable = false, length = LengthConstants.NATURE_IMMEUBLE)
	public String getNature() {
		return nature;
	}

	public void setNature(String nature) {
		this.nature = nature;
	}

	/**
	 * @return l'estimation fiscale en francs suisses.
	 */
	@Column(name = "ESTIMATION_FISCALE", nullable = false)
	public int getEstimationFiscale() {
		return estimationFiscale;
	}

	public void setEstimationFiscale(int estimationFiscale) {
		this.estimationFiscale = estimationFiscale;
	}

	/**
	 * @return la date de l'estimation fiscale actuelle
	 */
	@Column(name = "DATE_ESTIM_FISC", nullable = true)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEstimationFiscale() {
		return dateEstimationFiscale;
	}

	public void setDateEstimationFiscale(RegDate dateEstimationFiscale) {
		this.dateEstimationFiscale = dateEstimationFiscale;
	}

	/**
	 * @return l'ancienne estimation fiscale en francs suisses, ou <b>null</b> si cette information n'est pas disponible.
	 */
	@Column(name = "ANCIENNE_ESTIM_FISC", nullable = true)
	public Integer getAncienneEstimationFiscale() {
		return ancienneEstimationFiscale;
	}

	public void setAncienneEstimationFiscale(Integer ancienneEstimationFiscale) {
		this.ancienneEstimationFiscale = ancienneEstimationFiscale;
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

	/**
	 * @return le propriétaire
	 */
	@ManyToOne
	@JoinColumn(name = "CTB_ID", nullable = false)
	@Index(name = "IDX_IMM_CTB_ID", columnNames = "CTB_ID")
	public Contribuable getProprietaire() {
		return proprietaire;
	}

	public void setProprietaire(Contribuable proprietaire) {
		this.proprietaire = proprietaire;
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
}
