package ch.vd.unireg.acces.parDossier.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeUtilisateur;
import ch.vd.unireg.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.unireg.acces.parDossier.view.DroitAccesView;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.DroitAccesException;
import ch.vd.unireg.security.DroitAccesService;
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
	 * @param numeroTiers
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public DossierEditRestrictionView get(Long numeroTiers) throws ServiceInfrastructureException {

		final DossierEditRestrictionView dossierEditRestrictionView = new DossierEditRestrictionView();
		final Contribuable tiers = (Contribuable) tiersDAO.get(numeroTiers);
		final TiersGeneralView dossier = tiersGeneralManager.getTiers(tiers, true);
		dossierEditRestrictionView.setDossier(dossier);

		final Set<DroitAcces> droitsAccesAppliques = tiers.getDroitsAccesAppliques();
		final List<DroitAccesView> droitsAccesView =  new ArrayList<>(droitsAccesAppliques.size());
		for (DroitAcces droitAcces : droitsAccesAppliques) {
			final DroitAccesView droitAccesView = new DroitAccesView();
			droitAccesView.setId(droitAcces.getId());
			droitAccesView.setAnnule(droitAcces.isAnnule());
			droitAccesView.setType(droitAcces.getType());
			final Operateur operator = serviceSecuriteService.getOperateur(droitAcces.getNoIndividuOperateur());
			String prenomNom = "";
			if (operator != null) {
				if (operator.getPrenom() != null) {
					prenomNom = operator.getPrenom();
				}
				if (operator.getNom() != null) {
					prenomNom = prenomNom + ' ' + operator.getNom();
				}
				droitAccesView.setPrenomNom(prenomNom);
				droitAccesView.setVisaOperateur(operator.getCode());

				String officeImpot;
				try {
					final List<CollectiviteAdministrativeUtilisateur> collectivitesAdministratives = serviceSecuriteService.getCollectivitesUtilisateur(operator.getCode());
					final StringRenderer<CollectiviteAdministrative> nomsCourts = CollectiviteAdministrative::getNomCourt;
					officeImpot = CollectionsUtils.toString(collectivitesAdministratives, nomsCourts, ", ", null);
				}
				catch (ServiceSecuriteException e) {
					officeImpot = null;
					LOGGER.warn("Exception reçue à la réception des collectivités de l'utilisateur " + operator.getCode(), e);
				}
				if (officeImpot != null) {
					droitAccesView.setOfficeImpot(officeImpot);
				}
			}
			else{
				//SIFISC-26187 Pas d'opérateur trouvé, on affiche un texte dans le nom prenom
				final String msgErreur = String.format("Individu %d  non retourné par host-interfaces",droitAcces.getNoIndividuOperateur());
				droitAccesView.setPrenomNom(msgErreur);
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

	/**
	 * Persiste un droit d'acces
	 * @param droitAccesView
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(DroitAccesView droitAccesView) throws DroitAccesException {
		final long operateurId = droitAccesView.getNumeroUtilisateur();
		final long tiersId = droitAccesView.getNumero();
		final TypeDroitAcces type = droitAccesView.getType();
		final Niveau niveau = (droitAccesView.isLectureSeule() ? Niveau.LECTURE : Niveau.ECRITURE);

		droitAccesService.ajouteDroitAcces(operateurId, tiersId, type, niveau);
	}

	/**
	 * Annule une restriction
	 * @param idRestriction
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRestriction(Long idRestriction) throws DroitAccesException {
		droitAccesService.annuleDroitAcces(idRestriction);
	}
}