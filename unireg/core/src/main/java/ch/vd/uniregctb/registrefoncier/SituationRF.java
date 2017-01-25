package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;

/**
 * Situation d'un immeuble inscrit au registre foncier.
 */
@Entity
@Table(name = "RF_SITUATION")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class SituationRF extends HibernateDateRangeEntity implements LinkedEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Le numéro de la parcelle sur laquelle est construit l'immeuble.
	 */
	private int noParcelle;

	/**
	 * Numéro d'index de premier niveau.
	 */
	private Integer index1;

	/**
	 * Numéro d'index de deuxième niveau.
	 */
	private Integer index2;

	/**
	 * Numéro d'index de troisième niveau.
	 */
	private Integer index3;

	/**
	 * La commune sur laquelle est sis l'immeuble.
	 */
	private CommuneRF commune;

	/**
	 * L'immeuble concerné par la situation.
	 */
	private ImmeubleRF immeuble;

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

	@Column(name = "NO_PARCELLE", nullable = false)
	public int getNoParcelle() {
		return noParcelle;
	}

	public void setNoParcelle(int noParcelle) {
		this.noParcelle = noParcelle;
	}

	@Column(name = "INDEX1")
	public Integer getIndex1() {
		return index1;
	}

	public void setIndex1(Integer index1) {
		this.index1 = index1;
	}

	@Column(name = "INDEX2")
	public Integer getIndex2() {
		return index2;
	}

	public void setIndex2(Integer index2) {
		this.index2 = index2;
	}

	@Column(name = "INDEX3")
	public Integer getIndex3() {
		return index3;
	}

	public void setIndex3(Integer index3) {
		this.index3 = index3;
	}

	// configuration hibernate : la situation pointe vers une commune, mais ne la possède pas
	@ManyToOne
	@JoinColumn(name = "COMMUNE_ID", nullable = false)
	@ForeignKey(name = "FK_SITUATION_RF_COMMUNE_ID")
	@Index(name = "IDX_SITUATION_RF_COMMUNE_ID", columnNames = "COMMUNE_ID")
	public CommuneRF getCommune() {
		return commune;
	}

	public void setCommune(CommuneRF commune) {
		this.commune = commune;
	}

	// configuration hibernate : l'immeuble possède les situations
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "IMMEUBLE_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_SIT_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	/**
	 * Compare la situation courante avec une autre situation. Les propriétés utilisées pour la comparaison sont :
	 * <ul>
	 *     <li>les dates de début et de fin</li>
	 *     <li>le numéro Ofs de commune</li>
	 *     <li>le numéro de parcelle</li>
	 *     <li>les indexes PPE</li>
	 * </ul>
	 * @param right une autre situation.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	public int compareTo(@NotNull SituationRF right) {
		int c = DateRangeComparator.compareRanges(this, right);
		if (c != 0) {
			return c;
		}
		c = Integer.compare(commune.getNoOfs(), right.commune.getNoOfs());
		if (c != 0) {
			return c;
		}
		c = Integer.compare(noParcelle, right.noParcelle);
		if (c != 0) {
			return c;
		}
		c = ObjectUtils.compare(index1, right.index1, false);
		if (c != 0) {
			return c;
		}
		c = ObjectUtils.compare(index2, right.index2, false);
		if (c != 0) {
			return c;
		}
		return ObjectUtils.compare(index3, right.index3, false);
	}

	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return Collections.singletonList(immeuble);
	}
}
