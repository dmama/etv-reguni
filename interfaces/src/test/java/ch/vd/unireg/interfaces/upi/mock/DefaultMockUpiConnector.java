package ch.vd.unireg.interfaces.upi.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.mock.MockNationalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;
import ch.vd.unireg.type.Sexe;

public class DefaultMockUpiConnector extends MockUpiConnector {

	@Override
	protected void init() {
		addData(new UpiPersonInfo("7565115001333", "Susan Alexandra", "Weaver", Sexe.FEMININ, RegDate.get(1949, 10, 8), null, MockNationalite.of(null, null, MockPays.EtatsUnis), new NomPrenom("Inglis", "Elisabeth"), new NomPrenom("Weaver", "Sylvester")));
		addData(new UpiPersonInfo("7566285711978", "Michel", "Jackson", Sexe.MASCULIN, RegDate.get(1958, 8, 29), RegDate.get(2009, 6, 25), MockNationalite.of(null, null, MockPays.EtatsUnis), new NomPrenom("Scruse", "Katherine Esther"), new NomPrenom("Jackson", "Joseph")));
		addData(new UpiPersonInfo("7564457068837", "Philippe", "Martin", Sexe.MASCULIN, RegDate.get(1970, 10, 8), null, MockNationalite.of(null, null, MockPays.Suisse), new NomPrenom("Martin", "Elisabeth"), new NomPrenom("Martin", "Albert")));
		addData(new UpiPersonInfo("7569050304498", "Sophie Amandine", "Pittet", Sexe.FEMININ, RegDate.get(1980, 2, 6), null, MockNationalite.of(null, null, MockPays.Suisse), new NomPrenom("Pittet", "Françoise"), new NomPrenom("Pittet", "Alphonse")));
		addData(new UpiPersonInfo("7564775497586", "Caroline", "Toutcourt", Sexe.FEMININ, RegDate.get(2001, 1, 5), null, MockNationalite.of(null, null, MockPays.France), new NomPrenom("de France", "Hélène"), new NomPrenom("Toutcourt", "Henri")));

		addReplacement("7567986294906", "7565115001333");
		addReplacement("7566101270542", "7566285711978");
		addReplacement("7561163512081", "7564457068837");
		addReplacement("7560142399040", "7569050304498");
		addReplacement("7568683576722", "7564775497586");
	}
}
