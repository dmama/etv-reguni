package ch.vd.unireg.foncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;

@Entity
@Table(name = "ALLEGEMENT_FONCIER")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE_ALLEGEMENT", length = LengthConstants.AFONC_TYPE, discriminatorType = DiscriminatorType.STRING)
public abstract class AllegementFoncier extends HibernateDateRangeEntity implements LinkedEntity {

	private Long id;
	private ImmeubleRF immeuble;
	private ContribuableImpositionPersonnesMorales contribuable;

	/**
	 * Type d'impôt disponible pour un allègement foncier
	 * (l'idée est que pour un type d'impôt donné, il ne doit pas y avoir de recouvrement sur un immeuble donné)
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

	public AllegementFoncier() {
	}

	protected AllegementFoncier(AllegementFoncier src) {
		super(src);
		this.immeuble = src.getImmeuble();
		this.contribuable = src.getContribuable();
	}

	@Transient
	public abstract TypeImpot getTypeImpot();

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	@Column(name = "ID", nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "IMMEUBLE_ID", foreignKey = @ForeignKey(name = "FK_AFONC_RF_IMMEUBLE_ID"))
	@Index(name = "IDX_AFONC_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	@ManyToOne
	@JoinColumn(name = "CTB_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_AFONC_CTB_ID", columnNames = "CTB_ID")
	public ContribuableImpositionPersonnesMorales getContribuable() {
		return contribuable;
	}

	public void setContribuable(ContribuableImpositionPersonnesMorales contribuable) {
		this.contribuable = contribuable;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return contribuable != null ? Collections.singletonList(contribuable) : null;
	}
}
