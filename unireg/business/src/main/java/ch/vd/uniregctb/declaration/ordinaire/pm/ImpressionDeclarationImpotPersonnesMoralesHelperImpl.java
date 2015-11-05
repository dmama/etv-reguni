package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeAdresse;
import ch.vd.editique.unireg.CTypeAffranchissement;
import ch.vd.editique.unireg.CTypeDestinataire;
import ch.vd.editique.unireg.CTypeExpediteur;
import ch.vd.editique.unireg.CTypeInfoDocument;
import ch.vd.editique.unireg.CTypeInfoEnteteDocument;
import ch.vd.editique.unireg.FichierImpression;
import ch.vd.editique.unireg.STypeZoneAffranchissement;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
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
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.LocalisationDatee;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeDocument;

public class ImpressionDeclarationImpotPersonnesMoralesHelperImpl extends EditiqueAbstractHelper implements ImpressionDeclarationImpotPersonnesMoralesHelper {

	private static final String DI = "DI";
	private static final String COD_DOC_DI_PM = "DI_PM";
	private static final String COD_DOC_DI_APM = "DI_APM";            // TODO reste à valider...

	private IbanValidator ibanValidator;
	private ServiceInfrastructureService infraService;

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
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
		case DECLARATION_IMPOT_PM:
			return TypeDocumentEditique.DI_PM;
		case DECLARATION_IMPOT_APM:
			return TypeDocumentEditique.DI_APM;
		default:
			throw new IllegalArgumentException("Type de document non-supporté pour les déclarations des personnes morales : " + type);
		}
	}

	@Override
	public FichierImpression.Document buildDocument(DeclarationImpotOrdinairePM declaration) throws EditiqueException {
		try {
			final FichierImpression.Document document = new FichierImpression.Document();
			document.setDeclarationImpot(buildDeclarationImpot(declaration));
			document.setInfoDocument(buildInfoDocument(declaration, getAdresseEnvoi(declaration.getTiers())));
			document.setInfoEnteteDocument(buildInfoEnteteDocument(declaration));
			document.setInfoRoutage(null);
			return document;
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}
	}

	private CTypeInfoEnteteDocument buildInfoEnteteDocument(DeclarationImpotOrdinairePM declaration) throws ServiceInfrastructureException, AdresseException {
		final CTypeInfoEnteteDocument entete = new CTypeInfoEnteteDocument();
		entete.setDestinataire(buildDestinataire(declaration.getTiers()));
		entete.setExpediteur(buildExpediteur(declaration.getDateExpedition()));
		entete.setLigReference(null);
		entete.setPorteAdresse(null);
		return entete;
	}

	private CTypeDestinataire buildDestinataire(Contribuable ctb) throws AdresseException {
		final CTypeDestinataire destinataire = new CTypeDestinataire();
		destinataire.setAdresse(buildAdresse(ctb));
		destinataire.setNumContribuable(FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()));
		if (ctb instanceof Entreprise) {
			final String ide = FormatNumeroHelper.formatNumIDE(tiersService.getNumeroIDE((Entreprise) ctb));
			if (StringUtils.isNotBlank(ide)) {
				destinataire.getNumIDE().add(ide);
			}
		}
		return destinataire;
	}

	private CTypeExpediteur buildExpediteur(RegDate dateExpedition) throws ServiceInfrastructureException, AdresseException {
		final CTypeExpediteur expediteur = new CTypeExpediteur();
		final CollectiviteAdministrative oipm = infraService.getACIOIPM();
		final CTypeAdresse adresse = buildAdresse(oipm);
		expediteur.setAdresse(adresse);
		expediteur.setAdrMes(oipm.getAdresseEmail());
		expediteur.setDateExpedition(RegDateHelper.toIndexString(dateExpedition));
		expediteur.setLocaliteExpedition(oipm.getAdresse().getLocalite());
		expediteur.setNumCCP(oipm.getNoCCP());
		expediteur.setNumFax(oipm.getNoFax());
		expediteur.setNumIBAN(null);
		expediteur.setNumTelephone(oipm.getNoTelephone());
		expediteur.setTraitePar("");
		return expediteur;
	}

	private CTypeInfoDocument buildInfoDocument(DeclarationImpotOrdinairePM declaration, AdresseEnvoiDetaillee adresseContribuable) throws AdresseException {
		final CTypeInfoDocument infoDoc = new CTypeInfoDocument();
		final STypeZoneAffranchissement zoneAffranchissement;
		if (adresseContribuable.isIncomplete()) {
			infoDoc.setIdEnvoi(String.valueOf(ServiceInfrastructureService.noOIPM));
			zoneAffranchissement = STypeZoneAffranchissement.NA;
		}
		else {
			if (adresseContribuable.isSuisse()) {
				zoneAffranchissement = STypeZoneAffranchissement.CH;
			}
			else if (adresseContribuable.getPays() != null) {

				// TODO faut-il envoyer les courriers vers HS à l'OIPM ?

				switch (adresseContribuable.getPays().getTypeAffranchissement()) {
				case EUROPE:
					zoneAffranchissement = STypeZoneAffranchissement.EU;
					break;
				case MONDE:
					zoneAffranchissement = STypeZoneAffranchissement.RM;
					break;
				case SUISSE:
					zoneAffranchissement = STypeZoneAffranchissement.CH;
					break;
				default:
					throw new IllegalArgumentException("Type d'affranchissement associé au pays " + adresseContribuable.getPays() + " non supporté : " + adresseContribuable.getPays().getTypeAffranchissement());
				}
			}
			else {
				// on fait comme pour les adresses incomplètes
				infoDoc.setIdEnvoi(String.valueOf(ServiceInfrastructureService.noOIPM));
				zoneAffranchissement = STypeZoneAffranchissement.NA;
			}
		}
		final TypeDocumentEditique typeDocumentEditique = getTypeDocumentEditique(declaration);
		infoDoc.setAffranchissement(new CTypeAffranchissement(zoneAffranchissement, null));
		infoDoc.setCodDoc(getCodeDocument(typeDocumentEditique));
		infoDoc.setPopulations(POPULATION_PM);
		infoDoc.setPrefixe(buildPrefixeInfoDocument(typeDocumentEditique));
		infoDoc.setTypDoc(DI);
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

	private AdresseEnvoiDetaillee getAdresseEnvoi(Tiers tiers) throws AdresseException {
		return adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
	}

	private CTypeAdresse buildAdresse(CollectiviteAdministrative coll) throws AdresseException {
		final Tiers colAdm = tiersService.getCollectiviteAdministrative(coll.getNoColAdm());
		return buildAdresse(colAdm);
	}

	private CTypeAdresse buildAdresse(Tiers tiers) throws AdresseException {
		final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
		return buildAdresse(adresse);
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

	@Nullable
	private CTypeAdresse buildAdresse(AdresseEnvoi adresseEnvoi) {
		final List<String> lignes = new ArrayList<>(6);
		for (String ligne : adresseEnvoi.getLignes()) {
			if (StringUtils.isNotBlank(ligne)) {
				lignes.add(ligne);
			}
		}
		return lignes.isEmpty() ? null : new CTypeAdresse(lignes);
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
	 * @return le dernier for principal connu de l'entreprise avant la date de fin de la déclaration
	 */
	private ForFiscalPrincipalPM remplirSiegeEtAdministrationEffective(FichierImpression.Document.DeclarationImpot di, Entreprise entreprise, RegDate dateFinPeriode) {

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

		return forPrincipal;
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

	private FichierImpression.Document.DeclarationImpot buildDeclarationImpot(DeclarationImpotOrdinairePM declaration) throws AdresseException, DonneesCivilesException {
		final FichierImpression.Document.DeclarationImpot di = new FichierImpression.Document.DeclarationImpot();
		final Entreprise pm = (Entreprise) declaration.getTiers();
		final ForFiscalPrincipalPM ffp = remplirSiegeEtAdministrationEffective(di, pm, declaration.getDateFin());

		di.setAdresseRaisonSociale(buildAdresseRaisonSociale(pm, declaration.getDateFin()));
		di.setAdresseRetour(buildAdresseCEDI());      // TODO autre choix que retour au CEDI ?
		di.setCodeBarreDI(buildCodeBarre(declaration));
		di.setCodeControleNIP(declaration.getCodeControle());
		di.setCodeRoutage(buildCodeRoutage(ffp.getTypeAutoriteFiscale()));
		di.setDateLimiteRetour(RegDateHelper.toIndexString(declaration.getDelaiRetourImprime()));
		di.setDebutExerciceCommercial(RegDateHelper.toIndexString(declaration.getDateDebut()));
		di.setFinExerciceCommercial(RegDateHelper.toIndexString(declaration.getDateFin()));
		if (StringUtils.isNotBlank(pm.getNumeroCompteBancaire()) && ibanValidator.isValidIban(pm.getNumeroCompteBancaire())) {
			di.setIBAN(pm.getNumeroCompteBancaire());
			di.setTitulaireCompte(pm.getTitulaireCompteBancaire());
		}
		di.setPeriodeFiscale(XmlUtils.regdate2xmlcal(RegDate.get(declaration.getPeriode().getAnnee())));
		return di;
	}

	private static String buildCodeBarre(DeclarationImpotOrdinairePM declaration) {
		return String.format("%04d%05d%04d%09d%02d%02d",
		                     0,             // TODO no UIDFIN / CADEV ???
		                     0,             // TODO numéro de formulaire ACI ???
		                     declaration.getPeriode().getAnnee(),
		                     declaration.getTiers().getNumero(),
		                     declaration.getNumero() % 100,
		                     ServiceInfrastructureService.noOIPM);
	}

	private static String buildCodeRoutage(TypeAutoriteFiscale typeAutoriteFiscalePrincipale) {
		return String.format("%02d-%d",
		                     ServiceInfrastructureService.noOIPM,
		                     typeAutoriteFiscalePrincipale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD ? 1 : 3);
	}
}
