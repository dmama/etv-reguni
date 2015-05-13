package ch.vd.uniregctb.migration.pm.historizer.equalator;

import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.Etablissement;

public class EtablissementEqualator implements Equalator<Etablissement> {

	@Override
	public boolean test(Etablissement e1, Etablissement e2) {
		if (e1 == e2) {
			return true;
		}
		if (e1 == null || e2 == null || e1.getClass() != e2.getClass()) {
			return false;
		}

		if (e1.getId() != e2.getId()) return false;
		if (e1.getNoIde() != null ? !e1.getNoIde().equals(e2.getNoIde()) : e2.getNoIde() != null) return false;
		if (e1.getNom() != null ? !e1.getNom().equals(e2.getNom()) : e2.getNom() != null) return false;
		if (e1.getNoOfsCommune() != null ? !e1.getNoOfsCommune().equals(e2.getNoOfsCommune()) : e2.getNoOfsCommune() != null) return false;
		return !(e1.getNoga() != null ? !e1.getNoga().equals(e2.getNoga()) : e2.getNoga() != null);
	}
}
