package ch.vd.unireg.separation.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatCivil;


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

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void separeCouple(long idMenage, RegDate dateSeparation, EtatCivil etatCivil, String commentaire) throws MetierServiceException {
		final MenageCommun menage = (MenageCommun) tiersService.getTiers(idMenage);

		// détermination de l'état civil du couple (MARIE, PACS)
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menage, dateSeparation);
		final boolean memeSexe = tiersService.isMemeSexe(couple.getPrincipal(), couple.getConjoint());

		final EtatCivil etatCivilFamille;
		switch (etatCivil) {
		case DIVORCE:
			etatCivilFamille = memeSexe ? EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT : EtatCivil.DIVORCE;
			break;
		case SEPARE:
			etatCivilFamille = memeSexe ? EtatCivil.PARTENARIAT_SEPARE : EtatCivil.SEPARE;
			break;
		case NON_MARIE:
			// la ligne suivante implique la création systématique d’une situation de famille
			// car l'état civil NON_MARIE n'existe pas dans le registre civil
			etatCivilFamille = EtatCivil.NON_MARIE;
			break;
		default:
			etatCivilFamille = etatCivil;
			break;
		}

		metierService.separe(menage, dateSeparation, commentaire, etatCivilFamille, null);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isAvecForFiscalPrincipalActif(long noTiers) {
		final Tiers tiers = tiersService.getTiers(noTiers);
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			final ForFiscalPrincipal ffp = ((ContribuableImpositionPersonnesPhysiques) tiers).getDernierForFiscalPrincipal();
			return ffp != null && ffp.getDateFin() == null;
		}
		return false;
	}
}
