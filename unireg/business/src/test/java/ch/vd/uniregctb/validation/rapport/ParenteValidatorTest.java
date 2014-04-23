package ch.vd.uniregctb.validation.rapport;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique parent = addNonHabitant("Papa", "Barbapapa", null, Sexe.MASCULIN);
				final PersonnePhysique enfant = addNonHabitant("Barbidur", "Barbapapa", null, Sexe.MASCULIN);
				final Parente parente = addParente(enfant, parent, date(2000, 1, 1), null);
				final Ids ids = new Ids();
				ids.idParent = parent.getNumero();
				ids.idEnfant = enfant.getNumero();
				ids.idParente = parente.getId();
				return ids;
			}
		});

		// changement de la classe du tiers parent
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				tiersService.changeNHenMenage(ids.idParent);
				return null;
			}
		});

		// validation manuelle
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Parente parente = hibernateTemplate.get(Parente.class, ids.idParente);
				final ValidationResults vr = validate(parente);
				assertNotNull(vr);
				assertEquals(1, vr.getErrors().size());
				assertEquals("Le tiers parent " + FormatNumeroHelper.numeroCTBToDisplay(ids.idParent) + " n'est pas une personne physique", vr.getErrors().get(0));
				return null;
			}
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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique parent = addNonHabitant("Papa", "Barbapapa", null, Sexe.MASCULIN);
				final PersonnePhysique enfant = addNonHabitant("Barbidur", "Barbapapa", null, Sexe.MASCULIN);
				final Parente parente = addParente(enfant, parent, date(2000, 1, 1), null);
				final Ids ids = new Ids();
				ids.idParent = parent.getNumero();
				ids.idEnfant = enfant.getNumero();
				ids.idParente = parente.getId();
				return ids;
			}
		});

		// changement de la classe du tiers parent
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				tiersService.changeNHenMenage(ids.idEnfant);
				return null;
			}
		});

		// validation manuelle
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Parente parente = hibernateTemplate.get(Parente.class, ids.idParente);
				final ValidationResults vr = validate(parente);
				assertNotNull(vr);
				assertEquals(1, vr.getErrors().size());
				assertEquals("Le tiers enfant " + FormatNumeroHelper.numeroCTBToDisplay(ids.idEnfant) + " n'est pas une personne physique", vr.getErrors().get(0));
				return null;
			}
		});
	}
}
