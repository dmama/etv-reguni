package ch.vd.uniregctb.editique.mock;

import javax.jms.JMSException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ordinaire.common.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.declaration.ordinaire.pp.InformationsDocumentAdapter;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.impl.EditiqueResultatDocumentImpl;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeDocument;

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
	public EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinairePM declaration) throws EditiqueException, JMSException {
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
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinairePP declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws EditiqueException {
	}

	@Override
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinairePM declaration, RegDate dateEvenement) throws EditiqueException {
	}

	@Override
	public void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinairePM declaration, RegDate dateEvenement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeSommationLROnline(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public Pair<EditiqueResultat, String> imprimeConfirmationDelaiOnline(DeclarationImpotOrdinairePP di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		return Pair.of(dummyResultat(), null);
	}

	@Override
	public Pair<EditiqueResultat, String> imprimeLettreDecisionDelaiOnline(DeclarationImpotOrdinairePM di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		return Pair.of(dummyResultat(), null);
	}

	@Override
	public String imprimeLettreDecisionDelaiForBatch(DeclarationImpotOrdinairePM di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		return null;
	}

	@Override
	public EditiqueResultat imprimeLROnline(DeclarationImpotSource lr, TypeDocument typeDocument) throws EditiqueException, JMSException {
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
}
