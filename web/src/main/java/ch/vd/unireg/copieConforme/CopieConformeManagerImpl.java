package ch.vd.unireg.copieConforme;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationRappelee;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.snc.QuestionnaireSNCService;
import ch.vd.unireg.declaration.source.ListeRecapService;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalAvecSuivi;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueService;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.hibernate.HibernateTemplate;

public class CopieConformeManagerImpl implements CopieConformeManager {

	private HibernateTemplate hibernateTemplate;

	private DeclarationImpotService diService;
	private ListeRecapService lrService;
	private QuestionnaireSNCService qsncService;
	private AutreDocumentFiscalService autreDocumentFiscalService;
	private EditiqueService editiqueService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setLrService(ListeRecapService lrService) {
		this.lrService = lrService;
	}

	public void setQsncService(QuestionnaireSNCService qsncService) {
		this.qsncService = qsncService;
	}

	public void setAutreDocumentFiscalService(AutreDocumentFiscalService autreDocumentFiscalService) {
		this.autreDocumentFiscalService = autreDocumentFiscalService;
	}

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat getPdfCopieConformeSommation(Long idEtatSomme) throws EditiqueException {
		final EtatDeclarationSommee etat = hibernateTemplate.get(EtatDeclarationSommee.class, idEtatSomme);
		if (etat == null) {
			throw new ObjectNotFoundException("Etat de déclaration sommée inconnu pour l'identifiant " + idEtatSomme);
		}

		final Declaration declaration = etat.getDeclaration();
		if (declaration instanceof DeclarationImpotOrdinairePP) {
			return diService.getCopieConformeSommationDI((DeclarationImpotOrdinairePP) declaration);
		}
		else if (declaration instanceof DeclarationImpotOrdinairePM) {
			return diService.getCopieConformeSommationDI((DeclarationImpotOrdinairePM) declaration);
		}
		else if (declaration instanceof DeclarationImpotSource) {
			return lrService.getCopieConformeSommationLR((DeclarationImpotSource) declaration);
		}
		else {
			throw new IllegalArgumentException("La déclaration n'est ni une DI ni une LR (" + declaration.getClass().getName() + ')');
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat getPdfCopieConformeRappel(Long idEtatRappele) throws EditiqueException {
		final EtatDeclarationRappelee etat = hibernateTemplate.get(EtatDeclarationRappelee.class, idEtatRappele);
		if (etat == null) {
			throw new ObjectNotFoundException("Etat de déclaration rappelée inconnu pour l'identifiant " + idEtatRappele);
		}

		final Declaration declaration = etat.getDeclaration();
		if (declaration instanceof QuestionnaireSNC) {
			return qsncService.getCopieConformeRappelQuestionnaireSNC((QuestionnaireSNC) declaration);
		}
		else {
			throw new IllegalArgumentException("La déclaration n'est pas un questionnaire SNC (" + declaration.getClass().getName() + ')');
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat getPdfCopieConformeDelai(Long idDelai) throws EditiqueException {
		final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, idDelai);
		if (delai == null) {
			throw new IllegalArgumentException();
		}
		return diService.getCopieConformeConfirmationDelai(delai);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat getPdfCopieConformeEnvoiAutreDocumentFiscal(Long idDocument) throws EditiqueException {
		final AutreDocumentFiscal doc = hibernateTemplate.get(AutreDocumentFiscal.class, idDocument);
		if (doc == null) {
			throw new IllegalArgumentException();
		}
		return autreDocumentFiscalService.getCopieConformeDocumentInitial(doc);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat getPdfCopieConformeRappelAutreDocumentFiscal(Long idDocument) throws EditiqueException {
		final AutreDocumentFiscalAvecSuivi doc = hibernateTemplate.get(AutreDocumentFiscalAvecSuivi.class, idDocument);
		if (doc == null) {
			throw new IllegalArgumentException();
		}
		return autreDocumentFiscalService.getCopieConformeDocumentRappel(doc);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat getPdfCopieConforme(long noCtb, TypeDocumentEditique typeDoc, String key) throws EditiqueException {
		return editiqueService.getPDFDeDocumentDepuisArchive(noCtb, typeDoc, key);
	}
}