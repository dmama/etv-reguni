package ch.vd.unireg.editique.mock;

import javax.jms.JMSException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.cfg.NotYetImplementedException;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.MimeTypeHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.ordinaire.pp.InformationsDocumentAdapter;
import ch.vd.unireg.documentfiscal.AutorisationRadiationRC;
import ch.vd.unireg.documentfiscal.DemandeBilanFinal;
import ch.vd.unireg.documentfiscal.LettreBienvenue;
import ch.vd.unireg.documentfiscal.LettreTypeInformationLiquidation;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.ModeleFeuilleDocumentEditique;
import ch.vd.unireg.editique.impl.EditiqueResultatDocumentImpl;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.fourreNeutre.FourreNeutre;
import ch.vd.unireg.mouvement.BordereauMouvementDossier;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.TypeDocument;

public class MockEditiqueCompositionService implements EditiqueCompositionService {

	@Override
	public EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinairePP declaration) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinairePM declaration) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	private static EditiqueResultat dummyResultat() {
		return new EditiqueResultatDocumentImpl("DUMMY", MimeTypeHelper.MIME_PLAINTEXT, null, "Ceci est un test".getBytes());
	}

	@Override
	public EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinairePP declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinairePM declaration, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public void imprimeDIForBatch(DeclarationImpotOrdinairePP declaration) throws EditiqueException {
	}

	@Override
	public void imprimeDIForBatch(DeclarationImpotOrdinairePM declaration) throws EditiqueException {
	}

	@Override
	public void imprimeLRForBatch(DeclarationImpotSource lr) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeNouveauxDossiers(List<Contribuable> contribuables) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinairePP declaration, boolean miseSousPliImpossible, RegDate dateEvenement, @Nullable Integer emolument) throws EditiqueException {
	}

	@Override
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinairePM declaration, RegDate dateTraitement, RegDate dateOfficielleEnvoi) throws EditiqueException {
	}

	@Override
	public void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement, @Nullable Integer emolument) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinairePM declaration, RegDate dateTraitement, RegDate dateOfficielleEnvoi) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeSommationLROnline(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public Pair<EditiqueResultat, String> imprimeLettreDecisionDelaiOnline(DeclarationImpotOrdinairePP di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		return Pair.of(dummyResultat(), null);
	}

	@Override
	public Pair<EditiqueResultat, String> imprimeLettreDecisionDelaiOnline(DeclarationImpotOrdinairePM di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		return Pair.of(dummyResultat(), null);
	}

	@Override
	public String imprimeLettreDecisionDelaiForBatch(DeclarationImpotOrdinairePM di, DelaiDeclaration delai, RegDate dateExpedition) throws EditiqueException, JMSException {
		return null;
	}

	@Override
	public String imprimeLettreDecisionDelaiForBatch(DeclarationImpotOrdinairePP di, DelaiDeclaration delai, RegDate dateExpedition) throws EditiqueException, JMSException {
		return null;
	}

	@Override
	public EditiqueResultat imprimeLROnline(DeclarationImpotSource lr, TypeDocument typeDocument) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeDuplicataLROnline(DeclarationImpotSource lr, TypeDocument typeDocument) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereauMouvementDossier) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public String imprimeDocumentEfacture(Tiers tiers, TypeDocument typeDoc, Date dateTraitement, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException, JMSException {
		return null;
	}

	@Override
	public int imprimeAnnexeImmeubleForBatch(InformationsDocumentAdapter infosDocument, Set<ModeleFeuilleDocument> listeModele, int nombreAnnexesImmeuble) throws EditiqueException {
		return nombreAnnexesImmeuble;
	}

	@Override
	public void imprimeLettreBienvenueForBatch(LettreBienvenue lettre, RegDate dateTraitement) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeLettreBienvenueOnline(LettreBienvenue lettre, RegDate dateTraitement) throws EditiqueException {
		throw new NotImplementedException("");
	}

	@Override
	public EditiqueResultat imprimeDuplicataLettreBienvenueOnline(LettreBienvenue lettre, RegDate dateTraitement) throws EditiqueException {
		return dummyResultat();
	}

	@Override
	public void imprimeRappelLettreBienvenueForBatch(LettreBienvenue lettre, RegDate dateTraitement) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeQuestionnaireSNCOnline(QuestionnaireSNC questionnaire) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeDuplicataQuestionnaireSNCOnline(QuestionnaireSNC questionnaire) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public void imprimerQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire) throws EditiqueException {
	}

	@Override
	public void imprimeRappelQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire, RegDate dateTraitement, RegDate dateOfficielleEnvoi) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeRappelQuestionnaireSNCOnline(QuestionnaireSNC questionnaire, RegDate dateTraitement) throws EditiqueException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeAutorisationRadiationRCOnline(AutorisationRadiationRC lettre, RegDate dateTraitement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeDemandeBilanFinalOnline(DemandeBilanFinal lettre, RegDate dateTraitement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeLettreTypeInformationLiquidationOnline(LettreTypeInformationLiquidation lettre, RegDate dateTraitement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public void imprimeDemandeDegrevementICIForBatch(DemandeDegrevementICI demande, RegDate dateTraitement) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeDuplicataDemandeDegrevementICIOnline(DemandeDegrevementICI demande, RegDate dateTraitement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public void imprimeRappelFormulaireDemandeDegrevementICIForBatch(DemandeDegrevementICI formulaire, RegDate dateTraitement) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeDemandeDegrevementICIOnline(DemandeDegrevementICI demande, RegDate dateTraitement, boolean duplicata) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimerFourreNeutre(FourreNeutre fourreNeutre, RegDate dateTraitement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public Pair<String, String> imprimeLettreDecisionDelaiQSNCBatch(DelaiDeclaration delai, RegDate dateExpeditionc) throws EditiqueException {
		throw new NotYetImplementedException("imprimeLettreDecisionDelaiQSNCBatch");
	}

	@Override
	public Pair<EditiqueResultat, String> imprimeLettreDecisionDelaiQSNCOnline(DelaiDeclaration delai) throws EditiqueException {
		return Pair.of(dummyResultat(), null);
	}
}
