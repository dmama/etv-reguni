package ch.vd.unireg.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;

@Entity
@Table(name = "RF_COMMUNE", uniqueConstraints = @UniqueConstraint(name = "IDx_COMMUNE_NO_OFS", columnNames = "NO_OFS"))
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT")),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class CommuneRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Le numéro RF de la commune
	 *
	 * TODO (msi) : à supprimer quand le basculement aux numéros Ofs sera effectif en production (voir le SIFISC-30558)
	 */
	private int noRf;

	/**
	 * Le nom de la commune au registre foncier.
	 */
	private String nomRf;

	/**
	 * Le numéro OFS de la commune
	 */
	private int noOfs;

	public CommuneRF() {
	}

	public CommuneRF(int noRf, String nomRf, int noOfs) {
		this.noRf = noRf;
		this.nomRf = nomRf;
		this.noOfs = noOfs;
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

	@Column(name = "NO_RF", nullable = false)
	public int getNoRf() {
		return noRf;
	}

	public void setNoRf(int noRf) {
		this.noRf = noRf;
	}

	@Column(name = "NOM_RF", nullable = false, length = LengthConstants.RF_NOM_COMMUNE)
	public String getNomRf() {
		return nomRf;
	}

	public void setNomRf(String nomRf) {
		this.nomRf = nomRf;
	}

	@Column(name = "NO_OFS", nullable = false)
	public int getNoOfs() {
		return noOfs;
	}

	public void setNoOfs(int noOfs) {
		this.noOfs = noOfs;
	}
}
