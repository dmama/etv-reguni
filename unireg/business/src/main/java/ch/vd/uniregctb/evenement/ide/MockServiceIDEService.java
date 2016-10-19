package ch.vd.uniregctb.evenement.ide;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2016-10-13, <raphael.marmier@vd.ch>
 */
public class MockServiceIDEService implements ServiceIDEService {
	@Override
	public boolean isServiceIDEObligEtendues(Entreprise entreprise, RegDate date) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AnnonceIDEEnvoyee synchroniseIDE(Entreprise entreprise) throws ServiceIDEException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ProtoAnnonceIDE simuleSynchronisationIDE(Entreprise entreprise) throws ServiceIDEException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void validerAnnonceIDE(BaseAnnonceIDE proto) throws ServiceIDEException {
		throw new UnsupportedOperationException();
	}
}
