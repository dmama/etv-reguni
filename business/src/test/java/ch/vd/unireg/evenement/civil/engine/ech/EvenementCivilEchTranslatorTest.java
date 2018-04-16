package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class EvenementCivilEchTranslatorTest extends BusinessTest {
	
	private EvenementCivilEchTranslatorImplOverride translator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		
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
		translator.setParameters(getBean(EvenementCivilEchStrategyParameters.class, "evenementCivilEchStrategyParameters"));
		translator.afterPropertiesSet();
	}

	@Test(timeout = 10000L)
	public void testCouvertureStrategies() throws Exception {
		final Set<TypeEvenementCivilEch> typesIgnores = EnumSet.of(TypeEvenementCivilEch.TESTING);
		for (TypeEvenementCivilEch type : TypeEvenementCivilEch.values()) {
			if (!typesIgnores.contains(type)) {
				for (ActionEvenementCivilEch action : ActionEvenementCivilEch.values()) {
					final EvenementCivilEchTranslatorImpl.EventTypeKey key = new EvenementCivilEchTranslatorImpl.EventTypeKey(type, action);
					final EvenementCivilEchTranslationStrategy strategy = translator.getStrategyFromMap(key);
					Assert.assertNotNull(String.format("Pas de stratégie pour la combinaison %s/%s", type, action), strategy);
				}
			}
		}
	}
	
	@Test(timeout = 10000L)
	public void testStrategyOverride() throws Exception {

		// vérifier que les tests qui utiliseront la méthode de strategy override seront bien appelés sur la bonne stratégie

		Assert.assertNull(translator.getStrategy(new EvenementCivilEchTranslatorImpl.EventTypeKey(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.ANNULATION)));

		final MutableBoolean appel = new MutableBoolean(false);
		translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.ANNULATION, new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
				appel.setValue(true);
				return null;
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
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
