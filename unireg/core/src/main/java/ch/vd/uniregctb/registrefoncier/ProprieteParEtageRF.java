package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.jetbrains.annotations.Nullable;

@Entity
@DiscriminatorValue("ProprieteParEtage")
public class ProprieteParEtageRF extends ImmeubleRF implements ImmeubleAvecQuotePartRF {

	/**
	 * La quote-part de l'immeuble
	 */
	@Nullable
	private Fraction quotePart;

	@Nullable
	@AttributeOverrides({
			@AttributeOverride(name = "numerateur", column = @Column(name = "QUOTE_PART_NUM")),
			@AttributeOverride(name = "denominateur", column = @Column(name = "QUOTE_PART_DENOM"))
	})
	@Override
	public Fraction getQuotePart() {
		return quotePart;
	}

	public void setQuotePart(@Nullable Fraction quotePart) {
		this.quotePart = quotePart;
	}
}
