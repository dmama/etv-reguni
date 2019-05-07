package ch.vd.unireg.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.List;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;

@Entity
@Table(name = "RF_DROIT", uniqueConstraints = {
		@UniqueConstraint(name = "IDX_DROIT_MASTER_VERSION_ID_RF", columnNames = {"MASTER_ID_RF", "VERSION_ID_RF"}),
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public abstract class DroitRF extends HibernateDateRangeEntity implements LinkedEntity, Comparable<DroitRF> {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Identifiant technique du droit au registre foncier.
	 */
	private String masterIdRF;

	/**
	 * Numéro de révision du droit au registre foncier.
	 */
	private String versionIdRF;

	/**
	 * La date de début du droit telle que calculée par Unireg (droits de propriété) ou  ou renseignée dans le RF (servitudes).
	 */
	@Nullable
	private RegDate dateDebutMetier;

	/**
	 * La date de fin de droit telle que calculée par Unireg (droits de propriété) ou renseignée dans le RF (servitudes).
	 */
	@Nullable
	private RegDate dateFinMetier;

	/**
	 * Le motif de début du droit.
	 */
	@Nullable
	private String motifDebut;

	/**
	 * Le motif de fin du droit.
	 */
	@Nullable
	private String motifFin;

	public DroitRF() {
	}

	public DroitRF(DroitRF right) {
		super(right);
		this.id = right.id;
		this.masterIdRF = right.masterIdRF;
		this.versionIdRF = right.versionIdRF;
		this.dateDebutMetier = right.dateDebutMetier;
		this.dateFinMetier = right.dateFinMetier;
		this.motifDebut = right.motifDebut;
		this.motifFin = right.motifFin;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "MASTER_ID_RF", nullable = false, length = LengthConstants.RF_ID_RF)
	public String getMasterIdRF() {
		return masterIdRF;
	}

	public void setMasterIdRF(String masterIdRF) {
		this.masterIdRF = masterIdRF;
	}

	@Column(name = "VERSION_ID_RF", nullable = false, length = LengthConstants.RF_ID_RF)
	public String getVersionIdRF() {
		return versionIdRF;
	}

	public void setVersionIdRF(String versionIdRF) {
		this.versionIdRF = versionIdRF;
	}

	@Nullable
	@Column(name = "DATE_DEBUT_METIER")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebutMetier() {
		return dateDebutMetier;
	}

	public void setDateDebutMetier(@Nullable RegDate dateDebutMetier) {
		this.dateDebutMetier = dateDebutMetier;
	}

	@Nullable
	@Column(name = "DATE_FIN_METIER")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateFinMetier() {
		return dateFinMetier;
	}

	public void setDateFinMetier(@Nullable RegDate dateFinMetier) {
		this.dateFinMetier = dateFinMetier;
	}

	@Nullable
	@Column(name = "MOTIF_DEBUT_CODE", length = LengthConstants.RF_MOTIF)
	public String getMotifDebut() {
		return motifDebut;
	}

	public void setMotifDebut(@Nullable String motifDebut) {
		this.motifDebut = motifDebut;
	}

	@Nullable
	@Column(name = "MOTIF_FIN_CODE", length = LengthConstants.RF_MOTIF)
	public String getMotifFin() {
		return motifFin;
	}

	public void setMotifFin(@Nullable String motifFin) {
		this.motifFin = motifFin;
	}

	/**
	 * Compare le droit courant avec un autre droit. Les propriétés utilisées pour la comparaison sont :
	 * <ul>
	 * <li>les dates de début et de fin</li>
	 * </ul>
	 *
	 * @param right un autre droit.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	@Override
	public int compareTo(@NotNull DroitRF right) {
		return DateRangeComparator.compareRanges(this, right);
	}

	@Transient
	@NotNull
	public DateRange getRangeMetier() {
		return new DateRangeHelper.Range(dateDebutMetier, dateFinMetier);
	}

	@Transient
	@NotNull
	public abstract TypeDroit getTypeDroit();

	/**
	 * @return une liste des ayants-droits concernés par ce droit.
	 */
	@Transient
	@NotNull
	public abstract List<AyantDroitRF> getAyantDroitList();

	/**
	 * @return une liste des immeubles concernés par ce droit.
	 */
	@Transient
	@NotNull
	public abstract List<ImmeubleRF> getImmeubleList();
}
