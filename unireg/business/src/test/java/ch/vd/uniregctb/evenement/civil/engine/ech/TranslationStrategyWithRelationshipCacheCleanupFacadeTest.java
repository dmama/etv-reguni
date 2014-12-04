package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;
import net.sf.ehcache.CacheManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.cache.ServiceCivilCache;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class TranslationStrategyWithRelationshipCacheCleanupFacadeTest extends AbstractEvenementCivilEchProcessorTest {

	@Test
	public void testNettoyageCacheCivilSurEvenementTraite() throws Exception {
		final EvenementCivilEchTranslationStrategy noOpStrategy = new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
				return new EvenementCivilInterne(event, context, options) {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						return HandleStatus.TRAITE;
					}

					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					}
				};
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
				return false;
			}
		};

		checkNettoyageCacheCivil(noOpStrategy, EtatEvenementCivil.TRAITE);
	}

	@Test
	public void testNettoyageCacheCivilSurEvenementTraiteAvecParentesFiscales() throws Exception {
		final EvenementCivilEchTranslationStrategy noOpStrategy = new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
				return new EvenementCivilInterne(event, context, options) {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						return HandleStatus.TRAITE;
					}

					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					}
				};
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
				return false;
			}
		};

		checkNettoyageCacheCivilAvecParenteFiscales(noOpStrategy, EtatEvenementCivil.TRAITE);
	}

	@Test
	public void testNettoyageCacheCivilAvecExceptionDansStrategy() throws Exception {
		final EvenementCivilEchTranslationStrategy explodingStrategy = new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
				throw new EvenementCivilException("Boom!");
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
				return false;
			}
		};

		checkNettoyageCacheCivil(explodingStrategy, EtatEvenementCivil.EN_ERREUR);
	}

	@Test
	public void testNettoyageCacheCivilAvecExceptionDansStrategyAvecParentesFiscales() throws Exception {
		final EvenementCivilEchTranslationStrategy explodingStrategy = new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
				throw new EvenementCivilException("Boom!");
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
				return false;
			}
		};

		checkNettoyageCacheCivilAvecParenteFiscales(explodingStrategy, EtatEvenementCivil.EN_ERREUR);
	}

	@Test
	public void testNettoyageCacheCivilAvecExceptionDansTraitement() throws Exception {
		final EvenementCivilEchTranslationStrategy explodingStrategy = new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
				return new EvenementCivilInterne(event, context, options) {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						throw new EvenementCivilException("Boom!");
					}

					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					}
				};
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
				return false;
			}
		};

		checkNettoyageCacheCivil(explodingStrategy, EtatEvenementCivil.EN_ERREUR);
	}

	@Test
	public void testNettoyageCacheCivilAvecExceptionDansTraitementAvecParentesFiscales() throws Exception {
		final EvenementCivilEchTranslationStrategy explodingStrategy = new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
				return new EvenementCivilInterne(event, context, options) {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						throw new EvenementCivilException("Boom!");
					}

					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					}
				};
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEchFacade event, EvenementCivilContext context) throws EvenementCivilException {
				return false;
			}
		};

		checkNettoyageCacheCivilAvecParenteFiscales(explodingStrategy, EtatEvenementCivil.EN_ERREUR);
	}

	private void checkNettoyageCacheCivil(EvenementCivilEchTranslationStrategy finalStrategy, final EtatEvenementCivil expectedEventState) throws Exception {

		final long noIndividuGrandPere = 43842L;
		final long noIndividuPere = 32784264L;
		final long noIndividuMere = 674235L;
		final long noIndividuFils = 422331L;
		final RegDate dateNaissanceGrandPere = date(1912, 4, 30);
		final RegDate dateNaissancePere = date(1945, 9, 27);
		final RegDate dateNaissanceMere = date(1945, 4, 12);
		final RegDate dateMariage = date(1967, 5, 1);
		final RegDate dateNaissanceFils = date(1968, 12, 9);

		// créée le service civil et un cache par devant
		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setCacheManager(getBean(CacheManager.class, "ehCacheManager"));
		cache.setCacheName("serviceCivil");
		cache.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		cache.setTarget(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu pere = addIndividu(noIndividuPere, dateNaissancePere, "Tartempion", "John", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndividuMere, dateNaissanceMere, "Tartempion", "Rita", Sexe.FEMININ);
				final MockIndividu fils = addIndividu(noIndividuFils, dateNaissanceFils, "Tartempion", "Junior", Sexe.MASCULIN);
				final MockIndividu grandPere = addIndividu(noIndividuGrandPere, dateNaissanceGrandPere, "Tartempion", "Senior", Sexe.MASCULIN);
				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(pere, grandPere, null, dateNaissancePere, null);
				marieIndividus(pere, mere, dateMariage);
			}
		});
		cache.afterPropertiesSet();
		try {
			cache.reset();      // to make sure the cache is empty...
			serviceCivil.setUp(cache);

			// mise en place fiscale
			final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = addHabitant(noIndividuPere);
					return pp.getNumero();
				}
			});

			// remplissons le cache avec toutes les données
			serviceCivil.getIndividu(noIndividuFils, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);
			serviceCivil.getIndividu(noIndividuMere, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);
			serviceCivil.getIndividu(noIndividuPere, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);
			serviceCivil.getIndividu(noIndividuGrandPere, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);

			// modification des données sous-jacentes au cache
			doModificationIndividu(noIndividuFils, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					individu.setPrenomUsuel("Johnny");
				}
			});
			doModificationIndividu(noIndividuGrandPere, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					individu.setPrenomUsuel("John Senior");
				}
			});
			doModificationIndividu(noIndividuMere, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					individu.setPrenomUsuel("Barbara");
				}
			});

			// le cache n'a pas été notifié des changements donc on doit encore voir les anciens prénoms
			Assert.assertEquals("Junior", serviceCivil.getIndividu(noIndividuFils, null).getPrenomUsuel());
			Assert.assertEquals("Senior", serviceCivil.getIndividu(noIndividuGrandPere, null).getPrenomUsuel());
			Assert.assertEquals("Rita", serviceCivil.getIndividu(noIndividuMere, null).getPrenomUsuel());

			// mise en place de la stratégie
			final EvenementCivilEchTranslationStrategy strategy = new TranslationStrategyWithRelationshipCacheCleanupFacade(finalStrategy, serviceCivil, dataEventService, tiersService);
			buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
				@Override
				public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
					translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.PREMIERE_LIVRAISON, strategy);
				}
			});

			// création de l'événement civil qui va passer par cette stratégie sur le père (= celui qui a des relations avec tous les autres)
			final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(2367353L);
					evt.setNumeroIndividu(noIndividuPere);
					evt.setType(TypeEvenementCivilEch.TESTING);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(RegDate.get());
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					return hibernateTemplate.merge(evt).getId();
				}
			});

			// traite l'événement civil
			traiterEvenements(noIndividuPere);

			// l'événement civil doit avoir été traité
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = evtCivilDAO.get(evtId);
					Assert.assertNotNull(evt);
					Assert.assertEquals(expectedEventState, evt.getEtat());
					return null;
				}
			});

			// la relation père -> enfant n'est pas fournie par le civil, donc le cache de l'enfant n'a pas été rafraîchi
			Assert.assertEquals("Junior", serviceCivil.getIndividu(noIndividuFils, null).getPrenomUsuel());

			// le cache des relations du père doit avoir été invalidé, les nouveaux prénoms doivent apparaître
			Assert.assertEquals("John Senior", serviceCivil.getIndividu(noIndividuGrandPere, null).getPrenomUsuel());
			Assert.assertEquals("Barbara", serviceCivil.getIndividu(noIndividuMere, null).getPrenomUsuel());
		}
		finally {
			cache.destroy();
		}
	}

	private void checkNettoyageCacheCivilAvecParenteFiscales(EvenementCivilEchTranslationStrategy finalStrategy, final EtatEvenementCivil expectedEventState) throws Exception {

		final long noIndividuGrandPere = 43842L;
		final long noIndividuPere = 32784264L;
		final long noIndividuMere = 674235L;
		final long noIndividuFils = 422331L;
		final RegDate dateNaissanceGrandPere = date(1912, 4, 30);
		final RegDate dateNaissancePere = date(1945, 9, 27);
		final RegDate dateNaissanceMere = date(1945, 4, 12);
		final RegDate dateMariage = date(1967, 5, 1);
		final RegDate dateNaissanceFils = date(1968, 12, 9);

		// créée le service civil et un cache par devant
		final ServiceCivilCache cache = new ServiceCivilCache();
		cache.setCacheManager(getBean(CacheManager.class, "ehCacheManager"));
		cache.setCacheName("serviceCivil");
		cache.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		cache.setTarget(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu pere = addIndividu(noIndividuPere, dateNaissancePere, "Tartempion", "John", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndividuMere, dateNaissanceMere, "Tartempion", "Rita", Sexe.FEMININ);
				final MockIndividu fils = addIndividu(noIndividuFils, dateNaissanceFils, "Tartempion", "Junior", Sexe.MASCULIN);
				final MockIndividu grandPere = addIndividu(noIndividuGrandPere, dateNaissanceGrandPere, "Tartempion", "Senior", Sexe.MASCULIN);
				addLiensFiliation(fils, pere, mere, dateNaissanceFils, null);
				addLiensFiliation(pere, grandPere, null, dateNaissancePere, null);
				marieIndividus(pere, mere, dateMariage);
			}
		});
		cache.afterPropertiesSet();
		try {
			cache.reset();      // to make sure the cache is empty...
			serviceCivil.setUp(cache);

			// mise en place fiscale
			doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					addHabitant(noIndividuPere);
					addHabitant(noIndividuMere);
					addHabitant(noIndividuFils);
					addHabitant(noIndividuGrandPere);
					return null;
				}
			});

			// remplissons le cache avec toutes les données
			serviceCivil.getIndividu(noIndividuFils, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);
			serviceCivil.getIndividu(noIndividuMere, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);
			serviceCivil.getIndividu(noIndividuPere, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);
			serviceCivil.getIndividu(noIndividuGrandPere, null, AttributeIndividu.PARENTS, AttributeIndividu.CONJOINTS);

			// modification des données sous-jacentes au cache
			doModificationIndividu(noIndividuFils, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					individu.setPrenomUsuel("Johnny");
				}
			});
			doModificationIndividu(noIndividuGrandPere, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					individu.setPrenomUsuel("John Senior");
				}
			});
			doModificationIndividu(noIndividuMere, new IndividuModification() {
				@Override
				public void modifyIndividu(MockIndividu individu) {
					individu.setPrenomUsuel("Barbara");
				}
			});

			// le cache n'a pas été notifié des changements donc on doit encore voir les anciens prénoms
			Assert.assertEquals("Junior", serviceCivil.getIndividu(noIndividuFils, null).getPrenomUsuel());
			Assert.assertEquals("Senior", serviceCivil.getIndividu(noIndividuGrandPere, null).getPrenomUsuel());
			Assert.assertEquals("Rita", serviceCivil.getIndividu(noIndividuMere, null).getPrenomUsuel());

			// mise en place de la stratégie
			final EvenementCivilEchTranslationStrategy strategy = new TranslationStrategyWithRelationshipCacheCleanupFacade(finalStrategy, serviceCivil, dataEventService, tiersService);
			buildStrategyOverridingTranslatorAndProcessor(true, new StrategyOverridingCallback() {
				@Override
				public void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator) {
					translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.PREMIERE_LIVRAISON, strategy);
				}
			});

			// création de l'événement civil qui va passer par cette stratégie sur le père (= celui qui a des relations avec tous les autres)
			final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(2367353L);
					evt.setNumeroIndividu(noIndividuPere);
					evt.setType(TypeEvenementCivilEch.TESTING);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(RegDate.get());
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					return hibernateTemplate.merge(evt).getId();
				}
			});

			// traite l'événement civil
			traiterEvenements(noIndividuPere);

			// l'événement civil doit avoir été traité
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = evtCivilDAO.get(evtId);
					Assert.assertNotNull(evt);
					Assert.assertEquals(expectedEventState, evt.getEtat());
					return null;
				}
			});

			// la relation père -> enfant n'est pas fournie par le civil, mais par le fiscal, oui -> le nouveau prénom doit apparaître aussi
			Assert.assertEquals("Johnny", serviceCivil.getIndividu(noIndividuFils, null).getPrenomUsuel());

			// le cache des relations du père (parents + conjoints) doit avoir été invalidé, les nouveaux prénoms doivent apparaître
			Assert.assertEquals("John Senior", serviceCivil.getIndividu(noIndividuGrandPere, null).getPrenomUsuel());
			Assert.assertEquals("Barbara", serviceCivil.getIndividu(noIndividuMere, null).getPrenomUsuel());
		}
		finally {
			cache.destroy();
		}
	}
}
