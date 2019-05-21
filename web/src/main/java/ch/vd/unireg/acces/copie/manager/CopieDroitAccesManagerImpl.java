package ch.vd.unireg.acces.copie.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.acces.copie.view.ConfirmCopieView;
import ch.vd.unireg.acces.copie.view.ConfirmedDataView;
import ch.vd.unireg.acces.parUtilisateur.view.BaseDroitAccesDossierView;
import ch.vd.unireg.acces.parUtilisateur.view.DroitAccesUtilisateurView;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.general.manager.UtilisateurManager;
import ch.vd.unireg.general.view.UtilisateurView;
import ch.vd.unireg.interfaces.civil.IndividuConnectorException;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.security.DroitAccesConflit;
import ch.vd.unireg.security.DroitAccesConflitAvecDonneesContribuable;
import ch.vd.unireg.security.DroitAccesDAO;
import ch.vd.unireg.security.DroitAccesService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

public class CopieDroitAccesManagerImpl implements CopieDroitAccesManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(CopieDroitAccesManagerImpl.class);

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

	@Override
	@Transactional(readOnly = true)
	public ConfirmCopieView get(String visaOperateurReference, String visaOperateurDestination, ParamPagination pagination) throws AdresseException {
		final ConfirmCopieView confirmCopieView = new ConfirmCopieView();
		final UtilisateurView utilisateurReferenceView = utilisateurManager.get(visaOperateurReference);
		confirmCopieView.setUtilisateurReferenceView(utilisateurReferenceView);
		final UtilisateurView utilisateurDestinationView = utilisateurManager.get(visaOperateurDestination);
		confirmCopieView.setUtilisateurDestinationView(utilisateurDestinationView);
		confirmCopieView.setSize(droitAccesDAO.getDroitAccesCount(visaOperateurReference));

		final List<DroitAccesUtilisateurView> views = new ArrayList<>();
		final List<DroitAcces> restrictions = droitAccesDAO.getDroitsAcces(visaOperateurReference, pagination);
		for (DroitAcces droitAcces : restrictions) {
			try {
				final DroitAccesUtilisateurView view = new DroitAccesUtilisateurView(droitAcces, tiersService, adresseService);
				views.add(view);
			}
			catch (ServiceEntrepriseException | IndividuConnectorException e) {
				LOGGER.warn("Exception lors de la récupération des données du contribuable protégé " + FormatNumeroHelper.numeroCTBToDisplay(droitAcces.getTiers().getNumero()) + ".", e);
				final DroitAccesUtilisateurView view = new DroitAccesUtilisateurView(droitAcces, e);
				views.add(view);
			}
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
		final List<DroitAccesConflit> conflits = droitAccesService.copieDroitsAcces(view.getVisaOperateurReference(), view.getVisaOperateurDestination());
		return addDonneesContribuable(conflits);
	}

	/**
	 * Transfert les droits d'un utilisateur vers un autre
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public List<DroitAccesConflitAvecDonneesContribuable> transfert(ConfirmedDataView view) throws AdresseException {
		final List<DroitAccesConflit> conflits = droitAccesService.transfereDroitsAcces(view.getVisaOperateurReference(), view.getVisaOperateurDestination());
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
				if (tiers instanceof Contribuable) {
					try {
						final BaseDroitAccesDossierView ppView = new BaseDroitAccesDossierView((Contribuable) tiers, tiersService, adresseService);
						nomPrenom = ppView.getPrenomNom();
						dateNaissance = ppView.getDateNaissance();
						npaLocalite = ppView.getLocalite();
					}
					catch (ServiceEntrepriseException | IndividuConnectorException e) {
						LOGGER.warn("Exception lors de la récupération des données du contribuable protégé " + FormatNumeroHelper.numeroCTBToDisplay(conflit.getNoContribuable()) + ".", e);
					}
				}
				liste.add(new DroitAccesConflitAvecDonneesContribuable(conflit, nomPrenom, dateNaissance, npaLocalite));
			}
			return liste;
		}
		else {
			return Collections.emptyList();
		}
	}

}
