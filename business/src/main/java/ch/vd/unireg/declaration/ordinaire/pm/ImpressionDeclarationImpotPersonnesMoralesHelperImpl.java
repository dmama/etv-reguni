package ch.vd.unireg.declaration.ordinaire.pm;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeAnnexesDI;
import ch.vd.editique.unireg.CTypeAnnexesDIAPM;
import ch.vd.editique.unireg.CTypeDeclarationImpot;
import ch.vd.editique.unireg.CTypeDeclarationImpotAPM;
import ch.vd.editique.unireg.CTypeDeclarationImpotPM;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeReferenceAnnexeDI;
import ch.vd.editique.unireg.STypeReferenceAnnexeDIAPM;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.EditiqueAbstractHelperImpl;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiquePrefixeHelper;
import ch.vd.unireg.editique.ModeleFeuilleDocumentEditique;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.type.GroupeTypesDocumentBatchLocal;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

public class ImpressionDeclarationImpotPersonnesMoralesHelperImpl extends EditiqueAbstractHelperImpl implements ImpressionDeclarationImpotPersonnesMoralesHelper {

	private static final String COD_DOC_DI_PM = "U1P1";
	private static final String COD_DOC_DI_APM = "U1P2";

	private IbanValidator ibanValidator;

	private static final Map<Integer, STypeReferenceAnnexeDI> MAP_ANNEXES_PM = buildMapAnnexesPM();
	private static final Map<Integer, STypeReferenceAnnexeDIAPM> MAP_ANNEXES_APM = buildMapAnnexesAPM();

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	private static Map<Integer, STypeReferenceAnnexeDI> buildMapAnnexesPM() {
		final Map<Integer, STypeReferenceAnnexeDI> map = new HashMap<>();
		map.put(ModeleFeuille.ANNEXE_140.getNoCADEV(), STypeReferenceAnnexeDI.DI);
		map.put(ModeleFeuille.ANNEXE_141.getNoCADEV(), STypeReferenceAnnexeDI.A_01_A);
		map.put(ModeleFeuille.ANNEXE_142.getNoCADEV(), STypeReferenceAnnexeDI.A_01_B);
		map.put(ModeleFeuille.ANNEXE_143.getNoCADEV(), STypeReferenceAnnexeDI.A_01_C);
		map.put(ModeleFeuille.ANNEXE_144.getNoCADEV(), STypeReferenceAnnexeDI.A_01_D);
		map.put(ModeleFeuille.ANNEXE_145.getNoCADEV(), STypeReferenceAnnexeDI.A_01_E);
		map.put(ModeleFeuille.ANNEXE_146.getNoCADEV(), STypeReferenceAnnexeDI.A_02);
		map.put(ModeleFeuille.ANNEXE_147.getNoCADEV(), STypeReferenceAnnexeDI.A_03);
		map.put(ModeleFeuille.ANNEXE_148.getNoCADEV(), STypeReferenceAnnexeDI.A_04_A);
		map.put(ModeleFeuille.ANNEXE_149.getNoCADEV(), STypeReferenceAnnexeDI.A_04_B);
		return map;
	}

	private static Map<Integer, STypeReferenceAnnexeDIAPM> buildMapAnnexesAPM() {
		final Map<Integer, STypeReferenceAnnexeDIAPM> map = new HashMap<>();
		map.put(ModeleFeuille.ANNEXE_130.getNoCADEV(), STypeReferenceAnnexeDIAPM.DI);
		map.put(ModeleFeuille.ANNEXE_132.getNoCADEV(), STypeReferenceAnnexeDIAPM.A_01_A);
		map.put(ModeleFeuille.ANNEXE_134.getNoCADEV(), STypeReferenceAnnexeDIAPM.A_01_B);
		map.put(ModeleFeuille.ANNEXE_136.getNoCADEV(), STypeReferenceAnnexeDIAPM.A_02);
		map.put(ModeleFeuille.ANNEXE_137.getNoCADEV(), STypeReferenceAnnexeDIAPM.A_03_A);
		map.put(ModeleFeuille.ANNEXE_138.getNoCADEV(), STypeReferenceAnnexeDIAPM.A_03_B);
		map.put(ModeleFeuille.ANNEXE_139.getNoCADEV(), STypeReferenceAnnexeDIAPM.A_04);
		return map;
	}

	@Override
	public String getIdDocument(DeclarationImpotOrdinairePM declaration) {
		return String.format(
				"%s %s %s %s",
				declaration.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(declaration.getNumero().toString(), 2, '0'),
				StringUtils.leftPad(declaration.getTiers().getNumero().toString(), 9, '0'),
				new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate())
		);
	}

	@Override
	public TypeDocumentEditique getTypeDocumentEditique(DeclarationImpotOrdinairePM declaration) {
		final TypeDocument type = declaration.getModeleDocument().getTypeDocument();
		return getTypeDocumentEditique(type);
	}

	/**
	 * @param type un type de document interne (= core)
	 * @return le type de document éditique correspondant (= business)
	 */
	private static TypeDocumentEditique getTypeDocumentEditique(TypeDocument type) {
		if (type == null) {
			return null;
		}
		switch (type) {
		case DECLARATION_IMPOT_PM_LOCAL:
		case DECLARATION_IMPOT_PM_BATCH:
			return TypeDocumentEditique.DI_PM;
		case DECLARATION_IMPOT_APM_LOCAL:
		case DECLARATION_IMPOT_APM_BATCH:
			return TypeDocumentEditique.DI_APM;
		default:
			throw new IllegalArgumentException("Type de document non-supporté pour les déclarations des personnes morales : " + type);
		}
	}

	@Override
	public FichierImpression.Document buildDocument(DeclarationImpotOrdinairePM declaration, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException {
		try {
			final TypeDocumentEditique typeDocumentEditique;
			final boolean hasFeuilletPrincipal = hasFeuilletPrincipal(annexes);
			final FichierImpression.Document document = new FichierImpression.Document();
			if (GroupeTypesDocumentBatchLocal.DI_PM.hasType(declaration.getTypeDeclaration())) {
				final CTypeDeclarationImpotPM dipm = new CTypeDeclarationImpotPM();
				fillDocumentDI(dipm, declaration, hasFeuilletPrincipal);
				fillAnnexesDIPM(dipm, declaration, annexes);
				document.setDeclarationImpot(dipm);
				typeDocumentEditique = TypeDocumentEditique.DI_PM;
			}
			else if (GroupeTypesDocumentBatchLocal.DI_APM.hasType(declaration.getTypeDeclaration())) {
				final CTypeDeclarationImpotAPM diapm = new CTypeDeclarationImpotAPM();
				fillDocumentDI(diapm, declaration, hasFeuilletPrincipal);
				fillAnnexesDIAPM(diapm, declaration, annexes);
				document.setDeclarationImpotAPM(diapm);
				typeDocumentEditique = TypeDocumentEditique.DI_APM;
			}
			else {
				throw new IllegalArgumentException("Type de document non-supporté dans les DI des personnes morales : " + declaration.getTypeDeclaration());
			}
			final ContribuableImpositionPersonnesMorales pm = declaration.getTiers();
			document.setInfoDocument(buildInfoDocument(declaration, getAdresseEnvoi(pm)));
			document.setInfoEnteteDocument(buildInfoEnteteDocument(pm, declaration.getDateExpedition(), TRAITE_PAR, NOM_SERVICE_EXPEDITEUR, infraService.getACIOIPM(), infraService.getCAT()));
			if (hasFeuilletPrincipal) {
				document.setInfoArchivage(buildInfoArchivage(typeDocumentEditique, construitCleArchivageDocument(declaration), pm.getNumero(), RegDate.get()));
			}
			document.setInfoRoutage(null);
			return document;
		}
		catch (EditiqueException e) {
			throw e;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private static boolean hasFeuilletPrincipal(List<ModeleFeuilleDocumentEditique> annexes) {
		if (annexes == null || annexes.isEmpty()) {
			return false;
		}
		for (ModeleFeuilleDocumentEditique feuille : annexes) {
			if (feuille.isPrincipal() && feuille.getNombreFeuilles() > 0) {
				return true;
			}
		}
		return false;
	}

	private void fillAnnexesDIPM(CTypeDeclarationImpotPM document, DeclarationImpotOrdinairePM di, List<ModeleFeuilleDocumentEditique> annexes) throws AdresseException, DonneesCivilesException, EditiqueException {
		for (ModeleFeuilleDocumentEditique annexe : annexes) {
			if (annexe.getNombreFeuilles() > 0) {
				final CTypeAnnexesDI feuille = new CTypeAnnexesDI();
				feuille.setCodeBarreFeuille(buildCodeBarre(di, annexe, ServiceInfrastructureService.noOIPM));
				feuille.setNombreFeuille(BigInteger.valueOf(annexe.getNombreFeuilles()));
				feuille.setReferenceFeuille(MAP_ANNEXES_PM.get(annexe.getNoCADEV()));
				document.getFeuilles().add(feuille);
			}
		}
	}

	private void fillAnnexesDIAPM(CTypeDeclarationImpotAPM document, DeclarationImpotOrdinairePM di, List<ModeleFeuilleDocumentEditique> annexes) throws AdresseException, DonneesCivilesException, EditiqueException {
		for (ModeleFeuilleDocumentEditique annexe : annexes) {
			if (annexe.getNombreFeuilles() > 0) {
				final CTypeAnnexesDIAPM feuille = new CTypeAnnexesDIAPM();
				feuille.setCodeBarreFeuille(buildCodeBarre(di, annexe, ServiceInfrastructureService.noOIPM));
				feuille.setNombreFeuille(BigInteger.valueOf(annexe.getNombreFeuilles()));
				feuille.setReferenceFeuille(MAP_ANNEXES_APM.get(annexe.getNoCADEV()));
				document.getFeuilles().add(feuille);
			}
		}
	}

	private static Pair<STypeZoneAffranchissement, String> getInformationsAffranchissement(AdresseEnvoiDetaillee adresseContribuable, TypeContribuable typeContribuable) {
		switch (typeContribuable) {
		case UTILITE_PUBLIQUE:
			return getInformationAffranchissementIdEnvoi("LIASF");
		default:
			return getInformationsAffranchissement(adresseContribuable, false, ServiceInfrastructureService.noOIPM);
		}
	}

	private CTypeInfoDocument buildInfoDocument(DeclarationImpotOrdinairePM declaration, AdresseEnvoiDetaillee adresseContribuable) throws AdresseException {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		final Pair<STypeZoneAffranchissement, String> infoAffranchissement = getInformationsAffranchissement(adresseContribuable, declaration.getTypeContribuable());
		final STypeZoneAffranchissement zoneAffranchissement = assigneIdEnvoi(infoDoc, declaration.getTiers(), infoAffranchissement);
		infoDoc.setAffranchissement(new CTypeAffranchissement(zoneAffranchissement, null));
		infoDoc.setVersionXSD(VERSION_XSD);

		final TypeDocumentEditique typeDocumentEditique = getTypeDocumentEditique(declaration);
		infoDoc.setCodDoc(getCodeDocument(typeDocumentEditique));
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(typeDocumentEditique));
		infoDoc.setTypDoc(TYPE_DOCUMENT_DI);
		return infoDoc;
	}

	private static String getCodeDocument(TypeDocumentEditique typeDocument) {
		switch (typeDocument) {
		case DI_APM:
			return COD_DOC_DI_APM;
		case DI_PM:
			return COD_DOC_DI_PM;
		default:
			throw new IllegalArgumentException("Type de document non-associé à une déclaration d'impôt PM : " + typeDocument);
		}
	}

	/**
	 * Remplissage des données de siège et d'administration effective
	 * @param di la déclaration au format "éditique"
	 * @param entreprise l'entreprise qui nous intéresse
	 * @param dateFinPeriode la date de fin de la période d'imposition correspondant à la déclaration
	 */
	private void remplirSiegeEtAdministrationEffective(CTypeDeclarationImpot di, Entreprise entreprise, RegDate dateFinPeriode) {

		// récupération du dernier for principal et du dernier domicile de l'établissement principal
		final ForFiscalPrincipalPM forPrincipal = entreprise.getDernierForFiscalPrincipalAvant(dateFinPeriode);
		final List<DateRanged<Etablissement>> etablissementsPrincipaux = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
		final DateRanged<Etablissement> etablissementPrincipal = getLastBeforeOrAt(etablissementsPrincipaux, dateFinPeriode);

		// récupération du dernier domicile intéressant de l'établissement principal
		final DomicileHisto domicile = etablissementPrincipal != null
				? getLastBeforeOrAt(tiersService.getDomiciles(etablissementPrincipal.getPayload(), false), RegDateHelper.minimum(dateFinPeriode, etablissementPrincipal.getDateFin(), NullDateBehavior.LATEST))
				: null;

		if (domicile == null || (domicile.getTypeAutoriteFiscale() == forPrincipal.getTypeAutoriteFiscale() && domicile.getNumeroOfsAutoriteFiscale().equals(forPrincipal.getNumeroOfsAutoriteFiscale()))) {
			// on n'a que le siège, car tout est pareil (ou domicile inconnu)
			di.setSiege(getNomCommuneOuPays(forPrincipal));
			di.setAdministrationEffective(null);
		}
		else {
			// siège et administration effective sont dissociées
			di.setSiege(getNomCommuneOuPays(domicile));
			di.setAdministrationEffective(getNomCommuneOuPays(forPrincipal));
		}
	}

	private void fillDocumentDI(CTypeDeclarationImpot di, DeclarationImpotOrdinairePM declaration, boolean hasFeuilletPrincipal) throws AdresseException, DonneesCivilesException, EditiqueException {
		final Entreprise pm = (Entreprise) declaration.getTiers();

		remplirSiegeEtAdministrationEffective(di, pm, declaration.getDateFin());

		di.setAdresseRaisonSociale(buildAdresseRaisonSociale(pm, declaration.getDateFin()));
		di.setAdresseRetour(buildAdresseCEDI(ServiceInfrastructureService.noOIPM));      // TODO autre choix que retour au CEDI ?
		di.setCodeControleNIP(declaration.getCodeControle());

		if (declaration.getCodeSegment() != null) {
			final int codeSegment = declaration.getCodeSegment();
			if (hasFeuilletPrincipal) {
				// [SIFISC-19244] on ne met pas de code flyer si la fourre principale de la déclaration n'est pas dans les annexes imprimées
				di.setCodeFlyer(Integer.toString(codeSegment));     // aujourd'hui, c'est la même valeur que le code segment
			}
			di.setCodeRoutage(buildCodeRoutage(codeSegment));
		}

		di.setDateLimiteRetour(RegDateHelper.toIndexString(declaration.getDelaiRetourImprime()));
		di.setDebutExerciceCommercial(RegDateHelper.toIndexString(declaration.getDateDebutExerciceCommercial()));
		di.setFinExerciceCommercial(RegDateHelper.toIndexString(declaration.getDateFinExerciceCommercial()));
		Optional.ofNullable(pm.getCoordonneesFinancieresCourantes())
				.filter(coords -> coords.getCompteBancaire() != null)
				.filter(coords -> ibanValidator.isValidIban(coords.getCompteBancaire().getIban()))
				.ifPresent(coords -> {
					di.setIBAN(IbanHelper.toDisplayString(coords.getCompteBancaire().getIban()));
					di.setTitulaireCompte(coords.getTitulaire());
				});
		di.setPeriodeFiscale(XmlUtils.regdate2xmlcal(RegDate.get(declaration.getPeriode().getAnnee())));
	}

	private static String buildCodeRoutage(int codeSegment) {
		return String.format("%02d-%d", ServiceInfrastructureService.noOIPM, codeSegment);
	}

	private static String construitCleArchivageDocument(DeclarationImpotOrdinairePM declaration) {
		return String.format(
				"%s%s %s %s",
				declaration.getPeriode().getAnnee().toString(),
				StringUtils.leftPad(declaration.getNumero().toString(), 2, '0'),
				StringUtils.rightPad("DI entreprise", 19, ' '),
				new SimpleDateFormat("MMddHHmmssSSS").format(
						DateHelper.getCurrentDate()
				)
		);
	}

}
