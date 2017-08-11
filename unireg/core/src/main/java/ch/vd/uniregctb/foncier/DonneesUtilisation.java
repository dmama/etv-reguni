package ch.vd.uniregctb.foncier;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class DonneesUtilisation {

	/**
	 * Revenu reçu/estimé
	 */
	private Long revenu;

	/**
	 * Volume en mètres-cubes
	 */
	private Long volume;

	/**
	 * Surface en mètres-carrés
	 */
	private Long surface;

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

	public DonneesUtilisation(Long revenu, Long volume, Long surface, BigDecimal pourcentage, BigDecimal pourcentageArrete) {
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
	public Long getRevenu() {
		return revenu;
	}

	public void setRevenu(Long revenu) {
		this.revenu = revenu;
	}

	@Column(name = "VOLUME")
	public Long getVolume() {
		return volume;
	}

	public void setVolume(Long volume) {
		this.volume = volume;
	}

	@Column(name = "SURFACE")
	public Long getSurface() {
		return surface;
	}

	public void setSurface(Long surface) {
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
