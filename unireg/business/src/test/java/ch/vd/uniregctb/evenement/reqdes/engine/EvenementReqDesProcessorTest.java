package ch.vd.uniregctb.evenement.reqdes.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.Fuse;
import ch.vd.uniregctb.reqdes.ErreurTraitement;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.reqdes.EvenementReqDes;
import ch.vd.uniregctb.reqdes.EvenementReqDesDAO;
import ch.vd.uniregctb.reqdes.InformationsActeur;
import ch.vd.uniregctb.reqdes.ModeInscription;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.RolePartiePrenante;
import ch.vd.uniregctb.reqdes.TransactionImmobiliere;
import ch.vd.uniregctb.reqdes.TypeInscription;
import ch.vd.uniregctb.reqdes.TypeRole;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.reqdes.UniteTraitementDAO;
import ch.vd.uniregctb.type.EtatCivil;

public class EvenementReqDesProcessorTest extends BusinessTest {

	private EvenementReqDesProcessorImpl processor;
	private UniteTraitementDAO uniteTraitementDAO;
	private EvenementReqDesDAO evenementReqDesDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		uniteTraitementDAO = getBean(UniteTraitementDAO.class, "reqdesUniteTraitementDAO");
		evenementReqDesDAO = getBean(EvenementReqDesDAO.class, "reqdesEvenementDAO");

		processor = new EvenementReqDesProcessorImpl();
		processor.setHibernateTemplate(hibernateTemplate);
		processor.setInfraService(serviceInfra);
		processor.setTransactionManager(transactionManager);
		processor.setUniteTraitementDAO(uniteTraitementDAO);
		processor.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		if (processor != null) {
			processor.destroy();
			processor = null;
		}
		super.onTearDown();
	}

	/**
	 * Méthode de traitement synchrone d'une unité de traitement indiquée par son identifiant
	 * @param id identifiant de l'unité de traitement à traiter
	 * @throws InterruptedException si le thread est interrompu
	 */
	protected void traiteUniteTraitement(final long id) throws InterruptedException {

		// le fusible saute dès le traitement terminé
		final Fuse done = new Fuse();
		final EvenementReqDesProcessor.ListenerHandle handle = processor.registerListener(new EvenementReqDesProcessor.Listener() {
			@Override
			public void onUniteTraite(long idUniteTraitement) {
				if (idUniteTraitement == id) {
					done.blow();
				}
			}

			@Override
			public void onStop() {
				done.blow();
			}
		});

		try {
			processor.postUniteTraitement(id);

			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (done) {
				// tant que le traitement n'est pas terminé, on attend...
				while (!done.isBlown()) {
					done.wait();
				}
			}
		}
		finally {
			processor.unregisterListener(handle);
		}
	}

	protected EvenementReqDes addEvenementReqDes(InformationsActeur notaire, @Nullable InformationsActeur operateur, RegDate dateActe, String noMinute) {
		final EvenementReqDes evt = new EvenementReqDes();
		evt.setNotaire(notaire);
		evt.setOperateur(operateur);
		evt.setDateActe(dateActe);
		evt.setNumeroMinute(noMinute);
		evt.setXml("<tubidu/>");
		return evenementReqDesDAO.save(evt);
	}

	protected UniteTraitement addUniteTraitement(EvenementReqDes evt, EtatTraitement etat, @Nullable Date dateTraitement) {
		final UniteTraitement ut = new UniteTraitement();
		ut.setEvenement(evt);
		ut.setDateTraitement(dateTraitement);
		ut.setEtat(etat);
		return uniteTraitementDAO.save(ut);
	}

	protected PartiePrenante addPartiePrenante(UniteTraitement ut, String nom, String prenoms) {
		final PartiePrenante pp = new PartiePrenante();
		pp.setUniteTraitement(ut);
		pp.setNom(nom);
		pp.setPrenoms(prenoms);
		return hibernateTemplate.merge(pp);
	}

	protected TransactionImmobiliere addTransactionImmobiliere(EvenementReqDes evt, String description, ModeInscription modeInscription, TypeInscription typeInscription, int noOfsCommune) {
		final TransactionImmobiliere ti = new TransactionImmobiliere();
		ti.setEvenementReqDes(evt);
		ti.setDescription(description);
		ti.setModeInscription(modeInscription);
		ti.setTypeInscription(typeInscription);
		ti.setOfsCommune(noOfsCommune);
		return hibernateTemplate.merge(ti);
	}

	protected RolePartiePrenante addRole(PartiePrenante pp, TransactionImmobiliere transactionImmobiliere, TypeRole typeRole) {
		final RolePartiePrenante role = new RolePartiePrenante();
		role.setTransaction(transactionImmobiliere);
		role.setRole(typeRole);
		addRole(pp, role);
		return role;
	}

	protected void addRole(PartiePrenante pp, RolePartiePrenante role) {
		if (pp.getRoles() == null) {
			pp.setRoles(new HashSet<RolePartiePrenante>());
		}
		pp.getRoles().add(role);
	}

	@Test(timeout = 10000)
	public void testDateFuture() throws Exception {

		// mise en place d'une unité de traitement bidon dont l'événement a une date à demain
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Francis"), null, RegDate.get().getOneDayAfter(), "56754K");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				return ut.getId();
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(id);

		// vérification des erreurs
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());

				final Set<ErreurTraitement> erreurs = ut.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final ErreurTraitement erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
				Assert.assertEquals("La date de l'acte est dans le futur.", erreur.getMessage());
			}
		});
	}

	@Test(timeout = 10000)
	public void testControlesPreliminaires() throws Exception {

		final RegDate dateActe = RegDate.get();

		final class Ids {
			long utResidentVaudois;
			long utResidentCommuneInconnue;
		}

		// mise en place d'une unité de traitement
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Francis"), null, dateActe, "56754K");
				final TransactionImmobiliere t1 = addTransactionImmobiliere(evt, "Bonne commune", ModeInscription.INSCRIPTION, TypeInscription.CHARGE_FONCIERE, MockCommune.Lausanne.getNoOFS());
				final TransactionImmobiliere t2 = addTransactionImmobiliere(evt, "Commune inconnue", ModeInscription.MODIFICATION, TypeInscription.ANNOTATION, 9999);
				final TransactionImmobiliere t3 = addTransactionImmobiliere(evt, "Commune faîtiere", ModeInscription.RADIATION, TypeInscription.PROPRIETE, MockCommune.LeChenit.getNoOFS());

				final UniteTraitement ut1 = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut1, "Résident", "Vaudois");
				pp1.setOfsCommune(MockCommune.Echallens.getNoOFS());
				pp1.setEtatCivil(EtatCivil.CELIBATAIRE);
				pp1.setNomConjoint("Conjoint");
				addRole(pp1, t1, TypeRole.ACQUEREUR);

				final UniteTraitement ut2 = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp2 = addPartiePrenante(ut2, "Résident", "Commune inconnue");
				pp2.setOfsCommune(9999);
				pp2.setEtatCivil(EtatCivil.MARIE);
				pp2.setNomConjoint("Conjoint marié");
				addRole(pp2, t1, TypeRole.ALIENATEUR);
				addRole(pp2, t2, TypeRole.AUTRE);
				addRole(pp2, t3, TypeRole.ACQUEREUR);

				final Ids ids = new Ids();
				ids.utResidentVaudois = ut1.getId();
				ids.utResidentCommuneInconnue = ut2.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.utResidentVaudois);

		// vérification des erreurs
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement autreUT = uniteTraitementDAO.get(ids.utResidentCommuneInconnue);
				Assert.assertNotNull(autreUT);
				Assert.assertEquals(EtatTraitement.A_TRAITER, autreUT.getEtat());
				Assert.assertNull(autreUT.getDateTraitement());
				Assert.assertEquals(0, autreUT.getErreurs().size());

				final UniteTraitement ut = uniteTraitementDAO.get(ids.utResidentVaudois);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());

				final Set<ErreurTraitement> erreurs = ut.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(2, erreurs.size());

				final List<ErreurTraitement> sorted = new ArrayList<>(erreurs);
				Collections.sort(sorted, new Comparator<ErreurTraitement>() {
					@Override
					public int compare(ErreurTraitement o1, ErreurTraitement o2) {
						return o1.getMessage().compareTo(o2.getMessage());
					}
				});

				{
					final ErreurTraitement erreur = sorted.get(0);
					Assert.assertNotNull(erreur);
					Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
					Assert.assertEquals("Incohérence entre l'état civil (Célibataire) et la présence d'un conjoint.", erreur.getMessage());
				}
				{
					final ErreurTraitement erreur = sorted.get(1);
					Assert.assertNotNull(erreur);
					Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
					Assert.assertEquals("La commune de résidence (Echallens/5518) est vaudoise.", erreur.getMessage());
				}
			}
		});

		// traitement de l'unité suivante
		traiteUniteTraitement(ids.utResidentCommuneInconnue);

		// vérification des erreurs
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.utResidentCommuneInconnue);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());

				final Set<ErreurTraitement> erreurs = ut.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(2, erreurs.size());

				final List<ErreurTraitement> sorted = new ArrayList<>(erreurs);
				Collections.sort(sorted, new Comparator<ErreurTraitement>() {
					@Override
					public int compare(ErreurTraitement o1, ErreurTraitement o2) {
						return o1.getMessage().compareTo(o2.getMessage());
					}
				});

				{
					final ErreurTraitement erreur = sorted.get(0);
					Assert.assertNotNull(erreur);
					Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
					Assert.assertEquals("Commune 9999 inconnue au " + RegDateHelper.dateToDisplayString(dateActe) + ".", erreur.getMessage());
				}
				{
					final ErreurTraitement erreur = sorted.get(1);
					Assert.assertNotNull(erreur);
					Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
					Assert.assertEquals("La commune 'Le Chenit' (5872) est une commune faîtière de fractions.", erreur.getMessage());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testControlesPreliminairesSurLiensMatrimoniaux() throws Exception {

		final RegDate dateActe = RegDate.get().getOneDayBefore();

		// mise en place d'un lien matrimonial unidirectionel
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Francis"), null, dateActe, "56754K");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut, "Petit", "Bonhomme");
				final PartiePrenante pp2 = addPartiePrenante(ut, "Grand", "Garçon");
				pp1.setConjointPartiePrenante(pp2);
				return ut.getId();
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(id);

		// vérification des erreurs
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());

				final Set<ErreurTraitement> erreurs = ut.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final ErreurTraitement erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
				Assert.assertEquals("Liens matrimoniaux incohérents entres les parties prenantes.", erreur.getMessage());
			}
		});
	}
}
