package ch.vd.uniregctb.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscal;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneeCivileEntreprise;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.Tiers;

@Controller
public class ConsultLogController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsultLogController.class);

	private HibernateTemplate hibernateTemplate;

	/**
	 * Les modalités sont les valeurs passables dans le paramètre "nature" de l'appel HTTP
	 */
	private enum LoggableEntity {
		AdresseMandataire(AdresseMandataire.class),
		AdresseTiers(AdresseTiers.class),
		AllegementFiscal(AllegementFiscal.class),
		AutreDocumentFiscal(AutreDocumentFiscal.class),
		DecisionAci(DecisionAci.class),
		DelaiDeclaration(DelaiDeclaration.class),
		DI(DeclarationImpotOrdinaire.class),
		DonneeCivileEntreprise(DonneeCivileEntreprise.class),
		DomicileEtablissement(DomicileEtablissement.class),
		DroitAcces(DroitAcces.class),
		EtatEntreprise(EtatEntreprise.class),
		EtatDeclaration(EtatDeclaration.class),
		Etiquette(EtiquetteTiers.class),
		Evenement(EvenementCivilRegPP.class),
		EvenementEch(EvenementCivilEch.class),
		EvenementOrganisation(EvenementOrganisation.class),
		FlagEntreprise(FlagEntreprise.class),
		ForFiscal(ForFiscal.class),
		Identification(IdentificationContribuable.class),
		Immeuble(Immeuble.class),
		LR(DeclarationImpotSource.class),
		MouvementDossier(MouvementDossier.class),
		Periodicite(Periodicite.class),
		QSNC(QuestionnaireSNC.class),
		RapportEntreTiers(RapportEntreTiers.class),
		RegimeFiscal(RegimeFiscal.class),
		Remarque(Remarque.class),
		SituationFamille(SituationFamille.class),
		Tache(Tache.class),
		Tiers(Tiers.class),
		UniteTraitementReqDes(UniteTraitement.class),
		EvenementRFImport(EvenementRFImport.class),
		EvenementRFMutation(EvenementRFMutation.class);

		private final Class<? extends HibernateEntity> entityClass;

		LoggableEntity(Class<? extends HibernateEntity> entityClass) {
			this.entityClass = entityClass;
		}
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@ResponseBody
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/common/consult-log.do", method = RequestMethod.GET)
	public ConsultLogView consultLog(@RequestParam("id") final long id, @RequestParam("nature") final String nature) {

		final LoggableEntity type;
		try {
			 type = LoggableEntity.valueOf(nature);
		}
		catch (Exception e) {
			LOGGER.warn(String.format("Nature '%s' non-supportée dans le contrôleur de consultation des logs (%s).", nature, e.getMessage()));
			return null;
		}

		final HibernateEntity entity = hibernateTemplate.get(type.entityClass, id);
		return entity == null ? null : new ConsultLogView(entity);
	}
}
