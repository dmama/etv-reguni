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
	private RegDate dateMutation;
	private NumeroImmeuble numero;
	private NatureImmeuble nature;
	private int estimationFiscale;
	private GenrePropriete genrePropriete;
	private PartPropriete partPropriete;
	private Contribuable proprietaire;

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
	@Column(name = "DATE_DEBUT", nullable = false)
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
	 * @return la date d'inscription de la mutation au registre foncier.
	 */
	@Column(name = "DATE_MUTATION", nullable = true)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateMutation() {
		return dateMutation;
	}

	public void setDateMutation(RegDate dateMutation) {
		this.dateMutation = dateMutation;
	}

	/**
	 * @return le numéro d'identification de l'immeuble
	 */
	@Embedded
	public NumeroImmeuble getNumero() {
		return numero;
	}

	public void setNumero(NumeroImmeuble numero) {
		this.numero = numero;
	}

	/**
	 * @return la nature de l'immeuble.
	 */
	@Column(name = "NATURE_IMMEUBLE", nullable = false, length = LengthConstants.NATURE_IMMEUBLE)
	@Type(type = "ch.vd.uniregctb.hibernate.NatureImmeubleUserType")
	public NatureImmeuble getNature() {
		return nature;
	}

	public void setNature(NatureImmeuble nature) {
		this.nature = nature;
	}

	/**
	 * @return l'estimation fiscale en francs suisses.
	 */
	@Column(name = "ESTIMATION_FISCALE", nullable = true)
	public int getEstimationFiscale() {
		return estimationFiscale;
	}

	public void setEstimationFiscale(int estimationFiscale) {
		this.estimationFiscale = estimationFiscale;
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
