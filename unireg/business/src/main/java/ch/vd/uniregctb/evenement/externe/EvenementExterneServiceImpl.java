package ch.vd.uniregctb.evenement.externe;

import java.util.Calendar;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType.TypeQuittance;
import ch.vd.infrastructure.model.impl.DateUtils;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;

@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class EvenementExterneServiceImpl extends ApplicationObjectSupport implements EvenementExterneService, DelegateEvenementExterne {

	private static Logger logger = Logger.getLogger(EvenementExterneServiceImpl.class);

	private EvenementExterneFacade evenementExterneFacade;

	/**
	 * {@inheritDoc}
	 */
	public void surEvenementRecu(EvenementExterneResultat resultat) throws Exception {
		boolean done = false;
		if ( AuthenticationHelper.getAuthentication() == null) {
			AuthenticationHelper.setPrincipal("JMS-EvtExt");
			done = true;
		}
		try {
			this.publishEvent(new EvenementExterneReceivedEvent(resultat));
		} catch (Exception e) {
			logger.error(e);
			throw e;
		}
		finally {
			if (done) {
				AuthenticationHelper.resetAuthentication();
			}
		}
	}

	/**
	 * @return the evenementISFacade
	 */
	public EvenementExterneFacade getEvenementExterneFacade() {
		return evenementExterneFacade;
	}

	/**
	 * @param evenementExterneFacade
	 *            the evenementExterneFacade to set
	 */
	public void setEvenementExterneFacade(EvenementExterneFacade evenementExterneFacade) {
		this.evenementExterneFacade = evenementExterneFacade;
		this.evenementExterneFacade.setDelegate(this);
	}

	/**
	 *
	 * @see org.springframework.context.ApplicationEventPublisher#publishEvent(org.springframework.context.ApplicationEvent)
	 */
	protected void publishEvent(ApplicationEvent event) {
		if (this.getApplicationContext() != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("publication event: " + event.getClass());
			}

			this.getApplicationContext().publishEvent(event);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void sendEvenementExterne(IEvenementExterne evenement) throws Exception {
		this.evenementExterneFacade.sendEvent(evenement);
	}

	public EvenementImpotSourceQuittanceType createEvenementQuittancement(TypeQuittance.Enum quitancement, Long numeroCtb, RegDate dateDebut,
			RegDate dateFin, RegDate dateQuittance) {
		Assert.notNull(quitancement, "le type de quittancement est obligation");
		Assert.notNull(numeroCtb, "Le numero du débiteur est obligatoire");
		Assert.notNull(dateDebut, "la date du début du récapitulatif est obligatoire");
		// Assert.assertNotNull(dateFin);

		final EvenementImpotSourceQuittanceType evenement = evenementExterneFacade.creerEvenementImpotSource();
		evenement.setNumeroTiers(numeroCtb.toString());

		final Calendar datedebutC = DateUtils.calendar(dateDebut.asJavaDate());
		evenement.setDateDebutPeriode(datedebutC);
		if (dateFin != null) {
			final Calendar dateFinC = DateUtils.calendar(dateFin.asJavaDate());
			evenement.setDateFinPeriode(dateFinC);
		}

		evenement.setTypeQuittance(quitancement);
		if (TypeQuittance.QUITTANCEMENT == quitancement) {
			Assert.notNull(dateQuittance, "la date de quittancement du récapitulatif est obligatoire");
			evenement.setDateQuittance(DateUtils.calendar(dateQuittance.asJavaDate()));
		}
		return evenement;
	}

	public Collection<EvenementExterne> getEvenementExternes(boolean ascending, EtatEvenementExterne... etatEvenementExternes) {
		return null;
	}

}
