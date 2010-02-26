package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;

public class UNIREG1472 {
	
	private EnvoiSommationsDIsProcessor processor;
		
	@Before
	public void init () {
		processor = new EnvoiSommationsDIsProcessor(null, null, null,null, null);
	}
	
	@Test
	public void isIndigent() {
		
		DeclarationImpotOrdinaire di = new DeclarationImpotOrdinaire ();
		di.setDateDebut(RegDate.get(2008,1,1));
		di.setDateFin(RegDate.get(2008,12,31));
		
		List<Assujettissement> assujettissements = new ArrayList<Assujettissement> ( Arrays.asList(
				new Assujettissement[] {
						new Indigent(null,RegDate.get(2008,1,1), RegDate.get(2008,12,31), null, null)
				}
		));

		Assert.isTrue(processor.isIndigent(di,assujettissements));
		
		assujettissements = new ArrayList<Assujettissement> ( Arrays.asList(
				new Assujettissement[] {
						new VaudoisOrdinaire(null,RegDate.get(2008,1,1), RegDate.get(2008,12,31), null, null)
				}
		));
		
		Assert.isFalse(processor.isIndigent(di,assujettissements));

		assujettissements = new ArrayList<Assujettissement> ( Arrays.asList(
				new Assujettissement[] {
						new Indigent(null,RegDate.get(2008,1,1), RegDate.get(2008,1,31), null, null),
						new VaudoisOrdinaire(null,RegDate.get(2008,2,1), RegDate.get(2008,12,31), null, null)
				}
		));
		
		Assert.isFalse(processor.isIndigent(di,assujettissements));
	}

}
