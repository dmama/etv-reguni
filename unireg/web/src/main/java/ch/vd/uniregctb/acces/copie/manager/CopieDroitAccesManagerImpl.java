package ch.vd.uniregctb.acces.copie.manager;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.acces.copie.view.ConfirmCopieView;
import ch.vd.uniregctb.acces.parUtilisateur.view.DroitAccesUtilisateurView;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.general.manager.UtilisateurManager;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.DroitAccesService;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Niveau;

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
	@Transactional(readOnly = true)
	public ConfirmCopieView get(long noOperateurReference, long noOperateurDestination) throws AdresseException {
		ConfirmCopieView confirmCopieView = new ConfirmCopieView();
		UtilisateurView utilisateurReferenceView = utilisateurManager.get(noOperateurReference);
		confirmCopieView.setUtilisateurReferenceView(utilisateurReferenceView);
		UtilisateurView utilisateurDestinationView = utilisateurManager.get(noOperateurDestination);
		confirmCopieView.setUtilisateurDestinationView(utilisateurDestinationView);
		List<DroitAccesUtilisateurView> droitsAccesView = new ArrayList<DroitAccesUtilisateurView>();
		List<DroitAcces> restrictions = droitAccesDAO.getDroitsAcces(noOperateurReference);
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
		confirmCopieView.setDroitsAccesView(droitsAccesView);

		return confirmCopieView;
	}

	/**
	 * Copie les droits d'un utilisateur vers un autre
	 *
	 * @param confirmCopieView
	 */
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
	@Transactional(rollbackFor = Throwable.class)
	public void transfert(ConfirmCopieView confirmCopieView) throws DroitAccesException {
		long operateurSourceId = confirmCopieView.getUtilisateurReferenceView().getNumeroIndividu();
		long operateurTargetId = confirmCopieView.getUtilisateurDestinationView().getNumeroIndividu();
		droitAccesService.transfereDroitsAcces(operateurSourceId, operateurTargetId);
	}

}
