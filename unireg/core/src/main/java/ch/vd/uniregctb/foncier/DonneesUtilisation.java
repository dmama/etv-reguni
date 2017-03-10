package ch.vd.uniregctb.foncier;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class DonneesUtilisation {

	/**
	 * Revenu reçu/estimé
	 */
	private Integer revenu;

	/**
	 * Volume en mètres-cubes
	 */
	private Integer volume;

	/**
	 * Surface en mètres-carrés
	 */
	private Integer surface;

	/**
	 * Pourcentage (0-100, 2 décimales) déclaré
	 */
	private BigDecimal pourcentage;

	/**
	 * Pourcentage (0-100, 2 décimales) arrêté
	 */
	private BigDecimal pourcentageArrete;

	public DonneesUtilisation() {
	}

	public DonneesUtilisation(Integer revenu, Integer volume, Integer surface, BigDecimal pourcentage, BigDecimal pourcentageArrete) {
		this.revenu = revenu;
		this.volume = volume;
		this.surface = surface;
		this.pourcentage = pourcentage;
		this.pourcentageArrete = pourcentageArrete;
	}

	public DonneesUtilisation(DonneesUtilisation src) {
		this(src.revenu, src.volume, src.surface, src.pourcentage, src.pourcentageArrete);
	}

	@Column(name = "REVENU")
	public Integer getRevenu() {
		return revenu;
	}

	public void setRevenu(Integer revenu) {
		this.revenu = revenu;
	}

	@Column(name = "VOLUME")
	public Integer getVolume() {
		return volume;
	}

	public void setVolume(Integer volume) {
		this.volume = volume;
	}

	@Column(name = "SURFACE")
	public Integer getSurface() {
		return surface;
	}

	public void setSurface(Integer surface) {
		this.surface = surface;
	}

	@Column(name = "POURCENTAGE", precision = 5, scale = 2)
	public BigDecimal getPourcentage() {
		return pourcentage;
	}

	public void setPourcentage(BigDecimal pourcentage) {
		this.pourcentage = pourcentage;
	}

	@Column(name = "POURCENTAGE_ARRETE", precision = 5, scale = 2)
	public BigDecimal getPourcentageArrete() {
		return pourcentageArrete;
	}

	public void setPourcentageArrete(BigDecimal pourcentageArrete) {
		this.pourcentageArrete = pourcentageArrete;
	}
}
