package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Set;

import org.hibernate.annotations.Index;

import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;

/**
 * Représente un bâtiment au registre foncier.
 */
@Entity
@Table(name = "RF_BATIMENT")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class BatimentRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Identifiant technique du bâtiment au registre foncier.
	 */
	private String idRF;

	/**
	 * Le type de bâtiment.
	 */
	private CodeRF type;

	/**
	 * La surface en mètre carrés (m2).
	 */
	private int surface;

	/**
	 * Le ou les immeubles sur lesquels le bâtiment est construit.
	 * <p>
	 * La majorité des bâtiments sont construits sur une seule parcelle. Les bâtiments construits sur plusieurs parcelles
	 * existent néanmoins. Exemples : les garages souterrains qui lient plusieurs bâtiments, les ponts, les tunnels, etc...
	 */
	private Set<ImmeubleRF> immeubles;

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

	@Index(name = "IDX_BATIMENT_ID_RF")
	@Column(name = "ID_RF", nullable = false, length = LengthConstants.RF_ID_RF)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	@AttributeOverrides({
			@AttributeOverride(name = "code", column = @Column(name = "TYPE_CODE", length = LengthConstants.RF_CODE, nullable = false)),
			@AttributeOverride(name = "description", column = @Column(name = "TYPE_DESCRIPTION", length = LengthConstants.RF_CODE_DESCRIPTION))
	})
	public CodeRF getType() {
		return type;
	}

	public void setType(CodeRF type) {
		this.type = type;
	}

	@Column(name = "SURFACE", nullable = false)
	public int getSurface() {
		return surface;
	}

	public void setSurface(int surface) {
		this.surface = surface;
	}

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "batiments", targetEntity = ImmeubleRF.class)
	public Set<ImmeubleRF> getImmeubles() {
		return immeubles;
	}

	public void setImmeubles(Set<ImmeubleRF> immeubles) {
		this.immeubles = immeubles;
	}
}
