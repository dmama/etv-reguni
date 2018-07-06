package ch.vd.unireg.webservices.v7.cache;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ch.ech.ech0007.v4.CantonAbbreviation;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.WebserviceTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.efacture.EFactureServiceProxy;
import ch.vd.unireg.efacture.MockEFactureService;
import ch.vd.unireg.foncier.DonneesUtilisation;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCantonCommune;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.SituationFamillePersonnePhysique;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeDroitAcces;
import ch.vd.unireg.type.TypeMandat;
import ch.vd.unireg.type.TypeRapprochementRF;
import ch.vd.unireg.webservices.common.AccessDeniedException;
import ch.vd.unireg.webservices.v7.BusinessWebService;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.address.v3.Address;
import ch.vd.unireg.xml.party.address.v3.FormattedAddress;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.debtor.v5.Debtor;
import ch.vd.unireg.xml.party.ebilling.v1.EbillingStatus;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualInheritedLandRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualTransitiveLandRight;
import ch.vd.unireg.xml.party.person.v5.CommonHousehold;
import ch.vd.unireg.xml.party.person.v5.Nationality;
import ch.vd.unireg.xml.party.person.v5.NaturalPerson;
import ch.vd.unireg.xml.party.person.v5.Origin;
import ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxpayer.v5.Taxpayer;
import ch.vd.unireg.xml.party.taxresidence.v4.ExpenditureBased;
import ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v4.OrdinaryResident;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxResidence;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

import static ch.vd.unireg.webservices.v7.BusinessWebServiceTest.assertFoundEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class BusinessWebServiceCacheTest extends WebserviceTest {

	private Ehcache ehcache;
	private BusinessWebServiceCache cache;
	private BusinessWebServiceCacheEventListener wsCacheManager;
	private BusinessWebService implementation;
	private Map<String, List<Object[]>> calls;

	private static class Ids {
		public Long eric;
		public Long debiteur;
		public Long heritier1;
		public Long heritier2;
		public Long aieulEric;

		public Long monsieur;
		public Long madame;
		public Long menage;

		public Long immeuble0;
		public Long immeuble1;
		public Long immeuble2;
		public Long immeuble3;

		public Long batiment0;
		public Long batiment1;
		public Long batiment2;

		public Long communaute0;
		public Long communaute1;
		public Long communaute2;

		public Long entreprise;
	}

	private final Ids ids = new Ids();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService() {
			@Override
			public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
				if (application == ApplicationFiscale.CAPITASTRA) {
					return "http://example.com/";
				}
				else {
					return null;
				}
			}
		});
		final CacheManager manager = getBean(CacheManager.class, "ehCacheManager");
		final BusinessWebService webService = getBean(BusinessWebService.class, "wsv7Business");
		this.calls = new HashMap<>();
		implementation = buildTracingDelegator(webService, this.calls);

		cache = new BusinessWebServiceCache();
		cache.setCacheManager(manager);
		cache.setTarget(implementation);
		cache.setCacheName("webService7");
		cache.setSecurityProvider(getBean(SecurityProviderInterface.class, "securityProviderInterface"));
		cache.afterPropertiesSet();
		ehcache = cache.getEhCache();

		wsCacheManager = getBean(BusinessWebServiceCacheEventListener.class, "wsv7CacheEventListener");
		wsCacheManager.setCache(cache);

		final int noIndividu = 123456;
		final int noIndividuDefunt = 383828;
		final int noIndividuPapa = 111111;
		final int noIndividuJunior = 222222;
		final RegDate dateNaissance = date(1965, 4, 13);
		final RegDate dateNaissanceJunior = date(2002, 3, 7);
		final RegDate dateHeritage = date(2010, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Eric", "Bolomey", true);
				addIndividu(noIndividuDefunt, date(1910, 1, 1), "Old", "Timer", true);
				final MockIndividu papa = addIndividu(noIndividuPapa, date(1925, 11, 29), "Papa", "Bolomey", true);
				final MockIndividu junior = addIndividu(noIndividuJunior, dateNaissanceJunior, "Junior", "Bolomey", true);
				addLiensFiliation(junior, ind, null, dateNaissanceJunior, null);
				addLiensFiliation(ind, papa, null, dateNaissance, null);
				addOrigine(ind, MockCommune.Neuchatel);
				addOrigine(ind, MockCommune.Orbe);
				ind.setNomNaissance("Bolomey-de-naissance");
				addNationalite(ind, MockPays.Suisse, dateNaissance, null);
				addNationalite(ind, MockPays.France, dateNaissance.addMonths(1), null);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Morges.RueDeLAvenir, null, dateNaissance, null);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {

				// données globales
				final PeriodeFiscale periode2003 = addPeriodeFiscale(2003);

				// quelques immeubles, bâtiments, etc...
				final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
				final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
				final ProprieteParEtageRF immeuble1 = addProprieteParEtageRF("3893882", "other egrid", new Fraction(1, 3), laSarraz, 579, 11, null, null);
				final BienFondsRF immeuble2 = addBienFondsRF("93352512", "trois egrid", laSarraz, 12);
				final BienFondsRF immeuble3 = addBienFondsRF("347837", "quatre egrid", laSarraz, 13);
				ids.immeuble0 = immeuble0.getId();
				ids.immeuble1 = immeuble1.getId();
				ids.immeuble2 = immeuble2.getId();
				ids.immeuble3 = immeuble3.getId();

				// l'immeuble 0 possède lui-même l'immeuble 1
				addDroitPropriete(immeuble0, immeuble1, GenrePropriete.FONDS_DOMINANT, new Fraction(1, 34), date(2010, 1, 1), date(2010, 1, 1), null, null,
				                  "Constitution PPE", new IdentifiantAffaireRF(28, 2010, 208, 1), "02828289", "1");
				// l'immeuble 2 possède lui-même l'immeuble 3
				addDroitPropriete(immeuble2, immeuble3, GenrePropriete.FONDS_DOMINANT, new Fraction(1, 34), RegDate.get(1940, 3, 1), RegDate.get(1940, 2, 2), null, null,
				                  "Achat", new IdentifiantAffaireRF(3, 1940, 1, 1), "2783771", "1");


				final BatimentRF batiment0 = addBatimentRF("3838");
				final BatimentRF batiment1 = addBatimentRF("8482");
				final BatimentRF batiment2 = addBatimentRF("8929");
				ids.batiment0 = batiment0.getId();
				ids.batiment1 = batiment1.getId();
				ids.batiment2 = batiment2.getId();

				final CommunauteRF communaute0 = addCommunauteRF("228833", TypeCommunaute.COMMUNAUTE_DE_BIENS);
				final CommunauteRF communaute1 = addCommunauteRF("900022", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
				final CommunauteRF communaute2 = addCommunauteRF("11187282", TypeCommunaute.INDIVISION);
				ids.communaute0 = communaute0.getId();
				ids.communaute1 = communaute1.getId();
				ids.communaute2 = communaute2.getId();

				// Une personne physique avec avec toutes les parties renseignées
				final PersonnePhysique eric = addHabitant(noIndividu);
				{
					addAdresseSuisse(eric, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
					addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
					addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
					eric.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH9308440717427290198", null));

					final PersonnePhysique defunt = addHabitant(noIndividuDefunt);

					final PersonnePhysique pupille = addNonHabitant("Slobodan", "Pupille", date(1987, 7, 23), Sexe.MASCULIN);
					addTutelle(pupille, eric, null, date(2005, 7, 1), null);

					final PersonnePhysique aieulEric = addNonHabitant("Aïeul", "Bolomey", date(1904, 3, 11), Sexe.MASCULIN);
					addHeritage(eric, aieulEric, RegDate.get(1977, 8, 1), null, true);

					final PersonnePhysique heritier1 = addNonHabitant("Germaine", "Heritier", date(1987, 7, 23), Sexe.FEMININ);
					addHeritage(heritier1, eric, dateHeritage, null, true);
					final PersonnePhysique heritier2 = addNonHabitant("Adelaïde", "Heritier", date(1987, 7, 23), Sexe.FEMININ);
					addHeritage(heritier2, eric, dateHeritage, null, false);

					final SituationFamillePersonnePhysique situation = new SituationFamillePersonnePhysique();
					situation.setDateDebut(date(1989, 5, 1));
					situation.setNombreEnfants(0);
					eric.addSituationFamille(situation);

					final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2003);
					ch.vd.unireg.declaration.DeclarationImpotOrdinaire di = addDeclarationImpot(eric, periode2003, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
					addEtatDeclarationEmise(di, date(2003, 1, 10));
					addDelaiDeclaration(di, date(2003, 1, 10), date(2003, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);

					addHeritage(eric, defunt, date(1980, 1, 1), null, true);

					ids.eric = eric.getNumero();
					ids.heritier1 = heritier1.getNumero();
					ids.heritier2 = heritier2.getNumero();
					ids.aieulEric = aieulEric.getNumero();

					// le père et l'enfant
					final PersonnePhysique papa = addHabitant(noIndividuPapa);
					final PersonnePhysique junior = addHabitant(noIndividuJunior);

					addParente(eric, papa, dateNaissance, null);
					addParente(junior, eric, dateNaissanceJunior, null);

					// un débiteur
					final DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.ANNUEL, date(2009, 1, 1));
					ids.debiteur = debiteur.getId();

					// ... avec une LR sur 2009
					addForDebiteur(debiteur, date(2009, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Lausanne);
					final PeriodeFiscale pf2009 = addPeriodeFiscale(2009);
					final ModeleDocument modeleLr = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf2009);
					addListeRecapitulative(debiteur, pf2009, date(2009, 1, 1), date(2009, 12, 31), modeleLr);

					// un rapport de travail entre eric et le débiteur (pour avoir un calcul de PIIS)
					addRapportPrestationImposable(debiteur, eric, date(2009, 1, 1), date(2009, 5, 1), false);

					// une adresse mandataire
					addAdresseMandataireSuisse(eric, date(2009, 5, 1), null, TypeMandat.GENERAL, "Mon mandataire à moi", MockRue.Bex.CheminDeLaForet);

					// Eric possède un immeuble qui possède lui-même un immeuble
					final PersonnePhysiqueRF ericRF = addPersonnePhysiqueRF("Eric", "Bolomey", dateNaissance, "38383830ae3ff", 216451157465L, null);
					addDroitPersonnePhysiqueRF(RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, null, "Achat", null, "48390a0e044", "48390a0e043",
					                           new IdentifiantAffaireRF(123, 2004, 202, 3), new Fraction(1, 2), GenrePropriete.INDIVIDUELLE, ericRF, immeuble0, null);
					addRapprochementRF(eric, ericRF, RegDate.get(2000, 1, 1), null, TypeRapprochementRF.MANUEL);

					// L'aïeul d'Eric possèdait un immeuble qui possèdait lui-même un immeuble
					final PersonnePhysiqueRF aieulEricRF = addPersonnePhysiqueRF("Aïeul", "Bolomey", date(1904, 3, 11), "289218921", 3722L, null);
					addDroitPersonnePhysiqueRF(RegDate.get(1940, 3, 1), RegDate.get(1940, 2, 2), date(1980, 1, 1), date(1980, 1, 1), "Achat", "Succession", "476218937", "1",
					                           new IdentifiantAffaireRF(3, 1940, 1, 1), new Fraction(1, 2), GenrePropriete.INDIVIDUELLE, aieulEricRF, immeuble2, null);
					addRapprochementRF(aieulEric, aieulEricRF, RegDate.get(1940, 1, 1), null, TypeRapprochementRF.MANUEL);
				}

				// Une entreprise avec avec toutes les parties renseignées
				final RegDate dateCreationEntreprise = date(1970, 1, 1);
				final Entreprise entreprise = addEntrepriseInconnueAuCivil("Ma petite entreprise", dateCreationEntreprise);
				{
					// l'entreprise elle-même
					addRegimeFiscalVD(entreprise, dateCreationEntreprise, date(2011, 12, 31), MockTypeRegimeFiscal.SOCIETE_PERS);
					addRegimeFiscalCH(entreprise, dateCreationEntreprise, date(2011, 12, 31), MockTypeRegimeFiscal.SOCIETE_PERS);
					addRegimeFiscalVD(entreprise, date(2012, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addRegimeFiscalCH(entreprise, date(2012, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
					addForPrincipal(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, date(2011,12,31), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
					addForPrincipal(entreprise, date(2012, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.BENEFICE_CAPITAL);
					addForSecondaire(entreprise, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
					entreprise.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH9308440717427290198", null));
					addFormeJuridique(entreprise, dateCreationEntreprise, null, FormeJuridiqueEntreprise.SARL);
					addCapitalEntreprise(entreprise, dateCreationEntreprise, null, new MontantMonetaire(25000L, "CHF"));
					addAllegementFiscalCantonal(entreprise, dateCreationEntreprise, null, AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.TEN, AllegementFiscalCantonCommune.Type.HOLDING_IMMEUBLE);
					addAdresseMandataireSuisse(entreprise, date(2009, 5, 1), null, TypeMandat.GENERAL, "Mon mandataire à moi", MockRue.Bex.CheminDeLaForet);
					addDegrevementICI(entreprise, immeuble0, 2010, null, new DonneesUtilisation(10000L, 1000L, 100L, BigDecimal.valueOf(50), BigDecimal.valueOf(50)), null, null);
					addExonerationIFONC(entreprise, immeuble0, date(2005, 1, 1), date(2006, 12, 31), BigDecimal.TEN);

					final Etablissement etablissement = addEtablissement();
					etablissement.setRaisonSociale("Etablissement principal");
					addActiviteEconomique(entreprise, etablissement, dateCreationEntreprise, null, true);
					addDomicileEtablissement(etablissement, dateCreationEntreprise, null, MockCommune.Lausanne);

					final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode2003);
					final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, periode2003, date(2003, 1, 1), date(2003, 12, 31),
					                                                           date(2003, 1, 1), date(2003, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
					addEtatDeclarationEmise(di, date(2003, 1, 10));
					addDelaiDeclaration(di, date(2003, 1, 10), date(2003, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);

					// une entreprise absorbée par l'entreprise principale
					final RegDate dateFusion = RegDate.get(1990,1,1);
					final Entreprise absorbee = addEntrepriseInconnueAuCivil("Entreprise absorbée", dateCreationEntreprise);
					addRegimeFiscalVD(absorbee, dateCreationEntreprise, dateFusion.getOneDayBefore(), MockTypeRegimeFiscal.ORDINAIRE_PM);
					addRegimeFiscalCH(absorbee, dateCreationEntreprise, dateFusion.getOneDayBefore(), MockTypeRegimeFiscal.ORDINAIRE_PM);
					addAdresseSuisse(absorbee, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
					addForPrincipal(absorbee, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFusion.getOneDayBefore(), MotifFor.FUSION_ENTREPRISES, MockCommune.Lausanne);
					addFusionEntreprises(entreprise, absorbee, dateFusion);
					addDegrevementICI(absorbee, immeuble0, 2005, null, new DonneesUtilisation(500L, 200L, 120L, BigDecimal.valueOf(80), BigDecimal.valueOf(60)), null, null);
					addExonerationIFONC(absorbee, immeuble0, date(2002, 1, 1), date(2010, 12, 31), BigDecimal.valueOf(22));

					final PersonneMoraleRF entrepriseRF = addPersonneMoraleRF("Ma pEtItE entreprise", "CHE2222", "wliu239eru8", 82289, null);
					final PersonneMoraleRF absorbeeRF = addPersonneMoraleRF("Mon entreprise aBSorbEe", "CHE1221", "372282", 473232, null);

					// l'entreprise possède un immeuble qui possède lui-même un immeuble
					addDroitPersonneMoraleRF(RegDate.get(2004, 5, 21), RegDate.get(2001, 11, 7), null, null, "Achat", null, "3822282ef3434", "29891919",
					                         new IdentifiantAffaireRF(123, 2001, 3, 1), new Fraction(1, 2), GenrePropriete.INDIVIDUELLE, entrepriseRF, immeuble0, null);
					addRapprochementRF(entreprise, entrepriseRF, RegDate.get(2000, 1, 1), null, TypeRapprochementRF.MANUEL);

					// l'entreprise absorbée possède encore un immeuble qui possède lui-même un autre immeuble
					addDroitPersonneMoraleRF(RegDate.get(1940, 3, 1), RegDate.get(1940, 2, 2), null, null, "Achat", "Succession", "40303eff9339", "1",
					                         new IdentifiantAffaireRF(3, 1940, 1, 1), new Fraction(1, 2), GenrePropriete.INDIVIDUELLE, absorbeeRF, immeuble2, null);
					addRapprochementRF(absorbee, absorbeeRF, RegDate.get(1940, 1, 1), null, TypeRapprochementRF.MANUEL);

					ids.entreprise = entreprise.getNumero();
				}

				return null;
			}
		});

		// Un ménage commun
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {

				PersonnePhysique monsieur = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				PersonnePhysique madame = addNonHabitant("Monique", "Bolomey", date(1969, 12, 3), Sexe.FEMININ);
				EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, date(1989, 5, 1), null);
				ch.vd.unireg.tiers.MenageCommun mc = ensemble.getMenage();
				mc.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH9308440717427290198", null));

				SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				mc.addSituationFamille(situation);

				addAdresseSuisse(mc, TypeAdresseTiers.COURRIER, date(1989, 5, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(mc, date(1989, 5, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForSecondaire(mc, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne,
				                 MotifRattachement.IMMEUBLE_PRIVE);

				ids.monsieur = monsieur.getNumero();
				ids.madame = madame.getNumero();
				ids.menage = mc.getNumero();
				return null;
			}
		});


		final EFactureServiceProxy eFactureProxy = getBean(EFactureServiceProxy.class, "efactureService");
		eFactureProxy.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ids.eric);
				addEtatDestinataire(ids.eric, DateHelper.getCalendar(2013, 5, 12, 22, 24, 38).getTime(), "C'est maintenant ou jamais", null, TypeEtatDestinataire.INSCRIT, "toto@titi.com", null);
				addDestinataire(ids.entreprise);
				addEtatDestinataire(ids.entreprise, DateHelper.getCalendar(2013, 5, 12, 22, 24, 38).getTime(), "C'est maintenant ou jamais", null, TypeEtatDestinataire.INSCRIT, "toto@titi.com", null);
			}
		});
	}

	/**
	 * Construit un objet qui remplit la map des compteurs (clé = nom de la méthode, valeur = paramètres des appels) avant de déléguer l'appel plus loin
	 * @param target à qui déléguer les appels
	 * @param calls map à remplir
	 * @return l'objet qui fait tout ça...
	 */
	private static BusinessWebService buildTracingDelegator(final BusinessWebService target, final Map<String, List<Object[]>> calls) {
		return (BusinessWebService) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
		                                                   new Class[]{BusinessWebService.class},
		                                                   new InvocationHandler() {
			                                                   @Override
			                                                   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				                                                   synchronized (calls) {
					                                                   final List<Object[]> c = calls.computeIfAbsent(method.getName(), k -> new ArrayList<>());
					                                                   c.add(args);
				                                                   }
				                                                   return method.invoke(target, args);
			                                                   }
		                                                   });
	}

	private static int getNumberOfCalls(Map<String, List<Object[]>> calls) {
		int counter = 0;
		for (List<Object[]> methodCalls : calls.values()) {
			counter += methodCalls.size();
		}
		return counter;
	}

	private static int getNumberOfCalls(Map<String, List<Object[]>> calls, String methodName) {
		final List<Object[]> methodCalls = calls.get(methodName);
		return methodCalls == null ? 0 : methodCalls.size();
	}

	private static int getNumberOfCallsToGetParties(Map<String, List<Object[]>> calls) {
		return getNumberOfCalls(calls, "getParties");
	}

	private static int getNumberOfCallsToGetParty(Map<String, List<Object[]>> calls) {
		return getNumberOfCalls(calls, "getParty");
	}

	private static Object[] getLastCallParameters(Map<String, List<Object[]>> calls, String methodName) {
		final List<Object[]> methodCalls = calls.get(methodName);
		assertNotNull(methodCalls);
		return methodCalls.get(methodCalls.size() - 1);
	}

	@SuppressWarnings("unchecked")
	private static Pair<List<Integer>, Set<InternalPartyPart>> getLastCallParametersToGetParties(Map<String, List<Object[]>> calls) {
		final Object[] lastCall = getLastCallParameters(calls, "getParties");
		assertEquals(2, lastCall.length); // la méthode getParties possède deux paramètres
		return Pair.of((List<Integer>) lastCall[0], (Set<InternalPartyPart>) lastCall[1]);
	}

	@SuppressWarnings("unchecked")
	private static Pair<Integer, Set<InternalPartyPart>> getLastCallParametersToGetParty(Map<String, List<Object[]>> calls) {
		final Object[] lastCall = getLastCallParameters(calls, "getParty");
		assertEquals(2, lastCall.length); // la méthode getParty possède deux paramètres
		return Pair.of((Integer) lastCall[0], (Set<InternalPartyPart>) lastCall[1]);
	}

	@SuppressWarnings("unchecked")
	private static List<Long> getLastCallParametersToGetImmovableProperties(Map<String, List<Object[]>> calls) {
		final Object[] lastCall = getLastCallParameters(calls, "getImmovableProperties");
		assertEquals(1, lastCall.length); // la méthode getImmovableProperties possède un paramètre
		return (List<Long>) lastCall[0];
	}

	@SuppressWarnings("unchecked")
	private static List<Long> getLastCallParametersToGetBuildings(Map<String, List<Object[]>> calls) {
		final Object[] lastCall = getLastCallParameters(calls, "getBuildings");
		assertEquals(1, lastCall.length); // la méthode getBuildings possède un paramètre
		return (List<Long>) lastCall[0];
	}

	@SuppressWarnings("unchecked")
	private static List<Long> getLastCallParametersToGetCommunitiesOfOwners(Map<String, List<Object[]>> calls) {
		final Object[] lastCall = getLastCallParameters(calls, "getCommunitiesOfOwners");
		assertEquals(1, lastCall.length); // la méthode getCommunitiesOfOwners possède un paramètre
		return (List<Long>) lastCall[0];
	}

	@Override
	public void onTearDown() throws Exception {
		wsCacheManager.setCache(getBean(BusinessWebServiceCache.class, "wsv7Cache"));
		super.onTearDown();
	}

	/**
	 * [SIFISC-13558] En passant au travers du cache, ces données étaient oubliées
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNomNaissanceEtOrigineEtNationalite() throws Exception {

		final int partyNo = ids.eric.intValue();

		final Party party = cache.getParty(partyNo, null);
		assertInstanceOf(NaturalPerson.class, party);

		final NaturalPerson np = (NaturalPerson) party;

		// nom de naissance
		{
			assertEquals("Bolomey-de-naissance", np.getBirthName());
		}

		// orgines
		{
			final List<Origin> origines = np.getOrigins();
			assertNotNull(origines);
			assertEquals(2, origines.size());

			// tri dans l'ordre alphabétique des noms de lieux : 1. Neuch', 2. Orbe
			final List<Origin> sortedOrigins = new ArrayList<>(origines);
			Collections.sort(sortedOrigins, new Comparator<Origin>() {
				@Override
				public int compare(Origin o1, Origin o2) {
					return o1.getOriginName().compareTo(o2.getOriginName());
				}
			});

			{
				final Origin origine = sortedOrigins.get(0);
				assertNotNull(origine);
				assertEquals(CantonAbbreviation.NE, origine.getCanton());
				assertEquals(MockCommune.Neuchatel.getNomOfficiel(), origine.getOriginName());
			}
			{
				final Origin origine = sortedOrigins.get(1);
				assertNotNull(origine);
				assertEquals(CantonAbbreviation.VD, origine.getCanton());
				assertEquals(MockCommune.Orbe.getNomOfficiel(), origine.getOriginName());
			}
		}

		// nationalités
		{
			final List<Nationality> nationalites = np.getNationalities();
			assertNotNull(nationalites);
			assertEquals(2, nationalites.size());

			// tri dans l'ordre croissant des dates de début : 1. Suisse, 2. France
			final List<Nationality> sortedNationalities = new ArrayList<>(nationalites);
			Collections.sort(sortedNationalities, new Comparator<Nationality>() {
				@Override
				public int compare(Nationality o1, Nationality o2) {
					return o1.getDateFrom().compareTo(o2.getDateFrom());
				}
			});

			{
				final Nationality nationality = sortedNationalities.get(0);
				assertNotNull(nationality);
				assertEquals(new Date(1965, 4, 13), nationality.getDateFrom());
				assertNull(nationality.getDateTo());
				assertNotNull(nationality.getSwiss());
				assertNull(nationality.getForeignCountry());
				assertNull(nationality.getStateless());
			}
			{
				final Nationality nationality = sortedNationalities.get(1);
				assertNotNull(nationality);
				assertEquals(new Date(1965, 5, 13), nationality.getDateFrom());
				assertNull(nationality.getDateTo());
				assertNull(nationality.getSwiss());
				assertEquals((Integer) MockPays.France.getNoOFS(), nationality.getForeignCountry());
				assertNull(nationality.getStateless());
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetParty() throws Exception {

		final int partyNo = ids.eric.intValue();

		// sans parts
		{
			assertNoPart(cache.getParty(partyNo, null));

			final GetPartyValue value = getCacheValue(partyNo);
			assertNotNull(value);
			assertEmpty(value.getParts());
		}

		// ajout des adresses
		{
			assertAddressPart(cache.getParty(partyNo, EnumSet.of(InternalPartyPart.ADDRESSES)));
			assertNoPart(cache.getParty(partyNo, null)); // on vérifie que le tiers sans part fonctionne toujours bien

			final GetPartyValue value = getCacheValue(partyNo);
			assertNotNull(value);
			assertEquals(EnumSet.of(InternalPartyPart.ADDRESSES), value.getParts());
		}

		// ajout des fors
		{
			assertTaxResidenceAndAddressePart(cache.getParty(partyNo, EnumSet.of(InternalPartyPart.TAX_RESIDENCES, InternalPartyPart.ADDRESSES)));
			assertTaxResidencePart(cache.getParty(partyNo, EnumSet.of(InternalPartyPart.TAX_RESIDENCES))); // on vérifie que le tiers avec seulement les fors est correct
			assertNoPart(cache.getParty(partyNo, null)); // on vérifie que le tiers sans part fonctionne toujours bien
			assertAddressPart(cache.getParty(partyNo, EnumSet.of(InternalPartyPart.ADDRESSES))); // on vérifie que le tiers avec adresse fonctionne toujours bien

			final GetPartyValue value = getCacheValue(partyNo);
			assertNotNull(value);
			assertEquals(EnumSet.of(InternalPartyPart.TAX_RESIDENCES, InternalPartyPart.ADDRESSES), value.getParts());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPPAllParts() throws Exception {

		// on demande tour-à-tour les parties et on vérifie que 1) on les reçoit bien; et 2) qu'on ne reçoit qu'elles.
		for (InternalPartyPart p : InternalPartyPart.values()) {
			assertOnlyPart(p, cache.getParty(ids.eric.intValue(), EnumSet.of(p)));
		}

		// maintenant que le cache est chaud, on recommence la manipulation pour vérifier que cela fonctionne toujours
		for (InternalPartyPart p : InternalPartyPart.values()) {
			assertOnlyPart(p, cache.getParty(ids.eric.intValue(), EnumSet.of(p)));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPMAllParts() throws Exception {

		// on demande tour-à-tour les parties et on vérifie que 1) on les reçoit bien; et 2) qu'on ne reçoit qu'elles.
		for (InternalPartyPart p : InternalPartyPart.values()) {
			assertOnlyPart(p, cache.getParty(ids.entreprise.intValue(), EnumSet.of(p)));
		}

		// maintenant que le cache est chaud, on recommence la manipulation pour vérifier que cela fonctionne toujours
		for (InternalPartyPart p : InternalPartyPart.values()) {
			assertOnlyPart(p, cache.getParty(ids.entreprise.intValue(), EnumSet.of(p)));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEvictParty() throws Exception {

		final int partyNo = ids.eric.intValue();

		// On charge le cache avec des tiers

		assertNotNull(cache.getParty(partyNo, null));
		assertNotNull(getCacheValue(partyNo));

		// On evicte les tiers
		cache.evictParty(partyNo);

		// On vérifie que le cache est vide
		assertNull(getCacheValue(partyNo));
	}

	/**
	 * [SIFISC-27869] Ce test vérifie que les communautés d'héritiers sont bien supprimées du cache lorsque le décédé change.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEvictCommunityOfHeirs() throws Exception {

		final int partyNo = ids.eric.intValue();

		// On charge le cache avec des tiers
		assertNotNull(cache.getCommunityOfHeirs(partyNo));

		// on vérifie que l'élément est bien dans le cache
		{
			final GetCommunityOfHeirsKey key = new GetCommunityOfHeirsKey(partyNo);
			final Element element = ehcache.get(key);
			assertNotNull(element);
			assertNotNull(element.getObjectValue());
		}

		// On evicte les tiers
		cache.evictParty(partyNo);

		// On vérifie que le cache est vide
		assertNull(ehcache.get(new GetCommunityOfHeirsKey(partyNo)));
	}

	/**
	 * [UNIREG-2588] Vérifie que l'éviction d'un tiers se propage automatiquement à tous les tiers liés par rapport-entre-tiers
	 */
	@Test
	public void testEvictPartyCommonHousehold() throws Exception {

		// On charge le cache avec le ménage commun et ses adresses
		final CommonHousehold menageAvant = (CommonHousehold) cache.getParty(ids.menage.intValue(), EnumSet.of(InternalPartyPart.ADDRESSES));
		assertNotNull(menageAvant);

		// On vérifie l'adresse d'envoi
		final List<Address> mailAddressesAvant = menageAvant.getMailAddresses();
		final FormattedAddress adressesAvant = mailAddressesAvant.get(mailAddressesAvant.size() - 1).getPostAddress().getFormattedAddress();
		assertEquals("Monsieur et Madame", adressesAvant.getLine1());
		assertEquals("Eric Bolomey", adressesAvant.getLine2());
		assertEquals("Monique Bolomey", adressesAvant.getLine3());
		assertEquals("Avenue de Beaulieu", adressesAvant.getLine4());
		assertEquals("1003 Lausanne", adressesAvant.getLine5());
		assertNull(adressesAvant.getLine6());

		// On modifie le prénom de madame
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) {

				final ch.vd.unireg.tiers.MenageCommun mc = hibernateTemplate.get(ch.vd.unireg.tiers.MenageCommun.class, ids.menage);
				assertNotNull(mc);

				final Set<RapportEntreTiers> rapports = mc.getRapportsObjet();

				PersonnePhysique madame = null;
				for (RapportEntreTiers r : rapports) {
					final AppartenanceMenage am = (AppartenanceMenage) r;
					final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, am.getSujetId());
					assertNotNull(pp);

					if (pp.getPrenomUsuel().equals("Monique")) {
						madame = pp;
						break;
					}
				}
				assertNotNull(madame);
				madame.setPrenomUsuel("Gudrun");
				return null;
			}
		});

		// Cette modification va provoquer l'éviction de madame du cache, et par transitivité l'éviction du ménage commun. Si ce n'était pas le cas, les données (périmées) du ménage commun seraient encore dans le cache.
		// On vérifie donc que l'adresse d'envoi du ménage commun est bien mise-à-jour.

		final CommonHousehold menageApres = (CommonHousehold) cache.getParty(ids.menage.intValue(), EnumSet.of(InternalPartyPart.ADDRESSES));
		assertNotNull(menageApres);

		// On vérifie l'adresse d'envoi
		final List<Address> mailAddressesApres = menageApres.getMailAddresses();
		final FormattedAddress adressesApres = mailAddressesApres.get(mailAddressesApres.size() - 1).getPostAddress().getFormattedAddress();
		assertEquals("Monsieur et Madame", adressesApres.getLine1());
		assertEquals("Eric Bolomey", adressesApres.getLine2());
		assertEquals("Gudrun Bolomey", adressesApres.getLine3());
		assertEquals("Avenue de Beaulieu", adressesApres.getLine4());
		assertEquals("1003 Lausanne", adressesApres.getLine5());
		assertNull(adressesApres.getLine6());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEvictDebtorInfo() throws Exception {

		// au début, il n'y a rien
		assertNull(getCacheValue(ids.debiteur.intValue(), 2009));
		assertNull(getCacheValue(ids.debiteur.intValue(), 2010));

		// On charge le cache avec des tiers

		assertNotNull(cache.getDebtorInfo(ids.debiteur.intValue(), 2010));
		assertNotNull(getCacheValue(ids.debiteur.intValue(), 2010));
		assertNull("L'appel a été fait sur 2010, il ne devrait rien y avoir dans le cache pour 2009 !!", getCacheValue(ids.debiteur.intValue(), 2009));       // toujours rien pour le 2009

		// On evicte les tiers
		cache.evictParty(ids.debiteur);

		// On vérifie que le cache est vide
		assertNull(getCacheValue(ids.debiteur.intValue(), 2009));
		assertNull(getCacheValue(ids.debiteur.intValue(), 2010));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyInexistant() throws Exception {

		// Essaie une fois sans part
		assertNull(cache.getParty(1233455, null));
		assertNull(getCacheValue(1233455)); // null -> on ne cache pas la réponse pour un tiers inexistant !

		// Essai une seconde fois avec parts
		assertNull(cache.getParty(1233455, EnumSet.of(InternalPartyPart.ADDRESSES)));
		assertNull(getCacheValue(1233455));
	}

	/**
	 * [UNIREG-2587] Vérifie que le cache fonctionne correctement lorsqu'un tiers est demandé successivement <ol> <li>avec ses fors fiscaux virtuels, puis</li> <li>juste avec ses fors fiscaux, et</li>
	 * <li>finalement de nouveau avec ses fors fiscaux virtuels.</li> </ol>
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartySpecialCaseVirtualTaxResidences() throws Exception {

		// 1. on demande le tiers avec les fors fiscaux virtuels
		{
			final Party tiers = cache.getParty(ids.monsieur.intValue(), EnumSet.of(InternalPartyPart.VIRTUAL_TAX_RESIDENCES));
			assertNotNull(tiers);
			assertNotNull(tiers.getMainTaxResidences());
			assertEquals(1, tiers.getMainTaxResidences().size());

			final TaxResidence ffp = tiers.getMainTaxResidences().get(0);
			assertEquals(new Date(1989, 5, 1), ffp.getDateFrom());
			assertNull(ffp.getDateTo());
		}

		// 2. on demande le tiers *sans* les fors fiscaux virtuels
		{
			final Party tiers = cache.getParty(ids.monsieur.intValue(), EnumSet.of(InternalPartyPart.TAX_RESIDENCES));
			assertNotNull(tiers);
			assertEmpty(tiers.getMainTaxResidences());
		}

		// 3. on demande de nouveau le tiers avec les fors fiscaux virtuels => le résultat doit être identique à la demande du point 1.
		{
			final Party tiers = cache.getParty(ids.monsieur.intValue(), EnumSet.of(InternalPartyPart.VIRTUAL_TAX_RESIDENCES));
			assertNotNull(tiers);
			assertNotNull(tiers.getMainTaxResidences());
			assertEquals(1, tiers.getMainTaxResidences().size());

			final TaxResidence ffp = tiers.getMainTaxResidences().get(0);
			assertEquals(new Date(1989, 5, 1), ffp.getDateFrom());
			assertNull(ffp.getDateTo());
		}
	}

	/**
	 * [SIFISC-28888] Vérifie que le cache fonctionne correctement lorsque les droits de propriétés d'un contribuable qui hérite d'un défunt sont demandés successivements :
	 * <ul>
	 *     <li>avec les droits virtuels transitifs et d'héritage; puis</li>
	 *     <li>avec seulement les droits virtuels d'héritage</li>
	 * </ul>
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyVirtualTransitiveAndInheritanceLandRights() throws Exception {

		// 1. on demande l'héritier avec les droits virtuels transitifs et d'héritage
		{
			final NaturalPerson pp = (NaturalPerson) cache.getParty(ids.heritier1.intValue(), EnumSet.of(InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS,
			                                                                                             InternalPartyPart.VIRTUAL_INHERITED_REAL_LAND_RIGHTS,
			                                                                                             InternalPartyPart.VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS));
			assertNotNull(pp);
			final List<LandRight> landRights = pp.getLandRights();
			assertNotNull(landRights);
			assertEquals(2, landRights.size());

			// le droit virtuel d'héritage sur le droit de propriété réel
			final VirtualInheritedLandRight landRight0 = (VirtualInheritedLandRight) landRights.get(0);
			assertNotNull(landRight0);
			assertEquals(new Date(2010, 1, 1), landRight0.getDateFrom());
			assertNull(landRight0.getDateTo());
			assertEquals(ids.eric.longValue(), landRight0.getInheritedFromId());
			assertEquals(ids.immeuble0.longValue(), landRight0.getImmovablePropertyId());

			final LandOwnershipRight reference0 = (LandOwnershipRight) landRight0.getReference();
			assertNotNull(reference0);
			assertEquals(new Date(2004, 4, 12), reference0.getDateFrom());
			assertNull(reference0.getDateTo());
			assertEquals(ids.immeuble0.longValue(), reference0.getImmovablePropertyId());

			// le droit virtuel d'héritage sur le droit virtuel transitif
			final VirtualInheritedLandRight landRight1 = (VirtualInheritedLandRight) landRights.get(1);
			assertNotNull(landRight1);
			assertEquals(new Date(2010, 1, 1), landRight1.getDateFrom());
			assertNull(landRight1.getDateTo());
			assertEquals(ids.eric.longValue(), landRight1.getInheritedFromId());
			assertEquals(ids.immeuble1.longValue(), landRight1.getImmovablePropertyId());

			final VirtualTransitiveLandRight reference1 = (VirtualTransitiveLandRight) landRight1.getReference();
			assertNotNull(reference1);
			assertEquals(new Date(2010, 1, 1), reference1.getDateFrom());
			assertNull(reference1.getDateTo());
			assertEquals(ids.immeuble1.longValue(), reference1.getImmovablePropertyId());
		}

		// 2. on demande le tiers *sans* les droits virtuels transitifs : on ne devrait recevoir que le droit demandé
		{
			final NaturalPerson pp = (NaturalPerson) cache.getParty(ids.heritier1.intValue(), EnumSet.of(InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS,
			                                                                                             InternalPartyPart.VIRTUAL_INHERITED_REAL_LAND_RIGHTS));
			assertNotNull(pp);
			final List<LandRight> landRights = pp.getLandRights();
			assertNotNull(landRights);
			assertEquals(1, landRights.size());

			// le droit virtuel d'héritage sur le droit de propriété réel
			final VirtualInheritedLandRight landRight0 = (VirtualInheritedLandRight) landRights.get(0);
			assertNotNull(landRight0);
			assertEquals(new Date(2010, 1, 1), landRight0.getDateFrom());
			assertNull(landRight0.getDateTo());
			assertEquals(ids.eric.longValue(), landRight0.getInheritedFromId());
			assertEquals(ids.immeuble0.longValue(), landRight0.getImmovablePropertyId());

			final LandOwnershipRight reference0 = (LandOwnershipRight) landRight0.getReference();
			assertNotNull(reference0);
			assertEquals(new Date(2004, 4, 12), reference0.getDateFrom());
			assertNull(reference0.getDateTo());
			assertEquals(ids.immeuble0.longValue(), reference0.getImmovablePropertyId());
		}
	}

	/**
	 * Vérifie que le cache fonctionne correctement lorsqu'un tiers est demandé successivement <ol> <li>avec ses déclarations et leurs états, puis</li> <li>juste avec ses déclarations, et</li>
	 * <li>finalement de nouveau avec ses déclarations et leurs états.</li> </ol>
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartySpecialCaseTaxDeclarationsAndStatuses() throws Exception {

		// 1. on demande le tiers avec les déclarations et leurs états
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_STATUSES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());
		}

		// 2. on demande les déclarations *sans* les états
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());
			assertEmpty(di.getStatuses());
		}

		// 3. on demande de nouveau les déclarations avec leurs états => le résultat doit être identique à la demande du point 1.
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_STATUSES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());
		}
	}

	/**
	 * La <i>part</i> non-gérée par le cache doit toujours être récupérée depuis le véritable service
	 */
	@Test
	public void testGetPartyOnNonCacheablePart() throws Exception {

		// état initial -> aucun appel au web-service
		assertEquals(0, getNumberOfCalls(calls));

		// parts to ask for
		final Set<InternalPartyPart> parts = EnumSet.of(InternalPartyPart.TAX_RESIDENCES, InternalPartyPart.EBILLING_STATUSES, InternalPartyPart.ADDRESSES);

		// 1er appel
		{
			final Party party = cache.getParty(ids.eric.intValue(), parts);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
			assertEquals(1, party.getMainTaxResidences().size());
			assertEquals(1, party.getOtherTaxResidences().size());
			assertEquals(1, party.getResidenceAddresses().size());

			assertEquals(1, getNumberOfCallsToGetParty(calls));
			final Pair<Integer, Set<InternalPartyPart>> lastCall = getLastCallParametersToGetParty(calls);
			assertEquals((Integer) ids.eric.intValue(), lastCall.getLeft());
			assertEquals(parts, lastCall.getRight());
		}

		// 2ème appel
		{
			final Party party = cache.getParty(ids.eric.intValue(), parts);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
			assertEquals(1, party.getMainTaxResidences().size());
			assertEquals(1, party.getOtherTaxResidences().size());
			assertEquals(1, party.getResidenceAddresses().size());

			assertEquals(2, getNumberOfCallsToGetParty(calls));
			final Pair<Integer, Set<InternalPartyPart>> lastCall = getLastCallParametersToGetParty(calls);
			assertEquals((Integer) ids.eric.intValue(), lastCall.getLeft());
			assertEquals(EnumSet.of(InternalPartyPart.EBILLING_STATUSES), lastCall.getRight());
		}

		// 3ème appel
		{
			final Party party = cache.getParty(ids.eric.intValue(), parts);
			assertNotNull(party);
			assertInstanceOf(NaturalPerson.class, party);
			assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
			assertEquals(1, party.getMainTaxResidences().size());
			assertEquals(1, party.getOtherTaxResidences().size());
			assertEquals(1, party.getResidenceAddresses().size());

			assertEquals(3, getNumberOfCallsToGetParty(calls));
			final Pair<Integer, Set<InternalPartyPart>> lastCall = getLastCallParametersToGetParty(calls);
			assertEquals((Integer) ids.eric.intValue(), lastCall.getLeft());
			assertEquals(EnumSet.of(InternalPartyPart.EBILLING_STATUSES), lastCall.getRight());
		}
	}

	/**
	 * [SIFISC-23048] Quand on demande TAX_DECLARATIONS avec les TAX_DECLARATIONS_STATUSES, puis dans un autre appel les mêmes parts + TAX_DECLARATIONS_DEADLINES
	 * alors les états disparaissent de la deuxième réponse...
	 */
	@Test
	public void testRecompositionDonneesEtatsEtDelaisDeclaration() throws Exception {

		// 1. on demande le tiers avec les déclarations et les états
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_STATUSES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());

			// pas demandés -> pas reçus
			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEmpty(delais);
		}

		// 2. on demande maintenant les déclarations, leurs états et leurs délais
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_STATUSES, InternalPartyPart.TAX_DECLARATIONS_DEADLINES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());
		}

		// 3. et finalement on ne demande plus que les délais
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_DEADLINES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(0, etats.size());

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());
		}

		// 4. on demande à nouveau le tout
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_STATUSES, InternalPartyPart.TAX_DECLARATIONS_DEADLINES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());
		}
	}

	/**
	 * [SIFISC-23048] Quand on demande TAX_DECLARATIONS avec les TAX_DECLARATIONS_STATUSES, puis dans un autre appel les mêmes parts + TAX_DECLARATIONS_DEADLINES
	 * alors les états disparaissent de la deuxième réponse... (deuxième test avec la part EBILLING_STATUSES en plus, juste pour être sûr)
	 */
	@Test
	public void testRecompositionDonneesEtatsEtDelaisDeclarationAvecEFacture() throws Exception {

		// 1. on demande le tiers avec les déclarations et les états
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_STATUSES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());

			// pas demandés -> pas reçus
			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEmpty(delais);
		}

		// 2. on demande maintenant les déclarations, leurs états et leurs délais (+ efacture, non-cachable)
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_STATUSES, InternalPartyPart.TAX_DECLARATIONS_DEADLINES, InternalPartyPart.EBILLING_STATUSES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());

			final Taxpayer taxpayer = (Taxpayer) tiers;
			final List<EbillingStatus> efacture = taxpayer.getEbillingStatuses();
			assertNotNull(efacture);
			assertEquals(2, efacture.size());
		}

		// 3. et finalement on ne demande plus que les délais
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_DEADLINES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			// les états ont disparu
			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(0, etats.size());

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());
		}

		// 4. on demande à nouveau le tout
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_DECLARATIONS, InternalPartyPart.TAX_DECLARATIONS_STATUSES, InternalPartyPart.TAX_DECLARATIONS_DEADLINES, InternalPartyPart.EBILLING_STATUSES));
			assertNotNull(tiers);
			assertNotNull(tiers.getTaxDeclarations());
			assertEquals(1, tiers.getTaxDeclarations().size());

			final OrdinaryTaxDeclaration di = (OrdinaryTaxDeclaration) tiers.getTaxDeclarations().get(0);
			assertEquals(new Date(2003, 1, 1), di.getDateFrom());
			assertEquals(new Date(2003, 12, 31), di.getDateTo());

			final List<TaxDeclarationStatus> etats = di.getStatuses();
			assertNotNull(etats);
			assertEquals(1, etats.size());

			final TaxDeclarationStatus etat0 = etats.get(0);
			assertNotNull(etat0);
			assertEquals(new Date(2003, 1, 10), etat0.getDateFrom());
			assertEquals(TaxDeclarationStatusType.SENT, etat0.getType());

			final List<TaxDeclarationDeadline> delais = di.getDeadlines();
			assertNotNull(delais);
			assertEquals(1, delais.size());

			final Taxpayer taxpayer = (Taxpayer) tiers;
			final List<EbillingStatus> efacture = taxpayer.getEbillingStatuses();
			assertNotNull(efacture);
			assertEquals(2, efacture.size());
		}
	}

	/**
	 * Pour le renvoi des données en JSON, on modifie la donnée retournée par le cache avant de la renvoyer
	 * --> il faut donc vérifier que cette modification n'impacte pas le contenu du cache (autrement dit :
	 * que le cache nous renvoie toujours une copie de son contenu interne)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyModificationCollectionRenvoyeeEtValeurCachee() throws Exception {

		// 1. on demande le tiers avec les assujettissements
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_LIABILITIES));
			assertNotNull(tiers);
			assertEquals(NaturalPerson.class, tiers.getClass());

			final NaturalPerson np = (NaturalPerson) tiers;
			assertNotNull(np.getTaxLiabilities());
			assertEquals(1, np.getTaxLiabilities().size());

			final TaxLiability tl = np.getTaxLiabilities().get(0);
			assertNotNull(tl);
			assertEquals(OrdinaryResident.class, tl.getClass());
			assertEquals(LiabilityChangeReason.MAJORITY, tl.getStartReason());

			// ok, maintenant on modifie la collection des assujettissements en changeant la classe de l'élément (c'est ce que l'on fait
			// dans la manipulation JSON, au final)
			np.getTaxLiabilities().set(0, new ExpenditureBased(tl.getStartReason(), tl.getEndReason(), tl.getDateTo(), tl.getDateFrom(), null));
		}

		// 2. même appel -> rien ne doit avoir changé
		{
			final Party tiers = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_LIABILITIES));
			assertNotNull(tiers);
			assertEquals(NaturalPerson.class, tiers.getClass());

			final NaturalPerson np = (NaturalPerson) tiers;
			assertNotNull(np.getTaxLiabilities());
			assertEquals(1, np.getTaxLiabilities().size());

			final TaxLiability tl = np.getTaxLiabilities().get(0);
			assertNotNull(tl);
			assertEquals(OrdinaryResident.class, tl.getClass());
			assertEquals(LiabilityChangeReason.MAJORITY, tl.getStartReason());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetParties() throws Exception {

		// Etat initial : aucun appel au web-service
		assertEquals(0, getNumberOfCalls(calls));
		final List<Integer> partyNosMonsieurMadame = Arrays.asList(ids.monsieur.intValue(), ids.madame.intValue());

		// 1er appel
		{
			final Parties parties = cache.getParties(partyNosMonsieurMadame, null);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			// on vérifique que les données retournées sont correctes
			final Entry batch0 = parties.getEntries().get(0);
			final Entry batch1 = parties.getEntries().get(1);
			assertNotNull(batch0.getParty());
			assertNotNull(batch1.getParty());

			final Party monsieur = (batch0.getPartyNo() == ids.monsieur ? batch0.getParty() : batch1.getParty());
			final Party madame = (batch0.getPartyNo() == ids.madame ? batch0.getParty() : batch1.getParty());
			assertNoPart(monsieur);
			assertNoPart(madame);

			// on vérifie qu'il y a bien eu un appel au web-service
			assertEquals(1, getNumberOfCalls(calls));
			assertEquals(1, getNumberOfCallsToGetParties(calls));
			assertEquals(partyNosMonsieurMadame, getLastCallParametersToGetParties(calls).getLeft());
		}

		// 2ème appel : identique au premier
		{
			final Parties parties = cache.getParties(partyNosMonsieurMadame, null);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			// on vérifique que les données retournées sont correctes
			final Entry batch0 = parties.getEntries().get(0);
			final Entry batch1 = parties.getEntries().get(1);
			assertNotNull(batch0.getParty());
			assertNotNull(batch1.getParty());

			final Party monsieur = (batch0.getPartyNo() == ids.monsieur ? batch0.getParty() : batch1.getParty());
			final Party madame = (batch0.getPartyNo() == ids.madame ? batch0.getParty() : batch1.getParty());
			assertNoPart(monsieur);
			assertNoPart(madame);

			// on vérifie qu'il n'y a pas de second appel au web-service, c'est-à-dire que toutes les données ont été trouvées dans le cache
			assertEquals(1, getNumberOfCalls(calls));
			assertEquals(1, getNumberOfCallsToGetParties(calls));
		}

		// 3ème appel : avec un tiers de plus
		{
			final Parties parties = cache.getParties(Arrays.asList(ids.monsieur.intValue(), ids.madame.intValue(), ids.eric.intValue()), null);
			assertNotNull(parties);
			assertEquals(3, parties.getEntries().size());

			// on vérifique que les données retournées sont correctes
			Party monsieur = null;
			Party madame = null;
			Party eric = null;
			for (Entry entry : parties.getEntries()) {
				final Party party = entry.getParty();
				if (entry.getPartyNo() == ids.monsieur) {
					monsieur = party;
				}
				else if (entry.getPartyNo() == ids.madame) {
					madame = party;
				}
				else if (entry.getPartyNo() == ids.eric) {
					eric = party;
				}
				else {
					fail("Le batch contient un numéro de tiers inconnu = [" + entry.getPartyNo() + ']');
				}
			}
			assertNoPart(monsieur);
			assertNoPart(madame);
			assertNoPart(eric);

			// on vérifie qu'il y a un second appel au web-service, mais qu'il ne concerne que le tiers Eric
			assertEquals(2, getNumberOfCalls(calls));
			assertEquals(2, getNumberOfCallsToGetParties(calls));
			assertEquals(Collections.singletonList(ids.eric.intValue()), getLastCallParametersToGetParties(calls).getLeft());
		}
	}

	/**
	 * [SIFISC-28103] Vérifie que les appels concurrents sur getParties sont bien gérés et que les éléments liés aux parts n'apparaissent pas à double.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartiesConcurrent() throws Exception {

		// on utilise une implémentation du WS qui est assez lente pour que :
		//  1. les deux appels en parallèle ci-dessous trouvent chacun un cache vide au début de l'appel
		//  2. que le premier appel se termine bien avant le second
		//  3. que le second appel se trouve avec un cache partiellement (ou totalement) chargé au moment où le cache est mis-à-jour (rappel : le cache était vide au début des deux appels)
		// => avant correction du SIFISC-28103, la méthode 'cacheParties' ajoutait les parts du second appels aux parts déjà stockées par le premier appel et provoquait des doublons.
		cache.setTarget(new MockBusinessWebService(implementation) {
			@NotNull
			@Override
			public Parties getParties(List<Integer> partyNos, @Nullable Set<InternalPartyPart> parts) throws AccessDeniedException, ServiceException {
				try {
					// les deux appels doivent prendre du temps
					Thread.sleep(100);
					if (parts != null && parts.contains(InternalPartyPart.PARENTS)) {
						// le second appel doit prendre beaucoup de temps pour bien tester la méthode 'cacheParties'
						Thread.sleep(1000);
					}
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				return super.getParties(partyNos, parts);
			}
		});

		final List<Integer> idEric = Collections.singletonList(ids.eric.intValue());

		// Etat initial : aucun appel au web-service
		assertEquals(0, getNumberOfCalls(calls));

		final ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			// on lance deux appels en parallèle
			final CompletableFuture<Parties> futureParties1 = CompletableFuture.supplyAsync(() -> pushPrincipalAndGetParties(getDefaultOperateurName(), 22, idEric, EnumSet.of(InternalPartyPart.REAL_LAND_RIGHTS, InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS, InternalPartyPart.HOUSEHOLD_MEMBERS)), executor);
			final CompletableFuture<Parties> futureParties2 = CompletableFuture.supplyAsync(() -> pushPrincipalAndGetParties(getDefaultOperateurName(), 22, idEric, EnumSet.of(InternalPartyPart.REAL_LAND_RIGHTS, InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS, InternalPartyPart.HOUSEHOLD_MEMBERS, InternalPartyPart.PARENTS)), executor);

			// on vérifique que les données retournées sont correctes

			// 1er appel
			{
				final Parties parties = futureParties1.get();
				assertNotNull(parties);
				assertEquals(1, parties.getEntries().size());
				final NaturalPerson eric = (NaturalPerson) parties.getEntries().get(0).getParty();
				assertLandRightsEric(eric, ids);
			}

			// 2ème appel : identique au premier
			{
				final Parties parties = futureParties2.get();
				assertNotNull(parties);
				assertEquals(1, parties.getEntries().size());
				final NaturalPerson eric = (NaturalPerson) parties.getEntries().get(0).getParty();
				assertLandRightsEric(eric, ids);
			}
		}
		finally {
			executor.shutdown();
		}

		// on fait un appel synchrone supplémentaire pour vérifier que le cache est toujours cohérent
		{
			final Parties parties = getParties(idEric, EnumSet.of(InternalPartyPart.REAL_LAND_RIGHTS, InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS, InternalPartyPart.HOUSEHOLD_MEMBERS));
			assertNotNull(parties);
			assertEquals(1, parties.getEntries().size());
			final NaturalPerson eric = (NaturalPerson) parties.getEntries().get(0).getParty();
			// Note: avant la correction du SIFISC-28103, on se trouvait avec les droits virtuels à double
			assertLandRightsEric(eric, ids);
		}
	}

	private static void assertLandRightsEric(NaturalPerson eric, @NotNull Ids ids) {

		// on vérifie que que les données retournées sont correctes
		final List<LandRight> landRights = eric.getLandRights();
		assertEquals(2, landRights.size());

		// le droit de propriété réel
		final LandOwnershipRight landRight0 = (LandOwnershipRight) landRights.get(0);
		assertEquals(new Date(2004, 4, 12), landRight0.getDateFrom());
		assertNull(landRight0.getDateTo());
		assertEquals(ids.immeuble0.longValue(), landRight0.getImmovablePropertyId());

		// le droit de propriété virtuel
		final VirtualTransitiveLandRight landRight1 = (VirtualTransitiveLandRight) landRights.get(1);
		assertEquals(new Date(2010, 1, 1), landRight1.getDateFrom());
		assertNull(landRight1.getDateTo());
		assertEquals(ids.immeuble1.longValue(), landRight1.getImmovablePropertyId());
	}

	private Parties pushPrincipalAndGetParties(@NotNull String principal, int oid, @NotNull List<Integer> ids, @Nullable Set<InternalPartyPart> parts) {
		AuthenticationHelper.pushPrincipal(principal, oid);
		try {
			return getParties(ids, parts);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private Parties getParties(@NotNull List<Integer> ids, @Nullable Set<InternalPartyPart> parts) {
		try {
			return cache.getParties(ids, parts);
		}
		catch (AccessDeniedException | ServiceException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Le cache est systématiquemene ignoré pour les appels qui font référence à des <i>parts</i> non-gérées par le cache
	 */
	@Test
	public void testGetPartiesOnNonCacheablePart() throws Exception {

		// état initial -> aucun appel au web-service
		assertEquals(0, getNumberOfCalls(calls));

		// parts to ask for
		final EnumSet<InternalPartyPart> parts = EnumSet.of(InternalPartyPart.TAX_RESIDENCES, InternalPartyPart.EBILLING_STATUSES, InternalPartyPart.ADDRESSES);

		// parties to ask those parts on
		final List<Integer> partyNos = Arrays.asList(ids.menage.intValue(), ids.eric.intValue());

		// 1er appel
		{
			final Parties parties = cache.getParties(partyNos, parts);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			assertEquals(1, getNumberOfCallsToGetParties(calls));
			final Pair<List<Integer>, Set<InternalPartyPart>> lastCall = getLastCallParametersToGetParties(calls);
			assertEquals(partyNos, lastCall.getLeft());
			assertEquals(parts, lastCall.getRight());

			final List<Entry> sortedEntries = new ArrayList<>(parties.getEntries());
			Collections.sort(sortedEntries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			// eric d'abord
			{
				final Party party = sortedEntries.get(0).getParty();
				assertNotNull(party);
				assertEquals(ids.eric.intValue(), party.getNumber());

				assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}

			// ménage commun ensuite
			{
				final Party party = sortedEntries.get(1).getParty();
				assertNotNull(party);
				assertEquals(ids.menage.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}
		}

		// 2ème appel
		{
			final Parties parties = cache.getParties(partyNos, parts);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			assertEquals(2, getNumberOfCallsToGetParties(calls));
			final Pair<List<Integer>, Set<InternalPartyPart>> lastCall = getLastCallParametersToGetParties(calls);
			assertEquals(partyNos, lastCall.getLeft());
			assertEquals(parts, lastCall.getRight());

			final List<Entry> sortedEntries = new ArrayList<>(parties.getEntries());
			Collections.sort(sortedEntries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			// eric d'abord
			{
				final Party party = sortedEntries.get(0).getParty();
				assertNotNull(party);
				assertEquals(ids.eric.intValue(), party.getNumber());

				assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}

			// ménage commun ensuite
			{
				final Party party = sortedEntries.get(1).getParty();
				assertNotNull(party);
				assertEquals(ids.menage.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}
		}

		// 3ème appel
		{
			final Parties parties = cache.getParties(partyNos, parts);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());

			assertEquals(3, getNumberOfCallsToGetParties(calls));
			final Pair<List<Integer>, Set<InternalPartyPart>> lastCall = getLastCallParametersToGetParties(calls);
			assertEquals(partyNos, lastCall.getLeft());
			assertEquals(parts, lastCall.getRight());

			final List<Entry> sortedEntries = new ArrayList<>(parties.getEntries());
			Collections.sort(sortedEntries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			// eric d'abord
			{
				final Party party = sortedEntries.get(0).getParty();
				assertNotNull(party);
				assertEquals(ids.eric.intValue(), party.getNumber());

				assertEquals(2, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}

			// ménage commun ensuite
			{
				final Party party = sortedEntries.get(1).getParty();
				assertNotNull(party);
				assertEquals(ids.menage.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}
		}

		// appel sans la part non-gérée -> là le cache intervient sans problème
		{
			final Set<InternalPartyPart> cachedParts = EnumSet.copyOf(parts);
			cachedParts.remove(InternalPartyPart.EBILLING_STATUSES);

			final Parties parties = cache.getParties(partyNos, cachedParts);
			assertNotNull(parties);
			assertEquals(2, parties.getEntries().size());
			assertEquals(3, getNumberOfCallsToGetParties(calls));       // <- = pas de nouvel appel, car tout ce qui est demandé est déjà dans le cache

			final List<Entry> sortedEntries = new ArrayList<>(parties.getEntries());
			Collections.sort(sortedEntries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			// eric d'abord
			{
				final Party party = sortedEntries.get(0).getParty();
				assertNotNull(party);
				assertEquals(ids.eric.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}

			// ménage commun ensuite
			{
				final Party party = sortedEntries.get(1).getParty();
				assertNotNull(party);
				assertEquals(ids.menage.intValue(), party.getNumber());

				assertEquals(0, ((Taxpayer) party).getEbillingStatuses().size());
				assertEquals(1, party.getMainTaxResidences().size());
				assertEquals(1, party.getOtherTaxResidences().size());
				assertEquals(1, party.getResidenceAddresses().size());
			}
		}
	}

	/**
	 * [UNIREG-3288] Vérifie que les exceptions levées dans la méthode getBatchParty sont correctement gérées au niveau du cache.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartiesWithExceptionOnTiers() throws Exception {

		// on intercale une implémentation du web-service qui lèvera une exception lors de la récupération de madame
		cache.setTarget(new BusinessWebServiceCrashingWrapper(implementation, ids.madame.intValue()));

		// 1er appel : monsieur est correctement récupéré et une exception est retournée à la place de madame.
		{
			final Parties batch = cache.getParties(Arrays.asList(ids.monsieur.intValue(), ids.madame.intValue()), null);
			assertNotNull(batch);
			assertNotNull(batch.getEntries());
			assertEquals(2, batch.getEntries().size());

			// on vérifie que : monsieur est bien retourné et qu'une exception a été levée sur madame
			final Entry batch0 = batch.getEntries().get(0);
			final Entry batch1 = batch.getEntries().get(1);

			final Entry entryMonsieur = (batch0.getPartyNo() == ids.monsieur ? batch0 : batch1);
			assertNotNull(entryMonsieur.getParty());

			final Entry entryMadame = (batch0.getPartyNo() == ids.madame ? batch0 : batch1);
			assertNull(entryMadame.getParty());
			assertEquals("Boom badaboom !!", entryMadame.getError().getErrorMessage());

			// on vérifie que : seul monsieur est stocké dans le cache et que madame n'y est pas (parce qu'une exception a été levée)
			assertNotNull(getCacheValue(ids.monsieur));
			assertNull(getCacheValue(ids.madame));
		}

		// 2ème appel : identique au premier pour vérifier que le cache est dans un état cohérent (provoquait un crash avant la correction de UNIREG-3288)
		{
			final Parties batch = cache.getParties(Arrays.asList(ids.monsieur.intValue(), ids.madame.intValue()), null);
			assertNotNull(batch);
			assertEquals(2, batch.getEntries().size());

			// on vérifie que : monsieur est bien retourné et qu'une exception a été levée sur madame
			final Entry batch0 = batch.getEntries().get(0);
			final Entry batch1 = batch.getEntries().get(1);

			final Entry entryMonsieur = (batch0.getPartyNo() == ids.monsieur ? batch0 : batch1);
			assertNotNull(entryMonsieur.getParty());

			final Entry entryMadame = (batch0.getPartyNo() == ids.madame ? batch0 : batch1);
			assertNull(entryMadame.getParty());
			assertEquals("Boom badaboom !!", entryMadame.getError().getErrorMessage());

			// on vérifie que : seul monsieur est stocké dans le cache et que madame n'y est pas (parce qu'une exception a été levée)
			assertNotNull(getCacheValue(ids.monsieur));
			assertNull(getCacheValue(ids.madame));
		}
	}

	/**
	 * Ce n'est pas parce qu'une personne est dans le cache que tout le monde peut la voir !!!
	 */
	@Test
	public void testGetPartiesEtDroitAcces() throws Exception {

		final class Ids {
			int pp;
			int dpi;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Constantin", "Dugenou", null, Sexe.MASCULIN);
				addDroitAcces("zai1232", pp, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(2014, 1, 1), null);
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));

				final Ids ids = new Ids();
				ids.pp = pp.getNumero().intValue();
				ids.dpi = dpi.getNumero().intValue();
				return ids;
			}
		});

		// premier appel avec celui qui a le droit de tout voir -> mise en cache ok
		{
			final Parties parties = cache.getParties(Arrays.asList(ids.pp, ids.dpi), null);
			assertNotNull(parties);
			assertNotNull(parties.getEntries());
			assertEquals(2, parties.getEntries().size());

			assertNotNull(getCacheValue(ids.pp));
		}

		// deuxième appel avec celui qui n'a pas le droit de voir -> devrait être bloqué
		AuthenticationHelper.pushPrincipal("TOTO", 22);
		try
		{
			final Parties parties = cache.getParties(Arrays.asList(ids.pp, ids.dpi), null);
			assertNotNull(parties);
			assertNotNull(parties.getEntries());
			assertEquals(2, parties.getEntries().size());

			final List<Entry> entries = new ArrayList<>(parties.getEntries());
			Collections.sort(entries, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			{
				final Entry e = entries.get(0);
				assertEquals(ids.dpi, e.getPartyNo());
				assertNull(e.getError());
				assertNotNull(e.getParty());
				assertEquals(ids.dpi, e.getParty().getNumber());
			}
			{
				final Entry e = entries.get(1);
				assertEquals(ids.pp, e.getPartyNo());
				assertNull(e.getParty());
				assertNotNull(e.getError());
				assertEquals("L'utilisateur TOTO/22 ne possède aucun droit de lecture sur le dossier " + ids.pp, e.getError().getErrorMessage());
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	/**
	 * [SIFISC-5508] Vérifie que la date de début d'activité (activityStartDate) est bien cachée correctement.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetPartyActivityStartDate() throws Exception {

		// 1. on demande le tiers une première fois
		{
			final Party party = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_RESIDENCES));
			assertNotNull(party);
			assertEquals(new Date(1983, 4, 13), party.getActivityStartDate());
			assertNull(party.getActivityEndDate());
		}

		// 2. on demande le tiers une seconde fois
		{
			final Party party = cache.getParty(ids.eric.intValue(), EnumSet.of(InternalPartyPart.TAX_RESIDENCES));
			assertNotNull(party);
			assertEquals(new Date(1983, 4, 13), party.getActivityStartDate()); // [SIFISC-5508] cette date était nulle avant la correction du bug
			assertNull(party.getActivityEndDate());
		}
	}

	/**
	 * [SIFISC-20870] Problème vu en production, une java.util.ConcurrentModificationException qui saute pendant la sérialisation CXF de réponse du WS
	 */
	@Test
	public void testConcurrentAccessGetParty() throws Exception {

		// on va construire un contribuable avec toutes ses collections contenant quelque chose,
		// puis on va interroger depuis plusieurs threads le contribuable, avec un nombre de parts aléatoire à chaque fois...

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				// Un tiers avec avec toutes les parties renseignées
				final PersonnePhysique eric = addNonHabitant("Eric", "de Melniboné", date(1965, 4, 13), Sexe.MASCULIN);
				addAdresseSuisse(eric, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
				eric.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH9308440717427290198", null));

				final PersonnePhysique pupille = addNonHabitant("Slobodan", "Pupille", date(1987, 7, 23), Sexe.MASCULIN);
				addTutelle(pupille, eric, null, date(2005, 7, 1), null);

				final SituationFamillePersonnePhysique situation = new SituationFamillePersonnePhysique();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				eric.addSituationFamille(situation);

				final PeriodeFiscale periode = addPeriodeFiscale(2004);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				ch.vd.unireg.declaration.DeclarationImpotOrdinaire di = addDeclarationImpot(eric, periode, date(2004, 1, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(di, date(2004, 1, 10));

				return eric.getNumero();
			}
		});

		final Random rnd = new Random();
		final ch.vd.unireg.ws.party.v7.ObjectFactory partyFactory = new ch.vd.unireg.ws.party.v7.ObjectFactory();
		final JAXBContext jaxbContext = JAXBContext.newInstance(NaturalPerson.class);

		// lien direct entre le cache et l'implémentation, sans tracing
		cache.setTarget(getBean(BusinessWebService.class, "wsv7Business"));

		// classe de tâche en parallèle...
		final class Task implements Callable<Object> {

			private final Set<InternalPartyPart> parts = buildRandomPartSet();

			/**
			 * Construction d'un ensemble de parts prises au hasard
			 */
			private Set<InternalPartyPart> buildRandomPartSet() {
				final InternalPartyPart[] all = InternalPartyPart.values();
				final int nb = 3;
				final Set<InternalPartyPart> set = EnumSet.noneOf(InternalPartyPart.class);
				for (int i = 0; i < nb ; ++ i) {
					final int index = rnd.nextInt(all.length);
					set.add(all[index]);
				}
				return set;
			}

			@Override
			public Object call() throws Exception {
				// récupération des données + sérialisation
				final Party party = cache.getParty((int) id, parts);
				final Marshaller marshaller = jaxbContext.createMarshaller();
				try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					marshaller.marshal(partyFactory.createParty(party), out);
				}
				return null;
			}
		}

		// quelques threads, quelques appels
		final int nbThreads = 100;
		final int nbCalls = 5000;

		// executors
		final ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
		try {
			final ExecutorCompletionService<Object> completionService = new ExecutorCompletionService<>(executor);
			for (int i = 0 ; i < nbCalls ; ++ i) {
				completionService.submit(new Task());
			}
			executor.shutdown();
			for (int i = 0 ; i < nbCalls ; ++ i) {
				while (true) {
					final Future<Object> future = completionService.poll(1, TimeUnit.SECONDS);
					if (future != null) {
						future.get();
						break;
					}
				}
			}
		}
		finally {
			executor.shutdownNow();
			while (!executor.isTerminated()) {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * [SIFISC-20870] Problème vu en production, une java.util.ConcurrentModificationException qui saute pendant la sérialisation CXF de réponse du WS
	 */
	@Test
	public void testConcurrentAccessGetParties() throws Exception {

		// on va construire un contribuable avec toutes ses collections contenant quelque chose,
		// puis on va interroger depuis plusieurs threads le contribuable, avec un nombre de parts aléatoire à chaque fois...

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				// Un tiers avec avec toutes les parties renseignées
				final PersonnePhysique eric = addNonHabitant("Eric", "de Melniboné", date(1965, 4, 13), Sexe.MASCULIN);
				addAdresseSuisse(eric, TypeAdresseTiers.COURRIER, date(1983, 4, 13), null, MockRue.Lausanne.AvenueDeBeaulieu);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
				eric.addCoordonneesFinancieres(new CoordonneesFinancieres(null, "CH9308440717427290198", null));

				final PersonnePhysique pupille = addNonHabitant("Slobodan", "Pupille", date(1987, 7, 23), Sexe.MASCULIN);
				addTutelle(pupille, eric, null, date(2005, 7, 1), null);

				final SituationFamillePersonnePhysique situation = new SituationFamillePersonnePhysique();
				situation.setDateDebut(date(1989, 5, 1));
				situation.setNombreEnfants(0);
				eric.addSituationFamille(situation);

				final PeriodeFiscale periode = addPeriodeFiscale(2004);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				ch.vd.unireg.declaration.DeclarationImpotOrdinaire di = addDeclarationImpot(eric, periode, date(2004, 1, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(di, date(2004, 1, 10));

				return eric.getNumero();
			}
		});

		final Random rnd = new Random();
		final JAXBContext jaxbContext = JAXBContext.newInstance(Parties.class);

		// lien direct entre le cache et l'implémentation, sans tracing
		cache.setTarget(getBean(BusinessWebService.class, "wsv7Business"));

		// classe de tâche en parallèle...
		final class Task implements Callable<Object> {

			private final Set<InternalPartyPart> parts = buildRandomPartSet();

			/**
			 * Construction d'un ensemble de parts prises au hasard
			 */
			private Set<InternalPartyPart> buildRandomPartSet() {
				final InternalPartyPart[] all = InternalPartyPart.values();
				final int nb = 3;
				final Set<InternalPartyPart> set = EnumSet.noneOf(InternalPartyPart.class);
				for (int i = 0; i < nb ; ++ i) {
					final int index = rnd.nextInt(all.length);
					set.add(all[index]);
				}
				return set;
			}

			@Override
			public Object call() throws Exception {
				// récupération des données + sérialisation
				final Parties parties = pushPrincipalAndGetParties(getDefaultOperateurName(), 22, Collections.singletonList((int) id), parts);
				final Marshaller marshaller = jaxbContext.createMarshaller();
				try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					marshaller.marshal(parties, out);
				}
				return null;
			}
		}

		// quelques threads, quelques appels
		final int nbThreads = 100;
		final int nbCalls = 5000;

		// executors
		final ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
		try {
			final ExecutorCompletionService<Object> completionService = new ExecutorCompletionService<>(executor);
			for (int i = 0 ; i < nbCalls ; ++ i) {
				completionService.submit(new Task());
			}
			executor.shutdown();
			for (int i = 0 ; i < nbCalls ; ++ i) {
				while (true) {
					final Future<Object> future = completionService.poll(1, TimeUnit.SECONDS);
					if (future != null) {
						future.get();
						break;
					}
				}
			}
		}
		finally {
			executor.shutdownNow();
			while (!executor.isTerminated()) {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			}
		}
	}

	@Test
	public void testGetImmovableProperties() throws Exception {

		// 1er appel : on demande les immeubles 0 et 1
		calls.clear();
		final ImmovablePropertyList list1 = cache.getImmovableProperties(Arrays.asList(ids.immeuble0, ids.immeuble1));
		assertEquals(2, list1.getEntries().size());
		assertFoundEntry(ids.immeuble0, list1.getEntries().get(0));
		assertFoundEntry(ids.immeuble1, list1.getEntries().get(1));

		// les immeubles n'étaient pas dans le cache, on doit donc voir l'appel sur le service
		assertEquals(Arrays.asList(ids.immeuble0, ids.immeuble1), getLastCallParametersToGetImmovableProperties(calls));

		// 2ème appel : on demande les immeubles 1 et 2
		calls.clear();
		final ImmovablePropertyList list2 = cache.getImmovableProperties(Arrays.asList(ids.immeuble1, ids.immeuble2));
		assertEquals(2, list2.getEntries().size());
		assertFoundEntry(ids.immeuble1, list2.getEntries().get(0));
		assertFoundEntry(ids.immeuble2, list2.getEntries().get(1));

		// l'immeuble 1 était dans le cache, on doit donc uniquement voir l'appel sur l'immeuble 2
		assertEquals(Collections.singletonList(ids.immeuble2), getLastCallParametersToGetImmovableProperties(calls));

		// 3ème appel : on demande tous les immeubles
		calls.clear();
		final ImmovablePropertyList list3 = cache.getImmovableProperties(Arrays.asList(ids.immeuble0, ids.immeuble1, ids.immeuble2));
		assertEquals(3, list3.getEntries().size());
		assertFoundEntry(ids.immeuble0, list3.getEntries().get(0));
		assertFoundEntry(ids.immeuble1, list3.getEntries().get(1));
		assertFoundEntry(ids.immeuble2, list3.getEntries().get(2));

		// tous les immeubles étaient dans le cache, on ne doit donc voir aucun appel
		assertTrue(calls.isEmpty());
	}

	@Test
	public void testGetBuildings() throws Exception {

		// 1er appel : on demande les bâtiments 0 et 1
		calls.clear();
		final BuildingList list1 = cache.getBuildings(Arrays.asList(ids.batiment0, ids.batiment1));
		assertEquals(2, list1.getEntries().size());
		assertFoundEntry(ids.batiment0, list1.getEntries().get(0));
		assertFoundEntry(ids.batiment1, list1.getEntries().get(1));

		// les bâtiments n'étaient pas dans le cache, on doit donc voir l'appel sur le service
		assertEquals(Arrays.asList(ids.batiment0, ids.batiment1), getLastCallParametersToGetBuildings(calls));

		// 2ème appel : on demande les bâtiments 1 et 2
		calls.clear();
		final BuildingList list2 = cache.getBuildings(Arrays.asList(ids.batiment1, ids.batiment2));
		assertEquals(2, list2.getEntries().size());
		assertFoundEntry(ids.batiment1, list2.getEntries().get(0));
		assertFoundEntry(ids.batiment2, list2.getEntries().get(1));

		// l'batiment 1 était dans le cache, on doit donc uniquement voir l'appel sur le batiment 2
		assertEquals(Collections.singletonList(ids.batiment2), getLastCallParametersToGetBuildings(calls));

		// 3ème appel : on demande tous les bâtiments
		calls.clear();
		final BuildingList list3 = cache.getBuildings(Arrays.asList(ids.batiment0, ids.batiment1, ids.batiment2));
		assertEquals(3, list3.getEntries().size());
		assertFoundEntry(ids.batiment0, list3.getEntries().get(0));
		assertFoundEntry(ids.batiment1, list3.getEntries().get(1));
		assertFoundEntry(ids.batiment2, list3.getEntries().get(2));

		// tous les bâtiments étaient dans le cache, on ne doit donc voir aucun appel
		assertTrue(calls.isEmpty());
	}

	@Test
	public void testGetCommunitiesOfOwners() throws Exception {

		// 1er appel : on demande les communautés 0 et 1
		calls.clear();
		final CommunityOfOwnersList list1 = cache.getCommunitiesOfOwners(Arrays.asList(ids.communaute0, ids.communaute1));
		assertEquals(2, list1.getEntries().size());
		assertFoundEntry(ids.communaute0, list1.getEntries().get(0));
		assertFoundEntry(ids.communaute1, list1.getEntries().get(1));

		// les communautés n'étaient pas dans le cache, on doit donc voir l'appel sur le service
		assertEquals(Arrays.asList(ids.communaute0, ids.communaute1), getLastCallParametersToGetCommunitiesOfOwners(calls));

		// 2ème appel : on demande les communautés 1 et 2
		calls.clear();
		final CommunityOfOwnersList list2 = cache.getCommunitiesOfOwners(Arrays.asList(ids.communaute1, ids.communaute2));
		assertEquals(2, list2.getEntries().size());
		assertFoundEntry(ids.communaute1, list2.getEntries().get(0));
		assertFoundEntry(ids.communaute2, list2.getEntries().get(1));

		// l'communaute 1 était dans le cache, on doit donc uniquement voir l'appel sur le communaute 2
		assertEquals(Collections.singletonList(ids.communaute2), getLastCallParametersToGetCommunitiesOfOwners(calls));

		// 3ème appel : on demande toutes les communautés
		calls.clear();
		final CommunityOfOwnersList list3 = cache.getCommunitiesOfOwners(Arrays.asList(ids.communaute0, ids.communaute1, ids.communaute2));
		assertEquals(3, list3.getEntries().size());
		assertFoundEntry(ids.communaute0, list3.getEntries().get(0));
		assertFoundEntry(ids.communaute1, list3.getEntries().get(1));
		assertFoundEntry(ids.communaute2, list3.getEntries().get(2));

		// toutes les communautés étaient dans le cache, on ne doit donc voir aucun appel
		assertTrue(calls.isEmpty());
	}

	private GetPartyValue getCacheValue(long tiersNumber) {
		GetPartyValue value = null;
		final GetPartyKey key = new GetPartyKey(tiersNumber);
		final Element element = ehcache.get(key);
		if (element != null) {
			value = (GetPartyValue) element.getObjectValue();
		}
		return value;
	}

	private DebtorInfo getCacheValue(int debtorId, int pf) {
		DebtorInfo value = null;
		final GetDebtorInfoKey key = new GetDebtorInfoKey(debtorId, pf);
		final Element element = ehcache.get(key);
		if (element != null) {
			value = (DebtorInfo) element.getObjectValue();
		}
		return value;
	}

	/**
	 * Assert que la partie spécifiée et uniquement celle-ci est renseignée sur le tiers.
	 */
	private static void assertOnlyPart(InternalPartyPart p, Party tiers) {

		boolean checkAddresses = InternalPartyPart.ADDRESSES == p;
		boolean checkTaxLiabilities = InternalPartyPart.TAX_LIABILITIES == p;
		boolean checkSimplifiedTaxLiabilities = InternalPartyPart.SIMPLIFIED_TAX_LIABILITIES == p;
		boolean checkHouseholdMembers = InternalPartyPart.HOUSEHOLD_MEMBERS == p;
		boolean checkBankAccounts = InternalPartyPart.BANK_ACCOUNTS == p;
		boolean checkTaxDeclarations = InternalPartyPart.TAX_DECLARATIONS == p;
		boolean checkTaxDeclarationsStatuses = InternalPartyPart.TAX_DECLARATIONS_STATUSES == p;
		boolean checkTaxDeclarationsDeadlines = InternalPartyPart.TAX_DECLARATIONS_DEADLINES == p;
		boolean checkTaxResidences = InternalPartyPart.TAX_RESIDENCES == p;
		boolean checkVirtualTaxResidences = InternalPartyPart.VIRTUAL_TAX_RESIDENCES == p;
		boolean checkManagingTaxResidences = InternalPartyPart.MANAGING_TAX_RESIDENCES == p;
		boolean checkTaxationPeriods = InternalPartyPart.TAXATION_PERIODS == p;
		boolean checkRelationsBetweenParties = InternalPartyPart.RELATIONS_BETWEEN_PARTIES == p;
		boolean checkFamilyStatuses = InternalPartyPart.FAMILY_STATUSES == p;
		boolean checkCapitals = InternalPartyPart.CAPITALS == p;
		boolean checkTaxLightenings = InternalPartyPart.TAX_LIGHTENINGS == p;
		boolean checkLegalForms = InternalPartyPart.LEGAL_FORMS == p;
		boolean checkTaxSystems = InternalPartyPart.TAX_SYSTEMS == p;
		boolean checkLegalSeats = InternalPartyPart.LEGAL_SEATS == p;
		boolean checkDebtorPeriodicities = InternalPartyPart.DEBTOR_PERIODICITIES == p;
		boolean checkImmovableProperties = InternalPartyPart.IMMOVABLE_PROPERTIES == p;		// [SIFISC-26536] la part IMMOVABLE_PROPERTIES est dépréciée et n'a aucun effet
		boolean checkChildren = InternalPartyPart.CHILDREN == p;
		boolean checkParents = InternalPartyPart.PARENTS == p;
		boolean checkWithholdingTaxDeclarationPeriods = InternalPartyPart.WITHHOLDING_TAXATION_PERIODS == p;
		boolean checkEbillingStatuses = InternalPartyPart.EBILLING_STATUSES == p;
		boolean checkCorporationStatuses = InternalPartyPart.CORPORATION_STATUSES == p;
		boolean checkBusinessYears = InternalPartyPart.BUSINESS_YEARS == p;
		boolean checkCorporationFlags = InternalPartyPart.CORPORATION_FLAGS == p;
		boolean checkAgents = InternalPartyPart.AGENTS == p;
		boolean checkLabels = InternalPartyPart.LABELS == p;
		boolean checkRealLandRights = InternalPartyPart.REAL_LAND_RIGHTS == p;
		boolean checkVirtualTransitiveLandRights = InternalPartyPart.VIRTUAL_TRANSITIVE_LAND_RIGHTS == p;
		boolean checkVirtualInheritedRealLandRights = InternalPartyPart.VIRTUAL_INHERITED_REAL_LAND_RIGHTS == p;
		boolean checkVirtualInheritedVirtuelLandRights = InternalPartyPart.VIRTUAL_INHERITED_VIRTUAL_LAND_RIGHTS == p;
		boolean checkResidencyPeriods = InternalPartyPart.RESIDENCY_PERIODS == p;
		boolean checkLandTaxLightenings = InternalPartyPart.LAND_TAX_LIGHTENINGS == p;
		boolean checkInheritanceRelationships = InternalPartyPart.INHERITANCE_RELATIONSHIPS == p;
		boolean checkOperatingPeriods = InternalPartyPart.OPERATING_PERIODS == p;
		boolean checkVirtualLandTaxLightenings = InternalPartyPart.VIRTUAL_LAND_TAX_LIGHTENINGS == p;
		boolean checkPartnerRelationship = InternalPartyPart.PARTNER_RELATIONSHIP == p;

		assertNotNull(tiers);
		assertTrue("La partie [" + p + "] est inconnue",
		                  checkAddresses || checkTaxLiabilities || checkSimplifiedTaxLiabilities || checkHouseholdMembers || checkBankAccounts || checkTaxDeclarations || checkTaxDeclarationsStatuses || checkTaxDeclarationsDeadlines
				                  || checkTaxResidences || checkVirtualTaxResidences || checkManagingTaxResidences || checkTaxationPeriods || checkRelationsBetweenParties || checkFamilyStatuses || checkCapitals
				                  || checkTaxLightenings || checkLegalForms || checkTaxSystems || checkLegalSeats || checkDebtorPeriodicities || checkImmovableProperties || checkBusinessYears || checkCorporationFlags
				                  || checkChildren || checkParents || checkWithholdingTaxDeclarationPeriods || checkEbillingStatuses || checkCorporationStatuses || checkAgents || checkLabels
				                  || checkRealLandRights || checkVirtualTransitiveLandRights || checkResidencyPeriods || checkLandTaxLightenings || checkInheritanceRelationships || checkVirtualInheritedRealLandRights ||
				                  checkVirtualInheritedVirtuelLandRights || checkOperatingPeriods || checkVirtualLandTaxLightenings||checkPartnerRelationship);

		assertNullOrNotNull(checkAddresses, tiers.getMailAddresses(), "mailAddresses" + "(" + p + ")");
		assertNullOrNotNull(checkAddresses, tiers.getResidenceAddresses(), "residenceAddresses" + "(" + p + ")");
		assertNullOrNotNull(checkAddresses, tiers.getDebtProsecutionAddresses(), "debtProsecutionAddresses" + "(" + p + ")");
		assertNullOrNotNull(checkAddresses, tiers.getRepresentationAddresses(), "representationAddresses" + "(" + p + ")");
		assertNullOrNotNull(checkBankAccounts, tiers.getBankAccounts(), "bankAccounts" + "(" + p + ")");
		assertNullOrNotNull(checkTaxResidences || checkVirtualTaxResidences, tiers.getMainTaxResidences(), "mainTaxResidences" + "(" + p + ")");
		assertNullOrNotNull(checkTaxResidences || checkVirtualTaxResidences, tiers.getOtherTaxResidences(), "otherTaxResidences" + "(" + p + ")");

		if (tiers instanceof Taxpayer) {
			final Taxpayer ctb = (Taxpayer) tiers;
			assertNullOrNotNull(checkTaxLiabilities, ctb.getTaxLiabilities(), "taxLiabilities" + "(" + p + ")");
			assertNullOrNotNull(checkSimplifiedTaxLiabilities, ctb.getSimplifiedTaxLiabilityVD(), "simplifiedTaxLiabilityVD" + "(" + p + ")");
			assertNullOrNotNull(checkSimplifiedTaxLiabilities, ctb.getSimplifiedTaxLiabilityCH(), "simplifiedTaxLiabilityCH" + "(" + p + ")");
			assertNullOrNotNull(checkTaxDeclarations || checkTaxDeclarationsStatuses || checkTaxDeclarationsDeadlines, ctb.getTaxDeclarations(), "taxDeclarations" + "(" + p + ")");
			assertNullOrNotNull(checkTaxationPeriods, ctb.getTaxationPeriods(), "taxationPeriods" + "(" + p + ")");
			assertEmpty(ctb.getImmovableProperties());
			assertNullOrNotNull(checkEbillingStatuses, ctb.getEbillingStatuses(), "ebillingStatuses" + "(" + p + ")");
			assertNullOrNotNull(checkAgents, ctb.getAgents(), "agents" + "(" + p + ")");
		}

		if (tiers instanceof Debtor) {
			assertNullOrNotNull(checkRelationsBetweenParties, tiers.getRelationsBetweenParties(), ("relationsBetweenParties (" + p + ')') + "(" + p + ")");
		}

		if (tiers instanceof CommonHousehold) {
			final CommonHousehold mc = (CommonHousehold) tiers;
			assertNullOrNotNull(checkRelationsBetweenParties, tiers.getRelationsBetweenParties(), ("relationsBetweenParties (" + p + ')') + "(" + p + ")");
			assertNullOrNotNull(checkManagingTaxResidences, mc.getManagingTaxResidences(), "managingTaxResidences" + "(" + p + ")");
			assertNullOrNotNull(checkFamilyStatuses, mc.getFamilyStatuses(), "familyStatuses" + "(" + p + ")");
			assertNullOrNotNull(checkHouseholdMembers, mc.getMainTaxpayer(), "mainTaxpayer" + "(" + p + ")");
			assertNullOrNotNull(checkHouseholdMembers, mc.getSecondaryTaxpayer(), "secondaryTaxpayer" + "(" + p + ")");
		}

		if (tiers instanceof Corporation) {
			final Corporation pm = (Corporation) tiers;
			assertNullOrNotNull(checkRelationsBetweenParties, tiers.getRelationsBetweenParties(), ("relationsBetweenParties (" + p + ')') + "(" + p + ")");
			assertNullOrNotNull(checkCapitals, pm.getCapitals(), "capitals" + "(" + p + ")");
			assertNullOrNotNull(checkTaxLightenings, pm.getTaxLightenings(), "taxLightenings" + "(" + p + ")");
			assertNullOrNotNull(checkLegalForms, pm.getLegalForms(), "legalForms" + "(" + p + ")");
			assertNullOrNotNull(checkTaxSystems, pm.getTaxSystemsVD(), "taxSystemsVD" + "(" + p + ")");
			assertNullOrNotNull(checkTaxSystems, pm.getTaxSystemsCH(), "taxSystemsCH" + "(" + p + ")");
			assertNullOrNotNull(checkLegalSeats, pm.getLegalSeats(), "legalSeats" + "(" + p + ")");
			assertNullOrNotNull(checkBusinessYears, pm.getBusinessYears(), "businessYears" + "(" + p + ")");
			assertNullOrNotNull(checkRealLandRights || checkVirtualTransitiveLandRights || checkVirtualInheritedRealLandRights || checkVirtualInheritedVirtuelLandRights, pm.getLandRights(), "landRights" + "(" + p + ")");
			assertNullOrNotNull(checkLandTaxLightenings, pm.getIfoncExemptions(), "ifoncExemptions" + "(" + p + ")");
			assertNullOrNotNull(checkLandTaxLightenings, pm.getIciAbatements(), "iciAbatements" + "(" + p + ")");
			assertNullOrNotNull(checkOperatingPeriods, pm.getOperatingPeriods(), "operatingPeriods" + "(" + p + ")");
			assertNullOrNotNull(checkVirtualLandTaxLightenings, pm.getVirtualLandTaxLightenings(), "virtualLandTaxLightenings" + "(" + p + ")");
		}

		if (tiers instanceof NaturalPerson) {
			final NaturalPerson np = (NaturalPerson) tiers;
			assertNullOrNotNull(checkRelationsBetweenParties || checkChildren || checkParents || checkInheritanceRelationships, tiers.getRelationsBetweenParties(), ("relationsBetweenParties (" + p + ')') + "(" + p + ")");
			assertNullOrNotNull(checkManagingTaxResidences, np.getManagingTaxResidences(), "managingTaxResidences" + "(" + p + ")");
			assertNullOrNotNull(checkFamilyStatuses, np.getFamilyStatuses(), "familyStatuses" + "(" + p + ")");
			assertNullOrNotNull(checkWithholdingTaxDeclarationPeriods, np.getWithholdingTaxationPeriods(), "withholdingTaxDelarationPeriods" + "(" + p + ")");
			assertNullOrNotNull(checkResidencyPeriods, np.getResidencyPeriods(), "residencyPeriods" + "(" + p + ")");
			assertNullOrNotNull(checkRealLandRights || checkVirtualTransitiveLandRights || checkVirtualInheritedRealLandRights || checkVirtualInheritedVirtuelLandRights, np.getLandRights(), "landRights" + "(" + p + ")");
		}
	}

	private static void assertNullOrNotNull(boolean notNull, Object value, String prefix) {
		if (value instanceof Collection) {
			final Collection<?> coll = (Collection<?>) value;
			if (notNull) {
				assertNotNull(prefix + " expected=not null actual=" + coll, coll);
				assertFalse(prefix + " expected=not empty actual=" + coll, coll.isEmpty());
			}
			else {
				assertEmpty(prefix + " expected=empty actual=" + coll, coll);
			}
		}
		else {
			if (notNull) {
				assertNotNull(prefix + " expected=not null actual=" + value, value);
			}
			else {
				assertNull(prefix + " expected=null actual=" + value, value);
			}
		}
	}

	private static void assertTaxResidenceAndAddressePart(final Party tiers) {
		assertNotNull(tiers);
		assertNotEmpty(tiers.getMailAddresses());
		assertNotEmpty(tiers.getResidenceAddresses());
		assertNotEmpty(tiers.getDebtProsecutionAddresses());
		assertNotEmpty(tiers.getRepresentationAddresses());
		assertNotEmpty(tiers.getMainTaxResidences());
		assertNotEmpty(tiers.getOtherTaxResidences());
	}

	private static void assertTaxResidencePart(final Party tiers) {
		assertNotNull(tiers);
		assertEmpty(tiers.getMailAddresses());
		assertEmpty(tiers.getResidenceAddresses());
		assertEmpty(tiers.getDebtProsecutionAddresses());
		assertEmpty(tiers.getRepresentationAddresses());
		assertNotEmpty(tiers.getMainTaxResidences());
		assertNotEmpty(tiers.getOtherTaxResidences());
	}

	private static void assertAddressPart(final Party tiers) {
		assertNotNull(tiers);
		assertNotNull(tiers.getMailAddresses());
		assertNotEmpty(tiers.getResidenceAddresses());
		assertNotEmpty(tiers.getDebtProsecutionAddresses());
		assertNotEmpty(tiers.getRepresentationAddresses());
		assertEmpty(tiers.getMainTaxResidences());
		assertEmpty(tiers.getOtherTaxResidences());
	}

	private static void assertNoPart(final Party tiers) {
		assertNotNull(tiers);
		assertEmpty(tiers.getMailAddresses());
		assertEmpty(tiers.getResidenceAddresses());
		assertEmpty(tiers.getDebtProsecutionAddresses());
		assertEmpty(tiers.getRepresentationAddresses());
		assertEmpty(tiers.getMainTaxResidences());
		assertEmpty(tiers.getOtherTaxResidences());
	}

	private static void assertNotEmpty(Collection<?> coll) {
		assertNotNull(coll);
		assertFalse(coll.isEmpty());
	}
}
