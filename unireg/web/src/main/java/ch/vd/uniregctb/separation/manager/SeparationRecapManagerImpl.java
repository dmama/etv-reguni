package ch.vd.uniregctb.separation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.separation.view.SeparationRecapView;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;


public class SeparationRecapManagerImpl implements SeparationRecapManager {

	private TiersService tiersService;

	private MetierService metierService;

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
	 * Alimente la vue SeparationRecapView
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public SeparationRecapView get(Long numero) {
		SeparationRecapView separationRecapView =  new SeparationRecapView();
		//FIXME (CGD) impléementer Ifosec
		MenageCommun menage = (MenageCommun) tiersService.getTiers(numero);
		TiersGeneralView menageView = tiersGeneralManager.getTiers(menage, true);
		separationRecapView.setCouple(menageView);
		separationRecapView.setEtatCivil(EtatCivil.DIVORCE);

		return separationRecapView;
	}

	/**
	 * Persiste le rapport
	 *
	 * @param separationRecapView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(SeparationRecapView separationRecapView) throws MetierServiceException {
		final MenageCommun menage = (MenageCommun) tiersService.getTiers(separationRecapView.getCouple().getNumero());
		final RegDate dateSeparation = separationRecapView.getDateSeparation();

		// détermination de l'état civil du couple (MARIE, PACS)
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menage, dateSeparation);
		final boolean memeSexe = tiersService.isMemeSexe(couple.getPrincipal(), couple.getConjoint());

		EtatCivil etatCivilFamille = separationRecapView.getEtatCivil();
		switch (separationRecapView.getEtatCivil()) {
		case DIVORCE:
			etatCivilFamille = memeSexe ? EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT : EtatCivil.DIVORCE;
			break;
		case SEPARE:
			// pour l'instant il n'y pas des distinction d'état civil lors de la séparation d'un couple homo
			etatCivilFamille = EtatCivil.SEPARE;
			break;
		case NON_MARIE:
			// la ligne suivante implique la création systématique d’une situation de famille
			// car l'état civil NON_MARIE n'existe pas dans le registre civil
			etatCivilFamille = EtatCivil.NON_MARIE;
			break;
		}

		metierService.separe(menage, dateSeparation, separationRecapView.getRemarque(), etatCivilFamille, true, null);
	}

	@Transactional(readOnly = true)
	public boolean isAvecForFiscalPrincipalActif(long noTiers) {
		final Tiers tiers = tiersService.getTiers(noTiers);
		if (tiers != null) {
			final ForFiscalPrincipal ffp = tiers.getDernierForFiscalPrincipal();
			return ffp != null && ffp.getDateFin() == null;
		}
		return false;
	}
}
