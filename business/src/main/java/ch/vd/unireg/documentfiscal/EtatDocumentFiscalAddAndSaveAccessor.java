package ch.vd.unireg.documentfiscal;

import java.util.Collection;
import java.util.function.UnaryOperator;

import ch.vd.unireg.common.AddAndSaveHelper;
import ch.vd.unireg.common.HibernateEntity;

/**
 * @param <D> type de document fiscal
 * @param <E> type d'état de document fiscal
 * @see AddAndSaveHelper#addAndSave(HibernateEntity, HibernateEntity, UnaryOperator, AddAndSaveHelper.EntityAccessor)
 */
public final class EtatDocumentFiscalAddAndSaveAccessor<D extends DocumentFiscal, E extends EtatDocumentFiscal> implements AddAndSaveHelper.EntityAccessor<D, E> {

	@Override
	public Collection<? extends EtatDocumentFiscal> getEntities(D container) {
		return container.getEtats();
	}

	@Override
	public void addEntity(D container, E entity) {
		container.addEtat(entity);
	}

	@Override
	public void assertEquals(E e1, E e2) {
		if (e1.getClass() != e2.getClass()) {
			throw new IllegalArgumentException();
		}
		if (e1.getDateDebut() != e2.getDateDebut()) {
			throw new IllegalArgumentException();
		}
		if (e1.getDateObtention() != e2.getDateObtention()) {
			throw new IllegalArgumentException();
		}
		if (e1.getEtat() != e2.getEtat()) {
			throw new IllegalArgumentException();
		}
	}
}
