package ch.vd.unireg.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public abstract class ImmeubleAvecQuotePartRF extends ImmeubleRF {

	/**
	 * Les surfaces totales (historisées) de l'immeuble.
	 */
	private Set<QuotePartRF> quotesParts;

	// configuration hibernate : l'immeuble possède les quotes-parts
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_QUOTE_PART_RF_IMMEUBLE_ID"))
	public Set<QuotePartRF> getQuotesParts() {
		return quotesParts;
	}

	public void setQuotesParts(Set<QuotePartRF> quotesParts) {
		this.quotesParts = quotesParts;
	}

	public void addQuotePart(QuotePartRF quotePart) {
		if (this.quotesParts == null) {
			this.quotesParts = new HashSet<>();
		}
		quotePart.setImmeuble(this);
		this.quotesParts.add(quotePart);
	}
}
