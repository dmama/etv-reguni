package ch.vd.uniregctb.evenement.externe;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;

public interface EvenementExterneProcessor {

	TraiterEvenementExterneResult traiteEvenementExterne(RegDate dateTraitement, int nbThreads, StatusManager status);  
}
