package ch.vd.unireg.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

/**
 * Représente un lien entre un ayant-droit bénéficiaire de servitude et cette servitude, valide sur une certaine période.
 */
@Entity
@Table(name = "RF_SERVITUDE_AYANT_DROIT")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT")),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class BeneficeServitudeRF extends HibernateDateRangeEntity implements LinkedEntity, Duplicable<BeneficeServitudeRF> {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * La servitude concernée
	 */
	private ServitudeRF servitude;

	/**
	 * L'ayant-droit concerné
	 */
	private AyantDroitRF ayantDroit;

	public BeneficeServitudeRF() {
	}

	public BeneficeServitudeRF(RegDate dateDebut, RegDate dateFin, ServitudeRF servitude, AyantDroitRF ayantDroit) {
		super(dateDebut, dateFin);
		this.servitude = servitude;
		this.ayantDroit = ayantDroit;
	}

	public BeneficeServitudeRF(@NotNull BeneficeServitudeRF right) {
		super(right);
		this.servitude = right.getServitude();
		this.ayantDroit = right.getAyantDroit();
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

	// configuration hibernate : la servitude possède les bénéfices de servitude
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

	// configuration hibernate : l'ayant-droit ne possède pas les bénéfices de servitude
	@ManyToOne
	@JoinColumn(name = "AYANT_DROIT_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_SERV_AD_RF_AYANT_DROIT_ID"))
	public AyantDroitRF getAyantDroit() {
		return ayantDroit;
	}

	public void setAyantDroit(AyantDroitRF ayantDroit) {
		this.ayantDroit = ayantDroit;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return Arrays.asList(servitude, ayantDroit);
	}

	@Transient
	@Override
	public BeneficeServitudeRF duplicate() {
		return new BeneficeServitudeRF(this);
	}
}
