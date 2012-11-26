package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.interfaces.InterfaceDataException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.TiersEditView;

/**
 * Service à disposition du controller pour gérer un for fiscal
 * @author xcifde
 */
public class ForFiscalManagerImpl extends TiersManager implements ForFiscalManager {

	private ForFiscalDAO forFiscalDAO;

	public void setForFiscalDAO(ForFiscalDAO forFiscalDAO) {
		this.forFiscalDAO = forFiscalDAO;
	}

	/**
	 * Charge les informations dans TiersView
	 * @param numero un numéro de tiers
	 * @return un objet TiersView
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();

		if (numero == null) {
			return null;
		}
		final Tiers tiers = getTiersDAO().get(numero);

		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}
		if (tiers != null) {
			setTiersGeneralView(tiersEditView, tiers);
			tiersEditView.setTiers(tiers);
			if (tiers instanceof Contribuable) {
				final Contribuable contribuable = (Contribuable) tiers;
				setForsFiscaux(tiersEditView, contribuable);
				try {
					setSituationsFamille(tiersEditView, contribuable);
				}
				catch (InterfaceDataException e) {
					LOGGER.warn(String.format("Exception lors de la récupération des situations de familles du contribuable %d", numero), e);
					tiersEditView.setSituationsFamilleEnErreurMessage(e.getMessage());
				}
			}
			if (tiers instanceof DebiteurPrestationImposable) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
				setForsFiscauxDebiteur(tiersEditView, dpi);
				setPeriodiciteCourante(tiersEditView, dpi);
			}
		}

		return tiersEditView;
	}

	/**
	 * Annulation du for
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerFor(Long idFor) {
		ForFiscal forFiscal = forFiscalDAO.get(idFor);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + idFor + " n'existe pas.");
		}
		tiersService.annuleForFiscal(forFiscal, true);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void reouvrirFor(Long idFor) {
		ForFiscal forFiscal = forFiscalDAO.get(idFor);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + idFor + " n'existe pas.");
		}
		tiersService.traiterReOuvertureForDebiteur(forFiscal);
	}
}
