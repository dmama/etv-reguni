package ch.vd.unireg.common;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.parametrage.MockParameterAppService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.type.TypeContribuable;

public class TacheHelperTest extends WithoutSpringTest {

	@Test
	public void testDateEcheanceTacheEnvoiDIPM() throws Exception {
		final ParametreAppService parametres = new MockParameterAppService();

		Assert.assertEquals(date(2016, 2, 14), TacheHelper.getDateEcheanceTacheEnvoiDIPM(parametres, TypeContribuable.VAUDOIS_ORDINAIRE, date(2016, 2, 12), date(2015, 12, 31)));
		Assert.assertEquals(date(2016, 2, 14), TacheHelper.getDateEcheanceTacheEnvoiDIPM(parametres, TypeContribuable.HORS_CANTON, date(2016, 2, 12), date(2015, 12, 31)));
		Assert.assertEquals(date(2016, 2, 14), TacheHelper.getDateEcheanceTacheEnvoiDIPM(parametres, TypeContribuable.HORS_SUISSE, date(2016, 2, 12), date(2015, 12, 31)));
		Assert.assertEquals(date(2016, 2, 14), TacheHelper.getDateEcheanceTacheEnvoiDIPM(parametres, TypeContribuable.UTILITE_PUBLIQUE, date(2016, 2, 12), date(2015, 12, 31)));

		Assert.assertEquals(date(2016, 1, 10), TacheHelper.getDateEcheanceTacheEnvoiDIPM(parametres, TypeContribuable.VAUDOIS_ORDINAIRE, date(2016, 1, 6), date(2015, 12, 31)));
		Assert.assertEquals(date(2016, 1, 10), TacheHelper.getDateEcheanceTacheEnvoiDIPM(parametres, TypeContribuable.HORS_CANTON, date(2016, 1, 6), date(2015, 12, 31)));
		Assert.assertEquals(date(2016, 1, 10), TacheHelper.getDateEcheanceTacheEnvoiDIPM(parametres, TypeContribuable.HORS_SUISSE, date(2016, 1, 6), date(2015, 12, 31)));
		Assert.assertEquals(date(2016, 1, 31), TacheHelper.getDateEcheanceTacheEnvoiDIPM(parametres, TypeContribuable.UTILITE_PUBLIQUE, date(2016, 1, 6), date(2015, 12, 31)));
	}

}