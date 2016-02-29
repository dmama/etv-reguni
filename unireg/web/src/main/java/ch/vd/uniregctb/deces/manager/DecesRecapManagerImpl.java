package ch.vd.uniregctb.deces.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.deces.view.DecesRecapView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class DecesRecapManagerImpl implements DecesRecapManager  {

	private TiersService tiersService;

	private MetierService metierService;

	private TiersGeneralManager tiersGeneralManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	/**
	 * Alimente la vue DecesRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public DecesRecapView get(Long numero) {
		DecesRecapView decesRecapView =  new DecesRecapView();
		//FIXME (CGD) impléementer Ifosec
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numero);
		TiersGeneralView personneView = tiersGeneralManager.getPersonnePhysique(pp, true);
		decesRecapView.setPersonne(personneView);

		EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, null);
		decesRecapView.setMarieSeul(couple != null && couple.getConjoint(pp) == null);

		return decesRecapView;
	}

	/**
	 * Persiste le rapport
	 *
	 * @param decesRecapView
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(DecesRecapView decesRecapView) throws MetierServiceException {
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(decesRecapView.getPersonne().getNumero());

		if (decesRecapView.isMarieSeul() && decesRecapView.isVeuf()) {
			metierService.veuvage(pp, RegDateHelper.get(decesRecapView.getDateDeces()), decesRecapView.getRemarque(), null);
		}
		else {
			metierService.deces(pp, RegDateHelper.get(decesRecapView.getDateDeces()), decesRecapView.getRemarque(), null);
		}

	}

	@Override
	@Transactional(readOnly = true)
	public boolean isDecede(long numeroCtb) {
		final Tiers tiers = tiersService.getTiers(numeroCtb);
		return tiers instanceof PersonnePhysique && tiersService.isDecede((PersonnePhysique) tiers);
	}
}
