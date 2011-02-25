package ch.vd.uniregctb.annulation.separation.manager;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.annulation.separation.view.AnnulationSeparationRecapView;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.utils.WebContextUtils;

public class AnnulationSeparationRecapManagerImpl implements AnnulationSeparationRecapManager, MessageSourceAware{

	private TiersService tiersService;

	private MetierService metierService;

	private TiersGeneralManager tiersGeneralManager;

	private MessageSource messageSource;

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
	 * Alimente la vue AnnulationSeparationRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public AnnulationSeparationRecapView get(Long numero) {
		AnnulationSeparationRecapView annulationSeparationRecapView =  new AnnulationSeparationRecapView();
		//FIXME (CGD) impléementer Ifosec
		MenageCommun menageCommun = (MenageCommun) tiersService.getTiers(numero);
		if (menageCommun == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		ForFiscalPrincipal forFiscalPrincipal = menageCommun.getDernierForFiscalPrincipal();

		if (forFiscalPrincipal != null &&
				MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT == forFiscalPrincipal.getMotifFermeture()) {

			EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menageCommun, forFiscalPrincipal.getDateFin().getOneDayBefore());
			if (ensembleTiersCouple != null) {
				TiersGeneralView premierPPView = tiersGeneralManager.getPersonnePhysique(ensembleTiersCouple.getPrincipal(), true);
				annulationSeparationRecapView.setPremierePersonne(premierPPView);

				if (ensembleTiersCouple.getConjoint() != null) {
					TiersGeneralView secondPPView = tiersGeneralManager.getPersonnePhysique(ensembleTiersCouple.getConjoint(), true);
					annulationSeparationRecapView.setSecondePersonne(secondPPView);
				}

				if (ensembleTiersCouple.getMenage() != null) {
					annulationSeparationRecapView.setDateSeparation(ensembleTiersCouple.getMenage().getDateFinActivite().getOneDayAfter());
				}
			}
		}
		return annulationSeparationRecapView;
	}

	/**
	 * Persiste le rapport
	 *
	 * @param annulationSeparationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public MenageCommun save(AnnulationSeparationRecapView annulationSeparationRecapView) {
		PersonnePhysique premierPP = (PersonnePhysique) tiersService.getTiers(annulationSeparationRecapView.getPremierePersonne().getNumero());

		EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(premierPP, annulationSeparationRecapView.getDateSeparation().getOneDayBefore());
		metierService.annuleSeparation(ensembleTiersCouple.getMenage(), annulationSeparationRecapView.getDateSeparation(), null);
		return ensembleTiersCouple.getMenage();
	}

	@Transactional(readOnly = true)
	public boolean isDernierForFiscalPrincipalFermePourSeparation(long noCtb) {
		final Tiers tiers = tiersService.getTiers(noCtb);
		return tiers != null && tiersService.isDernierForFiscalPrincipalFermePourSeparation(tiers);
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
