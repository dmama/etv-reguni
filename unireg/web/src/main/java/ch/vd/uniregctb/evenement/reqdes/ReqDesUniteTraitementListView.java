package ch.vd.uniregctb.evenement.reqdes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.UniteTraitement;

public class ReqDesUniteTraitementListView extends ReqDesUniteTraitementAbstractView {

	private static final long serialVersionUID = 2121223555971797510L;

	private final NomPrenom partiePrenante1;
	private final NomPrenom partiePrenante2;

	public ReqDesUniteTraitementListView(UniteTraitement ut) {
		super(ut);

		final List<PartiePrenante> partiesPrenantes = new ArrayList<>(ut.getPartiesPrenantes());
		partiesPrenantes.sort(new Comparator<PartiePrenante>() {
			@Override
			public int compare(PartiePrenante o1, PartiePrenante o2) {
				return Long.compare(o1.getId(), o2.getId());
			}
		});
		if (!partiesPrenantes.isEmpty()) {
			final PartiePrenante pp1 = partiesPrenantes.get(0);
			partiePrenante1 = new NomPrenom(pp1.getNom(), pp1.getPrenoms());
			if (partiesPrenantes.size() > 1) {
				final PartiePrenante pp2 = partiesPrenantes.get(1);
				partiePrenante2 = new NomPrenom(pp2.getNom(), pp2.getPrenoms());
			}
			else {
				partiePrenante2 = null;
			}
		}
		else {
			partiePrenante1 = null;
			partiePrenante2 = null;
		}
	}

	public NomPrenom getPartiePrenante1() {
		return partiePrenante1;
	}

	public NomPrenom getPartiePrenante2() {
		return partiePrenante2;
	}
}
