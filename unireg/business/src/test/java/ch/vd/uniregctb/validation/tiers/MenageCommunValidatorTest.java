package ch.vd.uniregctb.validation.tiers;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

public class MenageCommunValidatorTest extends AbstractValidatorTest<MenageCommun> {

	@Override
	protected String getValidatorBeanName() {
		return "menageCommunValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateForsMenageCommun() {

		/*
		 * Cas valides
		 */
		{
			// un ménage commun sans aucun for ni période de validité
			final MenageCommun mc1 = hibernateTemplate.merge(new MenageCommun());
			assertFalse(validate(mc1).hasErrors());

			// un ménage commun sans aucun for mais avec une période de validité
			final MenageCommun mc2 = hibernateTemplate.merge(new MenageCommun());
			final PersonnePhysique pp2 = addNonHabitant("Albus", "Mandragore", null, Sexe.MASCULIN);
			mc2.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31), pp2, mc2));
			final ValidationResults vr2 = validate(mc2);
			assertFalse(vr2.toString(), vr2.hasErrors());

			// un ménage commun avec une période de validité et un for égal à cette période
			final MenageCommun mc3 = hibernateTemplate.merge(new MenageCommun());
			final PersonnePhysique pp3 = addNonHabitant("Albus", "Mandragore", null, Sexe.MASCULIN);
			mc3.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31), pp3, mc3));
			final ForFiscalPrincipal f3 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f3.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f3.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc3.addForFiscal(f3);
			assertValidation(null, null, validate(mc3));

			// un ménage commun avec une période de validité et un for compris dans cette période
			final MenageCommun mc4 = hibernateTemplate.merge(new MenageCommun());
			final PersonnePhysique pp4 = addNonHabitant("Albus", "Mandragore", null, Sexe.MASCULIN);
			mc4.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31), pp4, mc4));
			ForFiscalPrincipal f4 = new ForFiscalPrincipal(date(2001, 1, 1), date(2003, 12, 31), MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f4.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f4.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc4.addForFiscal(f4);
			assertValidation(null, null, validate(mc4));

			// un ménage commun avec deux périodes de validité adjacentes et un for compris dans ces deux périodes
			MenageCommun mc5 = hibernateTemplate.merge(new MenageCommun());
			final PersonnePhysique pp5 = addNonHabitant("Albus", "Mandragore", null, Sexe.MASCULIN);
			mc5.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2002, 12, 31), pp5, mc5));
			mc5.addRapportObjet(new AppartenanceMenage(date(2003, 1, 1), date(2004, 12, 31), pp5, mc5));
			ForFiscalPrincipal f5 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f5.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f5.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc5.addForFiscal(f5);
			assertValidation(null, null, validate(mc5));
		}

		/*
		 * Cas invalides
		 */
		{
			// un ménage commun sans période de validité mais avec un for
			final MenageCommun mc1 = hibernateTemplate.merge(new MenageCommun());
			final ForFiscalPrincipal f1 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f1.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f1.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc1.addForFiscal(f1);
			assertValidation(Arrays.asList(String.format("Le for fiscal [%s] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [%s]", f1, mc1.getNumero())), null, validate(mc1));

			// un ménage commun avec une période de validité et un for complétement en dehors de cette période
			final MenageCommun mc2 = hibernateTemplate.merge(new MenageCommun());
			final PersonnePhysique pp2 = addNonHabitant("Albus", "Mandragore", null, Sexe.MASCULIN);
			mc2.addRapportObjet(new AppartenanceMenage(date(1990, 1, 1), date(1994, 12, 31), pp2, mc2));
			final ForFiscalPrincipal f2 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f2.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f2.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc2.addForFiscal(f2);
			assertValidation(Arrays.asList(String.format("Le for fiscal [%s] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [%s]", f2, mc2.getNumero())), null, validate(mc2));

			// un ménage commun avec une période de validité et un for dépassant de la période
			final MenageCommun mc3 = hibernateTemplate.merge(new MenageCommun());
			final PersonnePhysique pp3 = addNonHabitant("Albus", "Mandragore", null, Sexe.MASCULIN);
			mc3.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31), pp3, mc3));
			final ForFiscalPrincipal f3 = new ForFiscalPrincipal(date(2003, 1, 1), date(2007, 12, 31), MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f3.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f3.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc3.addForFiscal(f3);
			assertValidation(Arrays.asList(String.format("Le for fiscal [%s] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [%s]", f3, mc3.getNumero())), null, validate(mc3));

			// un ménage commun avec deux périodes de validité non-adjacentes et un for compris dans ces deux périodes
			final MenageCommun mc4 = hibernateTemplate.merge(new MenageCommun());
			final PersonnePhysique pp4 = addNonHabitant("Albus", "Mandragore", null, Sexe.MASCULIN);
			mc4.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2001, 12, 31), pp4, mc4));
			mc4.addRapportObjet(new AppartenanceMenage(date(2003, 1, 1), date(2004, 12, 31), pp4, mc4));
			ForFiscalPrincipal f4 = new ForFiscalPrincipal(date(2001, 1, 1), date(2003, 12, 31), MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f4.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f4.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc4.addForFiscal(f4);
			assertValidation(Arrays.asList(String.format("Le for fiscal [%s] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [%s]", f4, mc4.getNumero())), null, validate(mc4));
		}
	}

	/**
	 * Teste le cas ou un ménage précédemment fermé (fors et rapports entre tiers) est rouvert comme résultat, par exemple, d'une action d'annulation.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testReovertureForsEtRapports() {

		{
			final MenageCommun mc = hibernateTemplate.merge(new MenageCommun());
			final PersonnePhysique pp = addNonHabitant("Albus", "Mandragore", null, Sexe.MASCULIN);

			// Rapports entre tiers
			// Premier rapport entre tiers: annulé
			RapportEntreTiers ret = new AppartenanceMenage(date(1982, 12, 4), date(2008, 1, 1), pp, mc);
			ret.setAnnule(true);
			mc.addRapportObjet(ret);
			// Deuxieme rapport: ouvert
			ret = new AppartenanceMenage(date(1982, 12, 4), null, pp, mc);
			mc.addRapportObjet(ret);

			// Fors
			// Premier for: annulé
			ForFiscalPrincipal ffp = new ForFiscalPrincipal(date(1982, 12, 4), date(2008, 1, 1), 261, TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			ffp.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			ffp.setMotifFermeture(MotifFor.VEUVAGE_DECES);
			ffp.setAnnule(true);
			mc.addForFiscal(ffp);
			// Deuxieme for: ouvert
			ffp = new ForFiscalPrincipal(date(1982, 12, 4), null, 261, TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			ffp.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			ffp.setMotifFermeture(MotifFor.VEUVAGE_DECES);
			mc.addForFiscal(ffp);

			// validations
			assertValidation(null, null, validate(mc));
		}
	}

	/**
	 * Vérifie que le validator détecte bien les ménages à trois.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetecteMenageATroisSimultanes() {

		final MenageCommun mc = hibernateTemplate.merge(new MenageCommun());
		final PersonnePhysique pp1 = addNonHabitant("Albus", "Dumbledore", null, Sexe.MASCULIN);
		final PersonnePhysique pp2 = addNonHabitant("Romualda", "Parsiflore", null, Sexe.FEMININ);
		final PersonnePhysique pp3 = addNonHabitant("Rémus", "Lupin", null, Sexe.MASCULIN);

		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), null, pp1, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), null, pp2, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), null, pp3, mc));

		assertValidation(Arrays.asList(String.format("Le ménage commun est lié avec plus de 2 personnes physiques distinctes [n°={%d,%d,%d}]", pp1.getNumero(), pp2.getNumero(), pp3.getNumero())), null, validate(mc));
	}

	/**
	 * Vérifie que le validator détecte bien les ménages à trois.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetecteMenageATroisSequentiel() {

		final MenageCommun mc = hibernateTemplate.merge(new MenageCommun());
		final PersonnePhysique pp1 = addNonHabitant("Albus", "Dumbledore", null, Sexe.MASCULIN);
		final PersonnePhysique pp2 = addNonHabitant("Romualda", "Parsiflore", null, Sexe.FEMININ);
		final PersonnePhysique pp3 = addNonHabitant("Rémus", "Lupin", null, Sexe.MASCULIN);

		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2000, 12, 31), pp1, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2002, 1, 1), date(2002, 12, 31), pp2, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2004, 1, 1), date(2004, 12, 31), pp3, mc));

		assertValidation(Arrays.asList(String.format("Le ménage commun est lié avec plus de 2 personnes physiques distinctes [n°={%d,%d,%d}]", pp1.getNumero(), pp2.getNumero(), pp3.getNumero())), null, validate(mc));
	}
}
