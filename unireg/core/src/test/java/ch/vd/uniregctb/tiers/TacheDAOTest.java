package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.type.*;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TacheDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(TacheDAOTest.class);

	private static final String DAO_NAME = "tacheDAO";

	private static final String DB_UNIT_DATA_FILE = "TacheDAOTest.xml";


	/**
	 * Le DAO.
	 */
	private TacheDAO dao;
	private HibernateTemplate hibernateTemplate;

	/**
	 * @throws Exception
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(TacheDAO.class, DAO_NAME);
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
	}

	@Test
	public void testFindParTypeEtatTache() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		// Tâches en instance
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(5), list.get(1).getId());
		}

		// Tâches traitées
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setEtatTache(TypeEtatTache.TRAITE);
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(Long.valueOf(2), list.get(0).getId());
			assertEquals(Long.valueOf(3), list.get(1).getId());
		}

		// Tâches en cours
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setEtatTache(TypeEtatTache.EN_COURS);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(4), list.get(0).getId());
		}
	}

	@Test
	public void testFindParTypeTache() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		// Type envoi di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(5), list.get(1).getId());
		}

		// Type annulation di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(2), list.get(0).getId());
		}

		// Type contrôle dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheControleDossier);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(3), list.get(0).getId());
		}

		// Type transmission dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheTransmissionDossier);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(4), list.get(0).getId());
		}
	}

	@Test
	public void testFindParTypeTacheInverse() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		// Type envoi di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(3, list.size());
			assertEquals(Long.valueOf(2), list.get(0).getId());
			assertEquals(Long.valueOf(3), list.get(1).getId());
			assertEquals(Long.valueOf(4), list.get(2).getId());
		}

		// Type annulation di
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(3), list.get(1).getId());
			assertEquals(Long.valueOf(4), list.get(2).getId());
			assertEquals(Long.valueOf(5), list.get(3).getId());
		}

		// Type contrôle dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheControleDossier);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(4), list.get(2).getId());
			assertEquals(Long.valueOf(5), list.get(3).getId());
		}

		// Type transmission dossier
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheTransmissionDossier);
			criterion.setInvertTypeTache(true);
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(3), list.get(2).getId());
			assertEquals(Long.valueOf(5), list.get(3).getId());
		}
	}

	@Test
	public void testFindParDateCreation() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

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
			assertEquals(Long.valueOf(1), list.get(0).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 2, 29));
			final List<Tache> list = dao.find(criterion);
			assertEquals(2, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 3, 31));
			final List<Tache> list = dao.find(criterion);
			assertEquals(3, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(3), list.get(2).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setDateCreationDepuis(DateHelper.getDate(2008, 1, 1));
			criterion.setDateCreationJusqua(DateHelper.getDate(2008, 4, 30));
			final List<Tache> list = dao.find(criterion);
			assertEquals(4, list.size());
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(3), list.get(2).getId());
			assertEquals(Long.valueOf(4), list.get(3).getId());
		}
	}

	@Test
	public void testFindParAnnee() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

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
			assertEquals(Long.valueOf(1), list.get(0).getId());
		}
		{
			TacheCriteria criterion = new TacheCriteria();
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);
			criterion.setAnnee(2009);
			final List<Tache> list = dao.find(criterion);
			assertEquals(1, list.size());
			assertEquals(Long.valueOf(5), list.get(0).getId());
		}
	}

	@Test
	public void testFindParContribuable() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

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
			assertEquals(Long.valueOf(1), list.get(0).getId());
			assertEquals(Long.valueOf(2), list.get(1).getId());
			assertEquals(Long.valueOf(3), list.get(2).getId());
			assertEquals(Long.valueOf(4), list.get(3).getId());
			assertEquals(Long.valueOf(5), list.get(4).getId());
		}
	}


	@Test
	public void testFindAvecPagination() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
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
		doInNewTransaction(new TxCallback() {
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
		doInNewTransaction(new TxCallback() {
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
				dao.updateCollAdmAssignee(ids.ctb, ca2.getNumeroCollectiviteAdministrative());
				return null;
			}
		});

		// Etat après changement
		doInNewTransaction(new TxCallback() {
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
}
