package ch.vd.unireg.fourreNeutre;

import javax.jms.JMSException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

public class FourreNeutreServiceImpl implements FourreNeutreService {
	private TiersDAO tiersDAO;
	private EditiqueCompositionService editiqueCompositionService;
	private EvenementFiscalService evenementFiscalService;
	private FourreNeutreHelper helper;


	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setHelper(FourreNeutreHelper helper) {
		this.helper = helper;
	}

	@Override
	public EditiqueResultat imprimerFourreNeutre(long tiersId, int periodeFiscale) throws FourreNeutreException {
		final Tiers ctb = tiersDAO.get(tiersId);
		final FourreNeutre fourreNeutre = new FourreNeutre(ctb,periodeFiscale);
		try {
			final EditiqueResultat resultat = editiqueCompositionService.imprimerFourreNeutre(fourreNeutre, RegDate.get());
			evenementFiscalService.publierEvenementFiscalImpressionFourreNeutre(fourreNeutre,RegDate.get());

			return resultat;
		}
		catch (EditiqueException | JMSException e) {
			throw new FourreNeutreException(e);
		}

	}

	@Override
	public Integer getPremierePeriodeSelonType(long tiersId) {
		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			return helper.getPremierePeriodePP();
		}
		else if (tiers instanceof Entreprise) {
			return helper.getPremierePeriodePM();
		}
		else  if (tiers instanceof DebiteurPrestationImposable) {
			return helper.getPremierePeriodeIS();
		}
		else {
			throw new IllegalArgumentException("type de tiers non pris en charge:" + tiers.getClass().getName());
		}
	}

	@Override
	public boolean isAutorisePourFourreNeutre(long tiersId) {

		final Tiers tiers = tiersDAO.get(tiersId);
		return helper.isTiersAutorisePourFourreNeutre(tiers);
	}
}
