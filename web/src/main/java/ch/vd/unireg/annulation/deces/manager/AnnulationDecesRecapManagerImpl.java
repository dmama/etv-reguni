package ch.vd.unireg.annulation.deces.manager;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.annulation.deces.view.AnnulationDecesRecapView;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.utils.WebContextUtils;

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
			final RegDate dateDebutVeuvage = tiersService.getDateDebutVeuvage(pp, RegDate.get());
			if (dateDebutVeuvage == null) {
				throw new ObjectNotFoundException("Impossible de déterminer la date de début de veuvage du contribuable");
			}

			// [SIFISC-13197] le métier-service s'attend à recevoir la date de décès du conjoint, alors que la date
			// que nous appelons ici la date de début de veuvage est au lendemain (= parce que le for du survivant est ouvert
			// au lendemain du décès)
			annulationDecesRecapView.setDateVeuvage(dateDebutVeuvage.getOneDayBefore());
		}

		return annulationDecesRecapView;
	}

	/**
	 * Persiste l'annulation de décès
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(AnnulationDecesRecapView annulationDecesRecapView) throws MetierServiceException {
		final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(annulationDecesRecapView.getPersonne().getNumero());
		if (isVeuvageMarieSeul(pp)) {
			final RegDate dateVeuvage = annulationDecesRecapView.getDateVeuvage();
			metierService.annuleVeuvage(pp, dateVeuvage, null);
		}
		else {
			if (annulationDecesRecapView.isWarningDateDecesModifie()) {
				metierService.annuleDeces(pp, annulationDecesRecapView.getDateDecesDepuisDernierForPrincipal());
			}
			else {
				metierService.annuleDeces(pp, annulationDecesRecapView.getDateDeces());
			}
		}
	}

	private boolean isVeuvageMarieSeul(PersonnePhysique pp) {
		return tiersService.isVeuvageMarieSeul(pp) && tiersService.getDateDeces(pp) == null;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
