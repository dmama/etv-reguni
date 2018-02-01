package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.tiers.TypeTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IdentCtbDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = LoggerFactory.getLogger(IdentCtbDAOTest.class);

	private static final String DAO_NAME = "identCtbDAO";

	private static final String DB_UNIT_DATA_FILE = "IdentCtbDAOTest.xml";

	/**
	 * Le DAO.
	 */
	private IdentCtbDAO dao;

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(IdentCtbDAO.class, DAO_NAME);
	}

	/**
	 * Teste la methode find simple
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFind() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);

		IdentificationContribuableCriteria identificationContribuableCriteria = new IdentificationContribuableCriteria();
		Calendar cal = new GregorianCalendar();
		cal.set(2000, Calendar.JANUARY, 1);
		identificationContribuableCriteria.setDateMessageDebut(cal.getTime());
		cal.set(2020, Calendar.JANUARY, 1);
		identificationContribuableCriteria.setDateMessageFin(cal.getTime());
		identificationContribuableCriteria.setDateNaissance(RegDate.get(1973, 7, 11));
		identificationContribuableCriteria.setEmetteurId("Test");
		identificationContribuableCriteria.setEtatMessage(IdentificationContribuable.Etat.EXCEPTION);
		identificationContribuableCriteria.setNAVS13("1234567890123");
		identificationContribuableCriteria.setNom("Larousse");
		identificationContribuableCriteria.setPeriodeFiscale(2008);
		identificationContribuableCriteria.setPrenoms("Lora");
		identificationContribuableCriteria.setPrioriteEmetteur(Demande.PrioriteEmetteur.NON_PRIORITAIRE);
		identificationContribuableCriteria.setTypeMessage("ssk-3001-000101");

		final ParamPagination paramPagination = new ParamPagination(1, 100, null, true);
		final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false);
		assertNotNull(list);
		assertEquals(1, list.size());
	}

	/**
	 * Teste la methode find avec le type de demande
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindTypeDemande() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);

		final IdentificationContribuableCriteria identificationContribuableCriteria = new IdentificationContribuableCriteria();
		final Calendar cal = new GregorianCalendar();
		cal.set(2000, Calendar.JANUARY, 1);
		identificationContribuableCriteria.setDateMessageDebut(cal.getTime());
		cal.set(2020, Calendar.JANUARY, 1);
		identificationContribuableCriteria.setDateMessageFin(cal.getTime());
		identificationContribuableCriteria.setDateNaissance(RegDate.get(1973, 7, 11));
		identificationContribuableCriteria.setEmetteurId("Test");
		identificationContribuableCriteria.setEtatMessage(IdentificationContribuable.Etat.EXCEPTION);
		identificationContribuableCriteria.setNAVS13("1234567890123");
		identificationContribuableCriteria.setNom("Larousse");
		identificationContribuableCriteria.setPeriodeFiscale(2008);
		identificationContribuableCriteria.setPrenoms("Lora");
		identificationContribuableCriteria.setPrioriteEmetteur(Demande.PrioriteEmetteur.NON_PRIORITAIRE);

		final ParamPagination paramPagination = new ParamPagination(1, 100, null, true);

		// tous les types...
		{
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false);
			assertNotNull(list);
			assertEquals(3, list.size());
		}
		// tous les types...
		{
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false, (TypeDemande[]) null);
			assertNotNull(list);
			assertEquals(3, list.size());
		}
		// tous les types...
		{
			@SuppressWarnings("RedundantArrayCreation")
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false, new TypeDemande[] {});
			assertNotNull(list);
			assertEquals(3, list.size());
		}
		// aucun type
		{
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false, new TypeDemande[] { null });
			assertNotNull(list);
			assertEquals(0, list.size());
		}
		// seulement un à chaque fois
		{
			for (TypeDemande type : EnumSet.of(TypeDemande.IMPOT_SOURCE, TypeDemande.MELDEWESEN, TypeDemande.NCS)) {
				final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false, type);
				assertNotNull(type.toString(), list);
				assertEquals(type.toString(), 1, list.size());
			}
		}
		// deux ensemble
		{
			final List<IdentificationContribuable> list = dao.find(identificationContribuableCriteria, paramPagination, false, false, false, TypeDemande.IMPOT_SOURCE, TypeDemande.NCS);
			assertNotNull(list);
			assertEquals(2, list.size());
		}
	}

	private static IdentificationContribuable buildDummyIdentificationContribuable(@Nullable Map<String, String> metadata) {
		final CriteresPersonne personne = new CriteresPersonne();
		personne.setNom("Talon");
		personne.setPrenoms("Achile");
		personne.setDateNaissance(date(1963, 2, 12));

		final Demande demande = new Demande();
		demande.setDate(DateHelper.getCurrentDate());
		demande.setEmetteurId("EmettEUR");
		demande.setMessageId("Mon message à moi");
		demande.setModeIdentification(Demande.ModeIdentificationType.MANUEL_AVEC_ACK);
		demande.setPeriodeFiscale(2012);
		demande.setPersonne(personne);
		demande.setPrioriteEmetteur(Demande.PrioriteEmetteur.NON_PRIORITAIRE);
		demande.setPrioriteUtilisateur(0);
		demande.setTransmetteur("TransMEtteUR");
		demande.setTypeDemande(TypeDemande.NCS);
		demande.setTypeMessage("CS_EMPLOYEUR");
		demande.setTypeContribuableRecherche(TypeTiers.PERSONNE_PHYSIQUE);

		final EsbHeader header = new EsbHeader();
		header.setBusinessId("Mon businessId");
		header.setBusinessUser("MOI");
		header.setReplyTo("Par là-bas...");
		header.setMetadata(metadata);

		final IdentificationContribuable identification = new IdentificationContribuable();
		identification.setEtat(IdentificationContribuable.Etat.A_EXPERTISER);
		identification.setDemande(demande);
		identification.setHeader(header);

		return identification;
	}

	@Test
	public void testMetaDataPersistenceNull() throws Exception {
		final long id = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				return hibernateTemplate.merge(buildDummyIdentificationContribuable(null)).getId();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final IdentificationContribuable ident = dao.get(id);
				assertNotNull(ident);
				assertNull(ident.getHeader().getMetadata());
				return null;
			}
		});
	}

	@Test
	public void testMetaDataPersistenceEmpty() throws Exception {
		final long id = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				return hibernateTemplate.merge(buildDummyIdentificationContribuable(new HashMap<String, String>())).getId();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final IdentificationContribuable ident = dao.get(id);
				assertNotNull(ident);
				assertNotNull(ident.getHeader().getMetadata());
				assertEquals(0, ident.getHeader().getMetadata().size());
				return null;
			}
		});
	}

	@Test
	public void testMetaDataPersistenceOneElement() throws Exception {
		final long id = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Map<String, String> metadata = new HashMap<>();
				metadata.put("MyKey", "MyValue");
				return hibernateTemplate.merge(buildDummyIdentificationContribuable(metadata)).getId();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final IdentificationContribuable ident = dao.get(id);
				assertNotNull(ident);

				final Map<String, String> metadata = ident.getHeader().getMetadata();
				assertNotNull(metadata);
				assertEquals(1, metadata.size());

				final Map.Entry<String, String> entry = metadata.entrySet().iterator().next();
				assertNotNull(entry);
				assertEquals("MyKey", entry.getKey());
				assertEquals("MyValue", entry.getValue());

				return null;
			}
		});
	}

	@Test
	public void testMetaDataPersistenceSeveralElements() throws Exception {
		final long id = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Map<String, String> metadata = new HashMap<>();
				metadata.put("MyKey", "{}#,,");
				metadata.put("YourKey{,", "YourValue");
				metadata.put("HerKey\"{,", "HerValue\"{{");
				metadata.put("HisKey", null);
				return hibernateTemplate.merge(buildDummyIdentificationContribuable(metadata)).getId();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final IdentificationContribuable ident = dao.get(id);
				assertNotNull(ident);

				final Map<String, String> metadata = ident.getHeader().getMetadata();
				assertNotNull(metadata);
				assertEquals(4, metadata.size());

				assertEquals("{}#,,", metadata.get("MyKey"));
				assertEquals("YourValue", metadata.get("YourKey{,"));
				assertEquals("HerValue\"{{", metadata.get("HerKey\"{,"));
				assertTrue(metadata.containsKey("HisKey"));
				assertNull(metadata.get("HisKey"));
				return null;
			}
		});
	}
}
