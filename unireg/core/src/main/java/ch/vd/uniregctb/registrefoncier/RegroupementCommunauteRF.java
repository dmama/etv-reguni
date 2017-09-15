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

/**
 * Lien entre une communauté RF (issue de Capitastra) et une communauté de référence (données propres à Unireg)
 */
@Entity
@Table(name = "REGROUPEMENT_COMMUNAUTE_RF")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public class RegroupementCommunauteRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * La communauté RF
	 */
	private CommunauteRF communaute;

	/**
	 * Le modèle de communauté associé.
	 */
	private ModeleCommunauteRF modele;

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

	// configuration hibernate : la communauté possède les regroupements
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "COMMUNAUTE_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_REGRCOMM_RF_COMMUNAUTE_ID", columnNames = "COMMUNAUTE_ID")
	public CommunauteRF getCommunaute() {
		return communaute;
	}

	public void setCommunaute(CommunauteRF communaute) {
		this.communaute = communaute;
	}

	// configuration hibernate : le modèle ne possède pas les regroupements
	@ManyToOne
	@JoinColumn(name = "MODEL_ID", nullable = false)
	@ForeignKey(name = "FK_REGRCOMM_RF_MODEL_ID")
	@Index(name = "IDX_REGRCOMM_RF_MODEL_ID", columnNames = "MODEL_ID")
	public ModeleCommunauteRF getModele() {
		return modele;
	}

	public void setModele(ModeleCommunauteRF modele) {
		this.modele = modele;
	}
}
