package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationDAO;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.declaration.PeriodiciteDAO;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.MouvementDossierDAO;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.rf.ImmeubleDAO;
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

public class ConsultLogController extends AbstractSimpleFormController {

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
	private EvenementCivilRegPPDAO evenementCivilExterneDAO;
	private IdentCtbDAO identCtbDAO;
	private PeriodiciteDAO periodiciteDAO;
	private EtatDeclarationDAO etatDeclarationDAO;
	private ImmeubleDAO immeubleDAO;

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
	public final static String NATURE_ETAT_PARAMETER_VALUE = "EtatDeclaration";
	public final static String NATURE_TIERS_PARAMETER_VALUE = "Tiers";
	public final static String NATURE_MOUVEMENT_PARAMETER_VALUE = "MouvementDossier";
	public final static String NATURE_TACHE_PARAMETER_VALUE = "Tache";
	public final static String NATURE_DROIT_ACCES_PARAMETER_VALUE = "DroitAcces";
	public final static String NATURE_EVENEMENT_PARAMETER_VALUE = "Evenement";
	public final static String NATURE_IDENTIFICATION_PARAMETER_VALUE = "identification";
	public final static String NATURE_PERIODICITE_PARAMETER_VALUE = "periodicite";
	public final static String NATURE_IMMEUBLE = "Immeuble";

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

		final ConsultLogView consultLogView = template.execute(new TransactionCallback<ConsultLogView>() {
			@Override
			public ConsultLogView doInTransaction(TransactionStatus status) {
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
					EvenementCivilRegPP evenementCivilExterne = evenementCivilExterneDAO.get(id);
					return fillConsultLogView(evenementCivilExterne);
				}
				else if (nature.equals(NATURE_IDENTIFICATION_PARAMETER_VALUE)) {
					IdentificationContribuable message = identCtbDAO.get(id);
					return fillConsultLogView(message);
				}
				else if (nature.equals(NATURE_PERIODICITE_PARAMETER_VALUE)) {
					Periodicite periodicite = periodiciteDAO.get(id);
					return fillConsultLogView(periodicite);
				}
				else if (nature.equals(NATURE_ETAT_PARAMETER_VALUE)) {
					EtatDeclaration etatDeclaration = etatDeclarationDAO.get(id);
					return fillConsultLogView(etatDeclaration);
				}
				else if (nature.equals(NATURE_IMMEUBLE)) {
					Immeuble i = immeubleDAO.get(id);
					return fillConsultLogView(i);
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setForFiscalDAO(ForFiscalDAO forFiscalDAO) {
		this.forFiscalDAO = forFiscalDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSituationFamilleDAO(SituationFamilleDAO situationFamilleDAO) {
		this.situationFamilleDAO = situationFamilleDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAdresseTiersDAO(AdresseTiersDAO adresseTiersDAO) {
		this.adresseTiersDAO = adresseTiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setLrDAO(ListeRecapitulativeDAO lrDAO) {
		this.lrDAO = lrDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMouvementDossierDAO(MouvementDossierDAO mouvementDossierDAO) {
		this.mouvementDossierDAO = mouvementDossierDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilExterneDAO(EvenementCivilRegPPDAO evenementCivilExterneDAO) {
		this.evenementCivilExterneDAO = evenementCivilExterneDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPeriodiciteDAO(PeriodiciteDAO periodiciteDAO) {
		this.periodiciteDAO = periodiciteDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEtatDeclarationDAO(EtatDeclarationDAO etatDeclarationDAO) {
		this.etatDeclarationDAO = etatDeclarationDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImmeubleDAO(ImmeubleDAO immeubleDAO) {
		this.immeubleDAO = immeubleDAO;
	}
}
