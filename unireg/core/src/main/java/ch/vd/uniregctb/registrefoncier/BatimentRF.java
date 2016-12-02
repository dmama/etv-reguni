package ch.vd.uniregctb.registrefoncier;

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
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.LengthConstants;

/**
 * Représente un bâtiment au registre foncier.
 */
@Entity
@Table(name = "RF_BATIMENT")
public class BatimentRF {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Identifiant technique du bâtiment au registre foncier.
	 */
	private String masterIdRF;

	/**
	 * Les surfaces (historisées) du bâtiment
	 */
	private Set<SurfaceBatimentRF> surfaces;

	/**
	 * L'implantation ou les implantations de l'immeuble (données historisées).
	 * <p>
	 * La majorité des bâtiments sont construits sur une seule parcelle. Les bâtiments construits sur plusieurs parcelles
	 * existent néanmoins. Exemples : les garages souterrains qui lient plusieurs bâtiments, les ponts, les tunnels, etc...
	 */
	private Set<ImplantationRF> implantations;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Index(name = "IDX_BATIMENT_MASTER_ID_RF")
	@Column(name = "MASTER_ID_RF", nullable = false, length = LengthConstants.RF_ID_RF)
	public String getMasterIdRF() {
		return masterIdRF;
	}

	public void setMasterIdRF(String masterIdRF) {
		this.masterIdRF = masterIdRF;
	}

	// configuration hibernate : le bâtiment possède les surfaces du bâtiment
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "BATIMENT_ID", nullable = false)
	@ForeignKey(name = "FK_SURF_BAT_RF_BATIMENT_ID")
	public Set<SurfaceBatimentRF> getSurfaces() {
		return surfaces;
	}

	public void setSurfaces(Set<SurfaceBatimentRF> surfaces) {
		this.surfaces = surfaces;
	}

	public void addSurface(@NotNull SurfaceBatimentRF surface) {
		if (this.surfaces == null) {
			this.surfaces = new HashSet<>();
		}
		surface.setBatiment(this);
		this.surfaces.add(surface);
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
