package ch.vd.uniregctb.foncier;

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
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.LinkedEntity;

@Entity
@Table(name = "ALLEGEMENT_FONCIER")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE_ALLEGEMENT", length = LengthConstants.AFONC_TYPE, discriminatorType = DiscriminatorType.STRING)
public abstract class AllegementFoncier extends HibernateDateRangeEntity implements LinkedEntity {

	private Long id;
	private ImmeubleRF immeuble;
	private Contribuable contribuable;

	/**
	 * Type d'impôt disponible pour un allègement foncier
	 * (l'idée est que pour un type d'impôt donnée, il ne doit pas y avoir de recouvrement sur un immeuble donné)
	 */
	public enum TypeImpot {
		/**
		 * Impôt foncier
		 */
		IFONC,

		/**
		 * Impôt complémentaire sur immeuble
		 */
		ICI
	}

	@Transient
	public abstract TypeImpot getTypeImpot();

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "IMMEUBLE_ID")
	@ForeignKey(name = "FK_AFONC_RF_IMMEUBLE_ID")
	@Index(name = "IDX_AFONC_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	@ManyToOne
	@JoinColumn(name = "CTB_ID")
	@ForeignKey(name = "FK_AFONC_CTB_ID")
	@Index(name = "IDX_AFONC_CTB_ID", columnNames = "CTB_ID")
	public Contribuable getContribuable() {
		return contribuable;
	}

	public void setContribuable(Contribuable contribuable) {
		this.contribuable = contribuable;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return contribuable != null ? Collections.singletonList(contribuable) : null;
	}
}
