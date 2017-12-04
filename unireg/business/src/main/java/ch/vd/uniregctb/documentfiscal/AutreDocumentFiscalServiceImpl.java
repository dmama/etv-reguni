package ch.vd.uniregctb.documentfiscal;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AddAndSaveHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.evenement.declaration.EvenementDeclarationException;
import ch.vd.uniregctb.evenement.declaration.EvenementDeclarationPMSender;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.foncier.EnvoiFormulairesDemandeDegrevementICIProcessor;
import ch.vd.uniregctb.foncier.EnvoiFormulairesDemandeDegrevementICIResults;
import ch.vd.uniregctb.foncier.RappelFormulairesDemandeDegrevementICIProcessor;
import ch.vd.uniregctb.foncier.RappelFormulairesDemandeDegrevementICIResults;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalService;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
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
	private EvenementDeclarationPMSender evtDeclarationPMSender;
	private RegistreFoncierService registreFoncierService;
	private RegimeFiscalService regimeFiscalService;

	private final Map<Class<? extends AutreDocumentFiscal>, TypeDocumentEditique> typesDocumentEnvoiInitial = buildTypesDocumentEnvoiInitial();
	private final Map<Class<? extends AutreDocumentFiscalAvecSuivi>, TypeDocumentEditique> typesDocumentEnvoiRappel = buildTypesDocumentEnvoiRappel();

	private static Map<Class<? extends AutreDocumentFiscal>, TypeDocumentEditique> buildTypesDocumentEnvoiInitial() {
		final Map<Class<? extends AutreDocumentFiscal>, TypeDocumentEditique> map = new HashMap<>();
		map.put(LettreBienvenue.class, TypeDocumentEditique.LETTRE_BIENVENUE);
		map.put(AutorisationRadiationRC.class, TypeDocumentEditique.AUTORISATION_RADIATION_RC);
		map.put(DemandeBilanFinal.class, TypeDocumentEditique.DEMANDE_BILAN_FINAL);
		map.put(LettreTypeInformationLiquidation.class, TypeDocumentEditique.LETTRE_TYPE_INFO_LIQUIDATION);
		map.put(DemandeDegrevementICI.class, TypeDocumentEditique.DEMANDE_DEGREVEMENT_ICI);
		return Collections.unmodifiableMap(map);
	}

	private static Map<Class<? extends AutreDocumentFiscalAvecSuivi>, TypeDocumentEditique> buildTypesDocumentEnvoiRappel() {
		final Map<Class<? extends AutreDocumentFiscalAvecSuivi>, TypeDocumentEditique> map = new HashMap<>();
		map.put(LettreBienvenue.class, TypeDocumentEditique.RAPPEL);
		map.put(DemandeDegrevementICI.class, TypeDocumentEditique.RAPPEL_DEMANDE_DEGREVEMENT_ICI);
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

	public void setEvtDeclarationPMSender(EvenementDeclarationPMSender evtDeclarationPMSender) {
		this.evtDeclarationPMSender = evtDeclarationPMSender;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		this.regimeFiscalService = regimeFiscalService;
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
	public EnvoiFormulairesDemandeDegrevementICIResults envoyerFormulairesDemandeDegrevementICIEnMasse(RegDate dateTraitement, int nbThreads, @Nullable Integer nbMaxEnvois, StatusManager statusManager) {
		final EnvoiFormulairesDemandeDegrevementICIProcessor processor = new EnvoiFormulairesDemandeDegrevementICIProcessor(parametreAppService, transactionManager, this, hibernateTemplate, registreFoncierService, regimeFiscalService);
		return processor.run(nbThreads, nbMaxEnvois, dateTraitement, statusManager);
	}

	@Override
	public RappelFormulairesDemandeDegrevementICIResults envoyerRappelsFormulairesDemandeDegrevementICIEnMasse(RegDate dateTraitement, StatusManager statusManager) {
		final RappelFormulairesDemandeDegrevementICIProcessor processor = new RappelFormulairesDemandeDegrevementICIProcessor(parametreAppService, transactionManager, this, hibernateTemplate, registreFoncierService, delaiService);
		return processor.run(dateTraitement, statusManager);
	}

	@Override
	public LettreBienvenue envoyerLettreBienvenueBatch(Entreprise entreprise, RegDate dateTraitement, RegDate dateDebutNouvelAssujettissement) throws AutreDocumentFiscalException {
		final RegDate dateEnvoi = delaiService.getDateFinDelaiCadevImpressionLettreBienvenue(dateTraitement);
		final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourLettreBienvenue());
		final TypeLettreBienvenue typeLettre = computeTypeLettreBienvenue(entreprise, dateTraitement, dateDebutNouvelAssujettissement);

		final LettreBienvenue lettre = new LettreBienvenue();
		lettre.setDateEnvoi(dateEnvoi);
		lettre.setType(typeLettre);
		lettre.setEntreprise(entreprise);

		final DelaiAutreDocumentFiscal delai = new DelaiAutreDocumentFiscal();
		delai.setDateDemande(dateTraitement);
		delai.setDateTraitement(dateTraitement);
		delai.setDelaiAccordeAu(delaiRetour);
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		lettre.addDelai(delai);

		final LettreBienvenue saved = hibernateTemplate.merge(lettre);
		try {
			editiqueCompositionService.imprimeLettreBienvenueForBatch(saved, dateTraitement);
			evenementFiscalService.publierEvenementFiscalEmissionLettreBienvenue(saved);
		}
		catch (EditiqueException e) {
			throw new AutreDocumentFiscalException(e);
		}
		return saved;
	}

	@Override
	public DelaiAutreDocumentFiscal addAndSave(AutreDocumentFiscal doc, DelaiAutreDocumentFiscal delai) {
		return AddAndSaveHelper.addAndSave(doc, delai, hibernateTemplate::merge, new DelaiDocumentFiscalAddAndSaveAccessor<>());
	}

	@Override
	public <T extends EtatAutreDocumentFiscal> T addAndSave(AutreDocumentFiscal doc, T etat) {
		return AddAndSaveHelper.addAndSave(doc, etat, hibernateTemplate::merge, new EtatDocumentFiscalAddAndSaveAccessor<>());
	}


	private TypeLettreBienvenue computeTypeLettreBienvenue(Entreprise e, RegDate dateTraitement, RegDate dateDebutNouvelAssujettissement) throws AutreDocumentFiscalException {

		// if faut tout d'abord regarder le for principal à la date de traitement
		final ForsParType fors = e.getForsParType(true);
		final ForFiscalPrincipalPM forPrincipal = DateRangeHelper.rangeAt(fors.principauxPM, dateTraitement);
		if (forPrincipal == null) {
			throw new AutreDocumentFiscalException("Pas de for principal actif à la date de traitement, impossible de déterminer le type d'autorité fiscale du siège fiscal de l'entreprise.");
		}
		final TypeAutoriteFiscale taf = forPrincipal.getTypeAutoriteFiscale();

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
			// [SIFISC-21646] Pour les fors secondaires, on va prendre en compte tous ceux qui sont actifs depuis la date de début du nouvel assujettissement
			final List<ForFiscalSecondaire> forsSecondaires = new ArrayList<>(fors.secondaires.size());
			final DateRange depuisNouvelAssujettissement = new DateRangeHelper.Range(dateDebutNouvelAssujettissement, dateTraitement);
			for (ForFiscalSecondaire ffs : fors.secondaires) {
				if (DateRangeHelper.intersect(ffs, depuisNouvelAssujettissement)) {
					forsSecondaires.add(ffs);
				}
			}

			// regardons les fors secondaires et, surtout leur motif de rattachement
			if (forsSecondaires.isEmpty()) {
				throw new AutreDocumentFiscalException("Pas de for secondaire actif entre la date de début d'assujettissement et la date de traitement sur une entreprise avec siège " + taf);
			}

			// quels motifs de rattachement a-t-on trouvés ?
			boolean trouveImmeuble = false;
			boolean trouveEtablissement = false;
			for (ForFiscalSecondaire ffs : forsSecondaires) {
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
	public DemandeDegrevementICI envoyerDemandeDegrevementICIBatch(Entreprise entreprise, ImmeubleRF immeuble, int periodeFiscale, RegDate dateTraitement) throws AutreDocumentFiscalException {
		final RegDate dateEnvoi = delaiService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
		final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());

		final DemandeDegrevementICI demande = new DemandeDegrevementICI();
		demande.setDateEnvoi(dateEnvoi);
		demande.setCodeControle(buildCodeControleDemandeDegrevementICI(entreprise));
		demande.setImmeuble(immeuble);
		demande.setNumeroSequence(getNewSequenceNumberDemandeDegrevementICI(entreprise, periodeFiscale));
		demande.setPeriodeFiscale(periodeFiscale);
		demande.setEntreprise(entreprise);

		final DelaiAutreDocumentFiscal delai = new DelaiAutreDocumentFiscal();
		delai.setDateDemande(dateTraitement);
		delai.setDateTraitement(dateTraitement);
		delai.setDelaiAccordeAu(delaiRetour);
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		demande.addDelai(delai);

		final DemandeDegrevementICI saved = hibernateTemplate.merge(demande);
		entreprise.addAutreDocumentFiscal(saved);
		try {
			// impression éditique
			editiqueCompositionService.imprimeDemandeDegrevementICIForBatch(saved, dateTraitement);

			// envoi du NIP à qui de droit
			envoiCodeControlePourDemandeDegrevementICI(saved);

			// TODO événement fiscal ?
		}
		catch (EditiqueException | EvenementDeclarationException e) {
			throw new AutreDocumentFiscalException(e);
		}
		return saved;
	}

	@Override
	public EditiqueResultat envoyerDemandeDegrevementICIOnline(Entreprise entreprise, ImmeubleRF immeuble, int periodeFiscale, RegDate dateTraitement, RegDate delaiRetour) throws AutreDocumentFiscalException {

		final DemandeDegrevementICI demande = new DemandeDegrevementICI();
		demande.setDateEnvoi(dateTraitement);
		demande.setCodeControle(buildCodeControleDemandeDegrevementICI(entreprise));
		demande.setImmeuble(immeuble);
		demande.setNumeroSequence(getNewSequenceNumberDemandeDegrevementICI(entreprise, periodeFiscale));
		demande.setPeriodeFiscale(periodeFiscale);
		demande.setEntreprise(entreprise);

		final DelaiAutreDocumentFiscal delai = new DelaiAutreDocumentFiscal();
		delai.setDateDemande(dateTraitement);
		delai.setDateTraitement(dateTraitement);
		delai.setDelaiAccordeAu(delaiRetour);
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		demande.addDelai(delai);

		final DemandeDegrevementICI saved = hibernateTemplate.merge(demande);
		entreprise.addAutreDocumentFiscal(saved);
		try {
			// envoi du NIP à qui de droit
			envoiCodeControlePourDemandeDegrevementICI(saved);

			// impression éditique
			return editiqueCompositionService.imprimeDemandeDegrevementICIOnline(saved, dateTraitement);

			// TODO événement fiscal ?
		}
		catch (EditiqueException | EvenementDeclarationException | JMSException e) {
			throw new AutreDocumentFiscalException(e);
		}
	}

	/**
	 * Envoi du NIP à qui de droit
	 * @param demande la demande de dégrèvement à émettre
	 */
	private void envoiCodeControlePourDemandeDegrevementICI(DemandeDegrevementICI demande) throws EvenementDeclarationException {
		final Entreprise entreprise = demande.getEntreprise();
		final RegDate dateReference = RegDate.get(demande.getPeriodeFiscale(), 1, 1);
		final ImmeubleRF immeuble = demande.getImmeuble();
		final String nomCommune = Optional.ofNullable(registreFoncierService.getCommune(immeuble, dateReference)).map(Commune::getNomOfficiel).orElse(null);
		final String numeroParcelle = registreFoncierService.getNumeroParcelleComplet(immeuble, dateReference);

		evtDeclarationPMSender.sendEmissionDemandeDegrevementICIEvent(entreprise.getNumero(), demande.getPeriodeFiscale(), demande.getNumeroSequence(), demande.getCodeControle(),
		                                                              nomCommune, numeroParcelle, demande.getDelaiRetour());
	}

	private static String buildCodeControleDemandeDegrevementICI(Entreprise e) {
		final Set<String> existing = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true).stream()
				.map(DemandeDegrevementICI::getCodeControle)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		String newCode;
		while (existing.contains(newCode = DemandeDegrevementICI.generateCodeControle())) {
		}
		return newCode;
	}

	private static int getNewSequenceNumberDemandeDegrevementICI(Entreprise e, int periodeFiscale) {
		final int maxUsed = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true).stream()
				.filter(dd -> dd.getPeriodeFiscale() != null && dd.getPeriodeFiscale() == periodeFiscale)
				.filter(dd -> dd.getNumeroSequence() != null)
				.mapToInt(DemandeDegrevementICI::getNumeroSequence)
				.max()
				.orElse(0);
		return maxUsed + 1;
	}

	@Override
	public void envoyerRappelLettreBienvenueBatch(LettreBienvenue lettre, RegDate dateTraitement, RegDate dateEnvoiRappel) throws AutreDocumentFiscalException {
		try {
			lettre.setDateRappel(dateEnvoiRappel);
			editiqueCompositionService.imprimeRappelLettreBienvenueForBatch(lettre, dateTraitement);
		}
		catch (EditiqueException e) {
			throw new AutreDocumentFiscalException(e);
		}
	}

	@Override
	public void envoyerRappelFormulaireDemandeDegrevementICIBatch(DemandeDegrevementICI formulaire, RegDate dateTraitement, RegDate dateEnvoiRappel) throws AutreDocumentFiscalException {
		try {
			formulaire.setDateRappel(dateEnvoiRappel);
			editiqueCompositionService.imprimeRappelFormulaireDemandeDegrevementICIForBatch(formulaire, dateTraitement);
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
