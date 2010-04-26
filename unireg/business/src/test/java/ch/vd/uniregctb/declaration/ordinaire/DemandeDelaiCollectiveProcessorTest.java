package ch.vd.uniregctb.declaration.ordinaire;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DemandeDelaiCollectiveResults.ErreurType;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class DemandeDelaiCollectiveProcessorTest extends BusinessTest {

	private HibernateTemplate hibernateTemplate;
	private DemandeDelaiCollectiveProcessor processor;
	private final RegDate dateTraitement = RegDate.get();

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new DemandeDelaiCollectiveProcessor(periodeDAO, hibernateTemplate, transactionManager);
	}

	@Test
	public void testAccorderDelaiTiersSansDeclaration() throws Exception {

		final RegDate dateDelai = RegDate.get(2010, 9, 1);

		final int annee = 2009;
		final PeriodeFiscale periode = addPeriodeFiscale(annee);
		final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final List<Long> ids = Collections.emptyList();
		final Contribuable mrKong = addNonHabitant("King", "Kong", date(1965, 4, 13), Sexe.MASCULIN);

		{
			// TEST : un tiers sans déclaration pour 2009.
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement);
			processor.setRapport(rapport);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(dateDelai));
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			assertEquals(ErreurType.CONTRIBUABLE_SANS_DI, rapport.errors.get(0).raison);
		}

		final Declaration d = addDeclarationImpot(mrKong, periode, RegDate.get(2009, 1, 1), RegDate.get(2009, 12, 31),
				TypeContribuable.HORS_CANTON, modeleDocument);
		d.setDelais(new HashSet<DelaiDeclaration>());
		assertNull(d.getDelaiAccordeAu());
		d.addEtat(newEtatDeclaration(TypeEtatDeclaration.EMISE));

		{
			// TEST : On lui ajoute 1 declaration pour 2009 à l'état émise :
			// - La déclaration n'a pas de délai
			// - La déclaration n'est pas retournée
			// - La déclaration n'est pas annulée
			// - On souhaite accordé un délai au 01.09.2010
			//
			// Resultats attendus :
			// - Le délai est d'abord null
			// - une fois le délai accordé, le délai est au au 01.09.2010

			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement);
			processor.setRapport(rapport);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(dateDelai));
			assertEquals(1, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
		}

		{
			// TEST : On essaye de rajouter un délai antérieur (au 31.08.2010)
			// Resultat attendu :
			// - le délai ne doit pas etre ajouté
			// - le délai est toujours au 01.09.2010

			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement);
			processor.setRapport(rapport);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 8, 31)));
			assertEquals(0, rapport.traites.size());
			assertEquals(1, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(1, d.getDelais().size());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
		}

		{
			// TEST : On essaye de rajouter le même délai (au 01.09.2010)
			// Resultat attendu :
			// - le délai ne doit pas etre ajouté
			// - le délai est toujours au 01.09.2010
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement);
			processor.setRapport(rapport);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(dateDelai));
			assertEquals(0, rapport.traites.size());
			assertEquals(1, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(1, d.getDelais().size());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
		}

		{
			// TEST : On essaye de rajouter un délai posterieur (au 02.09.2010)
			// Resultat attendu :
			// - le délai doit etre ajouté
			// - le délai est maintenant au 02.09.2010
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement);
			processor.setRapport(rapport);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 9, 2)));
			assertEquals(1, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(2, d.getDelais().size());
			assertEquals(RegDate.get(2010, 9, 2), d.getDelaiAccordeAu());
		}

		{
			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			((EtatDeclaration) (d.getEtats().toArray()[0])).setEtat(TypeEtatDeclaration.RETOURNEE);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement);
			processor.setRapport(rapport);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 12, 4)));
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			assertEquals(ErreurType.DI_RETOURNEE, rapport.errors.get(0).raison);
		}

		{
			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			((EtatDeclaration) (d.getEtats().toArray()[0])).setEtat(TypeEtatDeclaration.SOMMEE);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement);
			processor.setRapport(rapport);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 12, 4)));
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			assertEquals(ErreurType.DI_SOMMEE, rapport.errors.get(0).raison);
		}

		{
			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			((EtatDeclaration) (d.getEtats().toArray()[0])).setEtat(TypeEtatDeclaration.ECHUE);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement);
			processor.setRapport(rapport);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 12, 4)));
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			assertEquals(ErreurType.DI_ECHUE, rapport.errors.get(0).raison);
		}
	}

	private DelaiDeclaration newDelaiDeclaration(RegDate delaiAccordeAu) {
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDelaiAccordeAu(delaiAccordeAu);
		return delai;
	}

	private EtatDeclaration newEtatDeclaration(TypeEtatDeclaration typeEtat) {
		EtatDeclaration etat = new EtatDeclaration();
		etat.setEtat(typeEtat);
		return etat;
	}
}
