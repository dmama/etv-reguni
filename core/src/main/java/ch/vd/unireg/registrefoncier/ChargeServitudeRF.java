package ch.vd.unireg.registrefoncier;

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
import javax.persistence.UniqueConstraint;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

/**
 * Représente un lien entre un immeuble chargé par une servitude et cette servitude, valide sur une certaine période.
 */
@Entity
@Table(name = "RF_SERVITUDE_IMMEUBLE", uniqueConstraints = @UniqueConstraint(name = "IDX_SERVITUDE_IMMEUBLE_ID", columnNames = {"DROIT_ID", "IMMEUBLE_ID"}))
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT")),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class ChargeServitudeRF extends HibernateDateRangeEntity implements LinkedEntity, Duplicable<ChargeServitudeRF> {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * La servitude concernée
	 */
	private ServitudeRF servitude;

	/**
	 * L'immeuble grevé
	 */
	private ImmeubleRF immeuble;

	public ChargeServitudeRF() {
	}

	public ChargeServitudeRF(RegDate dateDebut, RegDate dateFin, ServitudeRF servitude, ImmeubleRF immeuble) {
		super(dateDebut, dateFin);
		this.servitude = servitude;
		this.immeuble = immeuble;
	}

	public ChargeServitudeRF(@NotNull ChargeServitudeRF right) {
		super(right);
		this.servitude = right.getServitude();
		this.immeuble = right.getImmeuble();
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

	public void setId(Long id) {
		this.id = id;
	}

	// configuration hibernate : la servitude possède les liens
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "DROIT_ID", insertable = false, updatable = false, nullable = false)
	public ServitudeRF getServitude() {
		return servitude;
	}

	public void setServitude(ServitudeRF servitude) {
		this.servitude = servitude;
	}

	// configuration hibernate : l'immeuble ne possède pas les liens
	@ManyToOne
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_SERV_IMM_RF_IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return null;
	}

	@Transient
	@Override
	public ChargeServitudeRF duplicate() {
		return new ChargeServitudeRF(this);
	}
}
