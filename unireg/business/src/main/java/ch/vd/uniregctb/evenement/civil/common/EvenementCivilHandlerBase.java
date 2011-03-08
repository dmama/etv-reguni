package ch.vd.uniregctb.evenement.civil.common;

import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.evenement.civil.engine.EvenementHandlerRegistrar;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Interface commune aux classes capables de traiter un événement d'état civil.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public abstract class EvenementCivilHandlerBase implements EvenementCivilHandler, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilHandlerBase.class);

	/**
	 * Service qui permet denregistrer le handler pour processing
	 *
	 * @see ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandler#setRegistrar(ch.vd.uniregctb.evenement.civil.engine.EvenementHandlerRegistrar)
	 */
	private EvenementHandlerRegistrar registrar;

	/**
	 * Renvoie le type d'evenement que ce handler supporte
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	protected abstract Set<TypeEvenementCivil> getHandledType();

	public void afterPropertiesSet() throws Exception {
		Set<TypeEvenementCivil> types = getHandledType();
		if(types == null) {
			LOGGER.warn("Le handler " + getClass().getSimpleName() + " ne supporte aucun evenement civil!");
		}
		else {
			for (final TypeEvenementCivil type : types) {
				registrar.register(type, this);
			}
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRegistrar(EvenementHandlerRegistrar registrar) {
		this.registrar = registrar;
	}

	public abstract EvenementCivilInterne createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException;
}
