package ch.vd.unireg.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

/**
 * Représente un bâtiment au registre foncier.
 */
@Entity
@Table(name = "RF_BATIMENT", uniqueConstraints = @UniqueConstraint(name = "IDX_BATIMENT_MASTER_ID_RF", columnNames = "MASTER_ID_RF"))
public class BatimentRF extends HibernateEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Identifiant technique du bâtiment au registre foncier.
	 */
	private String masterIdRF;

	/**
	 * Les descriptions (historisées) du bâtiment
	 */
	private Set<DescriptionBatimentRF> descriptions;

	/**
	 * L'implantation ou les implantations de l'immeuble (données historisées).
	 * <p>
	 * La majorité des bâtiments sont construits sur une seule parcelle. Les bâtiments construits sur plusieurs parcelles
	 * existent néanmoins. Exemples : les garages souterrains qui lient plusieurs bâtiments, les ponts, les tunnels, etc...
	 */
	private Set<ImplantationRF> implantations;

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

	@Column(name = "MASTER_ID_RF", nullable = false, length = LengthConstants.RF_ID_RF)
	public String getMasterIdRF() {
		return masterIdRF;
	}

	public void setMasterIdRF(String masterIdRF) {
		this.masterIdRF = masterIdRF;
	}

	// configuration hibernate : le bâtiment possède les descriptions du bâtiment
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "BATIMENT_ID", nullable = false)
	@ForeignKey(name = "FK_DESCR_BAT_RF_BATIMENT_ID")
	public Set<DescriptionBatimentRF> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(Set<DescriptionBatimentRF> descriptions) {
		this.descriptions = descriptions;
	}

	public void addDescription(@NotNull DescriptionBatimentRF description) {
		if (this.descriptions == null) {
			this.descriptions = new HashSet<>();
		}
		description.setBatiment(this);
		this.descriptions.add(description);
	}

	// configuration hibernate : le bâtiment possède les implantations
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "BATIMENT_ID", nullable = false)
	@ForeignKey(name = "FK_IMPLANTATION_RF_BATIMENT_ID")
	public Set<ImplantationRF> getImplantations() {
		return implantations;
	}

	public void setImplantations(Set<ImplantationRF> implantations) {
		this.implantations = implantations;
	}

	public void addImplantation(@NotNull ImplantationRF implantation) {
		if (this.implantations == null) {
			this.implantations = new HashSet<>();
		}
		implantation.setBatiment(this);
		this.implantations.add(implantation);
	}
}
