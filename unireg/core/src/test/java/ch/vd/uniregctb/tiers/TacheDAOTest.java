package ch.vd.uniregctb.tiers;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.hibernate.interceptor.ModificationLogInterceptor;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TacheDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(TacheDAOTest.class);

	private static final String DAO_NAME = "tacheDAO";

	private TacheDAO dao;
	private HibernateTemplate hibernateTemplate;
	private ModificationLogInterceptor modificationLogInterceptor;

	private static class Ids {
		Long tedi0;
		Long tadi0;
		Long tcd0;
		Long ttd0;
		Long tedi1;
	}
	private final Ids ids = new Ids();

	/**
	 * @throws Exception
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(TacheDAO.class, DAO_NAME);
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		modificationLogInterceptor = getBean(ModificationLogInterceptor.class, "modificationLogInterceptor");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindParTypeEtatTache() throws Exception {

		loadDatabase();

		// Tâches en instance
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
			assertEquals(ids.tedi1, list.get(1).getId());
		}

		// Tâches traitées
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setEtatTache(TypeEtatTache.TRAITE);
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(ids.tadi0, list.get(0).getId());
			assertEquals(ids.tcd0, list.get(1).getId());
		}

		// Tâches en cours
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setEtatTache(TypeEtatTache.EN_COURS);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(ids.ttd0, list.get(0).getId());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindParTypeTache() throws Exception {

		loadDatabase();

		// Type envoi di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
			assertEquals(ids.tedi1, list.get(1).getId());
		}

		// Type annulation di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(ids.tadi0, list.get(0).getId());
		}

		// Type contrôle dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheControleDossier);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(ids.tcd0, list.get(0).getId());
		}

		// Type transmission dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheTransmissionDossier);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(ids.ttd0, list.get(0).getId());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindParTypeTacheInverse() throws Exception {

		loadDatabase();

		// Type envoi di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(3, list.size());
			assertEquals(ids.tadi0, list.get(0).getId());
			assertEquals(ids.tcd0, list.get(1).getId());
			assertEquals(ids.ttd0, list.get(2).getId());
		}

		// Type annulation di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
			assertEquals(ids.tcd0, list.get(1).getId());
			assertEquals(ids.ttd0, list.get(2).getId());
			assertEquals(ids.tedi1, list.get(3).getId());
		}

		// Type contrôle dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheControleDossier);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
			assertEquals(ids.tadi0, list.get(1).getId());
			assertEquals(ids.ttd0, list.get(2).getId());
			assertEquals(ids.tedi1, list.get(3).getId());
		}

		// Type transmission dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheTransmissionDossier);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
			assertEquals(ids.tadi0, list.get(1).getId());
			assertEquals(ids.tcd0, list.get(2).getId());
			assertEquals(ids.tedi1, list.get(3).getId());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindParDateCreation() throws Exception {

		loadDatabase();

		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2007, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2007, 1, 31));
			final List<Tache> list = dao.find(criterion);
			assertEmpty(list);
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 1, 31));
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 2, 29));
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
			assertEquals(ids.tadi0, list.get(1).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 3, 31));
			final List<Tache> list = dao.find(criterion);
			assertEquals(3, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
			assertEquals(ids.tadi0, list.get(1).getId());
			assertEquals(ids.tcd0, list.get(2).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 4, 30));
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
			assertEquals(ids.tadi0, list.get(1).getId());
			assertEquals(ids.tcd0, list.get(2).getId());
			assertEquals(ids.ttd0, list.get(3).getId());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindParAnnee() throws Exception {

		loadDatabase();

		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(2007);
			final List<Tache> list = dao.find(criterion);
			assertEmpty(list);
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(2008);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(2009);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(ids.tedi1, list.get(0).getId());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindParContribuable() throws Exception {

		loadDatabase();

		final Contribuable gomez = (Contribuable) dao.getHibernateTemplate().get(Contribuable.class, 12600003L);
		assertNotNull(gomez);

		final Contribuable pelcrus = (Contribuable) dao.getHibernateTemplate().get(Contribuable.class, 12600456L);
		assertNotNull(pelcrus);

		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setContribuable(pelcrus);
			criterion.setAnnee(2008);
			final List<Tache> list = dao.find(criterion);
			assertEmpty(list);
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setContribuable(gomez);
			criterion.setAnnee(2008);
			final List<Tache> list = dao.find(criterion);
			assertEquals(5, list.size());
			assertEquals(ids.tedi0, list.get(0).getId());
			assertEquals(ids.tadi0, list.get(1).getId());
			assertEquals(ids.tcd0, list.get(2).getId());
			assertEquals(ids.ttd0, list.get(3).getId());
			assertEquals(ids.tedi1, list.get(4).getId());
		}
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindAvecPagination() throws Exception {
		loadDatabase();
		ParamPagination paramPagination = new ParamPagination(1, 1, null, false);
		TacheCriteria tacheCriteria = new TacheCriteria();
		tacheCriteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
		List<Tache> taches = dao.find(tacheCriteria, paramPagination);
		assertEquals(1, taches.size());

		paramPagination = new ParamPagination(2, 1, null, false);
		tacheCriteria = new TacheCriteria();
		taches = dao.find(tacheCriteria, paramPagination);
		assertEquals(1, taches.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testUpdateCollAdmAssignee() throws Exception {

		class Ids {
			long ctb;
			long ca1;
			long ca2;
			long ca3;

			long envoiEnInstance;
			long annulationEnInstance;
			long controleEnInstance;
			long nouveauEnInstance;
			long transmissionEnInstance;

			long envoiTraitee;
			long annulationTraitee;
			long controleTraitee;
			long nouveauTraitee;
			long transmissionTraitee;

			long envoiAnnulee;
			long annulationAnnulee;
			long controleAnnulee;
			long nouveauAnnulee;
			long transmissionAnnulee;
		}
		final Ids ids = new Ids();

		// Crée un contribuable avec plusieurs tâches dans différents états
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique ctb = addNonHabitant("Alfred", "Duah", null, null);
				ids.ctb = ctb.getNumero();

				PeriodeFiscale periode = addPeriodeFiscale(2006);
				ModeleDocument modele = addModele(periode);
				DeclarationImpotOrdinaire di = addDeclaration(date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, Qualification.AUTOMATIQUE, periode, modele, ctb);

				CollectiviteAdministrative ca = addCollAdm(3232);
				ids.ca1 = ca.getNumero();
				ids.ca2 = addCollAdm(7777).getNumero();
				ids.ca3 = addCollAdm(22).getNumero();

				ids.envoiEnInstance = addTacheEnvoi(ctb, TypeEtatTache.EN_INSTANCE, ca).getId();
				ids.annulationEnInstance = addTacheAnnulation(ctb, TypeEtatTache.EN_INSTANCE, ca, di).getId();
				ids.controleEnInstance = addTacheControle(ctb, TypeEtatTache.EN_INSTANCE, ca).getId();
				ids.nouveauEnInstance = addTacheNouveau(ctb, TypeEtatTache.EN_INSTANCE, ca).getId();
				ids.transmissionEnInstance = addTacheTransmission(ctb, TypeEtatTache.EN_INSTANCE, ca).getId();

				ids.envoiTraitee = addTacheEnvoi(ctb, TypeEtatTache.TRAITE, ca).getId();
				ids.annulationTraitee = addTacheAnnulation(ctb, TypeEtatTache.TRAITE, ca, di).getId();
				ids.controleTraitee = addTacheControle(ctb, TypeEtatTache.TRAITE, ca).getId();
				ids.nouveauTraitee = addTacheNouveau(ctb, TypeEtatTache.TRAITE, ca).getId();
				ids.transmissionTraitee = addTacheTransmission(ctb, TypeEtatTache.TRAITE, ca).getId();

				final TacheEnvoiDeclarationImpot envoi = addTacheEnvoi(ctb, TypeEtatTache.EN_INSTANCE, ca);
				envoi.setAnnule(true);
				ids.envoiAnnulee = envoi.getId();

				final TacheAnnulationDeclarationImpot annulation = addTacheAnnulation(ctb, TypeEtatTache.EN_INSTANCE, ca, di);
				annulation.setAnnule(true);
				ids.annulationAnnulee = annulation.getId();

				final TacheControleDossier controle = addTacheControle(ctb, TypeEtatTache.EN_INSTANCE, ca);
				controle.setAnnule(true);
				ids.controleAnnulee = controle.getId();

				final TacheNouveauDossier nouveau = addTacheNouveau(ctb, TypeEtatTache.EN_INSTANCE, ca);
				nouveau.setAnnule(true);
				ids.nouveauAnnulee = nouveau.getId();

				final TacheTransmissionDossier transmission = addTacheTransmission(ctb, TypeEtatTache.EN_INSTANCE, ca);
				transmission.setAnnule(true);
				ids.transmissionAnnulee = transmission.getId();

				return null;
			}
		});

		// Etat avant changement
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative ca1 = (CollectiviteAdministrative) hibernateTemplate.get(CollectiviteAdministrative.class, ids.ca1);
				assertNotNull(ca1);
				final CollectiviteAdministrative ca2 = (CollectiviteAdministrative) hibernateTemplate.get(CollectiviteAdministrative.class, ids.ca2);
				assertNotNull(ca2);

				assertCollAdm(ca1, ids.envoiEnInstance);
				assertCollAdm(ca1, ids.annulationEnInstance);
				assertCollAdm(ca1, ids.controleEnInstance);
				assertCollAdm(ca1, ids.nouveauEnInstance);
				assertCollAdm(ca1, ids.transmissionEnInstance);

				assertCollAdm(ca1, ids.envoiTraitee);
				assertCollAdm(ca1, ids.annulationTraitee);
				assertCollAdm(ca1, ids.controleTraitee);
				assertCollAdm(ca1, ids.nouveauTraitee);
				assertCollAdm(ca1, ids.transmissionTraitee);

				assertCollAdm(ca1, ids.envoiAnnulee);
				assertCollAdm(ca1, ids.annulationAnnulee);
				assertCollAdm(ca1, ids.controleAnnulee);
				assertCollAdm(ca1, ids.nouveauAnnulee);
				assertCollAdm(ca1, ids.transmissionAnnulee);

				// Changement de collectivité administative assignée
				final HashMap<Long, Integer> tiersOidsMapping = new HashMap<Long, Integer>();
				tiersOidsMapping.put(ids.ctb, ca2.getNumeroCollectiviteAdministrative());
				dao.updateCollAdmAssignee(tiersOidsMapping);
				return null;
			}
		});

		// Etat après changement
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative ca1 = (CollectiviteAdministrative) hibernateTemplate.get(CollectiviteAdministrative.class, ids.ca1);
				assertNotNull(ca1);
				final CollectiviteAdministrative ca2 = (CollectiviteAdministrative) hibernateTemplate.get(CollectiviteAdministrative.class, ids.ca2);
				assertNotNull(ca2);
				
				assertCollAdm(ca2, ids.envoiEnInstance);
				assertCollAdm(ca2, ids.annulationEnInstance);
				assertCollAdm(ca1, ids.controleEnInstance); // [UNIREG-1024] les tâches de contrôle de dossier ne doivent pas être impactée
				assertCollAdm(ca2, ids.nouveauEnInstance);
				assertCollAdm(ca2, ids.transmissionEnInstance);

				assertCollAdm(ca1, ids.envoiTraitee); // les tâches déjà traitées ne doivent pas être impactées
				assertCollAdm(ca1, ids.annulationTraitee);
				assertCollAdm(ca1, ids.controleTraitee);
				assertCollAdm(ca1, ids.nouveauTraitee);
				assertCollAdm(ca1, ids.transmissionTraitee);

				assertCollAdm(ca1, ids.envoiAnnulee); // les tâches annulées ne doivent pas être impactées
				assertCollAdm(ca1, ids.annulationAnnulee);
				assertCollAdm(ca1, ids.controleAnnulee);
				assertCollAdm(ca1, ids.nouveauAnnulee);
				assertCollAdm(ca1, ids.transmissionAnnulee);
				return null;
			}
		});
	}

	private ModeleDocument addModele(PeriodeFiscale periode) {
		ModeleDocument modele = new ModeleDocument();
		modele.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		modele.setPeriodeFiscale(periode);
		modele = (ModeleDocument) hibernateTemplate.merge(modele);
		return modele;
	}

	private DeclarationImpotOrdinaire addDeclaration(RegDate debut, RegDate fin, TypeContribuable typeCtb, Qualification qualif, PeriodeFiscale periode, ModeleDocument modele, Tiers tiers) {
		DeclarationImpotOrdinaire di = addDeclarationImpot((Contribuable)tiers, periode, debut, fin, null, typeCtb, modele);
		di.setQualification(qualif);
		tiers.addDeclaration(di);
		di = (DeclarationImpotOrdinaire) hibernateTemplate.merge(di);
		return di;
	}

	private void assertCollAdm(CollectiviteAdministrative ca, long tacheId) {
		final Tache tache = (Tache) hibernateTemplate.get(Tache.class, tacheId);
		assertNotNull(tache);
		assertEquals(ca, tache.getCollectiviteAdministrativeAssignee());
	}

	private TacheAnnulationDeclarationImpot addTacheAnnulation(PersonnePhysique ctb, TypeEtatTache etat, CollectiviteAdministrative ca, DeclarationImpotOrdinaire di) {
		return addTacheAnnulDI(etat, date(2010, 1, 1), di, ctb, ca);
	}

	private TacheEnvoiDeclarationImpot addTacheEnvoi(PersonnePhysique ctb, TypeEtatTache etat, CollectiviteAdministrative ca) {
		TacheEnvoiDeclarationImpot envoi = addTacheEnvoiDI(etat, date(2010, 1, 1), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ctb, Qualification.AUTOMATIQUE, ca);
		envoi.setAdresseRetour(TypeAdresseRetour.CEDI);
		envoi = (TacheEnvoiDeclarationImpot) hibernateTemplate.merge(envoi);
		return envoi;
	}

	@SuppressWarnings({"unchecked", "UnusedAssignment"})
	private void loadDatabase() throws Exception {

		PeriodeFiscale pf0 = new PeriodeFiscale();
		pf0.setId(1L);
		pf0.setAnnee(2008);
		pf0.setLogModifDate(new Timestamp(1199142000000L));
		pf0.setParametrePeriodeFiscale(new HashSet());
		pf0.setModelesDocument(new HashSet());
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscale ppf0 = new ParametrePeriodeFiscale();
		ppf0.setId(1L);
		ppf0.setDateFinEnvoiMasseDI(RegDate.get(2009, 4, 30));
		ppf0.setLogModifDate(new Timestamp(1199142000000L));
		ppf0.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf0.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf0.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf0.addParametrePeriodeFiscale(ppf0);
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscale ppf1 = new ParametrePeriodeFiscale();
		ppf1.setId(2L);
		ppf1.setDateFinEnvoiMasseDI(RegDate.get(2009, 6, 30));
		ppf1.setLogModifDate(new Timestamp(1199142000000L));
		ppf1.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf1.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf1.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf0.addParametrePeriodeFiscale(ppf1);
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscale ppf2 = new ParametrePeriodeFiscale();
		ppf2.setId(3L);
		ppf2.setDateFinEnvoiMasseDI(RegDate.get(2009, 6, 30));
		ppf2.setLogModifDate(new Timestamp(1199142000000L));
		ppf2.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf2.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf2.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf0.addParametrePeriodeFiscale(ppf2);
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscale ppf3 = new ParametrePeriodeFiscale();
		ppf3.setId(4L);
		ppf3.setDateFinEnvoiMasseDI(RegDate.get(2009, 6, 30));
		ppf3.setLogModifDate(new Timestamp(1199142000000L));
		ppf3.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf3.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf3.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf0.addParametrePeriodeFiscale(ppf3);
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		ModeleDocument md0 = new ModeleDocument();
		md0.setId(1L);
		md0.setLogModifDate(new Timestamp(1199142000000L));
		md0.setModelesFeuilleDocument(new HashSet());
		md0.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		pf0.addModeleDocument(md0);
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		PersonnePhysique pp0 = new PersonnePhysique();
		pp0.setNumero(12600003L);
		pp0.setBlocageRemboursementAutomatique(false);
		pp0.setMouvementsDossier(new HashSet());
		pp0.setSituationsFamille(new HashSet());
		pp0.setDebiteurInactif(false);
		pp0.setLogModifDate(new Timestamp(1199142000000L));
		pp0.setDateNaissance(RegDate.get(1953, 12, 18));
		pp0.setNom("Gomez");
		pp0.setNumeroOfsNationalite(8231);
		pp0.setPrenom("Mario");
		pp0.setSexe(Sexe.MASCULIN);
		pp0.setIdentificationsPersonnes(new HashSet());
		pp0.setHabitant(false);
		pp0.setAdressesTiers(new HashSet());
		pp0.setDeclarations(new HashSet());
		pp0.setDroitsAccesAppliques(new HashSet());
		pp0.setForsFiscaux(new HashSet());
		pp0.setRapportsObjet(new HashSet());
		pp0.setRapportsSujet(new HashSet());
		pp0 = (PersonnePhysique) hibernateTemplate.merge(pp0);

		PersonnePhysique pp1 = new PersonnePhysique();
		pp1.setNumero(12600456L);
		pp1.setBlocageRemboursementAutomatique(false);
		pp1.setMouvementsDossier(new HashSet());
		pp1.setSituationsFamille(new HashSet());
		pp1.setDebiteurInactif(false);
		pp1.setLogModifDate(new Timestamp(1199142000000L));
		pp1.setDateNaissance(RegDate.get(1989, 5, 29));
		pp1.setNom("Pelcrus");
		pp1.setNumeroOfsNationalite(8231);
		pp1.setPrenom("Jules");
		pp1.setSexe(Sexe.MASCULIN);
		pp1.setIdentificationsPersonnes(new HashSet());
		pp1.setHabitant(false);
		pp1.setAdressesTiers(new HashSet());
		pp1.setDeclarations(new HashSet());
		pp1.setDroitsAccesAppliques(new HashSet());
		pp1.setForsFiscaux(new HashSet());
		pp1.setRapportsObjet(new HashSet());
		pp1.setRapportsSujet(new HashSet());
		pp1 = (PersonnePhysique) hibernateTemplate.merge(pp1);

		DeclarationImpotOrdinaire dio0 = new DeclarationImpotOrdinaire();
		dio0.setId(1L);
		dio0.setDateDebut(RegDate.get(2008, 1, 1));
		dio0.setDateFin(RegDate.get(2008, 12, 31));
		dio0.setDelais(new HashSet());
		dio0.setEtats(new HashSet());
		dio0.setLibre(false);
		dio0.setLogModifDate(new Timestamp(1199142000000L));
		dio0.setModeleDocument(md0);
		dio0.setPeriode(pf0);
		dio0.setNumero(1);
		dio0.setTiers(pp0);
		dio0 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio0);

		try {
			modificationLogInterceptor.setCompleteOnly(true); // par garder les valeur de log creation date (voir test testFindParDateCreation)

			TacheEnvoiDeclarationImpot tedi0 = new TacheEnvoiDeclarationImpot();
			tedi0.setContribuable(pp0);
			tedi0.setDateEcheance(RegDate.get(2008, 10, 25));
			tedi0.setDateDebut(RegDate.get(2008, 1, 1));
			tedi0.setDateFin(RegDate.get(2008, 12, 31));
			tedi0.setEtat(TypeEtatTache.EN_INSTANCE);
			tedi0.setLogCreationDate(new Timestamp(1199142000000L));
			tedi0.setLogModifDate(new Timestamp(1199142000000L));
			tedi0.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
			tedi0.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
			tedi0 = (TacheEnvoiDeclarationImpot) hibernateTemplate.merge(tedi0);
			ids.tedi0 = tedi0.getId();

			TacheAnnulationDeclarationImpot tadi0 = new TacheAnnulationDeclarationImpot();
			tadi0.setContribuable(pp0);
			tadi0.setDateEcheance(RegDate.get(2008, 10, 25));
			tadi0.setDeclarationImpotOrdinaire(dio0);
			tadi0.setEtat(TypeEtatTache.TRAITE);
			tadi0.setLogCreationDate(new Timestamp(1201820400000L));
			tadi0.setLogModifDate(new Timestamp(1201820400000L));
			tadi0 = (TacheAnnulationDeclarationImpot) hibernateTemplate.merge(tadi0);
			ids.tadi0 = tadi0.getId();

			TacheControleDossier tcd0 = new TacheControleDossier();
			tcd0.setContribuable(pp0);
			tcd0.setDateEcheance(RegDate.get(2007, 10, 25));
			tcd0.setEtat(TypeEtatTache.TRAITE);
			tcd0.setLogCreationDate(new Timestamp(1204326000000L));
			tcd0.setLogModifDate(new Timestamp(1204326000000L));
			tcd0 = (TacheControleDossier) hibernateTemplate.merge(tcd0);
			ids.tcd0 = tcd0.getId();

			CollectiviteAdministrative aci = addCollAdm(21);

			TacheTransmissionDossier ttd0 = new TacheTransmissionDossier();
			ttd0.setContribuable(pp0);
			ttd0.setDateEcheance(RegDate.get(2007, 10, 25));
			ttd0.setEtat(TypeEtatTache.EN_COURS);
			ttd0.setLogCreationDate(new Timestamp(1207000800000L));
			ttd0.setLogModifDate(new Timestamp(1207000800000L));
			ttd0.setCollectiviteAdministrativeAssignee(aci);
			ttd0 = (TacheTransmissionDossier) hibernateTemplate.merge(ttd0);
			ids.ttd0 = ttd0.getId();

			TacheEnvoiDeclarationImpot tedi1 = new TacheEnvoiDeclarationImpot();
			tedi1.setContribuable(pp0);
			tedi1.setDateEcheance(RegDate.get(2009, 3, 31));
			tedi1.setDateDebut(RegDate.get(2009, 1, 1));
			tedi1.setDateFin(RegDate.get(2009, 2, 28));
			tedi1.setEtat(TypeEtatTache.EN_INSTANCE);
			tedi1.setLogCreationDate(new Timestamp(1230764400000L));
			tedi1.setLogModifDate(new Timestamp(1230764400000L));
			tedi1.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
			tedi1.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
			tedi1 = (TacheEnvoiDeclarationImpot) hibernateTemplate.merge(tedi1);
			ids.tedi1 = tedi1.getId();
		}
		finally {
			modificationLogInterceptor.setCompleteOnly(false);
		}

		hibernateTemplate.flush();
	}
}
