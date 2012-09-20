package ch.vd.uniregctb.hibernate;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.common.CoreDAOTest;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Test qui met en évidence le comportement étrange d'Hibernate qui créé des proxy sous-classant 'Tiers' à la place des classes concrètes
 * (Habitant, MenageCommun, ...) dans le case suivant:
 *
 * <ul>
 * <li>Les collections utilisent le mode fetch = LAZY</li>
 * <li>Les collections pointent vers une hiérarchie de classes (Tiers et sous-classes dans ce cas)</li>
 * <li>Il y a un cycle potentiel dans le graphe d'objets (Tiers -> Rapport -> Tiers)</li>
 * </ul>
 *
 * <p>
 * A ce moment-là, le parcours du graphe d'objets suivants :
 *
 * <pre>
 * +----------+ 1    n +---------+ n    1 +--------+
 * ¦ Personne ¦--------¦ Rapport ¦--------¦ Menage ¦
 * +----------+        +---------+        +--------+
 * </pre>
 *
 * retourne:
 *
 * <pre>
 * session.get(...)                  : personne (de type Personne)
 * personne.sujets.iterator().next() : rapport (de type Rapport)
 * rapport.objet                     : objet (de type Tiers$$EnhancerByCGLIB$$86278479)
 * </pre>
 *
 * <p>
 * <b>Update (15 juillet 2008):</p> le problème est résolu. En fait, il faut garder un fetch mode de type EAGER pour le lien retour de la collection:
 *
 * <pre>
 *                 ===> LAZY
 * +----------+ 1             n +---------+
 * ¦ Personne ¦-----------------¦ Rapport ¦
 * +----------+                 +---------+
 *                 EAGER <===
 * </pre>
 */
@ContextConfiguration(locations = {
		"classpath:ut/HibernateCollectionsTest-spring.xml"
})
public class HibernateCollectionsTest extends CoreDAOTest {

	@Entity
	@Table(name = "TEST_TIERS")
	@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
	@DiscriminatorValue("Tiers")
	public static class Tiers {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		@OneToMany(mappedBy = "sujet", fetch = FetchType.LAZY)
		public List<Rapport> rapportSujets;

		@OneToMany(mappedBy = "objet", fetch = FetchType.LAZY)
		public List<Rapport> rapportsObjet;

		public Tiers() {
			int z = 0;
			z = z+1;
		}
	}


	@Entity
	@DiscriminatorValue("Personne")
	public static class Personne extends Tiers {
		public String nom;
	}

	@Entity
	@DiscriminatorValue("Menage")
	public static class Menage extends Tiers {
	}

	@Entity
	@Table(name = "TEST_RAPPORT")
	public static class Rapport {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "TIERS_SUJET_ID")
		public Tiers sujet;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "TIERS_OBJET_ID")
		public Tiers objet;

		public Rapport() {
		}
	}

	private SessionFactory sessionFactory;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		sessionFactory = getBean(SessionFactory.class, "sessionFactoryHibernateCollections");
	}

	@Test
	public void testSaveReload() {

		Session session = sessionFactory.openSession();
		Transaction trans = session.beginTransaction();

		Long personneId;
		{
			// CTB 1
			Personne personne = new Personne();
			personne.nom = "Tommy";

			// CTB 2
			Menage menage = new Menage();

			personne = (Personne) session.merge(personne);
			menage = (Menage) session.merge(menage);

			Rapport rapport = new Rapport();
			rapport.objet = menage;
			rapport.sujet = personne;

			session.save(rapport);

			personneId = personne.id;
		}

		trans.commit();
		session.close();

		session = sessionFactory.openSession();
		{
			final Personne personne = (Personne) session.get(Personne.class, personneId);
			assertNotNull(personne);

			List<Rapport> rapports = personne.rapportSujets;
			Rapport rapport = rapports.iterator().next();
			assertNotNull(rapport);

			/* Cet assert saute avec le fetch mode = LAZY sur les collections ! */
			assertTrue(rapport.objet instanceof Menage);
		}
		session.close();
	}

}
