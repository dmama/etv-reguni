package ch.vd.uniregctb.etiquette;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.Tiers;

@Entity
@Table(name = "ETIQUETTE_TIERS")
public class EtiquetteTiers extends HibernateDateRangeEntity implements LinkedEntity, Duplicable<EtiquetteTiers> {

	private Long id;
	private Etiquette etiquette;
	private Tiers tiers;
	private String commentaire;

	public EtiquetteTiers() {
	}

	public EtiquetteTiers(RegDate dateDebut, RegDate dateFin, Etiquette etiquette) {
		super(dateDebut, dateFin);
		this.etiquette = etiquette;
	}

	private EtiquetteTiers(EtiquetteTiers src) {
		super(src);
		this.etiquette = src.etiquette;
		this.commentaire = src.commentaire;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "ETIQUETTE_ID", nullable = false)
	@Index(name = "IDX_ETIQTIERS_ETIQ_ID", columnNames = "ETIQUETTE_ID")
	@ForeignKey(name = "FK_ETIQTIERS_ETIQ_ID")
	public Etiquette getEtiquette() {
		return etiquette;
	}

	public void setEtiquette(Etiquette etiquette) {
		this.etiquette = etiquette;
	}

	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_ETIQTIERS_TIERS_ID", columnNames = "TIERS_ID")
	@ForeignKey(name = "FK_ETIQTIERS_TIERS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	@Column(name = "COMMENTAIRE", length = LengthConstants.ETIQUETTE_TIERS_COMMENTAIRE)
	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return tiers == null ? null : Collections.singletonList(tiers);
	}

	@Override
	public EtiquetteTiers duplicate() {
		return new EtiquetteTiers(this);
	}
}
