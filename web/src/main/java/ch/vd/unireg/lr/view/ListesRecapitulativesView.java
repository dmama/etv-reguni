package ch.vd.uniregctb.lr.view;

import java.util.List;
import java.util.stream.Collectors;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;

public class ListesRecapitulativesView {

	private final long idDebiteur;
	private final List<ListeRecapitulativeView> lrs;

	public ListesRecapitulativesView(DebiteurPrestationImposable dpi) {
		this.idDebiteur = dpi.getNumero();
		this.lrs = dpi.getDeclarationsTriees(DeclarationImpotSource.class, true).stream()
				.sorted(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>(DateRangeComparator.CompareOrder.DESCENDING)))
				.map(ListeRecapitulativeView::new)
				.collect(Collectors.toList());
	}

	public long getIdDebiteur() {
		return idDebiteur;
	}

	public List<ListeRecapitulativeView> getLrs() {
		return lrs;
	}
}
