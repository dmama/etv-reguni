package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeAdresse;
import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeAnnexeDI;
import ch.vd.editique.unireg.CTypeAnnexeDIA;
import ch.vd.editique.unireg.CTypeDeclarationImpot;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.uniregctb.adresse.AdresseCivileAdapter;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdressesCivilesHisto;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ordinaire.common.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiquePrefixeHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.LocalisationDatee;
import ch.vd.uniregctb.type.GroupeTypesDocumentBatchLocal;
import ch.vd.uniregctb.type.ModeleFeuille;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeDocument;

public class ImpressionDeclarationImpotPersonnesMoralesHelperImpl extends EditiqueAbstractHelper implements ImpressionDeclarationImpotPersonnesMoralesHelper {

	private static final String TYPE_DOC = "DI";
	private static final String COD_DOC_DI_PM = "U1P1";
	private static final String COD_DOC_DI_APM = "U1P2";
	private static final String TRAITE_PAR = "Registre PM";

	private IbanValidator ibanValidator;

	private static final Map<Integer, String> MAP_ANNEXES_PM = buildMapAnnexesPM();
	private static final Map<Integer, String> MAP_ANNEXES_APM = buildMapAnnexesAPM();

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	private static Map<Integer, String> buildMapAnnexesPM() {
		final Map<Integer, String> map = new HashMap<>();
		map.put(ModeleFeuille.ANNEXE_141.getNoCADEV(), "A01a");
		map.put(ModeleFeuille.ANNEXE_142.getNoCADEV(), "A01b");
		map.put(ModeleFeuille.ANNEXE_143.getNoCADEV(), "A01c");
		map.put(ModeleFeuille.ANNEXE_144.getNoCADEV(), "A01d");
		map.put(ModeleFeuille.ANNEXE_145.getNoCADEV(), "A01e");
		map.put(ModeleFeuille.ANNEXE_146.getNoCADEV(), "A02");
		map.put(ModeleFeuille.ANNEXE_147.getNoCADEV(), "A03");
		map.put(ModeleFeuille.ANNEXE_148.getNoCADEV(), "A04a");
		map.put(ModeleFeuille.ANNEXE_149.getNoCADEV(), "A04b");
		return map;
	}

	private static Map<Integer, String> buildMapAnnexesAPM() {
		final Map<Integer, String> map = new HashMap<>();
		map.put(ModeleFeuille.ANNEXE_132.getNoCADEV(), "A01a");
		map.put(ModeleFeuille.ANNEXE_134.getNoCADEV(), "A01b");
		map.put(ModeleFeuille.ANNEXE_136.getNoCADEV(), "A02");
		map.put(ModeleFeuille.ANNEXE_137.getNoCADEV(), "A03a");
		map.put(ModeleFeuille.ANNEXE_138.getNoCADEV(), "A03b");
		map.put(ModeleFeuille.ANNEXE_139.getNoCADEV(), "A04");
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
			final FichierImpression.Document document = new FichierImpression.Document();
			if (GroupeTypesDocumentBatchLocal.DI_PM.hasType(declaration.getTypeDeclaration())) {
				fillDocumentDIPM(document, declaration, annexes);
			}
			else if (GroupeTypesDocumentBatchLocal.DI_APM.hasType(declaration.getTypeDeclaration())) {
				fillDocumentDIAPM(document, declaration, annexes);
			}
			else {
				throw new IllegalArgumentException("Type de document non-supporté dans les DI des personnes morales : " + declaration.getTypeDeclaration());
			}
			document.setInfoDocument(buildInfoDocument(declaration, getAdresseEnvoi(declaration.getTiers())));
			document.setInfoEnteteDocument(buildInfoEnteteDocument(declaration.getTiers(), declaration.getDateExpedition(), TRAITE_PAR, infraService.getACIOIPM()));
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

	private void fillDocumentDIPM(FichierImpression.Document document, DeclarationImpotOrdinairePM di, List<ModeleFeuilleDocumentEditique> annexes) throws AdresseException, DonneesCivilesException, EditiqueException {
		final CTypeDeclarationImpot declarationImpot = buildDeclarationImpot(di);
		final CTypeAnnexeDI docAnnexes = new CTypeAnnexeDI();
		final List<String> referenceAnnexe = docAnnexes.getReferenceAnnexe();
		for (ModeleFeuilleDocumentEditique annexe : annexes) {
			if (annexe.getNoCADEV() == ModeleFeuille.ANNEXE_140.getNoCADEV()) {
				document.setDeclarationImpot(declarationImpot);
			}
			else {
				final String code = MAP_ANNEXES_PM.get(annexe.getNoCADEV());
				if (code != null && annexe.getNombreFeuilles() > 0) {
					for (int i = 0 ; i < annexe.getNombreFeuilles() ; ++ i) {
						referenceAnnexe.add(code);
					}
				}
			}
		}
		if (!referenceAnnexe.isEmpty()) {
			document.setAnnexesDI(docAnnexes);
		}
	}

	private void fillDocumentDIAPM(FichierImpression.Document document, DeclarationImpotOrdinairePM di, List<ModeleFeuilleDocumentEditique> annexes) throws AdresseException, DonneesCivilesException, EditiqueException {
		final CTypeDeclarationImpot declarationImpot = buildDeclarationImpot(di);
		final CTypeAnnexeDIA docAnnexes = new CTypeAnnexeDIA();
		final List<String> referenceAnnexe = docAnnexes.getReferenceAnnexe();
		for (ModeleFeuilleDocumentEditique annexe : annexes) {
			if (annexe.getNoCADEV() == ModeleFeuille.ANNEXE_130.getNoCADEV()) {
				document.setDeclarationImpotAssociation(declarationImpot);
			}
			else {
				final String code = MAP_ANNEXES_APM.get(annexe.getNoCADEV());
				if (code != null && annexe.getNombreFeuilles() > 0) {
					for (int i = 0 ; i < annexe.getNombreFeuilles() ; ++ i) {
						referenceAnnexe.add(code);
					}
				}
			}
		}
		if (!referenceAnnexe.isEmpty()) {
			document.setAnnexesDIA(docAnnexes);
		}
	}

	private CTypeInfoDocument buildInfoDocument(DeclarationImpotOrdinairePM declaration, AdresseEnvoiDetaillee adresseContribuable) throws AdresseException {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();

		// TODO faut-il envoyer les courriers vers HS à l'OIPM ?

		final Pair<STypeZoneAffranchissement, String> infoAffranchissement = getInformationsAffranchissement(adresseContribuable, false, ServiceInfrastructureService.noOIPM);
		infoDoc.setIdEnvoi(infoAffranchissement.getRight());
		infoDoc.setAffranchissement(new CTypeAffranchissement(infoAffranchissement.getLeft(), null));

		final TypeDocumentEditique typeDocumentEditique = getTypeDocumentEditique(declaration);
		infoDoc.setCodDoc(getCodeDocument(typeDocumentEditique));
		infoDoc.setPopulations(ConstantesEditique.POPULATION_PM);
		infoDoc.setPrefixe(EditiquePrefixeHelper.buildPrefixeInfoDocument(typeDocumentEditique));
		infoDoc.setTypDoc(TYPE_DOC);
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
	 * @return l'adresse du CEDI à utiliser comme adresse de retour
	 */
	private CTypeAdresse buildAdresseCEDI() {
		final CollectiviteAdministrative cedi = infraService.getCEDI();
		final List<String> lignes = new ArrayList<>(4);
		final Adresse adresse = cedi.getAdresse();
		lignes.add(cedi.getNomComplet1());
		lignes.add(cedi.getNomComplet2());
		lignes.add(cedi.getNomCourt() + ' ' + ServiceInfrastructureService.noOIPM);
		lignes.add(adresse.getNumeroPostal() + ' ' + adresse.getLocalite());
		return new CTypeAdresse(lignes);
	}

	@NotNull
	private String getNomCommuneOuPays(LocalisationDatee localisationDatee) {
		if (localisationDatee != null && localisationDatee.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			final Pays pays = infraService.getPays(localisationDatee.getNumeroOfsAutoriteFiscale(), localisationDatee.getDateFin());
			return String.format("Etranger (%s)", pays != null ? pays.getNomOfficiel() : "?");
		}
		else if (localisationDatee != null && localisationDatee.getTypeAutoriteFiscale() != null) {
			final Commune commune = infraService.getCommuneByNumeroOfs(localisationDatee.getNumeroOfsAutoriteFiscale(), localisationDatee.getDateFin());
			return commune != null ? commune.getNomOfficielAvecCanton() : "Suisse (?)";
		}
		else {
			return "-";
		}
	}

	/**
	 * @param liste liste de valeurs datées, supposées triées chronologiquement
	 * @param date date de référence
	 * @param <T> type des éléments dans la liste
	 * @return l'élément de la liste valide à la date de référence ou, s'il n'y en a pas, le dernier connu avant cette date
	 */
	@Nullable
	private static <T extends DateRange> T getLastBeforeOrAt(List<T> liste, RegDate date) {
		if (liste != null && !liste.isEmpty()) {
			for (T elt : CollectionsUtils.revertedOrder(liste)) {
				if (elt.getDateDebut() == null || date == null || elt.getDateDebut().isBeforeOrEqual(date)) {
					return elt;
				}
			}
		}
		return null;
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
		final DomicileEtablissement domicile = etablissementPrincipal != null
				? getLastBeforeOrAt(etablissementPrincipal.getPayload().getSortedDomiciles(false), RegDateHelper.minimum(dateFinPeriode, etablissementPrincipal.getDateFin(), NullDateBehavior.LATEST))
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

	/**
	 * Remplissage de l'adresse légale et de la raison sociale
	 * @param entreprise l'entreprise qui nous intéresse
	 * @param dateFinPeriode la date de fin de la période d'imposition correspondant à la déclaration
	 * @return une adresse (au format 'éditique') correspondant à l'adresse légale (= civile) de l'entreprise, si elle est connue (et à la
	 * raison sociale seule si l'adresse légale est inconnue)
	 */
	@NotNull
	private CTypeAdresse buildAdresseRaisonSociale(Entreprise entreprise, RegDate dateFinPeriode) throws AdresseException, DonneesCivilesException {

		// L'adresse qui m'intéresse est l'adresse à la date de fin de période
		final AdressesCivilesHisto adressesCiviles = adresseService.getAdressesCivilesHisto(entreprise, false);
		final List<Adresse> adressesDomicile = adressesCiviles != null ? adressesCiviles.principales : null;
		Adresse adresseRetenue = null;
		if (adressesDomicile != null) {
			adresseRetenue = getLastBeforeOrAt(adressesDomicile, dateFinPeriode);
		}

		if (adresseRetenue != null) {
			final AdresseGenerique adresseCivile = new AdresseCivileAdapter(adresseRetenue, entreprise, false, infraService);
			final AdresseEnvoi adresseEnvoi = adresseService.buildAdresseEnvoi(entreprise, adresseCivile, dateFinPeriode);
			final CTypeAdresse adresse = buildAdresse(adresseEnvoi);
			if (adresse != null) {
				return adresse;
			}
		}

		// pas d'adresse connue ? pas grave, on met au moins la raison sociale
		return new CTypeAdresse(Collections.singletonList(tiersService.getRaisonSociale(entreprise)));
	}

	private CTypeDeclarationImpot buildDeclarationImpot(DeclarationImpotOrdinairePM declaration) throws AdresseException, DonneesCivilesException, EditiqueException {
		final CTypeDeclarationImpot di = new CTypeDeclarationImpot();
		final Entreprise pm = (Entreprise) declaration.getTiers();

		remplirSiegeEtAdministrationEffective(di, pm, declaration.getDateFin());

		di.setAdresseRaisonSociale(buildAdresseRaisonSociale(pm, declaration.getDateFin()));
		di.setAdresseRetour(buildAdresseCEDI());      // TODO autre choix que retour au CEDI ?
		di.setCodeBarreDI(buildCodeBarre(declaration, getModelePrincipal(declaration)));
		di.setCodeControleNIP(declaration.getCodeControle());

		if (declaration.getCodeSegment() != null) {
			final int codeSegment = declaration.getCodeSegment();
			di.setCodeFlyer(Integer.toString(codeSegment));     // aujourd'hui, c'est la même valeur
			di.setCodeRoutage(buildCodeRoutage(codeSegment));
		}

		di.setDateLimiteRetour(RegDateHelper.toIndexString(declaration.getDelaiRetourImprime()));
		di.setDebutExerciceCommercial(RegDateHelper.toIndexString(declaration.getDateDebutExerciceCommercial()));
		di.setFinExerciceCommercial(RegDateHelper.toIndexString(declaration.getDateFinExerciceCommercial()));
		if (StringUtils.isNotBlank(pm.getNumeroCompteBancaire()) && ibanValidator.isValidIban(pm.getNumeroCompteBancaire())) {
			di.setIBAN(IbanHelper.toDisplayString(pm.getNumeroCompteBancaire()));
			di.setTitulaireCompte(pm.getTitulaireCompteBancaire());
		}
		di.setPeriodeFiscale(XmlUtils.regdate2xmlcal(RegDate.get(declaration.getPeriode().getAnnee())));
		return di;
	}

	@NotNull
	private static ModeleFeuilleDocument getModelePrincipal(DeclarationImpotOrdinairePM declaration) throws EditiqueException {
		final TypeDocumentEditique type = getTypeDocumentEditique(declaration.getTypeDeclaration());
		final int noCADEV;
		switch (type) {
		case DI_APM:
			noCADEV = ModeleFeuille.ANNEXE_130.getNoCADEV();
			break;
		case DI_PM:
			noCADEV = ModeleFeuille.ANNEXE_140.getNoCADEV();
			break;
		default:
			throw new IllegalArgumentException("Type de document éditique inattendu ici : " + type);
		}

		final Set<ModeleDocument> modeles = declaration.getPeriode().getModelesDocument();
		for (ModeleDocument modele : modeles) {
			if (modele.getTypeDocument() == declaration.getTypeDeclaration()) {
				final Set<ModeleFeuilleDocument> feuilles = modele.getModelesFeuilleDocument();
				for (ModeleFeuilleDocument feuille : feuilles) {
					if (feuille.getNoCADEV() == noCADEV) {
						return feuille;
					}
				}
			}
		}
		throw new EditiqueException("Le modèle de document " + declaration.getTypeDeclaration() + " pour la PF " + declaration.getPeriode().getAnnee() + " n'a pas de feuille identifiée comme 'principale'");
	}

	private static String buildCodeBarre(DeclarationImpotOrdinairePM declaration, ModeleFeuilleDocument modele) {
		return String.format("%04d%05d%04d%09d%02d%02d",
		                     modele.getNoCADEV(),
		                     modele.getNoFormulaireACI() != null ? modele.getNoFormulaireACI() : 0,
		                     declaration.getPeriode().getAnnee(),
		                     declaration.getTiers().getNumero(),
		                     declaration.getNumero() % 100,
		                     ServiceInfrastructureService.noOIPM);
	}

	private static String buildCodeRoutage(int codeSegment) {
		return String.format("%02d-%d", ServiceInfrastructureService.noOIPM, codeSegment);
	}
}
