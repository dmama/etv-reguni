package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementServiceImpl;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionServiceImpl;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.parametrage.MockParameterAppService;
import ch.vd.uniregctb.tiers.TiersServiceImpl;

public class UNIREG1472 {
	
	private EnvoiSommationsDIsPPProcessor processor;

	@Before
	public void init () throws Exception {
		final AssujettissementService assujettissementService = new AssujettissementServiceImpl();
		final PeriodeImpositionServiceImpl periodeImpositionService = new PeriodeImpositionServiceImpl();
		periodeImpositionService.setAssujettissementService(assujettissementService);
		periodeImpositionService.setTiersService(new TiersServiceImpl());
		periodeImpositionService.setParametreAppService(new MockParameterAppService());
		periodeImpositionService.afterPropertiesSet();
		processor = new EnvoiSommationsDIsPPProcessor(null, null, null,null, null, null, assujettissementService, periodeImpositionService, null);
	}
	
	@Test
	public void isIndigent() {

		DeclarationImpotOrdinairePP di = new DeclarationImpotOrdinairePP();
		di.setDateDebut(RegDate.get(2008,1,1));
		di.setDateFin(RegDate.get(2008,12,31));
		
		List<Assujettissement> assujettissements = new ArrayList<Assujettissement> ( Arrays.asList(
				new Indigent(null,RegDate.get(2008,1,1), RegDate.get(2008,12,31), null, null)));

		Assert.isTrue(processor.isIndigent(di,assujettissements));
		
		assujettissements = new ArrayList<Assujettissement> ( Arrays.asList(
				new VaudoisOrdinaire(null,RegDate.get(2008,1,1), RegDate.get(2008,12,31), null, null)));
		
		Assert.isFalse(processor.isIndigent(di,assujettissements));

		assujettissements = new ArrayList<>( Arrays.asList(
				new Indigent(null,RegDate.get(2008,1,1), RegDate.get(2008,1,31), null, null),
				new VaudoisOrdinaire(null,RegDate.get(2008,2,1), RegDate.get(2008,12,31), null, null)));
		
		Assert.isFalse(processor.isIndigent(di,assujettissements));
	}

}
