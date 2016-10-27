package ch.vd.uniregctb.etiquette;

import ch.vd.uniregctb.common.BaseDAOImpl;

public class EtiquetteDAOImpl extends BaseDAOImpl<Etiquette, Long> implements EtiquetteDAO {

	public EtiquetteDAOImpl() {
		super(Etiquette.class);
	}
}
