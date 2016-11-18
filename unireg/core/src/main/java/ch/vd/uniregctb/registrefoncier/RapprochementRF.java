package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.Rerangeable;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeRapprochementRF;

@Entity
@Table(name = "RAPPROCHEMENT_RF")
@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true))
public class RapprochementRF extends HibernateDateRangeEntity implements Duplicable<RapprochementRF>, Rerangeable<RapprochementRF> {

	private Long id;
	private TypeRapprochementRF typeRapprochement;
	private TiersRF tiersRF;
	private Contribuable contribuable;

	public RapprochementRF() {
	}

	private RapprochementRF(RapprochementRF source) {
		super(source);
		this.typeRapprochement = source.typeRapprochement;
		this.tiersRF = source.tiersRF;
		this.contribuable = source.contribuable;
	}

	private RapprochementRF(RapprochementRF source, DateRange newRange) {
		super(newRange.getDateDebut(), newRange.getDateFin());
		this.typeRapprochement = source.typeRapprochement;
		this.tiersRF = source.tiersRF;
		this.contribuable = source.contribuable;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "TYPE_RAPPROCHEMENT", nullable = false, length = LengthConstants.RAPPROCHEMENT_RF_TYPE)
	@Enumerated(EnumType.STRING)
	public TypeRapprochementRF getTypeRapprochement() {
		return typeRapprochement;
	}

	public void setTypeRapprochement(TypeRapprochementRF typeRapprochement) {
		this.typeRapprochement = typeRapprochement;
	}

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "RF_TIERS_ID", nullable = false)
	@Index(name = "IDX_RFAPP_RFTIERS_ID")
	@ForeignKey(name = "FK_RFAPP_RFTIERS_ID")
	public TiersRF getTiersRF() {
		return tiersRF;
	}

	public void setTiersRF(TiersRF tiersRF) {
		this.tiersRF = tiersRF;
	}

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "CTB_ID", nullable = false)
	@Index(name = "IDX_RFAPP_CTB_ID")
	@ForeignKey(name = "FK_RAPPRF_CTB_ID")
	public Contribuable getContribuable() {
		return contribuable;
	}

	public void setContribuable(Contribuable contribuable) {
		this.contribuable = contribuable;
	}

	@Transient
	@Override
	public RapprochementRF duplicate() {
		return new RapprochementRF(this);
	}

	@Transient
	@Override
	public RapprochementRF rerange(DateRange range) {
		return new RapprochementRF(this, range);
	}
}
