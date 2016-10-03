package ch.vd.uniregctb.evenement.docsortant;

import noNamespace.InfoArchivageDocument;

import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.documentfiscal.AutorisationRadiationRC;
import ch.vd.uniregctb.documentfiscal.DemandeBilanFinal;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.documentfiscal.LettreTypeInformationLiquidation;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * Service de constitution de message de signalement de document sortant (au DPerm)
 */
public interface EvenementDocumentSortantService {

	void signaleDemandeBilanFinal(DemandeBilanFinal bilanFinal, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleAutorisationRadiationRC(AutorisationRadiationRC autorisation, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleLettreTypeInformationLiquidation(LettreTypeInformationLiquidation lettre, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleQuestionnaireSNC(QuestionnaireSNC questionnaire, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleRappelQuestionnaireSNC(QuestionnaireSNC questionnaire, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleDeclarationImpot(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleAccordDelai(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleRefusDelai(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleSursis(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local);
	
	void signaleSommationDeclarationImpot(DeclarationImpotOrdinairePM di, CTypeInfoArchivage infoArchivage, boolean local);
	
	void signaleLettreBienvenue(LettreBienvenue lettre, CTypeInfoArchivage infoArchivage, boolean local);
	
	void signaleRappelLettreBienvenue(LettreBienvenue lettre, CTypeInfoArchivage infoArchivage, boolean local);

	void signaleDeclarationImpot(DeclarationImpotOrdinairePP di, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleSommationDeclarationImpot(DeclarationImpotOrdinairePP di, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleListeRecapitulative(DeclarationImpotSource lr, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleSommationListeRecapitulative(DeclarationImpotSource lr, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleConfirmationDelai(DeclarationImpotOrdinairePP di, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);

	void signaleDocumentEFacture(TypeDocument typeDoc, Tiers tiers, InfoArchivageDocument.InfoArchivage infoArchivage, boolean local);
}