package ch.vd.uniregctb.annulation.deces.manager;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.annulation.deces.view.AnnulationDecesRecapView;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.utils.WebContextUtils;

public class AnnulationDecesRecapManagerImpl implements AnnulationDecesRecapManager, MessageSourceAware{

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
		AnnulationDecesRecapView annulationDecesRecapView =  new AnnulationDecesRecapView();
		//FIXME (CGD) impléementer Ifosec
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numero);
		if (pp == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.pp.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}
		TiersGeneralView personneView = tiersGeneralManager.get(pp);
		annulationDecesRecapView.setPersonne(personneView);

		annulationDecesRecapView.setDateDeces(tiersService.getDateDeces(pp));

		if (annulationDecesRecapView.getDateDeces() == null) {
			throw new ObjectNotFoundException("Impossible de déterminer la date de décès du contribuable");
		}

		//TODO (CGD) a-t-on le droit d'annulé le décès d'un habitant décédé dans le civil ?
		if (pp.getDateDeces() == null) {
			throw new ObjectNotFoundException("Le contribuable habitant est mort dans le civil, il n'est pas possible d'annuler son décès dans le fiscal");
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
		metierService.annuleDeces(pp, annulationDecesRecapView.getDateDeces());

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
