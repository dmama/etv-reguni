package ch.vd.uniregctb.fors;

import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

public interface EditForRevenuFortuneView extends EditForView {

	MotifFor getMotifDebut();

	MotifFor getMotifFin();

	MotifRattachement getMotifRattachement();
}
