package ch.vd.uniregctb.acces.parUtilisateur.manager;

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.acces.parUtilisateur.view.DroitAccesUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.manager.UtilisateurManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.DroitAccesService;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class UtilisateurEditRestrictionManagerImpl implements UtilisateurEditRestrictionManager{

	private UtilisateurManager utilisateurManager;
	private TiersGeneralManager tiersGeneralManager;
	private DroitAccesDAO droitAccesDAO;
	private DroitAccesService droitAccesService;
	private AdresseService adresseService;
	private TiersService tiersService;

	public void setUtilisateurManager(UtilisateurManager utilisateurManager) {
		this.utilisateurManager = utilisateurManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	public void setDroitAccesService(DroitAccesService droitAccesService) {
		this.droitAccesService = droitAccesService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * Annule une restriction
	 *
	 * @param idRestriction
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRestriction( Long idRestriction) throws DroitAccesException {
		droitAccesService.annuleDroitAcces(idRestriction);
	}

	/**
	 * Alimente la vue du controller
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public UtilisateurEditRestrictionView get(long noIndividuOperateur) throws ServiceInfrastructureException, AdresseException {

		final UtilisateurView utilisateurView = utilisateurManager.get(noIndividuOperateur);
		final UtilisateurEditRestrictionView utilisateurEditRestrictionView = new UtilisateurEditRestrictionView();
		utilisateurEditRestrictionView.setUtilisateur(utilisateurView);
		final List<DroitAccesUtilisateurView> views = new ArrayList<DroitAccesUtilisateurView>();
		final List<DroitAcces> restrictions = droitAccesDAO.getDroitsAcces(noIndividuOperateur);
		for (DroitAcces droitAcces : restrictions) {
			final DroitAccesUtilisateurView droitAccesView = new DroitAccesUtilisateurView(droitAcces, tiersService, adresseService);
			views.add(droitAccesView);
		}
		utilisateurEditRestrictionView.setRestrictions(views);
		return utilisateurEditRestrictionView;
	}


	/**
	 * Alimente la vue RecapPersonneUtilisateurView
	 *
	 * @param numeroPP
	 * @param noIndividuOperateur
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public RecapPersonneUtilisateurView get(Long numeroPP, Long noIndividuOperateur) throws ServiceInfrastructureException, AdressesResolutionException {
		RecapPersonneUtilisateurView recapPersonneUtilisateurView = new RecapPersonneUtilisateurView();

		UtilisateurView utilisateurView = utilisateurManager.get(noIndividuOperateur);
		recapPersonneUtilisateurView.setUtilisateur(utilisateurView);

		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numeroPP);
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getPersonnePhysique(pp, true);
		recapPersonneUtilisateurView.setDossier(tiersGeneralView);

		recapPersonneUtilisateurView.setType(TypeDroitAcces.INTERDICTION);

		return recapPersonneUtilisateurView;
	}

	/**
	 * Persiste le DroitAcces
	 * @param recapPersonneUtilisateurView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(RecapPersonneUtilisateurView recapPersonneUtilisateurView) throws DroitAccesException {

		final long operateurId = recapPersonneUtilisateurView.getUtilisateur().getNumeroIndividu();
		final long tiersId = recapPersonneUtilisateurView.getDossier().getNumero();
		final TypeDroitAcces type = recapPersonneUtilisateurView.getType();
		final Niveau niveau = (recapPersonneUtilisateurView.isLectureSeule() ? Niveau.LECTURE : Niveau.ECRITURE);

		droitAccesService.ajouteDroitAcces(operateurId, tiersId, type, niveau);
	}

}
