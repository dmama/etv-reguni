package ch.vd.uniregctb.validation.tiers;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
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
			MenageCommun mc1 = new MenageCommun();
			assertFalse(validate(mc1).hasErrors());

			// un ménage commun sans aucun for mais avec une période de validité
			MenageCommun mc2 = new MenageCommun();
			mc2.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31), null, mc2));
			assertFalse(validate(mc2).hasErrors());

			// un ménage commun avec une période de validité et un for égal à cette période
			MenageCommun mc3 = new MenageCommun();
			mc3.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31), null, mc3));
			ForFiscalPrincipal f3 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f3.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f3.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc3.addForFiscal(f3);
			assertValidation(null, null, validate(mc3));

			// un ménage commun avec une période de validité et un for compris dans cette période
			MenageCommun mc4 = new MenageCommun();
			mc4.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31), null, mc4));
			ForFiscalPrincipal f4 = new ForFiscalPrincipal(date(2001, 1, 1), date(2003, 12, 31), MockCommune.Lausanne.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f4.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f4.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc4.addForFiscal(f4);
			assertValidation(null, null, validate(mc4));

			// un ménage commun avec deux périodes de validité adjacentes et un for compris dans ces deux périodes
			MenageCommun mc5 = new MenageCommun();
			mc5.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2002, 12, 31), null, mc5));
			mc5.addRapportObjet(new AppartenanceMenage(date(2003, 1, 1), date(2004, 12, 31), null, mc5));
			ForFiscalPrincipal f5 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
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
			MenageCommun mc1 = new MenageCommun();
			ForFiscalPrincipal f1 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f1.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f1.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc1.addForFiscal(f1);
			assertValidation(Arrays.asList(String.format("Le for fiscal [%s] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [%s]", f1, mc1.getNumero())), null, validate(mc1));

			// un ménage commun avec une période de validité et un for complétement en dehors de cette période
			MenageCommun mc2 = new MenageCommun();
			mc2.addRapportObjet(new AppartenanceMenage(date(1990, 1, 1), date(1994, 12, 31), null, mc2));
			ForFiscalPrincipal f2 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), MockCommune.Lausanne.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f2.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f2.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc2.addForFiscal(f2);
			assertValidation(Arrays.asList(String.format("Le for fiscal [%s] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [%s]", f2, mc2.getNumero())), null, validate(mc2));

			// un ménage commun avec une période de validité et un for dépassant de la période
			MenageCommun mc3 = new MenageCommun();
			mc3.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31), null, mc3));
			ForFiscalPrincipal f3 = new ForFiscalPrincipal(date(2003, 1, 1), date(2007, 12, 31), MockCommune.Lausanne.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f3.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f3.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc3.addForFiscal(f3);
			assertValidation(Arrays.asList(String.format("Le for fiscal [%s] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [%s]", f3, mc3.getNumero())), null, validate(mc3));

			// un ménage commun avec deux périodes de validité non-adjacentes et un for compris dans ces deux périodes
			MenageCommun mc4 = new MenageCommun();
			mc4.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2001, 12, 31), null, mc4));
			mc4.addRapportObjet(new AppartenanceMenage(date(2003, 1, 1), date(2004, 12, 31), null, mc4));
			ForFiscalPrincipal f4 = new ForFiscalPrincipal(date(2001, 1, 1), date(2003, 12, 31), MockCommune.Lausanne.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
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
			MenageCommun mc = new MenageCommun();

			// Rapports entre tiers
			// Premier rapport entre tiers: annulé
			RapportEntreTiers ret = new AppartenanceMenage(date(1982, 12, 4), date(2008, 1, 1), null, mc);
			ret.setAnnule(true);
			mc.addRapportObjet(ret);
			// Deuxieme rapport: ouvert
			ret = new AppartenanceMenage(date(1982, 12, 4), null, null, mc);
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

		MenageCommun mc = new MenageCommun();
		PersonnePhysique pp1 = new PersonnePhysique();
		pp1.setNumero(1L);
		PersonnePhysique pp2 = new PersonnePhysique();
		pp2.setNumero(2L);
		PersonnePhysique pp3 = new PersonnePhysique();
		pp3.setNumero(3L);

		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), null, pp1, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), null, pp2, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), null, pp3, mc));

		assertValidation(Arrays.asList("Le ménage commun est lié avec plus de 2 personnes physiques distinctes [n°={1,2,3}]"), null, validate(mc));
	}

	/**
	 * Vérifie que le validator détecte bien les ménages à trois.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetecteMenageATroisSequentiel() {

		MenageCommun mc = new MenageCommun();
		PersonnePhysique pp1 = new PersonnePhysique();
		pp1.setNumero(1L);
		PersonnePhysique pp2 = new PersonnePhysique();
		pp2.setNumero(2L);
		PersonnePhysique pp3 = new PersonnePhysique();
		pp3.setNumero(3L);

		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2000, 12, 31), pp1, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2002, 1, 1), date(2002, 12, 31), pp2, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2004, 1, 1), date(2004, 12, 31), pp3, mc));

		assertValidation(Arrays.asList("Le ménage commun est lié avec plus de 2 personnes physiques distinctes [n°={1,2,3}]"), null, validate(mc));
	}
}
