package ch.vd.uniregctb.evenement.externe;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;

public interface EvenementExterneProcessor {

	TraiterEvenementExterneResult traiteEvenementsExternes(RegDate dateTraitement, int nbThreads, @Nullable StatusManager status);
}
