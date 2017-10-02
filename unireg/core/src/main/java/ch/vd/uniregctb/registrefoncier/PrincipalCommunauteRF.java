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

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;

@Entity
@Table(name = "RF_PRINCIPAL_COMMUNAUTE")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public class PrincipalCommunauteRF extends HibernateDateRangeEntity implements LinkedEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	private ModeleCommunauteRF modeleCommunaute;

	/**
	 * Le principal valide pendant la période considérée
	 */
	private TiersRF principal;

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

	// configuration hibernate : le regroupement de communauté possède les principaux de communauté
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "MODEL_COMMUNAUTE_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_PRINC_MODCOMM_ID", columnNames = "MODEL_COMMUNAUTE_ID")
	public ModeleCommunauteRF getModeleCommunaute() {
		return modeleCommunaute;
	}

	public void setModeleCommunaute(ModeleCommunauteRF modeleCommunaute) {
		this.modeleCommunaute = modeleCommunaute;
	}

	// configuration hibernate : l'ayant-droit ne possède pas les principaux de communauté
	@ManyToOne
	@JoinColumn(name = "PRINCIPAL_ID", nullable = false)
	@ForeignKey(name = "FK_PRINCIPAL_ID")
	@Index(name = "IDX_PRINC_PRINCIPAL_ID", columnNames = "PRINCIPAL_ID")
	public TiersRF getPrincipal() {
		return principal;
	}

	public void setPrincipal(TiersRF principal) {
		this.principal = principal;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntity.Context context, boolean includeAnnuled) {
		// si le principal de communauté change (création, modification ou annulation), on veut notifier que le modèle correspondant a changé
		return modeleCommunaute == null ? null : Collections.singletonList(modeleCommunaute);
	}
}
