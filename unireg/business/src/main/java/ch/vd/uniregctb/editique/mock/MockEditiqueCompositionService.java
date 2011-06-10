package ch.vd.uniregctb.editique.mock;

import javax.jms.JMSException;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.impl.EditiqueResultatDocumentImpl;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class MockEditiqueCompositionService implements EditiqueCompositionService {

	@Override
	public EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	private static EditiqueResultat dummyResultat() {
		return new EditiqueResultatDocumentImpl("DUMMY", MimeTypeHelper.MIME_PLAINTEXT, null, "Ceci est un test".getBytes(), System.currentTimeMillis());
	}

	@Override
	public EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public void imprimeDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException {
	}

	@Override
	public void imprimeLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeNouveauxDossiers(List<Contribuable> contribuables) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws EditiqueException {
	}

	@Override
	public void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException {
	}

	@Override
	public EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeSommationLROnline(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeConfirmationDelaiOnline(DeclarationImpotOrdinaire di, DelaiDeclaration delai) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeLROnline(DeclarationImpotSource lr, RegDate dateEvenement, TypeDocument typeDocument) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public EditiqueResultat imprimeTaxationOfficeOnline(DeclarationImpotOrdinaire declaration) throws EditiqueException, JMSException {
		return dummyResultat();
	}

	@Override
	public void imprimeTaxationOfficeBatch(DeclarationImpotOrdinaire declaration) throws EditiqueException {
	}

	@Override
	public EditiqueResultat envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereauMouvementDossier) throws EditiqueException, JMSException {
		return dummyResultat();
	}
}
