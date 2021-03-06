package ch.vd.unireg.adresse;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.TypeAdresseTiers;

@Entity
@Table(name = "ADRESSE_TIERS", indexes = {
		@Index(name = "IDX_ADR_TRS_ID", columnList = "TIERS_ID"),
		@Index(name = "IDX_ADR_AT_TRS_ID", columnList = "AUTRE_TIERS_ID")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ADR_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class AdresseTiers extends HibernateDateRangeEntity implements Comparable<AdresseTiers>, Duplicable<AdresseTiers>, LinkedEntity {

	private Long id;

	private TypeAdresseTiers usage;

	private Tiers tiers;

	protected AdresseTiers() {
	}

	/**
	 * Utilisé par la duplication (on ne recopie ni l'identifiant, ni le tiers associé)
	 * @param src la source de la duplication
	 */
	protected AdresseTiers(AdresseTiers src) {
		super(src);
		this.usage = src.usage;
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

	public void setId(Long theId) {
		this.id = theId;
	}

	@Column(name = "USAGE_TYPE", length = LengthConstants.ADRESSE_TYPETIERS, nullable = false)
	// note : USAGE est un mot réservé sous MySql
	@Type(type = "ch.vd.unireg.hibernate.TypeAdresseTiersUserType")
	public TypeAdresseTiers getUsage() {
		return usage;
	}

	public void setUsage(TypeAdresseTiers theUsage) {
		usage = theUsage;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
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
				", dateDebut=" + RegDateHelper.dateToDisplayString(getDateDebut()) +
				", dateFin=" + RegDateHelper.dateToDisplayString(getDateFin()) +
				", usage=" + usage +
				", tiers=" + tiers +
				'}';
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return tiers == null ? null : Collections.singletonList(tiers);
	}
}
