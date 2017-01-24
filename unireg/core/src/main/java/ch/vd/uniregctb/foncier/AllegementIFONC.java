package ch.vd.uniregctb.foncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue(value = "AllegementIFONC")
public class AllegementIFONC extends AllegementFoncier {

	/**
	 * Pourcentage d'allègement pour l'impôt foncier (0-100, 2 décimales)
	 */
	private BigDecimal pourcentageAllegement;

	public AllegementIFONC() {
	}

	@Column(name = "IFONC_POURCENT_ALLGT", precision = 5, scale = 2)
	public BigDecimal getPourcentageAllegement() {
		return pourcentageAllegement;
	}

	public void setPourcentageAllegement(BigDecimal pourcentageAllegement) {
		this.pourcentageAllegement = pourcentageAllegement;
	}
}
