package ch.vd.unireg.declaration.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.LiberationDeclaration;
import ch.vd.unireg.documentfiscal.LiberationDocumentFiscal;

public class LiberationDocumentFiscalView implements Comparable<LiberationDocumentFiscalView> {

	private Long id;
	private final RegDate dateLiberation;
	private final String motif;
	private final String logModifUser;

	public LiberationDocumentFiscalView(LiberationDocumentFiscal liberation) {
		this.id = liberation.getId();
		this.logModifUser = liberation.getLogModifUser();
		this.dateLiberation = liberation.getDateLiberation();
		this.motif = liberation instanceof LiberationDeclaration ? ((LiberationDeclaration) liberation).getMotif() : null;
	}

	public RegDate getDateLiberation() {
		return dateLiberation;
	}

	public String getMotif() {
		return motif;
	}

	public String getLogModifUser() {
		return logModifUser;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Compare d'apres la date de LiberationDocumentFiscalView
	 *
	 * @see Comparable#compareTo(Object)
	 */
	@Override
	public int compareTo(@NotNull LiberationDocumentFiscalView liberationDocumentFiscalView) {
		int comparison = -dateLiberation.compareTo(liberationDocumentFiscalView.dateLiberation);
		if (comparison == 0) {
			comparison = -Long.compare(id, liberationDocumentFiscalView.id);
		}
		return comparison;
	}
}
