package ch.vd.unireg.evenement.docsortant;

import noNamespace.InfoArchivageDocument;
import org.jetbrains.annotations.Nullable;

import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.ordinaire.pp.InformationsDocumentAdapter;
import ch.vd.unireg.documentfiscal.AutorisationRadiationRC;
import ch.vd.unireg.documentfiscal.DemandeBilanFinal;
import ch.vd.unireg.documentfiscal.LettreBienvenue;
import ch.vd.unireg.documentfiscal.LettreTypeInformationLiquidation;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.TypeDocument;

/**
 * Service de constitution de message de signalement de document sortant (au DPerm)
 */
public interface EvenementDocumentSortantService {

	void signaleDemandeBilanFinal(DemandeBilanFinal bilanFinal, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleAutorisationRadiationRC(AutorisationRadiationRC autorisation, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleLettreTypeInformationLiquidation(LettreTypeInformationLiquidation lettre, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleQuestionnaireSNC(QuestionnaireSNC questionnaire, CTypeInfoArchivage infoArchivage, boolean local, boolean duplicata);

	void signaleRappelQuestionnaireSNC(QuestionnaireSNC questionnaire, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleDeclarationImpot(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local, boolean duplicata);

	void signaleAccordDelai(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleRefusDelai(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleSursis(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local);
	
	void signaleSommationDeclarationImpot(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local);
	
	void signaleLettreBienvenue(LettreBienvenue lettre, CTypeInfoArchivage infoArchivage, boolean local, boolean duplicata);

	void signaleRappelLettreBienvenue(LettreBienvenue lettre, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleDeclarationImpot(DeclarationImpotOrdinairePP di, @Nullable TypeDocument typeDocumentOverride, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local, boolean duplicata);

	void signaleAnnexeImmeuble(InformationsDocumentAdapter infoDocument, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleSommationDeclarationImpot(DeclarationImpotOrdinairePP di, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleListeRecapitulative(DeclarationImpotSource lr, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local, boolean duplicata);

	void signaleSommationListeRecapitulative(DeclarationImpotSource lr, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleConfirmationDelai(DeclarationImpotOrdinairePP di, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleDocumentEFacture(TypeDocument typeDoc, Tiers tiers, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleDemandeDegrevementICI(DemandeDegrevementICI dd, String nomCommune, String noParcelle, CTypeInfoArchivage infoArchivage, boolean local, boolean duplicata);

	void signaleRappelDemandeDegrevementICI(DemandeDegrevementICI dd, String nomCommune, String noParcelle, CTypeInfoArchivage infoArchivage, boolean local);
}
