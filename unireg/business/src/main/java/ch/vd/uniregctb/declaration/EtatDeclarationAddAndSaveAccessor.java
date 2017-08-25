package ch.vd.uniregctb.declaration;

import java.util.Collection;
import java.util.function.UnaryOperator;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AddAndSaveHelper;
import ch.vd.uniregctb.common.HibernateEntity;

/**
 * @param <D> type de déclaration
 * @param <E> type d'état de déclaration
 * @see AddAndSaveHelper#addAndSave(HibernateEntity, HibernateEntity, UnaryOperator, AddAndSaveHelper.EntityAccessor)
 */
public final class EtatDeclarationAddAndSaveAccessor<D extends Declaration, E extends EtatDeclaration> implements AddAndSaveHelper.EntityAccessor<D, E> {

	@Override
	public Collection<? extends HibernateEntity> getEntities(D container) {
		return container.getEtatsDeclaration();
	}

	@Override
	public void addEntity(D container, E entity) {
		container.addEtat(entity);
	}

	@Override
	public void assertEquals(E e1, E e2) {
		Assert.isSame(e1.getClass(), e2.getClass());
		Assert.isSame(e1.getDateDebut(), e2.getDateDebut());
		Assert.isSame(e1.getDateObtention(), e2.getDateObtention());
		Assert.isSame(e1.getEtat(), e2.getEtat());
	}
}
