package ch.vd.unireg.declaration;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesEtSNCException;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCService;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCServiceImpl;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static ch.vd.unireg.common.WithoutSpringTest.date;

@RunWith(MockitoJUnitRunner.class)
public class LienAssociesSNCServiceTest  {

	@InjectMocks
	private LienAssociesSNCService service = new LienAssociesSNCServiceImpl();

	@Mock
	private TiersService tiersService;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}


	/**
	 * Vérifie qu'un établissement ne peut pas servir de sujet (associé), pour les rapports entre tiers Associé - SNC .
	 */
	@Test
	public void testIsAllowedSujetTypeNonAcceptable() {
		final Etablissement sujet = getEtablissement();
		try {
			service.isAllowed(sujet, null, null);
		}
		catch (LienAssociesEtSNCException ex) {
			Assertions.assertThat(ex.getErreur()).isEqualTo(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_ASSOCIE);
		}
		catch (Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	/**
	 * Vérifie qu'un établissement ne peut pas servir d'objet (SNC), pour les rapports entre tiers Associé - SNC .
	 */
	@Test
	public void testIsAllowedObjetTypeNonAcceptable() {
		final Etablissement objet = getEtablissement();
		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			service.isAllowed(sujet, objet, null);
		}
		catch (LienAssociesEtSNCException ex) {
			Assertions.assertThat(ex.getErreur()).isEqualTo(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_SNC);
		}
		catch (Exception e) {
			Assertions.fail(e.getMessage());
		}
	}


	/**
	 * Vérifie qu'une PM non SNC, n'est pas autorisée, pour les rapports associé - SNC .
	 */
	@Test
	public void testIsAllowedObjetNonSNC() {
		final Entreprise objet = new Entreprise();
		objet.setNumero(1234L);
		final ForFiscalPrincipal dernierForSnc = getForFiscalPrincipal(GenreImpot.SUCCESSION);
		objet.addForFiscal(dernierForSnc);

		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			service.isAllowed(sujet, objet, null);
		}
		catch (LienAssociesEtSNCException ex) {
			Assertions.assertThat(ex.getErreur()).isEqualTo(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.TIERS_PAS_SNC);
		}
		catch (Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	/**
	 * Vérifie qu'on ne peut pas insérer de doublon de rapports entre tiers.
	 */
	@Test
	public void testIsAllowedExistRapport() {
		final Entreprise objet = new Entreprise();
		objet.setNumero(1234L);
		final ForFiscalPrincipal dernierForSnc = getForFiscalPrincipal(GenreImpot.REVENU_FORTUNE);
		objet.addForFiscal(dernierForSnc);
		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			Mockito.when(tiersService.existRapportEntreTiers(Mockito.any(TypeRapportEntreTiers.class), Mockito.any(Contribuable.class), Mockito.any(Contribuable.class), Mockito.any(RegDate.class))).thenReturn(Boolean.TRUE);
			service.isAllowed(sujet, objet, null);
		}
		catch (LienAssociesEtSNCException ex) {
			Assertions.assertThat(ex.getErreur()).isEqualTo(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.CHEVAUCHEMENT_LIEN);
		}
		catch (Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	/**
	 * Vérifie la création d'un rapport entre une Entreprise  et une PP.
	 */
	@Test
	public void testIsAllowedPP() {
		final Entreprise objet = new Entreprise();
		objet.setNumero(1234L);
		final ForFiscalPrincipal dernierForSnc = getForFiscalPrincipal(GenreImpot.REVENU_FORTUNE);
		objet.addForFiscal(dernierForSnc);
		final PersonnePhysique sujet = getPersonnePhysique();
		Mockito.when(tiersService.existRapportEntreTiers(Mockito.any(TypeRapportEntreTiers.class), Mockito.any(Contribuable.class), Mockito.any(Contribuable.class), Mockito.any(RegDate.class))).thenReturn(Boolean.FALSE);
		try {
			Assertions.assertThat(service.isAllowed(sujet, objet, date(2018, 12, 31))).isTrue();
		}
		catch (Exception e) {
			Assertions.fail(e.getMessage());
		}

	}

	/**
	 * Vérifie la création d'un rapport entre une Entreprise  et une PM.
	 */
	@Test
	public void testIsAllowedPM() {
		final Entreprise objet = new Entreprise();
		objet.setNumero(1234L);
		final ForFiscalPrincipal dernierForSnc = getForFiscalPrincipal(GenreImpot.REVENU_FORTUNE);
		objet.addForFiscal(dernierForSnc);
		final Entreprise sujet = new Entreprise();
		Mockito.when(tiersService.existRapportEntreTiers(Mockito.any(TypeRapportEntreTiers.class), Mockito.any(Contribuable.class), Mockito.any(Contribuable.class), Mockito.any(RegDate.class))).thenReturn(Boolean.FALSE);
		try {
			Assertions.assertThat(service.isAllowed(sujet, objet, date(2018, 12, 31))).isTrue();
		}
		catch (LienAssociesEtSNCException e) {
			Assertions.fail(e.getMessage());
		}

	}

	@NotNull
	private ForFiscalPrincipal getForFiscalPrincipal(GenreImpot genreImpot) {
		final ForFiscalPrincipal ffp = new ForFiscalPrincipalPM();
		ffp.setGenreImpot(genreImpot);
		ffp.setDateDebut(date(2000, 1, 1));
		ffp.setDateFin(date(2018, 12, 31));
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffp.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
		return ffp;
	}


	@NotNull
	private PersonnePhysique getPersonnePhysique() {
		final PersonnePhysique sujet = new PersonnePhysique();
		sujet.setNumero(5678L);
		return sujet;
	}

	@NotNull
	private Etablissement getEtablissement() {
		final Etablissement objet = new Etablissement();
		objet.setNumero(1234L);
		return objet;
	}

}