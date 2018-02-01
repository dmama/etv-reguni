package ch.vd.unireg.norentes.common;

import java.util.Collection;

import ch.vd.unireg.norentes.annotation.EtapeAttribute;

public interface NorentesRegistrar {

	public void register(NorentesScenario scenario);

	public Collection<EtapeAttribute> getEtapeAttributes(NorentesScenario scenario);
}
