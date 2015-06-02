package ch.vd.uniregctb.adresse;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;

@Entity
@Table(name = "ADRESSE_TIERS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ADR_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class AdresseTiers extends HibernateEntity implements Comparable<AdresseTiers>, DateRange, Duplicable<AdresseTiers>, LinkedEntity {

	private Long id;

	/**
	 * Date de début de la validité de l'adresse postale du tiers
	 */
	private RegDate dateDebut;

	/**
	 * Date de fin de la validité de l'adresse postale du tiers
	 */
	private RegDate dateFin;

	private TypeAdresseTiers usage;

	private Tiers tiers;

	protected AdresseTiers() {
	}

	/**
	 * Utilisé par la duplication (on ne recopie ni l'identifiant, ni le tiers associé)
	 * @param src la source de la duplication
	 */
	protected AdresseTiers(AdresseTiers src) {
		this.dateDebut = src.dateDebut;
		this.dateFin = src.dateFin;
		this.usage = src.usage;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long theId) {
		this.id = theId;
	}

	@Override
	@Column(name = "DATE_DEBUT", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate theDateDebut) {
		dateDebut = theDateDebut;
	}

	@Override
	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(@Nullable RegDate theDateFin) {
		dateFin = theDateFin;
	}

	@Column(name = "USAGE_TYPE", length = LengthConstants.ADRESSE_TYPETIERS, nullable = false)
	// note : USAGE est un mot réservé sous MySql
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAdresseTiersUserType")
	public TypeAdresseTiers getUsage() {
		return usage;
	}

	public void setUsage(TypeAdresseTiers theUsage) {
		usage = theUsage;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_ADR_TRS_ID", columnNames = "TIERS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers theTiers) {
		tiers = theTiers;
	}

	/**
	 * Compare 2 adresses. La plus grande est celle avec la date de début la plus tard
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public final int compareTo(AdresseTiers other) {
		return DateRangeComparator.compareRanges(this, other);
	}

	@Override
	public String toString() {
		return "AdresseTiers{" +
				"id=" + id +
				", dateDebut=" + RegDateHelper.dateToDisplayString(dateDebut) +
				", dateFin=" + RegDateHelper.dateToDisplayString(dateFin) +
				", usage=" + usage +
				", tiers=" + tiers +
				'}';
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return tiers == null ? null : Arrays.asList(tiers);
	}
}
