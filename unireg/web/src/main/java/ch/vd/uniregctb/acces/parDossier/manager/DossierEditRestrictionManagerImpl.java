package ch.vd.uniregctb.acces.parDossier.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.uniregctb.acces.parDossier.view.DroitAccesView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.DroitAccesService;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

/**
 * Classe qui gère le controller DossierEditRestrictionController
 *
 * @author xcifde
 *
 */
public class DossierEditRestrictionManagerImpl implements DossierEditRestrictionManager {

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
	 * @param numeroPP
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public DossierEditRestrictionView get(Long numeroPP) throws ServiceInfrastructureException {

		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(numeroPP);

		final DossierEditRestrictionView dossierEditRestrictionView = new DossierEditRestrictionView();

		final TiersGeneralView dossier = tiersGeneralManager.getPersonnePhysique(pp, true);
		dossierEditRestrictionView.setDossier(dossier);

		final Set<DroitAcces> droitsAccesAppliques = pp.getDroitsAccesAppliques();
		final List<DroitAccesView> droitsAccesView =  new ArrayList<DroitAccesView>(droitsAccesAppliques.size());
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
					prenomNom = prenomNom + " " + operator.getNom();
				}
				droitAccesView.setPrenomNom(prenomNom);
				droitAccesView.setVisaOperateur(operator.getCode());

				final List<ch.vd.infrastructure.model.CollectiviteAdministrative> collectivitesAdministrative = serviceSecuriteService.getCollectivitesUtilisateur(operator.getCode());
				String officeImpot = null;
				for (ch.vd.infrastructure.model.CollectiviteAdministrative collectiviteAdministrative : collectivitesAdministrative) {
					if (officeImpot != null) {
						officeImpot = officeImpot + ", " + collectiviteAdministrative.getNomCourt();
					}
					else {
						officeImpot = collectiviteAdministrative.getNomCourt();
					}
				}
				if (officeImpot != null) {
					droitAccesView.setOfficeImpot(officeImpot);
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
	 *
	 * @param dossierEditRestrictionView
	 * @param idRestriction
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRestriction(Long idRestriction) throws DroitAccesException {
		droitAccesService.annuleDroitAcces(idRestriction);
	}
}
