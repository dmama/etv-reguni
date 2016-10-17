package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateDateRangeEntity;

@Entity
@Table(name = "RF_DROIT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public abstract class DroitRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Le tiers concenré par le droit.
	 */
	private TiersRF tiers;

	/**
	 * L'immeuble concerné par le droit.
	 */
	private ImmeubleRF immeuble;

	/**
	 * Le motif de début du droit.
	 */
	private CodeRF motifDebut;

	/**
	 * Le motif de fin du droit.
	 */
	@Nullable
	private CodeRF motifFin;

	/**
	 * Le numéro d'affaire.
	 */
	private IdentifiantAffaireRF numeroAffaire;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_DROIT_RF_TIERS_ID", columnNames = "TIERS_ID")
	public TiersRF getTiers() {
		return tiers;
	}

	public void setTiers(TiersRF tiers) {
		this.tiers = tiers;
	}

	@ManyToOne
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false, insertable = false, updatable = false)
	@ForeignKey(name = "FK_DROIT_RF_IMMEUBLE_ID")
	@Index(name = "IDX_DROIT_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	@Column(name = "NO_AFFAIRE", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.IdentifiantAffaireRFUserType")
	public IdentifiantAffaireRF getNumeroAffaire() {
		return numeroAffaire;
	}

	public void setNumeroAffaire(IdentifiantAffaireRF numeroAffaire) {
		this.numeroAffaire = numeroAffaire;
	}

	@AttributeOverrides({
			@AttributeOverride(name = "code", column = @Column(name = "MOTIF_DEBUT_CODE", nullable = false)),
			@AttributeOverride(name = "description", column = @Column(name = "MOTIF_DEBUT_DESCRIPTION"))
	})
	public CodeRF getMotifDebut() {
		return motifDebut;
	}

	public void setMotifDebut(CodeRF motifDebut) {
		this.motifDebut = motifDebut;
	}

	@Nullable
	@AttributeOverrides({
			@AttributeOverride(name = "code", column = @Column(name = "MOTIF_FIN_CODE", nullable = false)),
			@AttributeOverride(name = "description", column = @Column(name = "MOTIF_FIN_DESCRIPTION"))
	})
	public CodeRF getMotifFin() {
		return motifFin;
	}

	public void setMotifFin(@Nullable CodeRF motifFin) {
		this.motifFin = motifFin;
	}
}
