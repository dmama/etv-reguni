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
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.utils.WebContextUtils;

public class AnnulationDecesRecapManagerImpl implements AnnulationDecesRecapManager, MessageSourceAware {

	private TiersService tiersService;

	private MetierService metierService;

	private MessageSource messageSource;

	private TiersGeneralManager tiersGeneralManager;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}


	/**
	 * Alimente la vue AnnulationDecesRecapView
	 */
	@Override
	@Transactional(readOnly = true)
	public AnnulationDecesRecapView get(Long numero) {
		AnnulationDecesRecapView annulationDecesRecapView = new AnnulationDecesRecapView();
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numero);
		if (pp == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.pp.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		TiersGeneralView personneView = tiersGeneralManager.getPersonnePhysique(pp, true);
		annulationDecesRecapView.setPersonne(personneView);
		annulationDecesRecapView.setMarieSeulAndVeuf(isVeuvageMarieSeul(pp));

		if (tiersService.isDecede(pp)) {
			RegDate dateDeces = tiersService.getDateDeces(pp);
			RegDate dateDecesDepuisFor = tiersService.getDateDecesDepuisDernierForPrincipal(pp);
			annulationDecesRecapView.setDateDeces(dateDeces);
			annulationDecesRecapView.setDateDecesDepuisDernierForPrincipal(dateDecesDepuisFor);
			annulationDecesRecapView.setWarningDateDecesModifie(dateDecesDepuisFor != null && dateDeces != dateDecesDepuisFor);
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
	 * Persiste l'annulation de décès
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(AnnulationDecesRecapView annulationDecesRecapView) throws MetierServiceException {
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(annulationDecesRecapView.getPersonne().getNumero());
		if (isVeuvageMarieSeul(pp)) {
			final RegDate dateVeuvage = annulationDecesRecapView.getDateVeuvage();
			metierService.annuleVeuvage(pp, dateVeuvage, 0L);
		}
		else {
			if (annulationDecesRecapView.isWarningDateDecesModifie()) {
				metierService.annuleDeces(pp, annulationDecesRecapView.getDateDecesDepuisDernierForPrincipal());
			} else {
				metierService.annuleDeces(pp, annulationDecesRecapView.getDateDeces());
			}
		}
	}

	private boolean isVeuvageMarieSeul(PersonnePhysique pp) {
		return tiersService.isVeuvageMarieSeul(pp);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isVeuvageMarieSeul(long noTiers) {
		final Tiers tiers = tiersService.getTiers(noTiers);
		return tiers instanceof PersonnePhysique && tiersService.isVeuvageMarieSeul((PersonnePhysique) tiers);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isDecede(long noTiers) {
		final Tiers tiers = tiersService.getTiers(noTiers);
		return tiers instanceof PersonnePhysique && tiersService.isDecede((PersonnePhysique) tiers);
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
