package ch.vd.uniregctb.norentes.common;

import java.util.Collection;

import annotation.EtapeAttribute;

public interface NorentesRegistrar {

	public void register(NorentesScenario scenario);

	public Collection<EtapeAttribute> getEtapeAttributes(NorentesScenario scenario);
}
