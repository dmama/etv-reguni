package ch.vd.uniregctb.acces.parUtilisateur.manager;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.acces.parUtilisateur.view.DroitAccesUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.manager.UtilisateurManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.general.view.UtilisateurView;
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
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public UtilisateurEditRestrictionView get(long noIndividuOperateur) throws InfrastructureException, AdresseException {

		UtilisateurView utilisateurView = utilisateurManager.get(noIndividuOperateur);
		UtilisateurEditRestrictionView utilisateurEditRestrictionView = new UtilisateurEditRestrictionView();
		utilisateurEditRestrictionView.setUtilisateur(utilisateurView);
		List<DroitAccesUtilisateurView> droitsAccesView = new ArrayList<DroitAccesUtilisateurView>();
		List<DroitAcces> restrictions = droitAccesDAO.getDroitsAcces(noIndividuOperateur);
		for (DroitAcces droitAcces : restrictions) {
			DroitAccesUtilisateurView droitAccesView = new DroitAccesUtilisateurView();
			droitAccesView.setId(droitAcces.getId());
			droitAccesView.setAnnule(droitAcces.isAnnule());
			droitAccesView.setType(droitAcces.getType());
			droitAccesView.setNumeroCTB(droitAcces.getTiers().getNumero());
			PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(droitAcces.getTiers().getNumero());
			AdresseEnvoiDetaillee adresseEnvoiDetaillee = adresseService.getAdresseEnvoi(pp, null, TypeAdresseFiscale.COURRIER, false);
			if (adresseEnvoiDetaillee != null) {
				List<String> noms = adresseEnvoiDetaillee.getNomPrenom();
				if ((noms != null) & (noms.get(0) != null)) {
					droitAccesView.setPrenomNom(noms.get(0));
				}
				droitAccesView.setLocalite(adresseEnvoiDetaillee.getNpaEtLocalite());
			}
			RegDate dateNaissance = tiersService.getDateNaissance(pp);
			droitAccesView.setDateNaissance(dateNaissance);
			droitAccesView.setNiveau(droitAcces.getNiveau());
			droitAccesView.setDateDebut(droitAcces.getDateDebut());
			droitAccesView.setDateFin(droitAcces.getDateFin());
			if (droitAcces.getNiveau().equals(Niveau.LECTURE)) {
				droitAccesView.setLectureSeule(true);
			}
			else if (droitAcces.getNiveau().equals(Niveau.ECRITURE)) {
				droitAccesView.setLectureSeule(false);
			}
			droitsAccesView.add(droitAccesView);
		}
		utilisateurEditRestrictionView.setRestrictions(droitsAccesView);
		return utilisateurEditRestrictionView;
	}


	/**
	 * Alimente la vue RecapPersonneUtilisateurView
	 *
	 * @param numeroPP
	 * @param noIndividuOperateur
	 * @return
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public RecapPersonneUtilisateurView get(Long numeroPP, Long noIndividuOperateur) throws InfrastructureException, AdressesResolutionException {
		RecapPersonneUtilisateurView recapPersonneUtilisateurView = new RecapPersonneUtilisateurView();

		UtilisateurView utilisateurView = utilisateurManager.get(noIndividuOperateur);
		recapPersonneUtilisateurView.setUtilisateur(utilisateurView);

		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numeroPP);
		TiersGeneralView tiersGeneralView = tiersGeneralManager.get(pp, true);
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

		droitAccesService.addDroitAcces(operateurId, tiersId, type, niveau);
	}

}
