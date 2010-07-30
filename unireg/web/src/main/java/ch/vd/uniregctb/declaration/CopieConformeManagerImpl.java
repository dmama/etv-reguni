package ch.vd.uniregctb.declaration;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class CopieConformeManagerImpl implements CopieConformeManager {

	private HibernateTemplate hibernateTemplate;

	private DeclarationImpotService diService;

	private ListeRecapService lrService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setLrService(ListeRecapService lrService) {
		this.lrService = lrService;
	}

	@Transactional(rollbackFor = Throwable.class)
	public byte[] getPdfCopieConformeSommation(Long idEtatSomme) throws EditiqueException {

		final EtatDeclaration etat = (EtatDeclaration) hibernateTemplate.get(EtatDeclaration.class, idEtatSomme);
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
			throw new IllegalArgumentException("La déclaration n'est ni une DI ni une LR (" + declaration.getClass().getName() + ")");
		}
	}
}