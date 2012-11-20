package ch.vd.uniregctb.fors;

import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

public interface AddForRevenuFortuneView extends AddForView {

	MotifFor getMotifDebut();

	MotifFor getMotifFin();

	MotifRattachement getMotifRattachement();
}
