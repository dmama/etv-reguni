package ch.vd.unireg.evenement.reqdes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.unireg.reqdes.ErreurTraitement;
import ch.vd.unireg.reqdes.PartiePrenante;
import ch.vd.unireg.reqdes.UniteTraitement;

public class ReqDesUniteTraitementDetailedView extends ReqDesUniteTraitementAbstractView {

	private static final long serialVersionUID = 2640299109532759289L;

	private final List<ErreurTraitementView> erreurs;
	private final List<PartiePrenanteView> partiesPrenantes;

	public ReqDesUniteTraitementDetailedView(UniteTraitement ut) {
		super(ut);

		// le détail des erreurs
		if (ut.getErreurs() == null || ut.getErreurs().isEmpty()) {
			this.erreurs = Collections.emptyList();
		}
		else {
			this.erreurs = new ArrayList<>(ut.getErreurs().size());
			for (ErreurTraitement et : ut.getErreurs()) {
				this.erreurs.add(new ErreurTraitementView(et));
			}
		}

		// la ou les parties prenantes
		if (ut.getPartiesPrenantes() == null || ut.getPartiesPrenantes().isEmpty()) {
			this.partiesPrenantes = Collections.emptyList();
		}
		else {
			this.partiesPrenantes = new ArrayList<>(ut.getPartiesPrenantes().size());
			for (PartiePrenante pp : ut.getPartiesPrenantes()) {
				this.partiesPrenantes.add(new PartiePrenanteView(pp));
			}
			this.partiesPrenantes.sort(new Comparator<PartiePrenanteView>() {
				@Override
				public int compare(PartiePrenanteView o1, PartiePrenanteView o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});
		}
	}

	public List<ErreurTraitementView> getErreurs() {
		return erreurs;
	}

	public List<PartiePrenanteView> getPartiesPrenantes() {
		return partiesPrenantes;
	}
}
