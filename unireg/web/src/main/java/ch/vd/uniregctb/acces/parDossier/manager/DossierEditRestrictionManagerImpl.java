package ch.vd.uniregctb.acces.parDossier.manager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.uniregctb.acces.parDossier.view.DroitAccesView;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
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
	 * @throws InfrastructureException
	 */
	public DossierEditRestrictionView get(Long numeroPP) throws InfrastructureException {
		PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(numeroPP);

		DossierEditRestrictionView dossierEditRestrictionView = new DossierEditRestrictionView();

		TiersGeneralView dossier = tiersGeneralManager.get(pp, true);
		dossierEditRestrictionView.setDossier(dossier);
		Set<DroitAcces> droitsAccesAppliques = pp.getDroitsAccesAppliques();
		Iterator<DroitAcces> itDroitAcces = droitsAccesAppliques.iterator();
		List<DroitAccesView> droitsAccesView =  new ArrayList<DroitAccesView>();
		while (itDroitAcces.hasNext()) {
			DroitAcces droitAcces = itDroitAcces.next();
			DroitAccesView droitAccesView = new DroitAccesView();
			droitAccesView.setId(droitAcces.getId());
			droitAccesView.setAnnule(droitAcces.isAnnule());
			droitAccesView.setType(droitAcces.getType());
			Operateur operator = serviceSecuriteService.getOperateur(droitAcces.getNoIndividuOperateur());
			String prenomNom = new String("");
			if (operator != null) {
				if (operator.getPrenom() != null) {
					prenomNom = operator.getPrenom();
				}
				if (operator.getNom() != null) {
					prenomNom = prenomNom + " " + operator.getNom();
				}
				droitAccesView.setPrenomNom(prenomNom);
				droitAccesView.setVisaOperateur(operator.getCode());

				List<ch.vd.infrastructure.model.CollectiviteAdministrative> collectivitesAdministrative = serviceSecuriteService.getCollectivitesUtilisateur(operator.getCode());
				Iterator<ch.vd.infrastructure.model.CollectiviteAdministrative> itCollectiviteAdministrative = collectivitesAdministrative.iterator();
				String officeImpot = null;
				while (itCollectiviteAdministrative.hasNext()) {
					ch.vd.infrastructure.model.CollectiviteAdministrative collectiviteAdministrative = itCollectiviteAdministrative.next();
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
			if (droitAcces.getNiveau().equals(Niveau.LECTURE)) {
				droitAccesView.setLectureSeule(true);
			}
			else if (droitAcces.getNiveau().equals(Niveau.ECRITURE)) {
				droitAccesView.setLectureSeule(false);
			}
			droitsAccesView.add(droitAccesView);
		}
		dossierEditRestrictionView.setRestrictions(droitsAccesView);
		return dossierEditRestrictionView;
	}

	/**
	 * Persiste un droit d'acces
	 * @param droitAccesView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(DroitAccesView droitAccesView) throws DroitAccesException {
		final long operateurId = droitAccesView.getNumeroUtilisateur();
		final long tiersId = droitAccesView.getNumero();
		final TypeDroitAcces type = droitAccesView.getType();
		final Niveau niveau = (droitAccesView.isLectureSeule() ? Niveau.LECTURE : Niveau.ECRITURE);

		droitAccesService.addDroitAcces(operateurId, tiersId, type, niveau);
	}

	/**
	 * Annule une restriction
	 *
	 * @param dossierEditRestrictionView
	 * @param idRestriction
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRestriction(Long idRestriction) throws DroitAccesException {
		droitAccesService.annuleDroitAcces(idRestriction);
	}
}
