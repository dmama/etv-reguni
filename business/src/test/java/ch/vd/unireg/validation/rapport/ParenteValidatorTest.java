package ch.vd.unireg.validation.rapport;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.Parente;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParenteValidatorTest extends AbstractValidatorTest<Parente> {

	@Override
	protected String getValidatorBeanName() {
		return "parenteValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testParenteComplete() throws Exception {
		final PersonnePhysique parent = addNonHabitant("Papa", "Barbapapa", null, Sexe.MASCULIN);
		final PersonnePhysique enfant = addNonHabitant("Barbidur", "Barbapapa", null, Sexe.MASCULIN);
		final Parente parente = addParente(enfant, parent, date(2000, 1, 1), null);
		final ValidationResults vr = validate(parente);
		assertNotNull(vr);
		assertEquals(0, vr.getErrors().size());
	}

	@Test
	public void testParenteAvecParentDeMauvaiseClasse() throws Exception {
		final class Ids {
			long idParent;
			long idEnfant;
			long idParente;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSessionWithoutValidation(status -> {
			final PersonnePhysique parent = addNonHabitant("Papa", "Barbapapa", null, Sexe.MASCULIN);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(parent, null, date(1974, 2, 1), null);
			final MenageCommun menage = couple.getMenage();

			// bricolage pour construire une parenté dont le parent est le ménage
			final PersonnePhysique enfant = addNonHabitant("Barbidur", "Barbapapa", null, Sexe.MASCULIN);
			final Parente parente = addParente(enfant, parent, date(2000, 1, 1), null);
			parente.setObjet(menage);
			parent.getRapportsObjet().remove(parente);
			menage.addRapportObjet(parente);

			final Ids ids1 = new Ids();
			ids1.idParent = menage.getNumero();
			ids1.idEnfant = enfant.getNumero();
			ids1.idParente = parente.getId();
			return ids1;
		});

		// validation manuelle
		doInNewTransactionAndSession(status -> {
			final Parente parente = hibernateTemplate.get(Parente.class, ids.idParente);
			final ValidationResults vr = validate(parente);
			assertNotNull(vr);
			assertEquals(1, vr.getErrors().size());
			assertEquals("Le tiers parent " + FormatNumeroHelper.numeroCTBToDisplay(ids.idParent) + " n'est pas une personne physique", vr.getErrors().get(0));
			return null;
		});
	}

	@Test
	public void testParenteAvecEnfantDeMauvaiseClasse() throws Exception {
		final class Ids {
			long idParent;
			long idEnfant;
			long idParente;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSessionWithoutValidation(status -> {
			final PersonnePhysique parent = addNonHabitant("Papa", "Barbapapa", null, Sexe.MASCULIN);
			final PersonnePhysique enfant = addNonHabitant("Barbidur", "Barbapapa", null, Sexe.MASCULIN);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(enfant, null, date(2000, 1, 1), null);
			final MenageCommun menage = couple.getMenage();

			// bricolage pour construire une parenté dont l'enfant est le ménage
			final Parente parente = addParente(enfant, parent, date(2000, 1, 1), null);
			parente.setSujet(menage);
			enfant.getRapportsSujet().remove(parente);
			menage.addRapportSujet(parente);

			final Ids ids1 = new Ids();
			ids1.idParent = parent.getNumero();
			ids1.idEnfant = menage.getNumero();
			ids1.idParente = parente.getId();
			return ids1;
		});

		// validation manuelle
		doInNewTransactionAndSession(status -> {
			final Parente parente = hibernateTemplate.get(Parente.class, ids.idParente);
			final ValidationResults vr = validate(parente);
			assertNotNull(vr);
			assertEquals(1, vr.getErrors().size());
			assertEquals("Le tiers enfant " + FormatNumeroHelper.numeroCTBToDisplay(ids.idEnfant) + " n'est pas une personne physique", vr.getErrors().get(0));
			return null;
		});
	}
}
