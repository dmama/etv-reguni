package ch.vd.unireg.declaration.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.declaration.LiberationDeclaration;
import ch.vd.unireg.declaration.LiberationQuestionnaireSNC;
import ch.vd.unireg.documentfiscal.LiberationDocumentFiscal;

public class LiberationDocumentFiscalView implements Comparable<LiberationDocumentFiscalView>, Annulable {

	private Long id;
	private final RegDate dateLiberation;
	private final String motif;
	private final String LogCreationUser;
	private final String entity;
	private final boolean annule;

	public LiberationDocumentFiscalView(LiberationDocumentFiscal liberation) {
		this.id = liberation.getId();
		this.LogCreationUser = liberation.getLogCreationUser();
		this.dateLiberation = liberation.getDateLiberation();
		this.annule = liberation.isAnnule();
		this.motif = liberation instanceof LiberationDeclaration ? ((LiberationDeclaration) liberation).getMotif() : ((LiberationQuestionnaireSNC) liberation).getMotif();
		this.entity = liberation instanceof LiberationDeclaration ? LiberationDeclaration.class.getSimpleName() : LiberationQuestionnaireSNC.class.getSimpleName();
	}

	public RegDate getDateLiberation() {
		return dateLiberation;
	}

	public String getMotif() {
		return motif;
	}

	public String getLogCreationUser() {
		return LogCreationUser;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEntity() {
		return entity;
	}

	@Override
	public boolean isAnnule() {
		return annule;
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
