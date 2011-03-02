package ch.vd.uniregctb.evenement.civil.engine;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public interface EvenementHandlerRegistrar {

	public void register(TypeEvenementCivil type, EvenementCivilHandler handler);

}
