package ch.vd.uniregctb.common;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class TransactionTemplateFactory implements ch.vd.shared.batchtemplate.TransactionTemplateFactory {

	@Override
	public TransactionTemplate createTransactionTemplate(PlatformTransactionManager transactionManager) {
		return new org.springframework.transaction.support.TransactionTemplate(transactionManager);
	}
}
