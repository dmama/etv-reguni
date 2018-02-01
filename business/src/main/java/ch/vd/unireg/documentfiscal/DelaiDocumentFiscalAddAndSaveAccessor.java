package ch.vd.unireg.documentfiscal;

import java.util.Collection;
import java.util.function.UnaryOperator;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.AddAndSaveHelper;
import ch.vd.unireg.common.HibernateEntity;

/**
 * @param <D> type de document fiscal
 * @param <E> type de délai de document fiscal
 * @see AddAndSaveHelper#addAndSave(HibernateEntity, HibernateEntity, UnaryOperator, AddAndSaveHelper.EntityAccessor)
 */
public final class DelaiDocumentFiscalAddAndSaveAccessor<D extends DocumentFiscal, E extends DelaiDocumentFiscal> implements AddAndSaveHelper.EntityAccessor<D, E> {

	@Override
	public Collection<? extends DelaiDocumentFiscal> getEntities(D container) {
		return container.getDelais();
	}

	@Override
	public void addEntity(D container, E entity) {
		container.addDelai(entity);
	}

	@Override
	public void assertEquals(E d1, E d2) {
		Assert.isSame(d1.getClass(), d2.getClass());
		Assert.isSame(d1.getDelaiAccordeAu(), d2.getDelaiAccordeAu());
		Assert.isSame(d1.getDateDemande(), d2.getDateDemande());
		Assert.isSame(d1.getDateTraitement(), d2.getDateTraitement());
	}
}

