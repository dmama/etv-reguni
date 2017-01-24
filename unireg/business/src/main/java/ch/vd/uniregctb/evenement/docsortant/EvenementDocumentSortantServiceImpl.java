package ch.vd.uniregctb.evenement.docsortant;

import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

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
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
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
		                       infoArchivage);
	}

	@Override
	public void signaleAutorisationRadiationRC(AutorisationRadiationRC autorisation, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("ARRC",
		                       TypeDocumentSortant.AUTORISATION_RADIATION_RC,
		                       autorisation.getEntreprise(),
		                       local,
		                       autorisation.getDateEnvoi().year(),        // année de l'envoi du courrier
		                       null,
		                       infoArchivage);
	}

	@Override
	public void signaleLettreTypeInformationLiquidation(LettreTypeInformationLiquidation lettre, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("LTIL",
		                       TypeDocumentSortant.LETTRE_TYPE_INFO_LIQUIDATION,
		                       lettre.getEntreprise(),
		                       local,
		                       lettre.getDateEnvoi().year(),        // année de l'envoi du courrier
		                       null,
		                       infoArchivage);
	}

	@Override
	public void signaleQuestionnaireSNC(QuestionnaireSNC questionnaire, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("QSNC",
		                       TypeDocumentSortant.QSNC,
		                       questionnaire.getTiers(),
		                       local,
		                       questionnaire.getPeriode().getAnnee(),
		                       questionnaire.getNumero(),
		                       infoArchivage);

	}

	@Override
	public void signaleRappelQuestionnaireSNC(QuestionnaireSNC questionnaire, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("RQSNC",
		                       TypeDocumentSortant.RAPPEL_QSNC,
		                       questionnaire.getTiers(),
		                       local,
		                       questionnaire.getPeriode().getAnnee(),
		                       questionnaire.getNumero(),
		                       infoArchivage);
	}

	private static final Map<TypeDocument, TypeDocumentSortant> TYPE_DI_ENTREPRISE_SORTANTE = buildMappingTypesDeclarationImpotEntrepriseSortante();

	private static Map<TypeDocument, TypeDocumentSortant> buildMappingTypesDeclarationImpotEntrepriseSortante() {
		final Map<TypeDocument, TypeDocumentSortant> map = new EnumMap<>(TypeDocument.class);
		map.put(TypeDocument.DECLARATION_IMPOT_APM_BATCH, TypeDocumentSortant.DI_APM);
		map.put(TypeDocument.DECLARATION_IMPOT_APM_LOCAL, TypeDocumentSortant.DI_APM);
		map.put(TypeDocument.DECLARATION_IMPOT_PM_BATCH, TypeDocumentSortant.DI_PM);
		map.put(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, TypeDocumentSortant.DI_PM);
		return map;
	}

	@Override
	public void signaleDeclarationImpot(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local) {
		final TypeDocument typeDocument = di.getModeleDocument().getTypeDocument();
		signaleDocumentSortant("DI",
		                       TYPE_DI_ENTREPRISE_SORTANTE.get(typeDocument),
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
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
		                       lettre.getDateEnvoi().year(),        // date de l'envoi du courrier
		                       null,
		                       infoArchivage);
	}

	@Override
	public void signaleRappelLettreBienvenue(LettreBienvenue lettre, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("RLB",
		                       TypeDocumentSortant.RAPPEL_LETTRE_BIENVENUE,
		                       lettre.getEntreprise(),
		                       local,
		                       lettre.getDateEnvoi().year(),        // date de l'envoi du courrier
		                       null,
		                       infoArchivage);
	}

	private static final Map<TypeDocument, TypeDocumentSortant> TYPE_DI_PP_SORTANTE = buildMappingTYpesDeclarationImpotPersonnePhysiqueSortante();

	private static Map<TypeDocument, TypeDocumentSortant> buildMappingTYpesDeclarationImpotPersonnePhysiqueSortante() {
		final Map<TypeDocument, TypeDocumentSortant> map = new EnumMap<>(TypeDocument.class);
		map.put(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeDocumentSortant.DI_PP_COMPLETE);
		map.put(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, TypeDocumentSortant.DI_PP_COMPLETE);
		map.put(TypeDocument.DECLARATION_IMPOT_DEPENSE, TypeDocumentSortant.DI_PP_DEPENSE);
		map.put(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, TypeDocumentSortant.DI_PP_HC_IMMEUBLE);
		map.put(TypeDocument.DECLARATION_IMPOT_VAUDTAX, TypeDocumentSortant.DI_PP_VAUDTAX);
		return map;
	}

	@Override
	public void signaleDeclarationImpot(DeclarationImpotOrdinairePP di, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local) {
		final TypeDocument typeDocument = di.getModeleDocument().getTypeDocument();
		signaleDocumentSortant("DI",
		                       TYPE_DI_PP_SORTANTE.get(typeDocument),
		                       di.getTiers(),
		                       local,
		                       di.getPeriode().getAnnee(),
		                       di.getNumero(),
		                       infoArchivage);
	}

	@Override
	public void signaleAnnexeImmeuble(InformationsDocumentAdapter infoDocument, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local) {
		final TypeDocument typeDocument = infoDocument.getTypeDocument();
		signaleDocumentSortant("ANNIMM",
		                       TYPE_DI_PP_SORTANTE.get(typeDocument),
		                       infoDocument.getTiers(),
		                       local,
		                       infoDocument.getAnnee(),
		                       infoDocument.getIdDocument(),
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
		                       infoArchivage);
	}

	private static int extractPseudoNumeroSequenceListeImpotSource(DeclarationImpotSource lr) {
		return lr.getDateFin().month();
	}

	@Override
	public void signaleListeRecapitulative(DeclarationImpotSource lr, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("LR",
		                       TypeDocumentSortant.LR,
		                       lr.getTiers(),
		                       local,
		                       lr.getPeriode().getAnnee(),
		                       extractPseudoNumeroSequenceListeImpotSource(lr),
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
		                       infoArchivage);
	}

	@Override
	public void signaleDemandeDegrevementICI(DemandeDegrevementICI dd, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("DDICI",
		                       TypeDocumentSortant.DEMANDE_DEGREVEMENT_ICI,
		                       dd.getEntreprise(),
		                       local,
		                       dd.getDateEnvoi().year(),                // date de l'envoi du courrier initial
		                       null,
		                       infoArchivage);
	}

	@Override
	public void signaleRappelDemandeDegrevementICI(DemandeDegrevementICI dd, CTypeInfoArchivage infoArchivage, boolean local) {
		signaleDocumentSortant("RDDICI",
		                       TypeDocumentSortant.RAPPEL_DEMANDE_DEGREVEMENT_ICI,
		                       dd.getEntreprise(),
		                       local,
		                       dd.getDateEnvoi().year(),                // date de l'envoi du courrier initial
		                       null,
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
	 * @param infoArchivage informations d'archivage du document
	 */
	private void signaleDocumentSortant(String prefixeBusinessId, TypeDocumentSortant typeDocument, Tiers tiers, boolean local, @Nullable Integer pf, @Nullable Integer noSequence, InfoArchivageDocument.InfoArchivage infoArchivage) {
		signaleDocumentSortant(prefixeBusinessId, typeDocument, tiers, local, pf, noSequence, new Archives(infoArchivage.getIdDocument(),
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
	 * @param infoArchivage informations d'archivage du document
	 */
	private void signaleDocumentSortant(String prefixeBusinessId, TypeDocumentSortant typeDocument, Tiers tiers, boolean local, @Nullable Integer pf, @Nullable Integer noSequence, CTypeInfoArchivage infoArchivage) {
		signaleDocumentSortant(prefixeBusinessId, typeDocument, tiers, local, pf, noSequence, new Archives(infoArchivage.getIdDocument(),
		                                                                                                   infoArchivage.getTypDocument(),
		                                                                                                   infoArchivage.getNomDossier(),
		                                                                                                   infoArchivage.getTypDossier()));
	}

	/**
	 * Envoi d'un événement de notification de l'envoi d'un nouveau document sortant - méthode centrale
	 * @param prefixeBusinessId le prefixe à utiliser pour le calcul du business ID du message
	 * @param typeDocument le type de document sortant
	 * @param tiers le tiers associé au document
	 * @param local <code>true</code> s'il s'agit d'une impression locale, <code>false</code> sinon
	 * @param pf période fiscale associée au document, ou <code>null</code> si document dit "pérenne"
	 * @param noSequence éventuellement un numéro de séquence du document dans la PF
	 * @param infoArchivage informations d'archivage du document
	 */
	private void signaleDocumentSortant(String prefixeBusinessId, TypeDocumentSortant typeDocument, Tiers tiers, boolean local, @Nullable Integer pf, @Nullable Integer noSequence, Archives infoArchivage) {

		final String pfInfo = pf == null ? StringUtils.EMPTY : String.format(" %d", pf);
		final String noSequenceInfo = noSequence == null ? StringUtils.EMPTY : String.format(" %d", noSequence);
		final String businessId = String.format("%s %d%s%s %d", prefixeBusinessId, tiers.getNumero(), pfInfo, noSequenceInfo, DateHelper.getCurrentDate().getTime());

		final Document document = new Document();
		document.setIdentifiantSupervision(String.format("%s-%s", EMETTEUR, businessId));
		document.setCaracteristiques(new Caracteristiques(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()),
		                                                  typeDocument.getCodeTypeDocumentSortant().getCode(),
		                                                  null,
		                                                  typeDocument.getNomDocument(),
		                                                  EMETTEUR,
		                                                  local ? CodeSupport.LOCAL : CodeSupport.CED,
		                                                  new Archivage(typeDocument.isArchivageValeurProbante(), null)));
		document.setDonneesMetier(new DonneesMetier(getTypePopulation(tiers),
		                                            pf != null ? new DonneesMetier.PeriodesFiscales(Collections.singletonList(BigInteger.valueOf(pf))) : null,
		                                            pf == null ? Boolean.TRUE : null,
		                                            tiers.getNumero().intValue(),
		                                            null));
		document.setArchive(infoArchivage);

		// envoi !
		try {
			sender.sendEvenementDocumentSortant(businessId, new Documents(Collections.singletonList(document),
			                                                              XmlUtils.date2xmlcal(DateHelper.getCurrentDate())));
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
