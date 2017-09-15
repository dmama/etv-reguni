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

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.uniregctb.common.HibernateDateRangeEntity;

@Entity
@Table(name = "PRINCIPAL_COMMUNAUTE_RF")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public class PrincipalCommunauteRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	private ModeleCommunauteRF regroupementCommunaute;

	/**
	 * Le principal valide pendant la période considérée
	 */
	private AyantDroitRF principal;

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
	public ModeleCommunauteRF getRegroupementCommunaute() {
		return regroupementCommunaute;
	}

	public void setRegroupementCommunaute(ModeleCommunauteRF regroupementCommunaute) {
		this.regroupementCommunaute = regroupementCommunaute;
	}

	// configuration hibernate : l'ayant-droit ne possède pas les principaux de communauté
	@ManyToOne
	@JoinColumn(name = "PRINCIPAL_ID", nullable = false)
	@ForeignKey(name = "FK_PRINCIPAL_ID")
	@Index(name = "IDX_PRINC_PRINCIPAL_ID", columnNames = "PRINCIPAL_ID")
	public AyantDroitRF getPrincipal() {
		return principal;
	}

	public void setPrincipal(AyantDroitRF principal) {
		this.principal = principal;
	}
}
