package ch.vd.unireg.tiers.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.interfaces.InterfaceDataException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.view.TiersEditView;

/**
 * Service à disposition du controller pour gérer un for fiscal
 * @author xcifde
 */
public class ForFiscalManagerImpl extends TiersManager implements ForFiscalManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ForFiscalManagerImpl.class);

	/**
	 * Charge les informations dans TiersView
	 * @param numero un numéro de tiers
	 * @return un objet TiersView
	 */
	@Override
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();

		if (numero == null) {
			return null;
		}
		final Tiers tiers = getTiersDAO().get(numero);

		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

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
			setDecisionAciView(tiersEditView,contribuable);
		}

		if (tiers instanceof DebiteurPrestationImposable) {
			DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			setForsFiscauxDebiteur(tiersEditView, dpi);
			setPeriodiciteCourante(tiersEditView, dpi);
		}

		return tiersEditView;
	}

}
