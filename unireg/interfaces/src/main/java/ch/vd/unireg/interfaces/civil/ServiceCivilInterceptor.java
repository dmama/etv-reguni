package ch.vd.unireg.interfaces.civil;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;

public interface ServiceCivilInterceptor {

	void afterGetIndividu(Individu individu, long noIndividu, RegDate date, AttributeIndividu... parties);

	void afterGetIndividus(Collection<Individu> individus, Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties);
}
