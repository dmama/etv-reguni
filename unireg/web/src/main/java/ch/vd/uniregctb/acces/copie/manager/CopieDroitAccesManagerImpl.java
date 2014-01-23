package ch.vd.uniregctb.acces.copie.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.acces.copie.view.ConfirmCopieView;
import ch.vd.uniregctb.acces.copie.view.ConfirmedDataView;
import ch.vd.uniregctb.acces.parUtilisateur.view.BaseDroitAccesDossierView;
import ch.vd.uniregctb.acces.parUtilisateur.view.DroitAccesUtilisateurView;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.general.manager.UtilisateurManager;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.security.DroitAccesConflit;
import ch.vd.uniregctb.security.DroitAccesConflitAvecDonneesContribuable;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.security.DroitAccesService;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
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

		final List<DroitAccesUtilisateurView> views = new ArrayList<>();
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
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public List<DroitAccesConflitAvecDonneesContribuable> copie(ConfirmedDataView view) throws AdresseException {
		final List<DroitAccesConflit> conflits = droitAccesService.copieDroitsAcces(view.getNoOperateurReference(), view.getNoOperateurDestination());
		return addDonneesContribuable(conflits);
	}

	private List<DroitAccesConflitAvecDonneesContribuable> addDonneesContribuable(List<DroitAccesConflit> conflits) throws AdresseException {
		if (!conflits.isEmpty()) {
			final List<DroitAccesConflitAvecDonneesContribuable> liste = new ArrayList<>(conflits.size());
			for (DroitAccesConflit conflit : conflits) {
				final Tiers tiers = tiersService.getTiers(conflit.getNoContribuable());
				String nomPrenom = null;
				RegDate dateNaissance = null;
				String npaLocalite = null;
				if (tiers instanceof PersonnePhysique) {
					final PersonnePhysique pp = (PersonnePhysique) tiers;
					final BaseDroitAccesDossierView ppView = new BaseDroitAccesDossierView(pp, tiersService, adresseService);
					nomPrenom = ppView.getPrenomNom();
					dateNaissance = ppView.getDateNaissance();
					npaLocalite = ppView.getLocalite();
				}
				liste.add(new DroitAccesConflitAvecDonneesContribuable(conflit, nomPrenom, dateNaissance, npaLocalite));
			}
			return liste;
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * Transfert les droits d'un utilisateur vers un autre
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public List<DroitAccesConflitAvecDonneesContribuable> transfert(ConfirmedDataView view) throws AdresseException {
		final List<DroitAccesConflit> conflits = droitAccesService.transfereDroitsAcces(view.getNoOperateurReference(), view.getNoOperateurDestination());
		return addDonneesContribuable(conflits);
	}

}
