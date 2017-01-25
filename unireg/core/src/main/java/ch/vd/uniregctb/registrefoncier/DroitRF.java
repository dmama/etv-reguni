package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
@Table(name = "RF_DROIT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
// TODO (msi) implémenter l'interface LinkedEntity pour retourner les tiers concernés par ce droit
public abstract class DroitRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Identifiant technique de l'immeuble au registre foncier.
	 */
	private String masterIdRF;

	/**
	 * L'ayant-droit concerné par le droit.
	 */
	private AyantDroitRF ayantDroit;

	/**
	 * L'immeuble concerné par le droit.
	 */
	private ImmeubleRF immeuble;

	/**
	 * Le motif de début du droit telle que renseignée dans le registre foncier (la date de début normale est une date technique qui correspond à la date d'import de la donnée).
	 */
	@Nullable
	private RegDate dateDebutOfficielle;

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

	/**
	 * Le numéro d'affaire.
	 */
	@Nullable
	private IdentifiantAffaireRF numeroAffaire;

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

	public void setId(Long id) {
		this.id = id;
	}

	// Note : entre deux imports, on peut recevoir un droit avec le même masterId mais avec des données différentes (certainement des corrections). On ne met pas de contrainte unique donc.
	@Index(name = "IDX_DROIT_MASTER_ID_RF")
	@Column(name = "MASTER_ID_RF", nullable = false, length = LengthConstants.RF_ID_RF)
	public String getMasterIdRF() {
		return masterIdRF;
	}

	public void setMasterIdRF(String masterIdRF) {
		this.masterIdRF = masterIdRF;
	}

	// configuration hibernate : l'ayant-droit ne possède pas les droits (les droits pointent vers les ayants-droits, c'est tout)
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "AYANT_DROIT_ID", nullable = false)
	@Index(name = "IDX_DROIT_RF_AYANT_DROIT_ID", columnNames = "AYANT_DROIT_ID")
	@ForeignKey(name = "FK_DROIT_RF_AYANT_DROIT_ID")
	public AyantDroitRF getAyantDroit() {
		return ayantDroit;
	}

	public void setAyantDroit(AyantDroitRF ayantDroit) {
		this.ayantDroit = ayantDroit;
	}

	// configuration hibernate : l'immeuble ne possède pas les droits (les droits pointent vers les immeubles, c'est tout)
	@ManyToOne
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_DROIT_RF_IMMEUBLE_ID")
	@Index(name = "IDX_DROIT_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	@Nullable
	@Column(name = "NO_AFFAIRE", length = LengthConstants.RF_NO_AFFAIRE)
	@Type(type = "ch.vd.uniregctb.hibernate.IdentifiantAffaireRFUserType")
	public IdentifiantAffaireRF getNumeroAffaire() {
		return numeroAffaire;
	}

	public void setNumeroAffaire(@Nullable IdentifiantAffaireRF numeroAffaire) {
		this.numeroAffaire = numeroAffaire;
	}

	@Nullable
	@Column(name = "DATE_DEBUT_OFFICIELLE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebutOfficielle() {
		return dateDebutOfficielle;
	}

	public void setDateDebutOfficielle(@Nullable RegDate dateDebutOfficielle) {
		this.dateDebutOfficielle = dateDebutOfficielle;
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
	 * <li>l'id de l'ayant-droit</li>
	 * <li>l'id de l'immeuble</li>
	 * </ul>
	 *
	 * @param right un autre droit.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	public int compareTo(@NotNull DroitRF right) {
		int c = DateRangeComparator.compareRanges(this, right);
		if (c != 0) {
			return c;
		}
		c = ObjectUtils.compare(ayantDroit.getId(), right.ayantDroit.getId(), false);
		if (c != 0) {
			return c;
		}
		return ObjectUtils.compare(immeuble.getId(), right.immeuble.getId(), false);
	}
}
