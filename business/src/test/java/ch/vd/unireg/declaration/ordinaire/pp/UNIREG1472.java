package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementPersonnesPhysiquesCalculator;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.AssujettissementServiceImpl;
import ch.vd.unireg.metier.assujettissement.Indigent;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionServiceImpl;
import ch.vd.unireg.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.unireg.parametrage.MockParameterAppService;
import ch.vd.unireg.tiers.TiersServiceImpl;

public class UNIREG1472 {
	
	private EnvoiSommationsDIsPPProcessor processor;

	@Before
	public void init() throws Exception {
		final AssujettissementService assujettissementService = new AssujettissementServiceImpl();
		final PeriodeImpositionServiceImpl periodeImpositionService = new PeriodeImpositionServiceImpl();
		periodeImpositionService.setAssujettissementService(assujettissementService);
		periodeImpositionService.setTiersService(new TiersServiceImpl());
		periodeImpositionService.setParametreAppService(new MockParameterAppService());
		periodeImpositionService.afterPropertiesSet();
		processor = new EnvoiSommationsDIsPPProcessor(null, null, null,null, null, null, assujettissementService, periodeImpositionService, null);
	}
	
	@Test
	public void testIsIndigent() {

		final DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.setDateDebut(RegDate.get(2008,1,1));
		di.setDateFin(RegDate.get(2008,12,31));

		{
			final List<Assujettissement> assujettissements = Collections.<Assujettissement>singletonList(new Indigent(null, RegDate.get(2008, 1, 1), RegDate.get(2008, 12, 31), null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER));
			Assert.isTrue(processor.isIndigent(di, assujettissements));
		}

		{
			final List<Assujettissement> assujettissements = Collections.<Assujettissement>singletonList(new VaudoisOrdinaire(null, RegDate.get(2008, 1, 1), RegDate.get(2008, 12, 31), null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER));
			Assert.isFalse(processor.isIndigent(di, assujettissements));
		}

		{
			final List<Assujettissement> assujettissements = Arrays.asList(new Indigent(null, RegDate.get(2008, 1, 1), RegDate.get(2008, 1, 31), null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER),
			                                                               new VaudoisOrdinaire(null, RegDate.get(2008, 2, 1), RegDate.get(2008, 12, 31), null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER));
			Assert.isFalse(processor.isIndigent(di, assujettissements));
		}
	}

}
