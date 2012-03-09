package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchContext;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchTranslatorTest extends BusinessTest {
	
	private EvenementCivilEchTranslatorImplOverride translator;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		
		translator = new EvenementCivilEchTranslatorImplOverride();
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setIndexer(globalTiersIndexer);
		translator.setMetierService(getBean(MetierService.class, "metierService"));
		translator.setServiceCivilService(serviceCivil);
		translator.setServiceInfrastructureService(serviceInfra);
		translator.setTiersDAO(tiersDAO);
		translator.setTiersService(tiersService);
		translator.afterPropertiesSet();
	}

	@Test
	public void testCouvertureStrategies() throws Exception {
		final Set<TypeEvenementCivilEch> typesIgnores = new HashSet<TypeEvenementCivilEch>(Arrays.asList(TypeEvenementCivilEch.TESTING));
		for (TypeEvenementCivilEch type : TypeEvenementCivilEch.values()) {
			if (!typesIgnores.contains(type)) {
				for (ActionEvenementCivilEch action : ActionEvenementCivilEch.values()) {
					final EvenementCivilEchTranslatorImpl.EventTypeKey key = new EvenementCivilEchTranslatorImpl.EventTypeKey(type, action);
					final EvenementCivilEchTranslationStrategy strategy = EvenementCivilEchTranslatorImpl.getStrategyFromMap(key);
					Assert.assertNotNull(String.format("Pas de stratégie pour la combinaison %s/%s", type, action), strategy);
				}
			}
		}
	}
	
	@Test
	public void testStrategyOverride() throws Exception {

		// vérifier que les tests qui utiliseront la méthode de strategy override seront bien appelés sur la bonne stratégie

		Assert.assertNull(translator.getStrategy(new EvenementCivilEchTranslatorImpl.EventTypeKey(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.ANNULATION)));

		final MutableBoolean appel = new MutableBoolean(false);
		translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.ANNULATION, new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilEchContext context, EvenementCivilOptions options) throws EvenementCivilException {
				appel.setValue(true);
				return null;
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilEchContext context) throws EvenementCivilException {
				return false;
			}
		});
		Assert.assertNotNull(translator.getStrategy(new EvenementCivilEchTranslatorImpl.EventTypeKey(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.ANNULATION)));
		Assert.assertFalse(appel.booleanValue());
		
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EvenementCivilEch evtCivil = new EvenementCivilEch();
				evtCivil.setId(12367L);
				evtCivil.setType(TypeEvenementCivilEch.TESTING);
				evtCivil.setAction(ActionEvenementCivilEch.ANNULATION);
				evtCivil.setEtat(EtatEvenementCivil.A_TRAITER);
				evtCivil.setDateEvenement(RegDate.get());
				evtCivil.setNumeroIndividu(12L);
				final EvenementCivilEch evt = hibernateTemplate.merge(evtCivil);
				
				final EvenementCivilInterne interne = translator.toInterne(evt, new EvenementCivilOptions(true));
				Assert.assertNull(interne);
				Assert.assertTrue(appel.booleanValue());
				return null;
			}
		});
	}
}
