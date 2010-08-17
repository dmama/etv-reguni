package ch.vd.uniregctb.annulation.deces.manager;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.annulation.deces.view.AnnulationDecesRecapView;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.utils.WebContextUtils;

public class AnnulationDecesRecapManagerImpl implements AnnulationDecesRecapManager, MessageSourceAware {

	private TiersService tiersService;

	private MetierService metierService;

	private MessageSource messageSource;

	private TiersGeneralManager tiersGeneralManager;


	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public MetierService getMetierService() {
		return metierService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}


	/**
	 * Alimente la vue AnnulationDecesRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public AnnulationDecesRecapView get(Long numero) {
		AnnulationDecesRecapView annulationDecesRecapView = new AnnulationDecesRecapView();
		//FIXME (CGD) impléementer Ifosec
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numero);
		if (pp == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.pp.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		TiersGeneralView personneView = tiersGeneralManager.get(pp);
		annulationDecesRecapView.setPersonne(personneView);
		annulationDecesRecapView.setMarieSeulAndVeuf(isVeuvageMarieSeul(pp));

		if (tiersService.isDecede(pp)) {
			annulationDecesRecapView.setDateDeces(tiersService.getDateDeces(pp));

			if (annulationDecesRecapView.getDateDeces() == null) {
				throw new ObjectNotFoundException("Impossible de déterminer la date de décès du contribuable");
			}
		}
		else if (annulationDecesRecapView.isMarieSeulAndVeuf()) {
			annulationDecesRecapView.setDateVeuvage(tiersService.getDateDebutVeuvage(pp, RegDate.get()));
			if (annulationDecesRecapView.getDateVeuvage() == null) {
				throw new ObjectNotFoundException("Impossible de déterminer la date de début de veuvage du contribuable");
			}
		}


		return annulationDecesRecapView;
	}

	/**
	 * Persiste le rapport
	 *
	 * @param annulationDecesRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(AnnulationDecesRecapView annulationDecesRecapView) {
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(annulationDecesRecapView.getPersonne().getNumero());
		if (isVeuvageMarieSeul(pp)) {
			final RegDate dateVeuvage = annulationDecesRecapView.getDateVeuvage();
			metierService.annuleVeuvage(pp, dateVeuvage, 0L);
		}
		else {
			metierService.annuleDeces(pp, annulationDecesRecapView.getDateDeces());
		}


	}

	@Transactional(readOnly = true)
	public boolean isVeuvageMarieSeul(PersonnePhysique tiers) {
		return tiersService.isVeuvageMarieSeul(tiers);
	}

	@Transactional(readOnly = true)
	public boolean isDecede(PersonnePhysique tiers) {
		return tiersService.isDecede(tiers);
	}

	/**
	 * @return the messageSource
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}


	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
