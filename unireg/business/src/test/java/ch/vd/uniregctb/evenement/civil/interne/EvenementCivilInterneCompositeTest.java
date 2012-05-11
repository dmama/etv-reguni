package ch.vd.uniregctb.evenement.civil.interne;

import java.util.HashSet;
import java.util.Set;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.tiers.MockTiersDAO;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class EvenementCivilInterneCompositeTest {

	private EvenementCivilInterne[] mockEvenementsComposants;
	private EvenementCivilEch mockEvenementCivilEch;
	private EvenementCivilContext mockEvenementCivilContext;
	private EvenementCivilOptions mockEvenementCivilOptions;
	private EvenementCivilErreurCollector mockEvtCivErrCol;
	private EvenementCivilWarningCollector mockEvtCivWarnCol;

	@Before
	public void init() throws EvenementCivilException {
		final MockTiersDAO mockTiersDAO = new MockTiersDAO();
		mockEvenementsComposants = new EvenementCivilInterne[] {
				createMock(EvenementCivilInterne.class),
				createMock(EvenementCivilInterne.class),
				createMock(EvenementCivilInterne.class)
		};
		mockEvenementCivilEch = createMock(EvenementCivilEch.class);
		mockEvenementCivilContext = createMock(EvenementCivilContext.class);
		mockEvenementCivilOptions = createMock(EvenementCivilOptions.class);
		mockEvtCivErrCol = createMock(EvenementCivilErreurCollector.class);
		mockEvtCivWarnCol = createMock(EvenementCivilWarningCollector.class);

		//Scenario pour les mocks lors de l'instanciation d l'evenement composite
		expect(mockEvenementCivilEch.getDateEvenement()).andReturn(RegDate.get(2011, 1, 1));
		expect(mockEvenementCivilEch.getId()).andReturn(1L);
		expect(mockEvenementCivilEch.getNumeroIndividu()).andReturn(1234567L);
		expect(mockEvenementCivilContext.getTiersDAO()).andReturn(mockTiersDAO);
		expect(mockEvenementCivilOptions.isRefreshCache()).andReturn(false);

	}

	@Test
	public void testValidate() throws Exception {

		// Scénario:
		//
		// - On s'attend à ce que toutes les méthodes validateXXX() des événements composants soient appelées lors de
		//   l'appel de validate() sur le composite

		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			m.validateCommon(mockEvtCivErrCol);
			m.validateSpecific(mockEvtCivErrCol, mockEvtCivWarnCol);
		}

		// Fin de la définition du scénario, on passe les mocks en mode replay
		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			replay(m);
		}
    	replay(mockEvenementCivilEch, mockEvenementCivilContext, mockEvenementCivilOptions);

		// Création de l'instance à tester
		EvenementCivilInterne evtComposite = new EvenementCivilInterneComposite(
				mockEvenementCivilEch, mockEvenementCivilContext,mockEvenementCivilOptions,
				mockEvenementsComposants);

		// Appel de la méthode testée
    	evtComposite.validate(mockEvtCivErrCol, mockEvtCivWarnCol);

		// Vérification que le scénario c'est bien déroulé
		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			verify(m);
		}
		verify(mockEvenementCivilEch, mockEvenementCivilContext, mockEvenementCivilOptions);

	}

	@Test
	public void testHandle_TousTraites() throws Exception {

		// Scénario:
		//
		// - On s'attend à ce que toutes les méthodes handle() des événements composants soient appelées lors de
		//   l'appel de handle() sur le composite
		//
		// - Dans ce cas, tous les evenements sont traités, le handleStatus du composite doit aussi être traités

		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			expect(m.handle(mockEvtCivWarnCol)).andReturn(HandleStatus.TRAITE);
		}
		final HandleStatus hs = testHandle();
		assertEquals(HandleStatus.TRAITE, hs);
	}

	@Test
	public void testHandle_TraiteEtRedondant() throws Exception {

		// Scénario:
		//
		// - On s'attend à ce que toutes les méthodes handle() des événements composants soient appelées lors de
		//   l'appel de handle() sur le composite
		//
		// - Dans ce cas, tous un evenement est traité et les autres sont redondants
		//   le handleStatus du composite doit être traité

		expect(mockEvenementsComposants[0].handle(mockEvtCivWarnCol)).andReturn(HandleStatus.TRAITE);
		expect(mockEvenementsComposants[1].handle(mockEvtCivWarnCol)).andReturn(HandleStatus.REDONDANT);
		expect(mockEvenementsComposants[2].handle(mockEvtCivWarnCol)).andReturn(HandleStatus.REDONDANT);

		HandleStatus hs = testHandle();
		assertEquals(HandleStatus.TRAITE, hs);
	}

	@Test
	public void testHandle_TousRedondants() throws Exception {

		// Scénario:
		//
		// - On s'attend à ce que toutes les méthodes handle() des événements composants soient appelées lors de
		//   l'appel de handle() sur le composite
		//
		// - Dans ce cas, tous les evenements sont redondants, le handleStatus du composite doit aussi être redondant

		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			expect(m.handle(mockEvtCivWarnCol)).andReturn(HandleStatus.REDONDANT);
		}
		HandleStatus hs = testHandle();
		assertEquals(HandleStatus.REDONDANT, hs);
	}

	/*
	 * Factorise le code pour les differents tests de la method handle()
	 */
	private HandleStatus testHandle() throws EvenementCivilException {

		// Fin de la définition du scénario, on passe les mocks en mode replay
		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			replay(m);
		}
		replay(mockEvenementCivilEch, mockEvenementCivilContext, mockEvenementCivilOptions);

		// Création de l'instance à tester
		EvenementCivilInterne evtComposite = new EvenementCivilInterneComposite(
				mockEvenementCivilEch, mockEvenementCivilContext,mockEvenementCivilOptions,
				mockEvenementsComposants);

		// Appel de la méthode testée
		final HandleStatus hs = evtComposite.handle(mockEvtCivWarnCol);

		// Vérification que le scénario c'est bien déroulé
		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			verify(m);
		}
		verify(mockEvenementCivilEch, mockEvenementCivilContext, mockEvenementCivilOptions);

		// Retour du résultat du test
		return hs;
	}

	@Test
	public void testFillRequiredParts() throws EvenementCivilException {

		/*
		 * Classe Implementant l'appel à fillRequiredPart sur les mocks d'EvenementCivilInterne
		 */
		class AnswerToFillRequiredParts implements  IAnswer {

			private AttributeIndividu attr;

			AnswerToFillRequiredParts (AttributeIndividu attr) {
				this.attr = attr;
			}

			@Override
			@SuppressWarnings("unchecked")
			public Object answer() {
				Set<AttributeIndividu> arg1 = (Set<AttributeIndividu>) getCurrentArguments()[0];
				arg1.add(attr);
				return null;
			}
		}

		// Scénario:
		//
		// - Chaque événements possède une part differente le composite devrait en possède 3

		final Set<AttributeIndividu> attrs = new HashSet<AttributeIndividu>();
		mockEvenementsComposants[0].fillRequiredParts(attrs);
		expectLastCall().andAnswer(new AnswerToFillRequiredParts(AttributeIndividu.ADRESSES));
		mockEvenementsComposants[1].fillRequiredParts(attrs);
		expectLastCall().andAnswer(new AnswerToFillRequiredParts(AttributeIndividu.ADOPTIONS));
		mockEvenementsComposants[2].fillRequiredParts(attrs);
		expectLastCall().andAnswer(new AnswerToFillRequiredParts(AttributeIndividu.NATIONALITE));

		// Fin de la définition du scénario, on passe les mocks en mode replay
		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			replay(m);
		}
		replay(mockEvenementCivilEch, mockEvenementCivilContext, mockEvenementCivilOptions);

		// Création de l'instance à tester
		EvenementCivilInterne evtComposite = new EvenementCivilInterneComposite(
				mockEvenementCivilEch, mockEvenementCivilContext,mockEvenementCivilOptions,
				mockEvenementsComposants);

		// Appel de la méthode testée
		assertTrue(attrs.isEmpty());
		evtComposite.fillRequiredParts(attrs);
		assertEquals(3, attrs.size());

		// Vérification que le scénario c'est bien déroulé
		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			verify(m);
		}
		verify(mockEvenementCivilEch, mockEvenementCivilContext, mockEvenementCivilOptions);
	}

	@Test
	public void testIsContribuableObligatoirementConnuAvantTraitement_AucunObligatoire() throws EvenementCivilException {
		// Scénario:
		// - Aucun des evenements composants ne doit connaitre le contribuable avant le traitement,
		//   Dans ce cas, le composite non plus.

		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			expect(m.isContribuableObligatoirementConnuAvantTraitement()).andReturn(false);
		}
		boolean res = testIsContribuableObligatoirementConnuAvantTraitement();
		assertFalse(res);

	}

	@Test
	public void testIsContribuableObligatoirementConnuAvantTraitement_CertainObligatoire() throws EvenementCivilException {
		// Scénario:
		// - Certain evenements composant doivent connaitre le contribuable avant le traitement,
		//   Dans ce cas, le composite aussi.
		
		expect(mockEvenementsComposants[0].isContribuableObligatoirementConnuAvantTraitement()).andReturn(false).anyTimes();
		expect(mockEvenementsComposants[1].isContribuableObligatoirementConnuAvantTraitement()).andReturn(true).anyTimes();
		expect(mockEvenementsComposants[2].isContribuableObligatoirementConnuAvantTraitement()).andReturn(false).anyTimes();

		boolean res = testIsContribuableObligatoirementConnuAvantTraitement();
		assertTrue(res);
	}

	@Test
	public void testIsContribuableObligatoirementConnuAvantTraitement_TousObligatoire() throws EvenementCivilException {
		// Scénario:
		// - Tous les evenements composants doivent connaitre le contribuable avant le traitement,
		//   Dans ce cas, le composite aussi.

		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			expect(m.isContribuableObligatoirementConnuAvantTraitement()).andReturn(true).anyTimes();
		}
		boolean res = testIsContribuableObligatoirementConnuAvantTraitement();
		assertTrue(res);
	
	}

	private boolean testIsContribuableObligatoirementConnuAvantTraitement() throws EvenementCivilException {
		
		// Fin de la définition du scénario, on passe les mocks en mode replay
		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			replay(m);
		}
		replay(mockEvenementCivilEch, mockEvenementCivilContext, mockEvenementCivilOptions);

		// Création de l'instance à tester
		EvenementCivilInterne evtComposite = new EvenementCivilInterneComposite(
				mockEvenementCivilEch, mockEvenementCivilContext,mockEvenementCivilOptions,
				mockEvenementsComposants);

		// Appel de la méthode testée
		final boolean res = evtComposite.isContribuableObligatoirementConnuAvantTraitement();

		// Vérification que le scénario c'est bien déroulé
		for(EvenementCivilInterne m : mockEvenementsComposants ) {
			verify(m);
		}
		verify(mockEvenementCivilEch, mockEvenementCivilContext, mockEvenementCivilOptions);

		// Retour du résultat du test
		return res;
	}

}

