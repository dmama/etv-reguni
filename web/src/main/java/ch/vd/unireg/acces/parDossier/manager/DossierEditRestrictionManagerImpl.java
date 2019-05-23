package ch.vd.unireg.acces.parDossier.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.unireg.acces.parDossier.view.DroitAccesView;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.DroitAccesException;
import ch.vd.unireg.security.DroitAccesService;
import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeDroitAcces;

/**
 * Classe qui gère le controller DossierEditRestrictionController
 *
 * @author xcifde
 *
 */
public class DossierEditRestrictionManagerImpl implements DossierEditRestrictionManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(DossierEditRestrictionManagerImpl.class);

	private TiersDAO tiersDAO;
	private TiersGeneralManager tiersGeneralManager;
	private ServiceSecuriteService serviceSecuriteService;
	private DroitAccesService droitAccesService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}

	public void setDroitAccesService(DroitAccesService droitAccesService) {
		this.droitAccesService = droitAccesService;
	}

	/**
	 * Alimente la vue du controller
	 */
	@Override
	@Transactional(readOnly = true)
	public DossierEditRestrictionView get(Long numeroTiers) throws InfrastructureException {

		final DossierEditRestrictionView dossierEditRestrictionView = new DossierEditRestrictionView();
		final Contribuable tiers = (Contribuable) tiersDAO.get(numeroTiers);
		final TiersGeneralView dossier = tiersGeneralManager.getTiers(tiers, true);
		dossierEditRestrictionView.setDossier(dossier);

		final Set<DroitAcces> droitsAccesAppliques = tiers.getDroitsAccesAppliques();
		final List<DroitAccesView> droitsAccesView =  new ArrayList<>(droitsAccesAppliques.size());
		for (DroitAcces droitAcces : droitsAccesAppliques) {
			final String visa = droitAcces.getVisaOperateur();
			final DroitAccesView droitAccesView = new DroitAccesView();
			droitAccesView.setId(droitAcces.getId());
			droitAccesView.setAnnule(droitAcces.isAnnule());
			droitAccesView.setType(droitAcces.getType());
			droitAccesView.setVisaOperateur(visa);
			if (visa == null) {
				// Opérateur inconnu, on affiche un texte dans le nom prenom
				droitAccesView.setPrenomNom("Opérateur inconnu");
			}
			else {
				final Operateur operator = serviceSecuriteService.getOperateur(visa);
				if (operator != null) {
					droitAccesView.setPrenomNom(buildPrenomNom(operator));
					droitAccesView.setOfficeImpot(buildOfficeImpot(visa));
				}
				else {
					//SIFISC-26187 Pas d'opérateur trouvé, on affiche un texte dans le nom prenom
					final String msgErreur = String.format("Opérateur %s non trouvé dans host-interfaces", visa);
					droitAccesView.setPrenomNom(msgErreur);
				}
			}

			droitAccesView.setNiveau(droitAcces.getNiveau());
			droitAccesView.setDateDebut(droitAcces.getDateDebut());
			droitAccesView.setDateFin(droitAcces.getDateFin());
			droitAccesView.setLectureSeule(droitAcces.getNiveau() == Niveau.LECTURE);

			droitsAccesView.add(droitAccesView);
		}
		dossierEditRestrictionView.setRestrictions(droitsAccesView);
		return dossierEditRestrictionView;
	}

	@Nullable
	private String buildOfficeImpot(String visa) {
		String officeImpot;
		try {
			final List<CollectiviteAdministrative> collectivitesAdministratives = serviceSecuriteService.getCollectivitesUtilisateur(visa);
			final StringRenderer<CollectiviteAdministrative> nomsCourts = CollectiviteAdministrative::getNomCourt;
			officeImpot = CollectionsUtils.toString(collectivitesAdministratives, nomsCourts, ", ", null);
		}
		catch (ServiceSecuriteException e) {
			officeImpot = null;
			LOGGER.warn("Exception reçue à la réception des collectivités de l'utilisateur " + visa, e);
		}
		return officeImpot;
	}

	private static String buildPrenomNom(Operateur operator) {
		String prenomNom = "";
		if (operator.getPrenom() != null) {
			prenomNom = operator.getPrenom();
		}
		if (operator.getNom() != null) {
			prenomNom = prenomNom + ' ' + operator.getNom();
		}
		return prenomNom;
	}

	/**
	 * Persiste un droit d'acces
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(DroitAccesView droitAccesView) throws DroitAccesException {
		final String visaOperateur = droitAccesView.getVisaOperateur();
		final long tiersId = droitAccesView.getNumero();
		final TypeDroitAcces type = droitAccesView.getType();
		final Niveau niveau = (droitAccesView.isLectureSeule() ? Niveau.LECTURE : Niveau.ECRITURE);

		droitAccesService.ajouteDroitAcces(visaOperateur, tiersId, type, niveau);
	}

	/**
	 * Annule une restriction
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRestriction(Long idRestriction) throws DroitAccesException {
		droitAccesService.annuleDroitAcces(idRestriction);
	}
}
