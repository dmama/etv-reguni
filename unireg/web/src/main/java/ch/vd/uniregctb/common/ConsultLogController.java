package ch.vd.uniregctb.common;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DelaiDeclarationDAO;
import ch.vd.uniregctb.declaration.EtatDeclarationDAO;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.PeriodiciteDAO;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.uniregctb.mouvement.MouvementDossierDAO;
import ch.vd.uniregctb.reqdes.UniteTraitementDAO;
import ch.vd.uniregctb.rf.ImmeubleDAO;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.tiers.ForFiscalDAO;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.SituationFamilleDAO;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.dao.DecisionAciDAO;
import ch.vd.uniregctb.tiers.dao.DomicileEtablissementDAO;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;

@Controller
public class ConsultLogController {

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
	private EvenementCivilRegPPDAO evenementCivilRegPPDAO;
	private EvenementCivilEchDAO evenementCivilEchDAO;
	private EvenementOrganisationDAO evenementOrganisationDAO;
	private IdentCtbDAO identCtbDAO;
	private PeriodiciteDAO periodiciteDAO;
	private EtatDeclarationDAO etatDeclarationDAO;
	private DelaiDeclarationDAO delaiDeclarationDAO;
	private ImmeubleDAO immeubleDAO;
	private UniteTraitementDAO uniteTraitementDAO;
	private RemarqueDAO remarqueDAO;
	private DecisionAciDAO decisionAciDAO;
	private DomicileEtablissementDAO domicileEtablissementDAO;

	public static final String NATURE_FOR_PARAMETER_VALUE = "ForFiscal";
	public static final String NATURE_SITUATION_PARAMETER_VALUE = "SituationFamille";
	public static final String NATURE_RAPPORT_PARAMETER_VALUE = "RapportEntreTiers";
	public static final String NATURE_ADRESSE_PARAMETER_VALUE = "AdresseTiers";
	public static final String NATURE_DI_PARAMETER_VALUE = "DI";
	public static final String NATURE_LR_PARAMETER_VALUE = "LR";
	public static final String NATURE_DELAI_PARAMETER_VALUE = "DelaiDeclaration";
	public static final String NATURE_ETAT_PARAMETER_VALUE = "EtatDeclaration";
	public static final String NATURE_TIERS_PARAMETER_VALUE = "Tiers";
	public static final String NATURE_MOUVEMENT_PARAMETER_VALUE = "MouvementDossier";
	public static final String NATURE_TACHE_PARAMETER_VALUE = "Tache";
	public static final String NATURE_DROIT_ACCES_PARAMETER_VALUE = "DroitAcces";
	public static final String NATURE_EVENEMENT_PARAMETER_VALUE = "Evenement";
	public static final String NATURE_EVENEMENT_ECH_PARAMETER_VALUE = "EvenementEch";
	public static final String NATURE_EVENEMENT_ORGANISATION_PARAMETER_VALUE = "EvenementOrganisation";
	public static final String NATURE_IDENTIFICATION_PARAMETER_VALUE = "identification";
	public static final String NATURE_PERIODICITE_PARAMETER_VALUE = "periodicite";
	public static final String NATURE_IMMEUBLE = "Immeuble";
	public static final String NATURE_UNITE_TRAITEMENT_REQDES = "UniteTraitementReqDes";
	public static final String NATURE_REMARQUE = "Remarque";
	public static final String NATURE_DECISION_ACI = "DecisionAci";
	public static final String NATURE_DOMICILE_ETABLISSEMENT = "DomicileEtablissement";

	@ResponseBody
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/common/consult-log.do", method = RequestMethod.GET)
	public ConsultLogView consultLog(@RequestParam("id") final long id, @RequestParam("nature") final String nature) {

		HibernateEntity objet = null;

		switch (nature) {
		case NATURE_FOR_PARAMETER_VALUE:
			objet = forFiscalDAO.get(id);
			break;
		case NATURE_SITUATION_PARAMETER_VALUE:
			objet = situationFamilleDAO.get(id);
			break;
		case NATURE_ADRESSE_PARAMETER_VALUE:
			objet = adresseTiersDAO.get(id);
			break;
		case NATURE_RAPPORT_PARAMETER_VALUE:
			objet = rapportEntreTiersDAO.get(id);
			break;
		case NATURE_DI_PARAMETER_VALUE:
			objet = diDAO.get(id);
			break;
		case NATURE_LR_PARAMETER_VALUE:
			objet = lrDAO.get(id);
			break;
		case NATURE_TIERS_PARAMETER_VALUE:
			objet = tiersDAO.get(id);
			break;
		case NATURE_MOUVEMENT_PARAMETER_VALUE:
			objet = mouvementDossierDAO.get(id);
			break;
		case NATURE_TACHE_PARAMETER_VALUE:
			objet = tacheDAO.get(id);
			break;
		case NATURE_DROIT_ACCES_PARAMETER_VALUE:
			objet = droitAccesDAO.get(id);
			break;
		case NATURE_EVENEMENT_PARAMETER_VALUE:
			objet = evenementCivilRegPPDAO.get(id);
			break;
		case NATURE_EVENEMENT_ECH_PARAMETER_VALUE:
			objet = evenementCivilEchDAO.get(id);
			break;
		case NATURE_EVENEMENT_ORGANISATION_PARAMETER_VALUE:
			objet = evenementOrganisationDAO.get(id);
			break;
		case NATURE_IDENTIFICATION_PARAMETER_VALUE:
			objet = identCtbDAO.get(id);
			break;
		case NATURE_PERIODICITE_PARAMETER_VALUE:
			objet = periodiciteDAO.get(id);
			break;
		case NATURE_ETAT_PARAMETER_VALUE:
			objet = etatDeclarationDAO.get(id);
			break;
		case NATURE_DELAI_PARAMETER_VALUE:
			objet = delaiDeclarationDAO.get(id);
			break;
		case NATURE_IMMEUBLE:
			objet = immeubleDAO.get(id);
			break;
		case NATURE_UNITE_TRAITEMENT_REQDES:
			objet = uniteTraitementDAO.get(id);
			break;
		case NATURE_REMARQUE:
			objet = remarqueDAO.get(id);
			break;
		case NATURE_DECISION_ACI:
			objet = decisionAciDAO.get(id);
			break;
		case NATURE_DOMICILE_ETABLISSEMENT:
			objet = domicileEtablissementDAO.get(id);
			break;
		}

		return objet == null ? null : new ConsultLogView(objet);
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
	public void setEvenementCivilRegPPDAO(EvenementCivilRegPPDAO evenementCivilRegPPDAO) {
		this.evenementCivilRegPPDAO = evenementCivilRegPPDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilEchDAO(EvenementCivilEchDAO evenementCivilEchDAO) {
		this.evenementCivilEchDAO = evenementCivilEchDAO;
	}

	public void setEvenementOrganisationDAO(EvenementOrganisationDAO evenementOrganisationDAO) {
		this.evenementOrganisationDAO = evenementOrganisationDAO;
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

	public void setDelaiDeclarationDAO(DelaiDeclarationDAO delaiDeclarationDAO) {
		this.delaiDeclarationDAO = delaiDeclarationDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImmeubleDAO(ImmeubleDAO immeubleDAO) {
		this.immeubleDAO = immeubleDAO;
	}

	public void setUniteTraitementDAO(UniteTraitementDAO uniteTraitementDAO) {
		this.uniteTraitementDAO = uniteTraitementDAO;
	}

	public void setRemarqueDAO(RemarqueDAO remarqueDAO) {
		this.remarqueDAO = remarqueDAO;
	}

	public void setDecisionAciDAO(DecisionAciDAO decisionAciDAO) {
		this.decisionAciDAO = decisionAciDAO;
	}

	public void setDomicileEtablissementDAO(DomicileEtablissementDAO domicileEtablissementDAO) {
		this.domicileEtablissementDAO = domicileEtablissementDAO;
	}
}
