package ch.vd.uniregctb.evenement.reqdes;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.xml.event.reqdes.v1.CreationModification;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.reqdes.EvenementReqDes;
import ch.vd.uniregctb.reqdes.EvenementReqDesDAO;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.RolePartiePrenante;
import ch.vd.uniregctb.reqdes.TypeRole;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.reqdes.UniteTraitementDAO;
import ch.vd.uniregctb.type.CategorieEtranger;

public class ReqDesEventHandlerTest extends BusinessTest {

	private static final long noCtb = 15948723L;

	private ReqDesEventHandler handler;
	private UniteTraitementDAO utDao;
	private Set<Long> idsUnitesTraitement;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		utDao = getBean(UniteTraitementDAO.class, "reqdesUniteTraitementDAO");

		idsUnitesTraitement = Collections.synchronizedSet(new HashSet<Long>());
		handler = new ReqDesEventHandler() {
			@Override
			protected void lancementTraitementAsynchrone(Set<Long> idsUnitesTraitement) {
				// on ne lance pas le traitement habituel, mais on ne fait que collecter les données
				ReqDesEventHandlerTest.this.idsUnitesTraitement.addAll(idsUnitesTraitement);
			}
		};
		handler.setInfraService(serviceInfra);
		handler.setTransactionManager(transactionManager);
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setEvenementDAO(getBean(EvenementReqDesDAO.class, "reqdesEvenementDAO"));
	}

	private CreationModification buildCreationModificationFromPath(String path) throws Exception {
		try (InputStream in = ReqDesEventHandlerTest.class.getResourceAsStream(path)) {
			final Source src = new StreamSource(in);
			return handler.parse(src);
		}
	}

	/**
	 * Test très basique qui montre juste que le parsing a été accepté par JAXB
	 */
	@Test
	public void testParsing() throws Exception {
		final CreationModification cm = buildCreationModificationFromPath("TestEvent.xml");
		Assert.assertNotNull(cm);
	}

	/**
	 * Là, on va un peu plus loin en extrayant les parties prenantes et en constituant des groupes
	 */
	@Test
	public void testExtractionPartiesPrenantes() throws Exception {
		final CreationModification cm = buildCreationModificationFromPath("TestEvent.xml");
		Assert.assertNotNull(cm);

		final Map<Integer, ReqDesPartiePrenante> partiesPrenantes = ReqDesEventHandler.extractPartiesPrenantes(cm.getStakeholder(), serviceInfra);
		Assert.assertNotNull(partiesPrenantes);
		Assert.assertEquals(3, partiesPrenantes.size());

		final List<Set<Integer>> groupes = ReqDesEventHandler.composeGroupes(partiesPrenantes);
		Assert.assertNotNull(groupes);
		Assert.assertEquals(2, groupes.size());

		// on trie les groupes par taille croissante (1 et 1 et 1 de 2)
		final List<Set<Integer>> sortedGroupes = new ArrayList<>(groupes);
		Collections.sort(sortedGroupes, new Comparator<Set<Integer>>() {
			@Override
			public int compare(Set<Integer> o1, Set<Integer> o2) {
				return o1.size() - o2.size();
			}
		});

		{
			final Set<Integer> set = sortedGroupes.get(0);
			Assert.assertNotNull(set);
			Assert.assertEquals(1, set.size());
			Assert.assertEquals((Integer) 1, set.iterator().next());
		}
		{
			final Set<Integer> set = sortedGroupes.get(1);
			Assert.assertNotNull(set);
			Assert.assertEquals(2, set.size());

			final List<Integer> sorted = new ArrayList<>(set);
			Collections.sort(sorted);
			Assert.assertEquals((Integer) 2, sorted.get(0));
			Assert.assertEquals((Integer) 3, sorted.get(1));
		}
	}

	@Test
	public void testExtractionRoles() throws Exception {
		final CreationModification cm = buildCreationModificationFromPath("TestEvent.xml");
		Assert.assertNotNull(cm);

		final Map<Integer, Set<Pair<RoleDansActe, Integer>>> map = ReqDesEventHandler.extractRoles(cm.getSubject());
		Assert.assertNotNull(map);
		Assert.assertEquals(3, map.size());

		// Albus
		{
			final Set<Pair<RoleDansActe, Integer>> roles = map.get(1);
			Assert.assertNotNull(roles);
			Assert.assertEquals(1, roles.size());

			final Pair<RoleDansActe, Integer> role = roles.iterator().next();
			Assert.assertEquals(RoleDansActe.ALIENATEUR, role.getLeft());
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), role.getRight());
		}

		// Harry
		{
			final Set<Pair<RoleDansActe, Integer>> roles = map.get(2);
			Assert.assertNotNull(roles);
			Assert.assertEquals(2, roles.size());

			final List<Pair<RoleDansActe, Integer>> sortedRoles = new ArrayList<>(roles);
			Collections.sort(sortedRoles, new Comparator<Pair<RoleDansActe, Integer>>() {
				@Override
				public int compare(Pair<RoleDansActe, Integer> o1, Pair<RoleDansActe, Integer> o2) {
					return o1.getRight() - o2.getRight();
				}
			});

			{
				final Pair<RoleDansActe, Integer> role = sortedRoles.get(0);
				Assert.assertEquals(RoleDansActe.AUTRE, role.getLeft());
				Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), role.getRight());
			}
			{
				final Pair<RoleDansActe, Integer> role = sortedRoles.get(1);
				Assert.assertEquals(RoleDansActe.ACQUEREUR, role.getLeft());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), role.getRight());
			}
		}

		// Ginny
		{
			final Set<Pair<RoleDansActe, Integer>> roles = map.get(3);
			Assert.assertNotNull(roles);
			Assert.assertEquals(1, roles.size());

			final Pair<RoleDansActe, Integer> role = roles.iterator().next();
			Assert.assertEquals(RoleDansActe.AUTRE, role.getLeft());
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), role.getRight());
		}
	}

	@Test
	public void testEnregistrement() throws Exception {

		final String path = "TestEvent.xml";

		// arrivée d'un événement
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				try (InputStream in = ReqDesEventHandlerTest.class.getResourceAsStream(path)) {
					final Source src = new StreamSource(in);
					handler.onMessage(src, "<empty/>");
				}
			}
		});

		// vérification que l'on a bien deux identifiants pour les unités de traitement
		Assert.assertEquals(2, idsUnitesTraitement.size());

		// vérification du contenu de la base de données
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// vérification qu'on n'a pas créé d'autres unités de traitement
				Assert.assertEquals(idsUnitesTraitement.size(), utDao.getAll().size());

				// récupération de toutes les unités de traitement
				final List<UniteTraitement> uts = new ArrayList<>(idsUnitesTraitement.size());
				for (Long id : idsUnitesTraitement) {
					final UniteTraitement ut = utDao.get(id);
					Assert.assertNotNull("identifiant " + id, ut);
					uts.add(ut);
				}

				// tri dans l'ordre du nombre de parties prenantes, (1 en a 1, l'autre en a 2)
				Collections.sort(uts, new Comparator<UniteTraitement>() {
					@Override
					public int compare(UniteTraitement o1, UniteTraitement o2) {
						return o1.getPartiesPrenantes().size() - o2.getPartiesPrenantes().size();
					}
				});

				// pour vérifier que le lien vers l'événement est bien le même à chaque fois
				final EvenementReqDes evt;

				{
					final UniteTraitement ut = uts.get(0);
					Assert.assertNotNull(ut);
					Assert.assertEquals(EtatTraitement.A_TRAITER, ut.getEtat());
					Assert.assertNull(ut.getDateTraitement());
					Assert.assertEquals(0, ut.getErreurs().size());
					Assert.assertEquals(1, ut.getPartiesPrenantes().size());

					evt = ut.getEvenement();
					Assert.assertNotNull(evt);

					final PartiePrenante pp = ut.getPartiesPrenantes().iterator().next();
					Assert.assertNotNull(pp);
					Assert.assertEquals("Dumbledore", pp.getNom());
					Assert.assertNull(pp.getConjointPartiePrenante());
					Assert.assertEquals(date(2012, 5, 24), pp.getDateDeces());
					Assert.assertNull(pp.getNumeroContribuable());
					Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), pp.getNoOfsPaysNationalite());
					Assert.assertEquals(CategorieEtranger._03_ETABLI_C, pp.getCategorieEtranger());
					Assert.assertEquals(1, pp.getRoles().size());

					final RolePartiePrenante rpp = pp.getRoles().iterator().next();
					Assert.assertNotNull(rpp);
					Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), rpp.getNoOfsCommune());
					Assert.assertEquals(TypeRole.ALIENATEUR, rpp.getRole());
				}
				{
					final UniteTraitement ut = uts.get(1);
					Assert.assertNotNull(ut);
					Assert.assertEquals(EtatTraitement.A_TRAITER, ut.getEtat());
					Assert.assertNull(ut.getDateTraitement());
					Assert.assertEquals(0, ut.getErreurs().size());
					Assert.assertEquals(2, ut.getPartiesPrenantes().size());

					final EvenementReqDes evtAutre = ut.getEvenement();
					Assert.assertNotNull(evtAutre);
					Assert.assertSame(evt, evtAutre);       // même session hibernate + même entité en base -> même objet en mémoire...

					// tri par la date de naissance -> Harry puis Ginny
					final List<PartiePrenante> sortedPPs = new ArrayList<>(ut.getPartiesPrenantes());
					Collections.sort(sortedPPs, new Comparator<PartiePrenante>() {
						@Override
						public int compare(PartiePrenante o1, PartiePrenante o2) {
							return o1.getDateNaissance().compareTo(o2.getDateNaissance());
						}
					});

					{
						final PartiePrenante pp = sortedPPs.get(0);
						Assert.assertNotNull(pp);
						Assert.assertEquals("Potter", pp.getNom());
						Assert.assertSame(sortedPPs.get(1), pp.getConjointPartiePrenante());    // même session hibernate + même entité en base -> même objet en mémoire...
						Assert.assertNull(pp.getDateDeces());
						Assert.assertNull(pp.getNumeroContribuable());
						Assert.assertEquals((Integer) MockPays.Apatridie.getNoOFS(), pp.getNoOfsPaysNationalite());
						Assert.assertEquals(CategorieEtranger._03_ETABLI_C, pp.getCategorieEtranger());
						Assert.assertEquals(2, pp.getRoles().size());

						final List<RolePartiePrenante> sortedRoles = new ArrayList<>(pp.getRoles());
						Collections.sort(sortedRoles, new Comparator<RolePartiePrenante>() {
							@Override
							public int compare(RolePartiePrenante o1, RolePartiePrenante o2) {
								return o1.getNoOfsCommune() - o2.getNoOfsCommune();
							}
						});

						{
							final RolePartiePrenante rpp = sortedRoles.get(0);
							Assert.assertNotNull(rpp);
							Assert.assertEquals(MockCommune.Cossonay.getNoOFS(), rpp.getNoOfsCommune());
							Assert.assertEquals(TypeRole.AUTRE, rpp.getRole());
						}
						{
							final RolePartiePrenante rpp = sortedRoles.get(1);
							Assert.assertNotNull(rpp);
							Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), rpp.getNoOfsCommune());
							Assert.assertEquals(TypeRole.ACQUEREUR, rpp.getRole());
						}
					}
					{
						final PartiePrenante pp = sortedPPs.get(1);
						Assert.assertNotNull(pp);
						Assert.assertEquals("Weasley", pp.getNom());
						Assert.assertSame(sortedPPs.get(0), pp.getConjointPartiePrenante());    // même session hibernate + même entité en base -> même objet en mémoire...
						Assert.assertNull(pp.getDateDeces());
						Assert.assertEquals((Long) noCtb, pp.getNumeroContribuable());
						Assert.assertEquals((Integer) MockPays.Suisse.getNoOFS(), pp.getNoOfsPaysNationalite());
						Assert.assertNull(pp.getCategorieEtranger());
						Assert.assertEquals(1, pp.getRoles().size());

						final RolePartiePrenante rpp = pp.getRoles().iterator().next();
						Assert.assertNotNull(rpp);
						Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), rpp.getNoOfsCommune());
						Assert.assertEquals(TypeRole.AUTRE, rpp.getRole());
					}
				}

				// vérification du contenu de l'événement lui-même
				Assert.assertEquals(date(2008, 9, 29), evt.getDateActe());
				Assert.assertEquals((Long) 124846154L, evt.getNumeroMinute());
				Assert.assertEquals("<empty/>", evt.getXml());
				Assert.assertNotNull(evt.getNotaire());
				Assert.assertEquals("moinotaire", evt.getNotaire().getVisa());
				Assert.assertEquals("Yesparler", evt.getNotaire().getNom());
				Assert.assertEquals("Clothaire", evt.getNotaire().getPrenom());
				Assert.assertNotNull(evt.getOperateur());
				Assert.assertEquals("secret", evt.getOperateur().getVisa());
				Assert.assertEquals("Cecrétère", evt.getOperateur().getNom());
				Assert.assertEquals("Alicia", evt.getOperateur().getPrenom());
			}
		});
	}
}
