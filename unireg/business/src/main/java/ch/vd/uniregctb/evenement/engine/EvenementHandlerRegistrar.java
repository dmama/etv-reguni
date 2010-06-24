package ch.vd.uniregctb.evenement.engine;

import ch.vd.uniregctb.evenement.common.EvenementCivilHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public interface EvenementHandlerRegistrar {

	public void register(TypeEvenementCivil type, EvenementCivilHandler handler);

}
