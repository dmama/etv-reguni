package ch.vd.unireg.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

@Entity
@Table(name = "RF_RAISON_ACQUISITION")
public class RaisonAcquisitionRF extends HibernateEntity implements Comparable<RaisonAcquisitionRF> {

	private static final Comparator<RegDate> REG_DATE_COMPARATOR = Comparator.nullsFirst(RegDate::compareTo);

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * La date de début technique.
	 */
	private RegDate dateDebut;

	/**
	 * La date de début du droit telle que renseignée dans le registre foncier (la date de début normale est une date technique qui correspond à la date d'import de la donnée).
	 */
	@Nullable
	private RegDate dateAcquisition;

	/**
	 * Le motif de début du droit.
	 */
	@Nullable
	private String motifAcquisition;

	/**
	 * Le numéro d'affaire.
	 */
	@Nullable
	private IdentifiantAffaireRF numeroAffaire;

	/**
	 * Le droit concerné par la raison d'acquisition.
	 */
	private DroitProprieteRF droit;

	// pour Hibernate
	public RaisonAcquisitionRF() {
	}

	public RaisonAcquisitionRF(@Nullable RegDate dateAcquisition, @Nullable String motifAcquisition, @Nullable IdentifiantAffaireRF numeroAffaire) {
		this.dateAcquisition = dateAcquisition;
		this.motifAcquisition = motifAcquisition;
		this.numeroAffaire = numeroAffaire;
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

	@Column(name = "DATE_DEBUT")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Nullable
	@Column(name = "DATE_ACQUISITION")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateAcquisition() {
		return dateAcquisition;
	}

	public void setDateAcquisition(@Nullable RegDate dateAcquisition) {
		this.dateAcquisition = dateAcquisition;
	}

	@Nullable
	@Column(name = "MOTIF_ACQUISITION", length = LengthConstants.RF_MOTIF)
	public String getMotifAcquisition() {
		return motifAcquisition;
	}

	public void setMotifAcquisition(@Nullable String motifAcquisition) {
		this.motifAcquisition = motifAcquisition;
	}

	@Nullable
	@Column(name = "NO_AFFAIRE", length = LengthConstants.RF_NO_AFFAIRE)
	@Type(type = "ch.vd.unireg.hibernate.IdentifiantAffaireRFUserType")
	public IdentifiantAffaireRF getNumeroAffaire() {
		return numeroAffaire;
	}

	public void setNumeroAffaire(@Nullable IdentifiantAffaireRF numeroAffaire) {
		this.numeroAffaire = numeroAffaire;
	}

	// configuration hibernate : le droit possède les raison d'acquisition
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "DROIT_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_RAISON_ACQ_RF_DROIT_ID", columnNames = "DROIT_ID")
	public DroitProprieteRF getDroit() {
		return droit;
	}

	public void setDroit(DroitProprieteRF droit) {
		this.droit = droit;
	}

	/**
	 * Ordre naturel : par date croissante (les dates nulles en premier) puis par numéro d'affaire, puis par motif d'acquisition.
	 */
	@Override
	public int compareTo(@NotNull RaisonAcquisitionRF o) {
		int c = REG_DATE_COMPARATOR.compare(this.dateAcquisition, o.getDateAcquisition());
		if (c != 0) {
			return c;
		}
		c = Objects.compare(this.numeroAffaire, o.numeroAffaire, Comparator.nullsFirst(Comparator.naturalOrder()));
		if (c != 0) {
			return c;
		}
		c = Objects.compare(this.motifAcquisition, o.motifAcquisition, Comparator.nullsFirst(Comparator.naturalOrder()));
		return c;
	}
}
