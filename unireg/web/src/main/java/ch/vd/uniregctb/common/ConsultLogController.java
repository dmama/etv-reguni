package ch.vd.uniregctb.common;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.adresse.AdresseTiersDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclarationDAO;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.PeriodiciteDAO;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.mouvement.MouvementDossierDAO;
import ch.vd.uniregctb.rf.ImmeubleDAO;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.tiers.ForFiscalDAO;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.SituationFamilleDAO;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TiersDAO;

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
	private IdentCtbDAO identCtbDAO;
	private PeriodiciteDAO periodiciteDAO;
	private EtatDeclarationDAO etatDeclarationDAO;
	private ImmeubleDAO immeubleDAO;

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
	public final static String NATURE_EVENEMENT_ECH_PARAMETER_VALUE = "EvenementEch";
	public final static String NATURE_IDENTIFICATION_PARAMETER_VALUE = "identification";
	public final static String NATURE_PERIODICITE_PARAMETER_VALUE = "periodicite";
	public final static String NATURE_IMMEUBLE = "Immeuble";

	@ResponseBody
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/common/consult-log.do", method = RequestMethod.GET)
	public ConsultLogView consultLog(@RequestParam("id") final long id, @RequestParam("nature") final String nature) {

		HibernateEntity objet = null;

		if (nature.equals(NATURE_FOR_PARAMETER_VALUE)) {
			objet = forFiscalDAO.get(id);
		}
		else if (nature.equals(NATURE_SITUATION_PARAMETER_VALUE)) {
			objet = situationFamilleDAO.get(id);
		}
		else if (nature.equals(NATURE_ADRESSE_PARAMETER_VALUE)) {
			objet = adresseTiersDAO.get(id);
		}
		else if (nature.equals(NATURE_RAPPORT_PARAMETER_VALUE)) {
			objet = rapportEntreTiersDAO.get(id);
		}
		else if (nature.equals(NATURE_DI_PARAMETER_VALUE)) {
			objet = diDAO.get(id);
		}
		else if (nature.equals(NATURE_LR_PARAMETER_VALUE)) {
			objet = lrDAO.get(id);
		}
		else if (nature.equals(NATURE_TIERS_PARAMETER_VALUE)) {
			objet = tiersDAO.get(id);
		}
		else if (nature.equals(NATURE_MOUVEMENT_PARAMETER_VALUE)) {
			objet = mouvementDossierDAO.get(id);
		}
		else if (nature.equals(NATURE_TACHE_PARAMETER_VALUE)) {
			objet = tacheDAO.get(id);
		}
		else if (nature.equals(NATURE_DROIT_ACCES_PARAMETER_VALUE)) {
			objet = droitAccesDAO.get(id);
		}
		else if (nature.equals(NATURE_EVENEMENT_PARAMETER_VALUE)) {
			objet = evenementCivilRegPPDAO.get(id);
		}
		else if (nature.equals(NATURE_EVENEMENT_ECH_PARAMETER_VALUE)) {
			objet = evenementCivilEchDAO.get(id);
		}
		else if (nature.equals(NATURE_IDENTIFICATION_PARAMETER_VALUE)) {
			objet = identCtbDAO.get(id);
		}
		else if (nature.equals(NATURE_PERIODICITE_PARAMETER_VALUE)) {
			objet = periodiciteDAO.get(id);
		}
		else if (nature.equals(NATURE_ETAT_PARAMETER_VALUE)) {
			objet = etatDeclarationDAO.get(id);
		}
		else if (nature.equals(NATURE_IMMEUBLE)) {
			objet = immeubleDAO.get(id);
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
