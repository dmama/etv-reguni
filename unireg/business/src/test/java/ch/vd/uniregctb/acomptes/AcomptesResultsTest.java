package ch.vd.uniregctb.acomptes;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import org.junit.Assert;
import org.junit.Test;

public class AcomptesResultsTest extends BusinessTest {

	private static final RegDate dateTraitement = RegDate.get();
	private static final RegDate dateOuvertureForPrincipal = RegDate.get(1976,11,2);
	private static final RegDate dateOuvertureForSecondaire = RegDate.get(1998,5,12);

	@Test
	public void testCalculerInfoAssujettissementHC() {

		final int annee = dateTraitement.year();
		final AcomptesResults results = new AcomptesResults(dateTraitement, 1, annee, tiersService, null);

		final PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNumero(12345678L);
		pp.addForFiscal(new ForFiscalPrincipal(dateOuvertureForPrincipal, null, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(dateOuvertureForSecondaire, null, MockCommune.Cossonay.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		final AcomptesResults.InfoAssujettissementContribuable info = results.calculerInfoAssujettissement(pp, dateTraitement.year());
		Assert.assertNotNull(info);
	}

	@Test
	public void testCalculerInfoAssujettissementHS() {

		final int annee = dateTraitement.year();
		final AcomptesResults results = new AcomptesResults(dateTraitement, 1, annee, tiersService, null);

		final PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNumero(12345678L);
		pp.addForFiscal(new ForFiscalPrincipal(dateOuvertureForPrincipal, null, MockPays.Danemark.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(dateOuvertureForSecondaire, null, MockCommune.Cossonay.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		final AcomptesResults.InfoAssujettissementContribuable info = results.calculerInfoAssujettissement(pp, dateTraitement.year());
		Assert.assertNotNull(info);
	}

}
