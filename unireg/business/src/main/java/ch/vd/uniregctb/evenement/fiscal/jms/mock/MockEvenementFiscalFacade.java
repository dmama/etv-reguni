package ch.vd.uniregctb.evenement.fiscal.jms.mock;

import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalException;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalFacade;

public class MockEvenementFiscalFacade implements EvenementFiscalFacade {

	public  int count;

	public void publierEvenement(EvenementFiscal evenement) throws EvenementFiscalException {
		count++;
	}


}
