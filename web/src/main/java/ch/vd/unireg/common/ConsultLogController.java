package ch.vd.unireg.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.LiberationDeclaration;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;
import ch.vd.unireg.documentfiscal.LiberationDocumentFiscal;
import ch.vd.unireg.etiquette.EtiquetteTiers;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.foncier.AllegementFoncier;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.mouvement.MouvementDossier;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.PrincipalCommunauteRF;
import ch.vd.unireg.reqdes.UniteTraitement;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.DonneeCivileEntreprise;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.Remarque;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.Tiers;

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
		CoordonneesFinancieres(CoordonneesFinancieres.class),
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
		EvenementEntreprise(EvenementEntreprise.class),
		FlagEntreprise(FlagEntreprise.class),
		ForFiscal(ForFiscal.class),
		Identification(IdentificationContribuable.class),
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
		EvenementRFMutation(EvenementRFMutation.class),
		AllegementFoncier(AllegementFoncier.class),
		DroitRF(DroitRF.class),
		PrincipalCommunauteRF(PrincipalCommunauteRF.class),
		LiberationDocumentFiscal(LiberationDocumentFiscal.class),
		LiberationDeclaration(LiberationDeclaration.class),
		DelaiDocumentFiscal(DelaiDocumentFiscal.class),
		EtatDocumentFiscal(EtatDocumentFiscal.class);

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
