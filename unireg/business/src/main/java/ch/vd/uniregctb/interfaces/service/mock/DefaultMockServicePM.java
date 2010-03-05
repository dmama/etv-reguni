package ch.vd.uniregctb.interfaces.service.mock;

import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;

public class DefaultMockServicePM extends MockServicePM {

	@Override
	protected void init() {
		addPM(MockPersonneMorale.NestleSuisse);
		addPM(MockPersonneMorale.BCV);
		addPM(MockPersonneMorale.KPMG);
		addPM(MockPersonneMorale.CuriaTreuhand);
	}

}
