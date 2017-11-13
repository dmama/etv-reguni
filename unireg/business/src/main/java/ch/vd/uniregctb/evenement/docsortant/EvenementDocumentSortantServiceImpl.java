package ch.vd.uniregctb.evenement.docsortant;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import noNamespace.InfoArchivageDocument;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.xml.event.docsortant.v1.Archivage;
import ch.vd.unireg.xml.event.docsortant.v1.Archives;
import ch.vd.unireg.xml.event.docsortant.v1.Caracteristiques;
import ch.vd.unireg.xml.event.docsortant.v1.CodeSupport;
import ch.vd.unireg.xml.event.docsortant.v1.Document;
import ch.vd.unireg.xml.event.docsortant.v1.Documents;
import ch.vd.unireg.xml.event.docsortant.v1.DonneesMetier;
import ch.vd.unireg.xml.event.docsortant.v1.Population;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationAvecDocumentArchive;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRappelee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.ordinaire.pp.InformationsDocumentAdapter;
import ch.vd.uniregctb.documentfiscal.AutorisationRadiationRC;
import ch.vd.uniregctb.documentfiscal.DemandeBilanFinal;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.documentfiscal.LettreTypeInformationLiquidation;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeLettreBienvenue;

/**
 * Implémentation du service de notification de la présence d'un document sortant
 */
public class EvenementDocumentSortantServiceImpl implements EvenementDocumentSortantService {

	private static final String EMETTEUR = "UNIREG";

	private EvenementDocumentSortantSender sender;

	public void setSender(EvenementDocumentSortantSender sender) {
		this.sender = sender;
	}

	@Override
	public void signaleDemandeBilanFinal(DemandeBilanFinal bilanFinal, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("DBF",
		                       TypeDocumentSortant.DEMANDE_BILAN_FINAL,
		                       bilanFinal.getEntreprise(),
		                       local,
		                       bilanFinal.getPeriodeFiscale(),
		                       null,
		                       String.valueOf(bilanFinal.getId()),
		                       infoArchivage);
	}

	@Override
	public void signaleAutorisationRadiationRC(AutorisationRadiationRC autorisation, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("ARRC",
		                       TypeDocumentSortant.AUTORISATION_RADIATION_RC,
		                       autorisation.getEntreprise(),
		                       local,
		                       autorisation.getPeriodeFiscale(),
		                       null,
		                       String.valueOf(autorisation.getId()),
		                       infoArchivage);
	}

	@Override
	public void signaleLettreTypeInformationLiquidation(LettreTypeInformationLiquidation lettre, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("LTIL",
		                       TypeDocumentSortant.LETTRE_TYPE_INFO_LIQUIDATION,
		                       lettre.getEntreprise(),
		                       local,
		                       lettre.getPeriodeFiscale(),
		                       null,
		                       String.valueOf(lettre.getId()),
		                       infoArchivage);
	}

	@Override
	public void signaleQuestionnaireSNC(QuestionnaireSNC questionnaire, CTypeInfoArchivage infoArchivage, boolean local, boolean duplicata) {
		signaleDocumentSortant("QSNC",
		                       duplicata ? TypeDocumentSortant.DUPLICATA_QSNC : TypeDocumentSortant.QSNC,
		                       questionnaire.getTiers(),
		                       local,
		                       questionnaire.getPeriode().getAnnee(),
		                       questionnaire.getNumero(),
		                       duplicata ? null : getIdEtatDeclaration(questionnaire, EtatDeclarationEmise.class),
		                       infoArchivage);

	}

	@Nullable
	private static <T extends EtatDeclaration & EtatDeclarationAvecDocumentArchive> String getIdEtatDeclaration(Declaration declaration, Class<T> clazz) {
		return declaration.getEtatsDeclaration().stream()
				.filter(clazz::isInstance)
				.map(clazz::cast)
				.filter(AnnulableHelper::nonAnnule)
				.filter(etat -> etat.getCleDocument() == null)
				.sorted(Comparator.comparingLong(EtatDeclaration::getId).reversed())
				.mapToLong(EtatDeclaration::getId)
				.mapToObj(String::valueOf)
				.findFirst()
				.orElse(null);
	}

	@Nullable
	private static String getIdDelaiDeclaration(Declaration declaration, Predicate<? super DelaiDeclaration> predicate) {
		return declaration.getDelaisDeclaration().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(predicate)
				.filter(delai -> delai.getCleDocument() == null)
				.sorted(Comparator.comparingLong(DelaiDeclaration::getId).reversed())
				.mapToLong(DelaiDeclaration::getId)
				.mapToObj(String::valueOf)
				.findFirst()
				.orElse(null);
	}

	@Override
	public void signaleRappelQuestionnaireSNC(QuestionnaireSNC questionnaire, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("RQSNC",
		                       TypeDocumentSortant.RAPPEL_QSNC,
		                       questionnaire.getTiers(),
		                       local,
		                       questionnaire.getPeriode().getAnnee(),
		                       questionnaire.getNumero(),
		                       getIdEtatDeclaration(questionnaire, EtatDeclarationRappelee.class),
		                       infoArchivage);
	}

	private static final Map<TypeDocument, TypeDocumentSortant> TYPE_DI_ENTREPRISE_SORTANTE = buildMappingTypesDeclarationImpotEntrepriseSortante();
	private static final Map<TypeDocument, TypeDocumentSortant> TYPE_DUPLICATA_DI_ENTREPRISE_SORTANTE = buildMappingTypesDuplicataDeclarationImpotEntrepriseSortante();

	private static Map<TypeDocument, TypeDocumentSortant> buildMappingTypesDeclarationImpotEntrepriseSortante() {
		final Map<TypeDocument, TypeDocumentSortant> map = new EnumMap<>(TypeDocument.class);
		map.put(TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocumentSortant.DI_APM);
		map.put(TypeDocument.DECLARATION_IMPOT_APM_LOCAL, TypeDocumentSortant.DI_APM);
		map.put(TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocumentSortant.DI_PM);
		map.put(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, TypeDocumentSortant.DI_PM);
		return map;
	}

	private static Map<TypeDocument, TypeDocumentSortant> buildMappingTypesDuplicataDeclarationImpotEntrepriseSortante() {
		final Map<TypeDocument, TypeDocumentSortant> map = new EnumMap<>(TypeDocument.class);
		map.put(TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocumentSortant.DUPLICATA_DI_APM);
		map.put(TypeDocument.DECLARATION_IMPOT_APM_LOCAL, TypeDocumentSortant.DUPLICATA_DI_APM);
		map.put(TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocumentSortant.DUPLICATA_DI_PM);
		map.put(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, TypeDocumentSortant.DUPLICATA_DI_PM);
		return map;
	}

	@Override
	public void signaleDeclarationImpot(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local, boolean duplicata) {
		final TypeDocument typeDocument = di.getModeleDocument().getTypeDocument();
		signaleDocumentSortant("DI",
		                       (duplicata ? TYPE_DUPLICATA_DI_ENTREPRISE_SORTANTE : TYPE_DI_ENTREPRISE_SORTANTE).get(typeDocument),
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
		                       duplicata ? null : getIdEtatDeclaration(di, EtatDeclarationEmise.class),
		                       infoArchivage);
	}

	@Override
	public void signaleAccordDelai(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("ACDEL",
		                       TypeDocumentSortant.ACCORD_DELAI_PM,
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
		                       getIdDelaiDeclaration(di, delai -> delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE && !delai.isSursis()),
		                       infoArchivage);

	}

	@Override
	public void signaleRefusDelai(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("REDEL",
		                       TypeDocumentSortant.REFUS_DELAI_PM,
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
		                       getIdDelaiDeclaration(di, delai -> delai.getEtat() == EtatDelaiDocumentFiscal.REFUSE),
		                       infoArchivage);
	}

	@Override
	public void signaleSursis(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("SURSIS",
		                       TypeDocumentSortant.SURSIS,
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
		                       getIdDelaiDeclaration(di, delai -> delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE && delai.isSursis()),
		                       infoArchivage);
	}

	@Override
	public void signaleSommationDeclarationImpot(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("SOMMDI",
		                       TypeDocumentSortant.SOMMATION_DI_ENTREPRISE,
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
		                       getIdEtatDeclaration(di, EtatDeclarationSommee.class),
		                       infoArchivage);
	}

	private static final Map<TypeLettreBienvenue, TypeDocumentSortant> TYPE_LETTRE_BIENVENUE_SORTANTE = buildMappingTypesLettreBienvenueSortante();

	private static Map<TypeLettreBienvenue, TypeDocumentSortant> buildMappingTypesLettreBienvenueSortante() {
		final Map<TypeLettreBienvenue, TypeDocumentSortant> map = new EnumMap<>(TypeLettreBienvenue.class);
		map.put(TypeLettreBienvenue.APM_VD_NON_RC, TypeDocumentSortant.LETTRE_BIENENUE_APM);
		map.put(TypeLettreBienvenue.HS_HC_ETABLISSEMENT, TypeDocumentSortant.LETTRE_BIENVENUE_PM_HC_ETABLISSEMENT);
		map.put(TypeLettreBienvenue.HS_HC_IMMEUBLE, TypeDocumentSortant.LETTRE_BIENVENUE_PM_HC_IMMEUBLE);
		map.put(TypeLettreBienvenue.VD_RC, TypeDocumentSortant.LETTRE_BIENVENUE_RC_VD);
		return map;
	}

	@Override
	public void signaleLettreBienvenue(LettreBienvenue lettre, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("LB",
		                       TYPE_LETTRE_BIENVENUE_SORTANTE.get(lettre.getType()),
		                       lettre.getEntreprise(),
		                       local,
		                       lettre.getPeriodeFiscale(),
		                       null,
		                       String.valueOf(lettre.getId()),
		                       infoArchivage);
	}

	@Override
	public void signaleRappelLettreBienvenue(LettreBienvenue lettre, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("RLB",
		                       TypeDocumentSortant.RAPPEL_LETTRE_BIENVENUE,
		                       lettre.getEntreprise(),
		                       local,
		                       lettre.getPeriodeFiscale(),
		                       null,
		                       String.valueOf(lettre.getId()),
		                       infoArchivage);
	}

	private static final Map<TypeDocument, TypeDocumentSortant> TYPE_DI_PP_SORTANTE = buildMappingTypesDeclarationImpotPersonnePhysiqueSortante();
	private static final Map<TypeDocument, TypeDocumentSortant> TYPE_DUPLICATA_DI_PP_SORTANTE = buildMappingTypesDuplicataDeclarationImpotPersonnePhysiqueSortante();

	private static Map<TypeDocument, TypeDocumentSortant> buildMappingTypesDeclarationImpotPersonnePhysiqueSortante() {
		final Map<TypeDocument, TypeDocumentSortant> map = new EnumMap<>(TypeDocument.class);
		map.put(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocumentSortant.DI_PP_COMPLETE);
		map.put(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, TypeDocumentSortant.DI_PP_COMPLETE);
		map.put(TypeDocument.DECLARATION_IMPOT_DEPENSE, TypeDocumentSortant.DI_PP_DEPENSE);
		map.put(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, TypeDocumentSortant.DI_PP_HC_IMMEUBLE);
		map.put(TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeDocumentSortant.DI_PP_VAUDTAX);
		return map;
	}

	private static Map<TypeDocument, TypeDocumentSortant> buildMappingTypesDuplicataDeclarationImpotPersonnePhysiqueSortante() {
		final Map<TypeDocument, TypeDocumentSortant> map = new EnumMap<>(TypeDocument.class);
		map.put(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocumentSortant.DUPLICATA_DI_PP_COMPLETE);
		map.put(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, TypeDocumentSortant.DUPLICATA_DI_PP_COMPLETE);
		map.put(TypeDocument.DECLARATION_IMPOT_DEPENSE, TypeDocumentSortant.DUPLICATA_DI_PP_DEPENSE);
		map.put(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, TypeDocumentSortant.DUPLICATA_DI_PP_HC_IMMEUBLE);
		map.put(TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeDocumentSortant.DUPLICATA_DI_PP_VAUDTAX);
		return map;
	}

	@Override
	public void signaleDeclarationImpot(DeclarationImpotOrdinairePP di, @Nullable TypeDocument typeDocumentOverride, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local, boolean duplicata) {
		final TypeDocument typeDocument = typeDocumentOverride != null ? typeDocumentOverride : di.getModeleDocument().getTypeDocument();
		signaleDocumentSortant("DI",
		                       (duplicata ? TYPE_DUPLICATA_DI_PP_SORTANTE : TYPE_DI_PP_SORTANTE).get(typeDocument),
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
		                       duplicata ? null : getIdEtatDeclaration(di, EtatDeclarationEmise.class),
		                       infoArchivage);
	}

	@Override
	public void signaleAnnexeImmeuble(InformationsDocumentAdapter infoDocument, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("ANNIMM",
		                       TypeDocumentSortant.DI_PP_ANNEXE_IMMEUBLE,
		                       infoDocument.getTiers(),
		                       local,
		                       infoDocument.getAnnee(),
		                       infoDocument.getIdDocument(),
		                       null,
		                       infoArchivage);
	}

	@Override
	public void signaleSommationDeclarationImpot(DeclarationImpotOrdinairePP di, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("SOMMDI",
		                       TypeDocumentSortant.SOMMATION_DI_PP,
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
		                       getIdEtatDeclaration(di, EtatDeclarationSommee.class),
		                       infoArchivage);
	}

	private static int extractPseudoNumeroSequenceListeImpotSource(DeclarationImpotSource lr) {
		return lr.getDateFin().month();
	}

	@Override
	public void signaleListeRecapitulative(DeclarationImpotSource lr, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local, boolean duplicata) {
		signaleDocumentSortant("LR",
		                       duplicata ? TypeDocumentSortant.DUPLICATA_LR : TypeDocumentSortant.LR,
		                       lr.getTiers(),
		                       local,
		                       lr.getPeriode().getAnnee(),
		                       extractPseudoNumeroSequenceListeImpotSource(lr),
		                       duplicata ? null : getIdEtatDeclaration(lr, EtatDeclarationEmise.class),
		                       infoArchivage);
	}

	@Override
	public void signaleSommationListeRecapitulative(DeclarationImpotSource lr, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("SOMMLR",
		                       TypeDocumentSortant.SOMMATION_LR,
		                       lr.getTiers(),
		                       local,
		                       lr.getPeriode().getAnnee(),
		                       extractPseudoNumeroSequenceListeImpotSource(lr),
		                       getIdEtatDeclaration(lr, EtatDeclarationSommee.class),
		                       infoArchivage);
	}

	@Override
	public void signaleConfirmationDelai(DeclarationImpotOrdinairePP di, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("CODEL",
		                       TypeDocumentSortant.CONFIRMATION_DELAI,
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
		                       getIdDelaiDeclaration(di, delai -> true),
		                       infoArchivage);
	}

	private static final Map<TypeDocument, TypeDocumentSortant> TYPE_DOC_EFACTURE_SORTANT = builMappingTypesDocumentEFactureSortant();

	private static Map<TypeDocument, TypeDocumentSortant> builMappingTypesDocumentEFactureSortant() {
		final Map<TypeDocument, TypeDocumentSortant> map = new EnumMap<>(TypeDocument.class);
		map.put(TypeDocument.E_FACTURE_ATTENTE_CONTACT, TypeDocumentSortant.E_FACTURE_CONTACT);
		map.put(TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, TypeDocumentSortant.E_FACTURE_SIGNATURE);
		return map;
	}

	@Override
	public void signaleDocumentEFacture(TypeDocument typeDoc, Tiers tiers, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("EFACTURE",
		                       TYPE_DOC_EFACTURE_SORTANT.get(typeDoc),
		                       tiers,
		                       local,
		                       null,
		                       null,
		                       DocumentEFactureHelper.encodeIdentifiant(tiers, infoArchivage.getIdDocument()),
		                       infoArchivage);
	}

	@Override
	public void signaleDemandeDegrevementICI(DemandeDegrevementICI dd, String nomCommune, String noParcelle, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("DDICI",
		                       TypeDocumentSortant.DEMANDE_DEGREVEMENT_ICI,
		                       CollectionsUtils.concat(Arrays.asList(TypeDocumentSortant.DEMANDE_DEGREVEMENT_ICI.getNomDocument(), nomCommune, noParcelle), " "),
		                       dd.getEntreprise(),
		                       local,
		                       dd.getPeriodeFiscale(),
		                       dd.getNumeroSequence(),
		                       String.valueOf(dd.getId()),
		                       infoArchivage);
	}

	@Override
	public void signaleRappelDemandeDegrevementICI(DemandeDegrevementICI dd, String nomCommune, String noParcelle, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("RDDICI",
		                       TypeDocumentSortant.RAPPEL_DEMANDE_DEGREVEMENT_ICI,
		                       CollectionsUtils.concat(Arrays.asList(TypeDocumentSortant.RAPPEL_DEMANDE_DEGREVEMENT_ICI.getNomDocument(), nomCommune, noParcelle), " "),
		                       dd.getEntreprise(),
		                       local,
		                       dd.getPeriodeFiscale(),
		                       dd.getNumeroSequence(),
		                       String.valueOf(dd.getId()),
		                       infoArchivage);
	}

	/**
	 * Envoi d'un événement de notification de l'envoi d'un nouveau document sortant - méthode légacy (vieux documents...)
	 * @param prefixeBusinessId le prefixe à utiliser pour le calcul du business ID du message
	 * @param typeDocument le type de document sortant
	 * @param tiers le tiers associé au document
	 * @param local <code>true</code> s'il s'agit d'une impression locale, <code>false</code> sinon
	 * @param pf période fiscale associée au document, ou <code>null</code> si document dit "pérenne"
	 * @param noSequence éventuellement un numéro de séquence du document dans la PF
	 * @param idEntityPourReponse identifiant de l'entité à passer dans le message sortant qui pourra être utilisé dans la réponse
	 * @param infoArchivage informations d'archivage du document
	 */
	private void signaleDocumentSortant(String prefixeBusinessId, TypeDocumentSortant typeDocument, Tiers tiers, boolean local, @Nullable Integer pf, @Nullable Integer noSequence, String idEntityPourReponse, InfoArchivageDocument.InfoArchivage infoArchivage) {
		signaleDocumentSortant(prefixeBusinessId, typeDocument, typeDocument.getNomDocument(), tiers, local, pf, noSequence, idEntityPourReponse, infoArchivage);
	}

	/**
	 * Envoi d'un événement de notification de l'envoi d'un nouveau document sortant - méthode légacy (vieux documents...)
	 * @param prefixeBusinessId le prefixe à utiliser pour le calcul du business ID du message
	 * @param typeDocument le type de document sortant
	 * @param nomDocument le nom (lisible pour un utilisateur humain dans le contexte du contribuable) à associer au document dans l'annonce
	 * @param tiers le tiers associé au document
	 * @param local <code>true</code> s'il s'agit d'une impression locale, <code>false</code> sinon
	 * @param pf période fiscale associée au document, ou <code>null</code> si document dit "pérenne"
	 * @param noSequence éventuellement un numéro de séquence du document dans la PF
	 * @param idEntityPourReponse identifiant de l'entité à passer dans le message sortant qui pourra être utilisé dans la réponse
	 * @param infoArchivage informations d'archivage du document
	 */
	private void signaleDocumentSortant(String prefixeBusinessId, TypeDocumentSortant typeDocument, String nomDocument, Tiers tiers, boolean local, @Nullable Integer pf, @Nullable Integer noSequence, String idEntityPourReponse, InfoArchivageDocument.InfoArchivage infoArchivage) {
		signaleDocumentSortant(prefixeBusinessId, typeDocument, nomDocument, tiers, local, pf, noSequence, idEntityPourReponse, new Archives(infoArchivage.getIdDocument(),
		                                                                                                                                     infoArchivage.getTypDocument(),
		                                                                                                                                     infoArchivage.getNomDossier(),
		                                                                                                                                     infoArchivage.getTypDossier()));
	}

	/**
	 * Envoi d'un événement de notification de l'envoi d'un nouveau document sortant - méthode utilisable pour les nouveaux types de document (PM)
	 * @param prefixeBusinessId le prefixe à utiliser pour le calcul du business ID du message
	 * @param typeDocument le type de document sortant
	 * @param tiers le tiers associé au document
	 * @param local <code>true</code> s'il s'agit d'une impression locale, <code>false</code> sinon
	 * @param pf période fiscale associée au document, ou <code>null</code> si document dit "pérenne"
	 * @param noSequence éventuellement un numéro de séquence du document dans la PF
	 * @param idEntityPourReponse identifiant de l'entité à passer dans le message sortant qui pourra être utilisé dans la réponse
	 * @param infoArchivage informations d'archivage du document
	 */
	private void signaleDocumentSortant(String prefixeBusinessId, TypeDocumentSortant typeDocument, Tiers tiers, boolean local, @Nullable Integer pf, @Nullable Integer noSequence, String idEntityPourReponse, CTypeInfoArchivage infoArchivage) {
		signaleDocumentSortant(prefixeBusinessId, typeDocument, typeDocument.getNomDocument(), tiers, local, pf, noSequence, idEntityPourReponse, infoArchivage);
	}

	/**
	 * Envoi d'un événement de notification de l'envoi d'un nouveau document sortant - méthode utilisable pour les nouveaux types de document (PM)
	 * @param prefixeBusinessId le prefixe à utiliser pour le calcul du business ID du message
	 * @param typeDocument le type de document sortant
	 * @param nomDocument le nom (lisible pour un utilisateur humain dans le contexte du contribuable) à associer au document dans l'annonce
	 * @param tiers le tiers associé au document
	 * @param local <code>true</code> s'il s'agit d'une impression locale, <code>false</code> sinon
	 * @param pf période fiscale associée au document, ou <code>null</code> si document dit "pérenne"
	 * @param noSequence éventuellement un numéro de séquence du document dans la PF
	 * @param idEntityPourReponse identifiant de l'entité à passer dans le message sortant qui pourra être utilisé dans la réponse
	 * @param infoArchivage informations d'archivage du document
	 */
	private void signaleDocumentSortant(String prefixeBusinessId, TypeDocumentSortant typeDocument, String nomDocument, Tiers tiers, boolean local, @Nullable Integer pf, @Nullable Integer noSequence, String idEntityPourReponse, CTypeInfoArchivage infoArchivage) {
		signaleDocumentSortant(prefixeBusinessId, typeDocument, nomDocument, tiers, local, pf, noSequence, idEntityPourReponse, new Archives(infoArchivage.getIdDocument(),
		                                                                                                                                     infoArchivage.getTypDocument(),
		                                                                                                                                     infoArchivage.getNomDossier(),
		                                                                                                                                     infoArchivage.getTypDossier()));
	}

	/**
	 * Envoi d'un événement de notification de l'envoi d'un nouveau document sortant - méthode centrale
	 * @param prefixeBusinessId le prefixe à utiliser pour le calcul du business ID du message
	 * @param typeDocument le type de document sortant
	 * @param nomDocument le nom (lisible pour un utilisateur humain dans le contexte du contribuable) à associer au document dans l'annonce
	 * @param tiers le tiers associé au document
	 * @param local <code>true</code> s'il s'agit d'une impression locale, <code>false</code> sinon
	 * @param pf période fiscale associée au document, ou <code>null</code> si document dit "pérenne"
	 * @param noSequence éventuellement un numéro de séquence du document dans la PF
	 * @param idEntityPourReponse identifiant de l'entité à passer dans le message sortant qui pourra être utilisé dans la réponse
	 * @param infoArchivage informations d'archivage du document
	 */
	private void signaleDocumentSortant(String prefixeBusinessId, TypeDocumentSortant typeDocument, String nomDocument,
	                                    Tiers tiers, boolean local, @Nullable Integer pf, @Nullable Integer noSequence,
	                                    String idEntityPourReponse, Archives infoArchivage) {

		final String pfInfo = pf == null ? StringUtils.EMPTY : String.format(" %d", pf);
		final String noSequenceInfo = noSequence == null ? StringUtils.EMPTY : String.format(" %d", noSequence);
		final String businessId = String.format("%s %d%s%s %d", prefixeBusinessId, tiers.getNumero(), pfInfo, noSequenceInfo, DateHelper.getCurrentDate().getTime());

		final Document document = new Document();
		document.setIdentifiantSupervision(String.format("%s-%s", EMETTEUR, businessId));
		document.setCaracteristiques(new Caracteristiques(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()),
		                                                  typeDocument.getCodeTypeDocumentSortant().getCode(),
		                                                  null,
		                                                  nomDocument,
		                                                  EMETTEUR,
		                                                  local ? CodeSupport.LOCAL : CodeSupport.CED,
		                                                  new Archivage(typeDocument.isArchivageValeurProbante(), null)));
		document.setDonneesMetier(new DonneesMetier(getTypePopulation(tiers),
		                                            pf != null ? new DonneesMetier.PeriodesFiscales(Collections.singletonList(BigInteger.valueOf(pf))) : null,
		                                            pf == null ? Boolean.TRUE : null,
		                                            tiers.getNumero().intValue(),
		                                            null));
		document.setArchive(infoArchivage);

		// constiturion d'une map d'entêtes additionnelles
		final Map<String, String> headers = new HashMap<>();
		headers.put(RetourDocumentSortantHandler.TYPE_DOCUMENT_HEADER_NAME, typeDocument.name());
		headers.put(RetourDocumentSortantHandler.ID_ENTITE_DOCUMENT_ANNONCE_HEADER_NAME, idEntityPourReponse);

		// envoi !
		try {
			sender.sendEvenementDocumentSortant(businessId,
			                                    new Documents(Collections.singletonList(document),
			                                                  XmlUtils.date2xmlcal(DateHelper.getCurrentDate())),
			                                    typeDocument.isQuittanceAnnonceDemandee(),
			                                    headers);
		}
		catch (EvenementDocumentSortantException e) {
			throw new RuntimeException(e);
		}
	}

	private static Population getTypePopulation(Tiers tiers) {
		if (tiers instanceof ContribuableImpositionPersonnesPhysiques) {
			return Population.PP;
		}
		else if (tiers instanceof ContribuableImpositionPersonnesMorales) {
			return Population.PM;
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			return Population.IS;
		}
		else {
			// de quoi peut-il bien s'agir ???
			return null;
		}
	}
}
