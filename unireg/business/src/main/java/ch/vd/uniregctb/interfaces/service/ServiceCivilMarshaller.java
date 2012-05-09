package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Service civil qui permet de choisir l'implémentation RegPP ou RcPers à utiliser
 */
public class ServiceCivilMarshaller implements ServiceCivilRaw, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilMarshaller.class);

	private UniregModeHelper modeHelper;
	private ServiceCivilRaw target;
	private ServiceCivilRaw regpp;
	private ServiceCivilRaw rcpers;

	@SuppressWarnings("UnusedDeclaration")
	public void setModeHelper(UniregModeHelper modeHelper) {
		this.modeHelper = modeHelper;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setRegpp(ServiceCivilRaw regpp) {
		this.regpp = regpp;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setRcpers(ServiceCivilRaw rcpers) {
		this.rcpers = rcpers;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final String targetName = modeHelper.getServiceCivilSource();
		if ("regpp".equalsIgnoreCase(targetName)) {
			LOGGER.info("Utilisation du service civil RegPP.");
			target = regpp;
		}
		else if ("rcpers".equalsIgnoreCase(targetName)) {
			LOGGER.info("Utilisation du service civil RcPers.");
			target = rcpers;
		}
		else {
			throw new IllegalArgumentException("La valeur [" + targetName + "] est incorrect pour le choix du service civil");
		}
	}

	@Override
	public Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) {
		return target.getIndividu(noIndividu, date, parties);
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) {
		return target.getIndividus(nosIndividus, date, parties);
	}

	@Override
	public boolean isWarmable() {
		return target.isWarmable();
	}

	@Override
	public void setIndividuLogger(boolean value) {
		target.setIndividuLogger(value);
	}
}
