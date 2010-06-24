package ch.vd.uniregctb.common;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilDAO;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.MouvementDossierDAO;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalDAO;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleDAO;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class ConsultLogController  extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(ConsultLogController.class);

	private ForFiscalDAO forFiscalDAO;
	private SituationFamilleDAO situationFamilleDAO;
	private AdresseTiersDAO adresseTiersDAO;
	private RapportEntreTiersDAO rapportEntreTiersDAO;
	private DeclarationImpotOrdinaireDAO diDAO;
	private ListeRecapitulativeDAO lrDAO;
	private TiersDAO tiersDAO;
	private MouvementDossierDAO mouvementDossierDAO;
	private TacheDAO tacheDAO;
	private DroitAccesDAO droitAccesDAO;
	private EvenementCivilDAO evenementCivilDAO;
	private IdentCtbDAO identCtbDAO;

	private PlatformTransactionManager transactionManager;

	public final static String ID_PARAMETER_NAME = "id";
	public final static String NATURE_PARAMETER_NAME = "nature";
	public final static String NATURE_FOR_PARAMETER_VALUE = "ForFiscal";
	public final static String NATURE_SITUATION_PARAMETER_VALUE = "SituationFamille";
	public final static String NATURE_RAPPORT_PARAMETER_VALUE = "RapportEntreTiers";
	public final static String NATURE_ADRESSE_PARAMETER_VALUE = "AdresseTiers";
	public final static String NATURE_DI_PARAMETER_VALUE = "DI";
	public final static String NATURE_LR_PARAMETER_VALUE = "LR";
	public final static String NATURE_DELAI_PARAMETER_VALUE = "Delai";
	public final static String NATURE_ETAT_PARAMETER_VALUE = "Etat";
	public final static String NATURE_TIERS_PARAMETER_VALUE = "Tiers";
	public final static String NATURE_MOUVEMENT_PARAMETER_VALUE = "MouvementDossier";
	public final static String NATURE_TACHE_PARAMETER_VALUE = "Tache";
	public final static String NATURE_DROIT_ACCES_PARAMETER_VALUE = "DroitAcces";
	public final static String NATURE_EVENEMENT_PARAMETER_VALUE = "Evenement";
	public final static String NATURE_IDENTIFICATION_PARAMETER_VALUE = "identification";
	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@SuppressWarnings({"UnnecessaryLocalVariable"})
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final String idParam = request.getParameter(ID_PARAMETER_NAME);
		final String nature = request.getParameter(NATURE_PARAMETER_NAME);

		if (idParam == null || "".equals(idParam.trim())) {
			return null;
		}

		final Long id = Long.parseLong(idParam);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final ConsultLogView consultLogView = (ConsultLogView) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				if (nature.equals(NATURE_FOR_PARAMETER_VALUE)) {
					ForFiscal forFiscal = forFiscalDAO.get(id);
					return fillConsultLogView(forFiscal);
				}
				else if (nature.equals(NATURE_SITUATION_PARAMETER_VALUE)) {
					SituationFamille situationFamille = situationFamilleDAO.get(id);
					return fillConsultLogView(situationFamille);
				}
				else if (nature.equals(NATURE_ADRESSE_PARAMETER_VALUE)) {
					AdresseTiers adresseTiers = adresseTiersDAO.get(id);
					return fillConsultLogView(adresseTiers);
				}
				else if (nature.equals(NATURE_RAPPORT_PARAMETER_VALUE)) {
					RapportEntreTiers rapportEntreTiers = rapportEntreTiersDAO.get(id);
					return fillConsultLogView(rapportEntreTiers);
				}
				else if (nature.equals(NATURE_DI_PARAMETER_VALUE)) {
					DeclarationImpotOrdinaire di = diDAO.get(id);
					return fillConsultLogView(di);
				}
				else if (nature.equals(NATURE_LR_PARAMETER_VALUE)) {
					DeclarationImpotSource lr = lrDAO.get(id);
					return fillConsultLogView(lr);
				}
				else if (nature.equals(NATURE_TIERS_PARAMETER_VALUE)) {
					Tiers tiers = tiersDAO.get(id);
					return fillConsultLogView(tiers);
				}
				else if (nature.equals(NATURE_MOUVEMENT_PARAMETER_VALUE)) {
					MouvementDossier mvt = mouvementDossierDAO.get(id);
					return fillConsultLogView(mvt);
				}
				else if (nature.equals(NATURE_TACHE_PARAMETER_VALUE)) {
					Tache tache = tacheDAO.get(id);
					return fillConsultLogView(tache);
				}
				else if (nature.equals(NATURE_DROIT_ACCES_PARAMETER_VALUE)) {
					DroitAcces droitAcces = droitAccesDAO.get(id);
					return fillConsultLogView(droitAcces);
				}
				else if (nature.equals(NATURE_EVENEMENT_PARAMETER_VALUE)) {
					EvenementCivilData evenementCivilData = evenementCivilDAO.get(id);
					return fillConsultLogView(evenementCivilData);
				}
				else if (nature.equals(NATURE_IDENTIFICATION_PARAMETER_VALUE)) {
					IdentificationContribuable message  = identCtbDAO.get(id);
					return fillConsultLogView(message);
				}
				return null;
			}
		});


		return consultLogView;
	}

	private ConsultLogView fillConsultLogView(HibernateEntity objet) {
		ConsultLogView consultLogView = new ConsultLogView();
		consultLogView.setDateHeureCreation(objet.getLogCreationDate());
		consultLogView.setUtilisateurCreation(objet.getLogCreationUser());
		consultLogView.setDateHeureDerniereModif(objet.getLogModifDate());
		consultLogView.setUtilisateurDerniereModif(objet.getLogModifUser());
		consultLogView.setDateHeureAnnulation(objet.getAnnulationDate());
		consultLogView.setUtilisateurAnnulation(objet.getAnnulationUser());
		return consultLogView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		return mav;
	}

	public void setForFiscalDAO(ForFiscalDAO forFiscalDAO) {
		this.forFiscalDAO = forFiscalDAO;
	}

	public void setSituationFamilleDAO(SituationFamilleDAO situationFamilleDAO) {
		this.situationFamilleDAO = situationFamilleDAO;
	}

	public void setAdresseTiersDAO(AdresseTiersDAO adresseTiersDAO) {
		this.adresseTiersDAO = adresseTiersDAO;
	}

	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	public void setLrDAO(ListeRecapitulativeDAO lrDAO) {
		this.lrDAO = lrDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setMouvementDossierDAO(MouvementDossierDAO mouvementDossierDAO) {
		this.mouvementDossierDAO = mouvementDossierDAO;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public EvenementCivilDAO getEvenementCivilDAO() {
		return evenementCivilDAO;
	}

	public void setEvenementCivilDAO(EvenementCivilDAO evenementCivilDAO) {
		this.evenementCivilDAO = evenementCivilDAO;
	}

	public IdentCtbDAO getIdentCtbDAO() {
		return identCtbDAO;
	}

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}
}
