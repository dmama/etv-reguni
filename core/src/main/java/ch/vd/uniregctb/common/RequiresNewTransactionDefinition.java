package ch.vd.uniregctb.common;

import org.springframework.transaction.support.DefaultTransactionDefinition;

public class RequiresNewTransactionDefinition extends DefaultTransactionDefinition {

	private static final long serialVersionUID = -370488358108671952L;

	public RequiresNewTransactionDefinition() {

		setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
	}

}
