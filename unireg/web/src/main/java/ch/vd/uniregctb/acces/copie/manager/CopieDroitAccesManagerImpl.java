package ch.vd.uniregctb.acces.copie.manager;

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.acces.copie.view.ConfirmCopieView;
import ch.vd.uniregctb.acces.parUtilisateur.view.DroitAccesUtilisateurView;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.general.manager.UtilisateurManager;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.DroitAccesService;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.TiersService;

public class CopieDroitAccesManagerImpl implements CopieDroitAccesManager {

	private UtilisateurManager utilisateurManager;
	private DroitAccesDAO droitAccesDAO;
	private DroitAccesService droitAccesService;
	private TiersService tiersService;
	private AdresseService adresseService;

	public void setUtilisateurManager(UtilisateurManager utilisateurManager) {
		this.utilisateurManager = utilisateurManager;
	}

	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	public void setDroitAccesService(DroitAccesService droitAccesService) {
		this.droitAccesService = droitAccesService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	/**
	 * Alimente le frombacking du controller
	 * @param noOperateurReference
	 * @param noOperateurDestination
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Override
	@Transactional(readOnly = true)
	public ConfirmCopieView get(long noOperateurReference, long noOperateurDestination) throws AdresseException {
		final ConfirmCopieView confirmCopieView = new ConfirmCopieView();
		final UtilisateurView utilisateurReferenceView = utilisateurManager.get(noOperateurReference);
		confirmCopieView.setUtilisateurReferenceView(utilisateurReferenceView);
		final UtilisateurView utilisateurDestinationView = utilisateurManager.get(noOperateurDestination);
		confirmCopieView.setUtilisateurDestinationView(utilisateurDestinationView);

		final List<DroitAccesUtilisateurView> views = new ArrayList<DroitAccesUtilisateurView>();
		final List<DroitAcces> restrictions = droitAccesDAO.getDroitsAcces(noOperateurReference);
		for (DroitAcces droitAcces : restrictions) {
			final DroitAccesUtilisateurView droitAccesView = new DroitAccesUtilisateurView(droitAcces, tiersService, adresseService);
			views.add(droitAccesView);
		}
		confirmCopieView.setDroitsAccesView(views);
		return confirmCopieView;
	}

	/**
	 * Copie les droits d'un utilisateur vers un autre
	 *
	 * @param confirmCopieView
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void copie(ConfirmCopieView confirmCopieView) throws DroitAccesException {
		long operateurSourceId = confirmCopieView.getUtilisateurReferenceView().getNumeroIndividu();
		long operateurTargetId = confirmCopieView.getUtilisateurDestinationView().getNumeroIndividu();
		droitAccesService.copieDroitsAcces(operateurSourceId, operateurTargetId);
	}

	/**
	 * Transfert les droits d'un utilisateur vers un autre
	 *
	 * @param confirmCopieView
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void transfert(ConfirmCopieView confirmCopieView) throws DroitAccesException {
		long operateurSourceId = confirmCopieView.getUtilisateurReferenceView().getNumeroIndividu();
		long operateurTargetId = confirmCopieView.getUtilisateurDestinationView().getNumeroIndividu();
		droitAccesService.transfereDroitsAcces(operateurSourceId, operateurTargetId);
	}

}
