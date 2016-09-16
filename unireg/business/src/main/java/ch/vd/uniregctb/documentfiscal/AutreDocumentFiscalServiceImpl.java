package ch.vd.uniregctb.documentfiscal;

import javax.jms.JMSException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParTypeAt;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeLettreBienvenue;

/**
 * Implémentation du service de gestion des "autres documents fiscaux"
 */
public class AutreDocumentFiscalServiceImpl implements AutreDocumentFiscalService {

	private ParametreAppService parametreAppService;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private AssujettissementService assujettissementService;
	private DelaisService delaiService;
	private EditiqueService editiqueService;
	private EditiqueCompositionService editiqueCompositionService;
	private EvenementFiscalService evenementFiscalService;

	private final Map<Class<? extends AutreDocumentFiscal>, TypeDocumentEditique> typesDocumentEnvoiInitial = buildTypesDocumentEnvoiInitial();
	private final Map<Class<? extends AutreDocumentFiscalAvecSuivi>, TypeDocumentEditique> typesDocumentEnvoiRappel = buildTypesDocumentEnvoiRappel();

	private static Map<Class<? extends AutreDocumentFiscal>, TypeDocumentEditique> buildTypesDocumentEnvoiInitial() {
		final Map<Class<? extends AutreDocumentFiscal>, TypeDocumentEditique> map = new HashMap<>();
		map.put(LettreBienvenue.class, TypeDocumentEditique.LETTRE_BIENVENUE);
		map.put(AutorisationRadiationRC.class, TypeDocumentEditique.AUTORISATION_RADIATION_RC);
		map.put(DemandeBilanFinal.class, TypeDocumentEditique.DEMANDE_BILAN_FINAL);
		map.put(LettreTypeInformationLiquidation.class, TypeDocumentEditique.LETTRE_TYPE_INFO_LIQUIDATION);
		return Collections.unmodifiableMap(map);
	}

	private static Map<Class<? extends AutreDocumentFiscalAvecSuivi>, TypeDocumentEditique> buildTypesDocumentEnvoiRappel() {
		final Map<Class<? extends AutreDocumentFiscalAvecSuivi>, TypeDocumentEditique> map = new HashMap<>();
		map.put(LettreBienvenue.class, TypeDocumentEditique.RAPPEL);
		return Collections.unmodifiableMap(map);
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setDelaiService(DelaisService delaiService) {
		this.delaiService = delaiService;
	}

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	@Override
	public EnvoiLettresBienvenueResults envoyerLettresBienvenueEnMasse(RegDate dateTraitement, int delaiCarence, StatusManager statusManager) {
		final EnvoiLettresBienvenueProcessor processor = new EnvoiLettresBienvenueProcessor(parametreAppService, hibernateTemplate, transactionManager, tiersService, assujettissementService, this);
		return processor.run(dateTraitement, delaiCarence, statusManager);
	}

	@Override
	public RappelLettresBienvenueResults envoyerRappelsLettresBienvenueEnMasse(RegDate dateTraitement, StatusManager statusManager) {
		final RappelLettresBienvenueProcessor processor = new RappelLettresBienvenueProcessor(parametreAppService, hibernateTemplate, transactionManager, this, delaiService);
		return processor.run(dateTraitement, statusManager);
	}

	@Override
	public LettreBienvenue envoyerLettreBienvenueBatch(Entreprise entreprise, RegDate dateTraitement) throws AutreDocumentFiscalException {
		final RegDate dateEnvoi = delaiService.getDateFinDelaiCadevImpressionLettreBienvenue(dateTraitement);
		final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourLettreBienvenue());
		final TypeLettreBienvenue typeLettre = computeTypeLettreBienvenue(entreprise, dateTraitement);

		final LettreBienvenue lettre = new LettreBienvenue();
		lettre.setDateEnvoi(dateEnvoi);
		lettre.setDelaiRetour(delaiRetour);
		lettre.setType(typeLettre);
		lettre.setEntreprise(entreprise);

		final LettreBienvenue saved = hibernateTemplate.merge(lettre);
		try {
			editiqueCompositionService.imprimeLettreBienvenueForBatch(saved, dateTraitement);
			evenementFiscalService.publierEvenementFiscalEmissionLettreBienvenue(saved);
		}
		catch (EditiqueException e) {
			throw new AutreDocumentFiscalException(e);
		}
		return lettre;
	}

	private TypeLettreBienvenue computeTypeLettreBienvenue(Entreprise e, RegDate dateTraitement) throws AutreDocumentFiscalException {

		// if faut tout d'abord regarder le for principal à la date de traitement
		final ForsParTypeAt fors = e.getForsParTypeAt(dateTraitement, false);
		if (fors.principal == null) {
			throw new AutreDocumentFiscalException("Pas de for principal actif à la date de traitement, impossible de déterminer le type d'autorité fiscale du siège fiscal de l'entreprise.");
		}
		final TypeAutoriteFiscale taf = fors.principal.getTypeAutoriteFiscale();

		final TypeLettreBienvenue type;
		if (taf == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			if (tiersService.hasInscriptionActiveRC(e, dateTraitement)) {
				type = TypeLettreBienvenue.VD_RC;
			}
			else {
				final CategorieEntreprise categorie = tiersService.getCategorieEntreprise(e, dateTraitement);
				if (categorie == CategorieEntreprise.APM) {
					type = TypeLettreBienvenue.APM_VD_NON_RC;
				}
				else {
					throw new AutreDocumentFiscalException("Entreprise non-APM avec siège vaudois mais non-inscrite au RC.");
				}
			}
		}
		else {
			// regardons les fors secondaires et, surtout leur motif de rattachement
			if (fors.secondaires.isEmpty()) {
				throw new AutreDocumentFiscalException("Pas de for secondaire actif à la date de traitement sur une entreprise avec siège " + taf);
			}

			// quels motifs de rattachement a-t-on trouvés ?
			boolean trouveImmeuble = false;
			boolean trouveEtablissement = false;
			for (ForFiscalSecondaire ffs : fors.secondaires) {
				switch (ffs.getMotifRattachement()) {
				case IMMEUBLE_PRIVE:
					trouveImmeuble = true;
					break;
				case ETABLISSEMENT_STABLE:
					trouveEtablissement = true;
					break;
				default:
					break;
				}
				if (trouveEtablissement && trouveImmeuble) {
					// pas la peine de chercher plus loin, on a déjà trouvé tout ce qu'on cherchait
					break;
				}
			}

			// c'est le cas immeuble qui gagne
			if (trouveImmeuble) {
				type = TypeLettreBienvenue.HS_HC_IMMEUBLE;
			}
			else if (trouveEtablissement) {
				type = TypeLettreBienvenue.HS_HC_ETABLISSEMENT;
			}
			else {
				throw new AutreDocumentFiscalException("Ni for secondaire immeuble, ni for secondaire établissement trouvé, à la date de traitement, sur une entreprise avec siège " + taf);
			}
		}

		return type;
	}

	@Override
	public void envoyerRappelLettreBienvenueBatch(LettreBienvenue lettre, RegDate dateTraitement) throws AutreDocumentFiscalException {
		try {
			editiqueCompositionService.imprimeRappelLettreBienvenueForBatch(lettre, dateTraitement);
		}
		catch (EditiqueException e) {
			throw new AutreDocumentFiscalException(e);
		}
	}

	@Override
	public EditiqueResultat envoyerAutorisationRadiationRCOnline(Entreprise e, RegDate dateTraitement, RegDate dateDemandeInitiale) throws AutreDocumentFiscalException {
		try {
			final AutorisationRadiationRC lettre = new AutorisationRadiationRC();
			lettre.setDateDemande(dateDemandeInitiale);
			lettre.setDateEnvoi(dateTraitement);
			lettre.setEntreprise(e);

			final AutorisationRadiationRC saved = hibernateTemplate.merge(lettre);
			e.addAutreDocumentFiscal(saved);
			return editiqueCompositionService.imprimeAutorisationRadiationRCOnline(saved, dateTraitement);
		}
		catch (EditiqueException | JMSException ex) {
			throw new AutreDocumentFiscalException(ex);
		}
	}

	@Override
	public EditiqueResultat envoyerDemandeBilanFinalOnline(Entreprise e, RegDate dateTraitement, int periodeFiscale, RegDate dateRequisitionRadiation) throws AutreDocumentFiscalException {
		try {
			final DemandeBilanFinal lettre = new DemandeBilanFinal();
			lettre.setDateEnvoi(dateTraitement);
			lettre.setDateRequisitionRadiation(dateRequisitionRadiation);
			lettre.setPeriodeFiscale(periodeFiscale);
			lettre.setEntreprise(e);

			final DemandeBilanFinal saved = hibernateTemplate.merge(lettre);
			e.addAutreDocumentFiscal(saved);
			return editiqueCompositionService.imprimeDemandeBilanFinalOnline(saved, dateTraitement);
		}
		catch (EditiqueException | JMSException ex) {
			throw new AutreDocumentFiscalException(ex);
		}
	}

	@Override
	public EditiqueResultat envoyerLettreTypeInformationLiquidationOnline(Entreprise e, RegDate dateTraitement) throws AutreDocumentFiscalException {
		try {
			final LettreTypeInformationLiquidation lettre = new LettreTypeInformationLiquidation();
			lettre.setDateEnvoi(dateTraitement);
			lettre.setEntreprise(e);

			final LettreTypeInformationLiquidation saved = hibernateTemplate.merge(lettre);
			e.addAutreDocumentFiscal(saved);
			return editiqueCompositionService.imprimeLettreTypeInformationLiquidationOnline(saved, dateTraitement);
		}
		catch (EditiqueException | JMSException ex) {
			throw new AutreDocumentFiscalException(ex);
		}
	}

	@Override
	public EditiqueResultat getCopieConformeDocumentInitial(AutreDocumentFiscal document) throws EditiqueException {
		final TypeDocumentEditique typeDocument = typesDocumentEnvoiInitial.get(document.getClass());
		return editiqueService.getPDFDeDocumentDepuisArchive(document.getEntreprise().getNumero(),
		                                                     typeDocument,
		                                                     document.getCleArchivage());
	}

	@Override
	public EditiqueResultat getCopieConformeDocumentRappel(AutreDocumentFiscalAvecSuivi document) throws EditiqueException {
		final TypeDocumentEditique typeDocument = typesDocumentEnvoiRappel.get(document.getClass());
		return editiqueService.getPDFDeDocumentDepuisArchive(document.getEntreprise().getNumero(),
		                                                     typeDocument,
		                                                     document.getCleArchivageRappel());
	}
}
