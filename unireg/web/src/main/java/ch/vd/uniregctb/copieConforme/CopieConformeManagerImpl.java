package ch.vd.uniregctb.copieConforme;

import java.io.InputStream;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class CopieConformeManagerImpl implements CopieConformeManager {

	private HibernateTemplate hibernateTemplate;

	private DeclarationImpotService diService;

	private ListeRecapService lrService;

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

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public InputStream getPdfCopieConformeSommation(Long idEtatSomme) throws EditiqueException {

		final EtatDeclaration etat = hibernateTemplate.get(EtatDeclaration.class, idEtatSomme);
		Assert.notNull(etat);
		Assert.isEqual(TypeEtatDeclaration.SOMMEE, etat.getEtat());

		final Declaration declaration = etat.getDeclaration();
		if (declaration instanceof DeclarationImpotOrdinaire) {
			return diService.getCopieConformeSommationDI((DeclarationImpotOrdinaire) declaration);
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
	public InputStream getPdfCopieConformeDelai(Long idDelai) throws EditiqueException {
		final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, idDelai);
		Assert.notNull(delai);
		return diService.getCopieConformeConfirmationDelai(delai);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public InputStream getPdfCopieConforme(long noCtb, String key) throws EditiqueException {

		// TODO jde coder le service de récupération de copie conforme...
//		return editiqueService.getPDFDeDocumentDepuisArchive(noCtb, TypeDocumentEditique.???, key);
		try {
			Thread.sleep(3000);
		}
		catch (InterruptedException e) {
			// pas grave...
		}
		return null;
	}
}