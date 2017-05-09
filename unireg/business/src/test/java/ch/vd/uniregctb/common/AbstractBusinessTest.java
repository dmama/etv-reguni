package ch.vd.uniregctb.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseMandataireEtrangere;
import ch.vd.uniregctb.adresse.AdresseMandataireSuisse;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.declaration.DeclarationAvecNumeroSequence;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationHelper;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.etiquette.ActionAutoEtiquette;
import ch.vd.uniregctb.etiquette.CorrectionSurDate;
import ch.vd.uniregctb.etiquette.Decalage;
import ch.vd.uniregctb.etiquette.DecalageAvecCorrection;
import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.etiquette.UniteDecalageDate;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DonneesLoiLogement;
import ch.vd.uniregctb.foncier.DonneesUtilisation;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.indexer.messageidentification.GlobalMessageIdentificationIndexer;
import ch.vd.uniregctb.indexer.messageidentification.GlobalMessageIdentificationSearcher;
import ch.vd.uniregctb.indexer.messageidentification.MessageIdentificationIndexerHibernateInterceptor;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parentes.ParentesSynchronizerInterceptor;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DescriptionBatimentRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.QuotePartRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.EstimationRFHelper;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tache.TacheSynchronizerInterceptor;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCanton;
import ch.vd.uniregctb.tiers.AllegementFiscalCantonCommune;
import ch.vd.uniregctb.tiers.AllegementFiscalCommune;
import ch.vd.uniregctb.tiers.AllegementFiscalConfederation;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneeCivileEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.type.TypeRapprochementRF;
import ch.vd.uniregctb.type.TypeTiersEtiquette;
import ch.vd.uniregctb.validation.ValidationInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

// Surcharge des fichiers de config Spring. Il faut mettre les fichiers
// UT a la fin
@ContextConfiguration(locations = {
        BusinessTestingConstants.UNIREG_BUSINESS_UT_CACHE,
        BusinessTestingConstants.UNIREG_BUSINESS_ESSENTIALS,
        BusinessTestingConstants.UNIREG_BUSINESS_SERVICES,
        BusinessTestingConstants.UNIREG_BUSINESS_RF,
        BusinessTestingConstants.UNIREG_BUSINESS_REGISTREFONCIER,
        BusinessTestingConstants.UNIREG_BUSINESS_REGISTREFONCIER_IMPORT,
        BusinessTestingConstants.UNIREG_BUSINESS_EVT_CIVIL,
        BusinessTestingConstants.UNIREG_BUSINESS_EVT_ORGANISATION,
        BusinessTestingConstants.UNIREG_BUSINESS_EVT_FISCAL,
        BusinessTestingConstants.UNIREG_BUSINESS_ANNONCE_IDE,
        BusinessTestingConstants.UNIREG_BUSINESS_UT_EDITIQUE,
        BusinessTestingConstants.UNIREG_BUSINESS_UT_INTERFACES,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_EVT_ORGANISATION,
        BusinessTestingConstants.UNIREG_BUSINESS_UT_JMS,
        BusinessTestingConstants.UNIREG_BUSINESS_UT_SERVICES,
        BusinessTestingConstants.UNIREG_BUSINESS_UT_CLIENT_WEBSERVICE,
        BusinessTestingConstants.UNIREG_BUSINESS_UT_CONFIG,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_ADRESSES,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_REGISTREFONCIER_IMPORT,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_REGIME_FISCAL
})
public abstract class AbstractBusinessTest extends AbstractCoreDAOTest {

    // private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBusinessTest.class);

    protected boolean wantIndexationTiers = false;
	protected boolean wantIndexationMessagesIdentification = false;
    protected boolean wantSynchroTache = false;
	protected boolean wantSynchroParentes = false;
	protected boolean wantCollectivitesAdministratives = true;
    protected TiersService tiersService;
    protected GlobalTiersIndexer globalTiersIndexer;
    protected GlobalTiersSearcher globalTiersSearcher;
	protected GlobalMessageIdentificationIndexer globalMessageIdentificationIndexer;
	protected GlobalMessageIdentificationSearcher globalMessageIdentificationSearcher;
	protected MessageIdentificationIndexerHibernateInterceptor messageIdentificationIndexerHibernateInterceptor;
    protected TacheSynchronizerInterceptor tacheSynchronizer;
    protected ValidationInterceptor validationInterceptor;
	protected ParentesSynchronizerInterceptor parentesSynchronizer;

	public final static String CODE_ETIQUETTE_HERITAGE = "HERITAGE";
	public final static String CODE_ETIQUETTE_COLLABORATEUR = "COLLABORATEUR";

	static {
		forceLoadAllCollectivitesAdministratives();
	}

	private static void forceLoadAllCollectivitesAdministratives() {
		forceLoadStaticInstances(MockCollectiviteAdministrative.class);
		forceLoadStaticInstances(MockOfficeImpot.class);
		forceLoadStaticInstances(MockCollectiviteAdministrative.JusticePaix.class);
	}

	private static void forceLoadStaticInstances(Class<?> clazz) {
		for (Field field : clazz.getDeclaredFields()) {
			final int modifiers = field.getModifiers();
			if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && Modifier.isPublic(modifiers) && MockCollectiviteAdministrative.class.isAssignableFrom(field.getType())) {
				try {
					field.get(null);
				}
				catch (IllegalAccessException e) {
					// on aura essayé... mais c'est quand-même assez bizarre, puisque on ne fait le GET que sur les membres publics
				}
			}
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		if (wantCollectivitesAdministratives) {
			/*
			 * Ici, on désactive temporairement l'intercepteur hibernate qui s'occupe du calcul de l'OID d'un contribuable
			 * (on est justement en train d'insérer ces OIDs en base) car il arrive apparemment que des contribuables soient
			 * pris avec dans le commit de cette transaction (je n'ai pas compris comment) si plusieurs tests annotés @Transactional
			 * sont lancés rapidement les uns à la suite des autres...
			 */
			AuthenticationHelper.pushPrincipal(getDefaultOperateurName());      // pour les tests qui veulent gérer l'authentification eux-mêmes, et surchargent setAuthentication()
			try {
				final Switchable oidInterceptor = getBean(Switchable.class, "officeImpotHibernateInterceptor");
				doInNewTransactionAndSessionUnderSwitch(oidInterceptor, false, new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						for (MockCollectiviteAdministrative collAdm : MockCollectiviteAdministrative.getAll()) {
							final CollectiviteAdministrative ca;
							if (collAdm instanceof MockOfficeImpot) {
								ca = addCollAdm((MockOfficeImpot) collAdm);
							}
							else {
								ca = addCollAdm(collAdm);
							}

							// [SIFISC-20149] on va également créer les étiquettes qui vont bien si la bonne collectivité adminstrative est là
							if (collAdm.getNoColAdm() == MockCollectiviteAdministrative.noNouvelleEntite) {
								final Etiquette heritage = addEtiquette(CODE_ETIQUETTE_HERITAGE, "Héritage", TypeTiersEtiquette.PP, ca);
								heritage.setActionSurDeces(new ActionAutoEtiquette(new Decalage(1, UniteDecalageDate.JOUR),
								                                                   new DecalageAvecCorrection(2, UniteDecalageDate.ANNEE, CorrectionSurDate.FIN_ANNEE)));
								addEtiquette(CODE_ETIQUETTE_COLLABORATEUR, "DS Collaborateur", TypeTiersEtiquette.PP, ca);
							}
						}
					}
				});
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	@Override
    protected void runOnSetUp() throws Exception {
        tiersService = getBean(TiersService.class, "tiersService");
	    globalTiersSearcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
	    globalTiersIndexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
        globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(wantIndexationTiers);
		globalMessageIdentificationSearcher = getBean(GlobalMessageIdentificationSearcher.class, "globalMessageIdentificationSearcher");
		globalMessageIdentificationIndexer = getBean(GlobalMessageIdentificationIndexer.class, "globalMessageIdentificationIndexer");
		messageIdentificationIndexerHibernateInterceptor = getBean(MessageIdentificationIndexerHibernateInterceptor.class, "messageIdentificationIndexInterceptor");
		messageIdentificationIndexerHibernateInterceptor.setEnabled(wantIndexationMessagesIdentification);
        tacheSynchronizer = getBean(TacheSynchronizerInterceptor.class, "tacheSynchronizerInterceptor");
        tacheSynchronizer.setOnTheFlySynchronization(wantSynchroTache);
        validationInterceptor = getBean(ValidationInterceptor.class, "validationInterceptor");
	    parentesSynchronizer = getBean(ParentesSynchronizerInterceptor.class, "parentesSynchronizerInterceptor");
	    parentesSynchronizer.setEnabled(wantSynchroParentes);
        super.runOnSetUp();
    }

	@Override
	public void onTearDown() throws Exception {
		// dans tous les cas, que l'indexation asynchrone soit demandée ou pas, on s'assure qu'elle est terminée avant la fin du test afin que
		// le test suivant ne tente pas de vider la base de données alors que l'indexation est encore en cours, ce qui peut
		// aller jusqu'à causer un deadlock (que l'on voit parfois çà et là dans les tests Jenkins)
		// (on doit le faire dans tous les cas car même si l'indexation asynchrone n'est pas demandée, elle est toujours active sur un nouveau thread par défaut,
		// et donc si le test est multi-threadé, certaines indexations on-the-fly peuvent tout de même être lancées...)
		globalTiersIndexer.sync();
		super.onTearDown();
	}

	@Override
    protected void truncateDatabase() throws Exception {
        super.truncateDatabase();

        if (wantIndexationTiers) {
            removeTiersIndexData();
        }
		if (wantIndexationMessagesIdentification) {
			removeMessageIdentificationIndexData();
		}
    }

    @Override
    protected void loadDatabase(String filename) throws Exception {
        super.loadDatabase(filename);

        if (wantIndexationTiers) {
            indexTiersData();
        }
	    if (wantIndexationMessagesIdentification) {
		    indexMessagesIdentificationData();
	    }
    }

    public void setWantIndexationTiers(boolean wantIndexationTiers) {
        this.wantIndexationTiers = wantIndexationTiers;

        if (globalTiersIndexer != null) {
            globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(wantIndexationTiers);
        }
    }

	public void setWantIndexationMessagesIdentification(boolean wantIndexationMessagesIdentification) {
		this.wantIndexationMessagesIdentification = wantIndexationMessagesIdentification;

		if (messageIdentificationIndexerHibernateInterceptor != null) {
			messageIdentificationIndexerHibernateInterceptor.setEnabled(wantIndexationMessagesIdentification);
		}
	}

    public void setWantSynchroTache(boolean wantSynchroTache) {
        this.wantSynchroTache = wantSynchroTache;

        if (tacheSynchronizer != null) {
            tacheSynchronizer.setOnTheFlySynchronization(wantSynchroTache);
        }
    }

    public void setWantSynchroParentes(boolean wantSynchroParentes) {
        this.wantSynchroParentes = wantSynchroParentes;

        if (parentesSynchronizer != null) {
            parentesSynchronizer.setEnabled(wantSynchroParentes);
        }
    }

	public void setWantCollectivitesAdministratives(boolean wantCollectivitesAdministratives) {
		this.wantCollectivitesAdministratives = wantCollectivitesAdministratives;
	}

	protected void indexTiersData() throws Exception {
        globalTiersIndexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.FULL);
    }

	protected void indexMessagesIdentificationData() throws Exception {
		globalMessageIdentificationIndexer.indexAllDatabase(null, 1);
	}

    protected void removeTiersIndexData() throws Exception {
        globalTiersIndexer.overwriteIndex();
    }

	protected void removeMessageIdentificationIndexData() throws Exception {
		globalMessageIdentificationIndexer.overwriteIndex();
	}

	protected <T> T doInNewTransactionAndSession(final TransactionCallback<T> action) throws Exception {
        return doInNewTransaction(new TransactionCallback<T>() {
            @Override
            public T doInTransaction(final TransactionStatus status) {
                return hibernateTemplate.executeWithNewSession(new HibernateCallback<T>() {
                    @Override
                    public T doInHibernate(Session session) throws HibernateException, SQLException {
                        return action.doInTransaction(status);
                    }
                });
            }
        });
    }

	protected RapprochementRF addRapprochementRF(@NotNull PersonnePhysique ctb, @NotNull PersonnePhysiqueRF tiersRF, RegDate dateDebut, RegDate dateFin, TypeRapprochementRF type) {
		final RapprochementRF rapprochement = new RapprochementRF();
		rapprochement.setDateDebut(dateDebut);
		rapprochement.setDateFin(dateFin);
		rapprochement.setContribuable(ctb);
		rapprochement.setTiersRF(tiersRF);
		rapprochement.setTypeRapprochement(type);
		return hibernateTemplate.merge(rapprochement);
	}

	protected RapprochementRF addRapprochementRF(@NotNull Entreprise ctb, @NotNull PersonneMoraleRF tiersRF, RegDate dateDebut, RegDate dateFin, TypeRapprochementRF type) {
		final RapprochementRF rapprochement = new RapprochementRF();
		rapprochement.setDateDebut(dateDebut);
		rapprochement.setDateFin(dateFin);
		rapprochement.setContribuable(ctb);
		rapprochement.setTiersRF(tiersRF);
		rapprochement.setTypeRapprochement(type);
		return hibernateTemplate.merge(rapprochement);
	}

	protected DroitProprietePersonnePhysiqueRF addDroitPropriete(PersonnePhysiqueRF tiersRF, BienFondRF immeuble, CommunauteRF communaute, GenrePropriete regime, Fraction part, RegDate dateDebut, RegDate dateFin, RegDate dateDebutMetier,
	                                                             RegDate dateFinMetier, String motifDebut, String motifFin, IdentifiantAffaireRF numeroAffaire, String masterIdRF, String versionIdRF) {
		final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
		droit0.setCommunaute(communaute);
		droit0.setRegime(regime);
		droit0.setPart(part);
		droit0.setDateDebut(dateDebut);
		droit0.setDateFin(dateFin);
		droit0.setDateDebutMetier(dateDebutMetier);
		droit0.setDateFinMetier(dateFinMetier);
		droit0.setMotifDebut(motifDebut);
		droit0.setMotifFin(motifFin);
		droit0.setAyantDroit(tiersRF);
		droit0.setMasterIdRF(masterIdRF);
		droit0.setVersionIdRF(versionIdRF);
		droit0.setImmeuble(immeuble);
		droit0.addRaisonAcquisition(new RaisonAcquisitionRF(dateDebutMetier, motifDebut, numeroAffaire));
		return hibernateTemplate.merge(droit0);
	}

	protected DroitProprieteCommunauteRF addDroitPropriete(CommunauteRF communaute, BienFondRF immeuble, GenrePropriete regime, Fraction part, RegDate dateDebut, RegDate dateDebutMetier, RegDate dateFin,
	                                                       String motifDebut, String motifFin, IdentifiantAffaireRF numeroAffaire, String masterIdRF, String versionIdRF) {
		final DroitProprieteCommunauteRF droit0 = new DroitProprieteCommunauteRF();
		droit0.setRegime(regime);
		droit0.setPart(part);
		droit0.setDateDebut(dateDebut);
		droit0.setDateDebutMetier(dateDebutMetier);
		droit0.setDateFin(dateFin);
		droit0.setMotifDebut(motifDebut);
		droit0.setMotifFin(motifFin);
		droit0.setAyantDroit(communaute);
		droit0.setMasterIdRF(masterIdRF);
		droit0.setVersionIdRF(versionIdRF);
		droit0.setImmeuble(immeuble);
		droit0.addRaisonAcquisition(new RaisonAcquisitionRF(dateDebutMetier, motifDebut, numeroAffaire));
		return hibernateTemplate.merge(droit0);
	}

	protected PersonnePhysiqueRF addPersonnePhysiqueRF(String idRF, String prenom, String nom, RegDate dateNaissance) {
		PersonnePhysiqueRF tiersRF = new PersonnePhysiqueRF();
		tiersRF.setNom(nom);
		tiersRF.setPrenom(prenom);
		tiersRF.setIdRF(idRF);
		tiersRF.setDateNaissance(dateNaissance);
		tiersRF = hibernateTemplate.merge(tiersRF);
		return tiersRF;
	}

	@NotNull
	protected CommuneRF addCommuneRF(int noRf, String nomRf, int noOfs) {
		return hibernateTemplate.merge(new CommuneRF(noRf, nomRf, noOfs));
	}

	@NotNull
	protected BienFondRF addBienFondRF(String idRF, String egrid, CommuneRF commune, int noParcelle) {
		return addBienFondRF(idRF, egrid, commune, noParcelle, null, null, null);
	}

	@NotNull
	protected BienFondRF addBienFondRF(String idRF, String egrid, CommuneRF commune, int noParcelle, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) {
		final SituationRF situation = new SituationRF();
		situation.setDateDebut(RegDate.get(2000, 1, 1));
		situation.setNoParcelle(noParcelle);
		situation.setIndex1(index1);
		situation.setIndex2(index2);
		situation.setIndex3(index3);
		situation.setCommune(commune);

		final BienFondRF im0 = new BienFondRF();
		im0.setIdRF(idRF);
		im0.setEgrid(egrid);
		im0.addSituation(situation);
		return hibernateTemplate.merge(im0);
	}

	@NotNull
	protected ProprieteParEtageRF addProprieteParEtageRF(String idRF, String egrid, Fraction quotePart, CommuneRF commune, int noParcelle, Integer index1, Integer index2, Integer index3) {
		final SituationRF situation = new SituationRF();
		situation.setDateDebut(RegDate.get(2000, 1, 1));
		situation.setNoParcelle(noParcelle);
		situation.setIndex1(index1);
		situation.setIndex2(index2);
		situation.setIndex3(index3);
		situation.setCommune(commune);

		ProprieteParEtageRF im0 = new ProprieteParEtageRF();
		im0.setIdRF(idRF);
		im0.setEgrid(egrid);
		im0.addQuotePart(new QuotePartRF(null, null, quotePart));
		im0.addSituation(situation);
		return hibernateTemplate.merge(im0);
	}

	@NotNull
	protected BatimentRF addBatimentRF(String masterIdRF) {
		final BatimentRF batiment = new BatimentRF();
		batiment.setMasterIdRF(masterIdRF);
		return hibernateTemplate.merge(batiment);
	}

	protected void addDescriptionBatimentRF(RegDate dateDebut, RegDate dateFin, String type, Integer surface, BatimentRF batiment) {
		final DescriptionBatimentRF description = new DescriptionBatimentRF();
		description.setDateDebut(dateDebut);
		description.setDateFin(dateFin);
		description.setType(type);
		description.setSurface(surface);
		batiment.addDescription(description);
	}

	protected void addImplantationRF(RegDate dateDebut, RegDate dateFin, Integer surface, ImmeubleRF immeuble, BatimentRF batiment) {
		final ImplantationRF implantation = new ImplantationRF();
		implantation.setDateDebut(dateDebut);
		implantation.setDateFin(dateFin);
		implantation.setSurface(surface);
		implantation.setImmeuble(immeuble);
		batiment.addImplantation(implantation);
	}

	protected void addEstimationFiscale(RegDate dateInscription, RegDate dateDebut, RegDate dateFin, boolean enRevision, Long montant, String reference, ImmeubleRF immeuble) {
		final EstimationRF estimation = new EstimationRF();
		estimation.setDateDebut(dateDebut);
		estimation.setDateFin(dateFin);
		estimation.setDateInscription(dateInscription);
		estimation.setEnRevision(enRevision);
		estimation.setImmeuble(immeuble);
		estimation.setMontant(montant);
		estimation.setReference(reference);
		estimation.setAnneeReference(EstimationRFHelper.determineAnneeReference(reference));
		estimation.setDateDebutMetier(EstimationRFHelper.determineDateDebutMetier(reference, dateInscription));
		immeuble.addEstimation(estimation);
	}

	protected SurfaceAuSolRF addSurfaceAuSol(RegDate dateDebut, RegDate dateFin, int aire, String type, ImmeubleRF immeuble) {
		final SurfaceAuSolRF surface = new SurfaceAuSolRF();
		surface.setDateDebut(dateDebut);
		surface.setDateFin(dateFin);
		surface.setSurface(aire);
		surface.setType(type);
		surface.setImmeuble(immeuble);

		final SurfaceAuSolRF saved = hibernateTemplate.merge(surface);
		immeuble.addSurfaceAuSol(saved);
		return saved;
	}

	protected interface ExecuteCallback<T> {
        T execute() throws Exception;
    }

    /**
     * Exécute une portion de code avec la validation des objets métiers désactivée. Cette méthode permet typiquement de stocker des données dans le base de données qui ne valident plus avec les règles
     * de validation actuelles, mais qui validaient par le passé, et donc qui peuvent encore exister actuellement sous cette forme dans la base de données.
     *
     * @param action l'action qui à effecuter
     * @param <T>    le type d'objet retourné par l'action
     * @return l'objet retourné par l'action
     * @throws Exception en case d'exception
     */
    protected <T> T doWithoutValidation(ExecuteCallback<T> action) throws Exception {
	    return doUnderSwitch(validationInterceptor, false, action);
    }

	/**
	 * Interface de définition d'une initialisation / d'un nettoyage final dans le scope
	 * desquels on veut englober une action (qui peut être une transaction, c'est un peu le but)
	 */
	public interface InitCleanupCallback {
		/**
		 * Procédure d'initialisation - appelée en tout premier lieu
		 * @throws Exception en cas de souci
		 */
		void init() throws Exception;

		/**
		 * Procédure de nettoyage - appelée en tout dernier lieu, à partir du moment où la procédure d'initialisation a été terminée sans exception
		 * @throws Exception en cas de souci
		 */
		void cleanup() throws Exception;
	}

	/**
	 * Lancement d'une action en l'englobant dans un scope d'initialisation/nettoyage
	 */
	protected <T> T doWithInitCleanup(InitCleanupCallback initCleanup, ExecuteCallback<T> action) throws Exception {
		initCleanup.init();
		try {
			return action.execute();
		}
		finally {
			initCleanup.cleanup();
		}
	}

	protected <T> T doUnderSwitch(final Switchable switchable, final boolean switchValue, ExecuteCallback<T> action) throws Exception {
		final InitCleanupCallback initCleanup = new InitCleanupCallback() {
			private boolean oldSwitchValue;

			@Override
			public void init() throws Exception {
				oldSwitchValue = switchable.isEnabled();
				switchable.setEnabled(switchValue);
			}

			@Override
			public void cleanup() throws Exception {
				switchable.setEnabled(oldSwitchValue);
			}
		};
		return doWithInitCleanup(initCleanup, action);
	}

    /**
     * Exécute une portion de code dans une nouvelle transaction et une nouvelle session hibernate tout en désactivant la validation des objets métiers. Cette méthode combine donc les méthodes
     * #doInNewTransactionAndSession et #doWithoutValidation en une.
     *
     * @param action l'action à effectuer
     * @param <T>    le type d'objet retourné par l'action
     * @return l'objet retourné par l'action
     * @throws Exception en case d'exception
     */
    protected <T> T doInNewTransactionAndSessionWithoutValidation(final TransactionCallback<T> action) throws Exception {
	    return doWithoutValidation(new ExecuteCallback<T>() {
            @Override
            public T execute() throws Exception {
                return doInNewTransactionAndSession(action);
            }
        });
    }

	protected <T> T doInNewTransactionAndSessionUnderSwitch(Switchable switchable, boolean switchValue, final TransactionCallback<T> action) throws Exception {
		return doUnderSwitch(switchable, switchValue, new ExecuteCallback<T>() {
            @Override
            public T execute() throws Exception {
                return doInNewTransactionAndSession(action);
            }
        });
	}

	/**
	 * Exécute une portion de code dans une nouvelle transaction et une nouvelle session hibernate, le tout dans un scope d'initialisation/nettoyage
	 * (ces deux phases - initialisation et nettoyage - étant lancées hors de la transaction)
	 */
	protected <T> T doInNewTransactionAndSessionWithInitCleanup(InitCleanupCallback initCleanup, final TransactionCallback<T> action) throws Exception {
		return doWithInitCleanup(initCleanup, new ExecuteCallback<T>() {
			@Override
			public T execute() throws Exception {
				return doInNewTransactionAndSession(action);
			}
		});
	}

    protected static void assertForPrincipal(RegDate debut, MotifFor motifOuverture, Commune commune, MotifRattachement motif, ModeImposition modeImposition, ForFiscalPrincipalPP forPrincipal) {
        assertNotNull(forPrincipal);
        assertEquals(debut, forPrincipal.getDateDebut());
        assertEquals(motifOuverture, forPrincipal.getMotifOuverture());
        assertNull(forPrincipal.getDateFin());
        assertNull(forPrincipal.getMotifFermeture());
        final TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
        assertEquals(type, forPrincipal.getTypeAutoriteFiscale());
        assertEquals(Integer.valueOf(commune.getNoOFS()), forPrincipal.getNumeroOfsAutoriteFiscale());
        assertEquals(motif, forPrincipal.getMotifRattachement());
        assertEquals(modeImposition, forPrincipal.getModeImposition());
    }

    protected static void assertForPrincipal(RegDate debut, MotifFor motifOuverture, RegDate fin, MotifFor motifFermeture, Commune commune, MotifRattachement motif, ModeImposition modeImposition,
                                             ForFiscalPrincipalPP forPrincipal) {
        assertNotNull(forPrincipal);
        assertEquals(debut, forPrincipal.getDateDebut());
        assertEquals(motifOuverture, forPrincipal.getMotifOuverture());
        assertEquals(fin, forPrincipal.getDateFin());
        assertEquals(motifFermeture, forPrincipal.getMotifFermeture());
        final TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
        assertEquals(type, forPrincipal.getTypeAutoriteFiscale());
        assertEquals(Integer.valueOf(commune.getNoOFS()), forPrincipal.getNumeroOfsAutoriteFiscale());
        assertEquals(motif, forPrincipal.getMotifRattachement());
        assertEquals(modeImposition, forPrincipal.getModeImposition());
    }

    protected static void assertForAutreImpot(RegDate debut, RegDate fin, TypeAutoriteFiscale taf, MockCommune commune, GenreImpot genreImpot, ForFiscalAutreImpot forFiscal) {
        Assert.assertEquals(debut, forFiscal.getDateDebut());
        Assert.assertEquals(fin, forFiscal.getDateFin());
        Assert.assertEquals(taf, forFiscal.getTypeAutoriteFiscale());
        Assert.assertEquals(commune.getNoOFS(), forFiscal.getNumeroOfsAutoriteFiscale().intValue());
        Assert.assertEquals(genreImpot, forFiscal.getGenreImpot());
    }

    protected static void assertForAutreElementImposable(RegDate debut, RegDate fin, TypeAutoriteFiscale taf, MockCommune commune, MotifRattachement rattachement,
                                                         ForFiscalAutreElementImposable forFiscal) {
        Assert.assertEquals(debut, forFiscal.getDateDebut());
        Assert.assertEquals(fin, forFiscal.getDateFin());
        Assert.assertEquals(taf, forFiscal.getTypeAutoriteFiscale());
        Assert.assertEquals(commune.getNoOFS(), forFiscal.getNumeroOfsAutoriteFiscale().intValue());
        Assert.assertEquals(rattachement, forFiscal.getMotifRattachement());
    }

    protected static void assertEtatCivil(RegDate debut, TypeEtatCivil type, EtatCivil etatCivil) {
        assertNotNull(etatCivil);
        assertEquals(debut, etatCivil.getDateDebut());
        assertEquals(type, etatCivil.getTypeEtatCivil());
    }

    protected static void assertAdresseCivile(@Nullable RegDate debut, @Nullable RegDate fin, String rue, String npa, String localite, Adresse adresse) {
        assertNotNull(adresse);
        assertEquals(debut, adresse.getDateDebut());
        assertEquals(fin, adresse.getDateFin());
        assertEquals(rue, adresse.getRue());
        assertEquals(npa, adresse.getNumeroPostal());
        assertEquals(localite, adresse.getLocalite());
    }

    protected static void assertAdresseCivile(@Nullable RegDate debut, @Nullable RegDate fin, String rue, String npa, String localite, @Nullable Integer egid, Adresse adresse) {
        assertNotNull(adresse);
        assertEquals(debut, adresse.getDateDebut());
        assertEquals(fin, adresse.getDateFin());
        assertEquals(rue, adresse.getRue());
        assertEquals(npa, adresse.getNumeroPostal());
        assertEquals(localite, adresse.getLocalite());
        assertEquals(egid, adresse.getEgid());
    }

    protected static void assertAdresseCivile(@Nullable RegDate debut, @Nullable RegDate fin, String rue, String npa, String localite, @Nullable Integer egid, @Nullable Integer ewid,
                                              Adresse adresse) {
        assertNotNull(adresse);
        assertEquals(debut, adresse.getDateDebut());
        assertEquals(fin, adresse.getDateFin());
        assertEquals(rue, adresse.getRue());
        assertEquals(npa, adresse.getNumeroPostal());
        assertEquals(localite, adresse.getLocalite());
        assertEquals(egid, adresse.getEgid());
        assertEquals(ewid, adresse.getEwid());
    }

    protected static void assertLocalisation(LocalisationType type, Integer noOfs, Localisation localisation) {
        assertNotNull(localisation);
        assertEquals(type, localisation.getType());
        assertEquals(noOfs, localisation.getNoOfs());
    }

    /**
     * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockCommune commune) {
        TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune.getNoOFS(), type, ModeImposition.ORDINAIRE, MotifRattachement.DOMICILE);
    }

    /**
     * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockCommune commune, ModeImposition modeImposition) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune, MotifRattachement.DOMICILE, modeImposition);
    }

    /**
     * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, MotifFor motifOuverture, MockCommune commune, MotifRattachement motifRattachement) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune, motifRattachement);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, RegDate fermeture, @Nullable MotifFor motifFermeture,
                                                   MockCommune commune) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, MotifRattachement.DOMICILE);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, RegDate fermeture, @Nullable MotifFor motifFermeture,
                                                   MockCommune commune, ModeImposition modeImposition) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, MotifRattachement.DOMICILE, modeImposition);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, MotifFor motifOuverture, @Nullable RegDate fermeture, @Nullable MotifFor motifFermeture, MockCommune commune,
                                                   MotifRattachement motifRattachement) {
        final TypeAutoriteFiscale type = (commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune.getNoOFS(), type, ModeImposition.ORDINAIRE, motifRattachement);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse sur le contribuable spécifié avec le mode d'imposition spécifié
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, MotifFor motifOuverture, @Nullable RegDate fermeture, @Nullable MotifFor motifFermeture,
                                                 MockCommune commune, MotifRattachement motifRattachement, ModeImposition modeImposition) {
        final ForFiscalPrincipalPP ffp = addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, motifRattachement);
        ffp.setModeImposition(modeImposition);
        return ffp;
    }

    /**
     * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockPays pays) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, pays, MotifRattachement.DOMICILE);
    }

    /**
     * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockPays pays, ModeImposition modeImposition) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, pays, modeImposition, MotifRattachement.DOMICILE);
    }

    /**
     * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, MotifFor motifOuverture, MockPays pays, MotifRattachement motifRattachement) {
        assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, ModeImposition.ORDINAIRE, motifRattachement);
    }

    /**
     * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, MotifFor motifOuverture, MockPays pays, ModeImposition modeImposition, MotifRattachement motifRattachement) {
        assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, modeImposition, motifRattachement);
    }

    /**
     * Ajoute un for principal fermé à l'étranger sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, RegDate fermeture, @Nullable MotifFor motifFermeture, MockPays pays) {
        assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, ModeImposition.ORDINAIRE, MotifRattachement.DOMICILE);
    }

	/**
     * Ajoute un for principal fermé à l'étranger sur le contribuable spécifié avec le mode d'imposition spécifié
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockPays pays,
                                                   ModeImposition modeImposition) {
        final ForFiscalPrincipalPP ffp = addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays);
        ffp.setModeImposition(modeImposition);
        return ffp;
    }

	/**
     * Ajoute un for principal fermé à l'étranger sur le contribuable spécifié avec le mode d'imposition spécifié
     */
    protected ForFiscalPrincipalPP addForPrincipal(ContribuableImpositionPersonnesPhysiques contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockPays pays,
                                                   MotifRattachement motifRattachement) {
        final ForFiscalPrincipalPP ffp = addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays);
        ffp.setMotifRattachement(motifRattachement);
        return ffp;
    }

    /**
     * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockCommune commune, GenreImpot genreImpot) {
        TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune.getNoOFS(), type, MotifRattachement.DOMICILE, genreImpot);
    }

    /**
     * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockCommune commune) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, commune, GenreImpot.BENEFICE_CAPITAL);
    }

    /**
     * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, MotifFor motifOuverture, MockCommune commune, MotifRattachement motifRattachement) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune, motifRattachement);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture,
                                                   @Nullable RegDate fermeture, @Nullable MotifFor motifFermeture, MockCommune commune) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, MotifRattachement.DOMICILE);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture,
                                                   @Nullable RegDate fermeture, @Nullable MotifFor motifFermeture, MockCommune commune, GenreImpot genreImpot) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, MotifRattachement.DOMICILE, genreImpot);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, MotifFor motifOuverture,
                                                   @Nullable RegDate fermeture, @Nullable MotifFor motifFermeture, MockCommune commune, MotifRattachement motifRattachement) {
	    return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, motifRattachement, GenreImpot.BENEFICE_CAPITAL);
    }

    /**
     * Ajoute un for principal fermé sur une commune Suisse sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, MotifFor motifOuverture,
                                                   @Nullable RegDate fermeture, @Nullable MotifFor motifFermeture, MockCommune commune, MotifRattachement motifRattachement, GenreImpot genreImpot) {
        final TypeAutoriteFiscale type = (commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune.getNoOFS(), type, motifRattachement, genreImpot);
    }

    /**
     * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, MockPays pays) {
        return addForPrincipal(contribuable, ouverture, motifOuverture, pays, MotifRattachement.DOMICILE);
    }

    /**
     * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, MotifFor motifOuverture, MockPays pays, MotifRattachement motifRattachement) {
        assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
        return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, motifRattachement, GenreImpot.BENEFICE_CAPITAL);
    }

    /**
     * Ajoute un for principal fermé à l'étranger sur le contribuable spécifié.
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, @Nullable MotifFor motifOuverture, RegDate fermeture, @Nullable MotifFor motifFermeture, MockPays pays) {
        assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
        return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
    }

	/**
     * Ajoute un for principal fermé à l'étranger sur le contribuable spécifié avec le mode de rattachement spécifié
     */
    protected ForFiscalPrincipalPM addForPrincipal(ContribuableImpositionPersonnesMorales contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockPays pays,
                                                   MotifRattachement motifRattachement) {
        final ForFiscalPrincipalPM ffp = addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays);
        ffp.setMotifRattachement(motifRattachement);
        return ffp;
    }

    protected ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable dpi, RegDate debut, MotifFor motifOuverture, @Nullable RegDate fin, @Nullable MotifFor motifFermeture, MockCommune commune) {
        ForDebiteurPrestationImposable f = new ForDebiteurPrestationImposable();
        f.setDateDebut(debut);
	    f.setMotifOuverture(motifOuverture);
        f.setDateFin(fin);
	    f.setMotifFermeture(motifFermeture);
        f.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
        f.setTypeAutoriteFiscale(commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
        f.setNumeroOfsAutoriteFiscale(commune.getNoOFS());
        if (dpi.getDernierForDebiteur() == null) {
            tiersService.adaptPremierePeriodicite(dpi, debut);
        }
        f = tiersDAO.addAndSave(dpi, f);
        return f;
    }

    protected RapportPrestationImposable addRapportPrestationImposable(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RegDate debut, @Nullable RegDate fin, boolean annule) {
        RapportPrestationImposable rpi = new RapportPrestationImposable(debut, fin, sourcier, dpi);
        rpi.setAnnule(annule);
        rpi = hibernateTemplate.merge(rpi);

        dpi.addRapportObjet(rpi);
        sourcier.addRapportSujet(rpi);

        return rpi;
    }

    protected AdresseSuisse addAdresseSuisse(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, MockRue rue) {
       return addAdresseSuisse(tiers, usage, debut,fin,rue,null);
    }

	protected Mandat addMandatGeneral(Contribuable mandant, Contribuable mandataire, RegDate dateDebut, @Nullable RegDate dateFin, boolean withCopy) {
		final Mandat mandat = hibernateTemplate.merge(Mandat.general(dateDebut, dateFin, mandant, mandataire, withCopy));
		mandant.addRapportSujet(mandat);
		mandataire.addRapportObjet(mandat);
		return mandat;
	}

    protected AdresseMandataireSuisse addAdresseMandataireSuisse(Contribuable ctb, RegDate debut, @Nullable RegDate fin, TypeMandat type, String nomMandataire, MockRue rue) {
       return addAdresseMandataireSuisse(ctb, debut, fin, type, nomMandataire, rue, null);
    }

	protected AdresseSuisse addAdresseSuisse(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, MockRue rue,CasePostale casePostale) {
		AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setUsage(usage);
		adresse.setNumeroRue(rue.getNoRue());
		if (casePostale != null) {
			adresse.setTexteCasePostale(casePostale.getType());
			adresse.setNumeroCasePostale(casePostale.getNumero());
			adresse.setNpaCasePostale(casePostale.getNpa());
		}
		adresse.setNumeroOrdrePoste(rue.getLocalite().getNoOrdre());
		adresse = (AdresseSuisse) tiersDAO.addAndSave(tiers, adresse);
		return adresse;
	}

	protected AdresseMandataireSuisse addAdresseMandataireSuisse(Contribuable ctb, RegDate debut, @Nullable RegDate fin, TypeMandat type, String nomMandataire, MockRue rue, CasePostale casePostale) {
		AdresseMandataireSuisse adresse = new AdresseMandataireSuisse();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setTypeMandat(type);
		adresse.setNomDestinataire(nomMandataire);
		adresse.setNumeroRue(rue.getNoRue());
		if (casePostale != null) {
			adresse.setTexteCasePostale(casePostale.getType());
			adresse.setNumeroCasePostale(casePostale.getNumero());
			adresse.setNpaCasePostale(casePostale.getNpa());
		}
		adresse.setNumeroOrdrePoste(rue.getLocalite().getNoOrdre());
		adresse = (AdresseMandataireSuisse) tiersDAO.addAndSave(ctb, adresse);
		return adresse;
	}

	// adresse suisse sans rue
	protected AdresseSuisse addAdresseSuisse(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, Integer noOrdre,CasePostale casePostale) {
		AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setUsage(usage);
		if (casePostale != null) {
			adresse.setTexteCasePostale(casePostale.getType());
			adresse.setNumeroCasePostale(casePostale.getNumero());
			adresse.setNpaCasePostale(casePostale.getNpa());
		}
		adresse.setNumeroOrdrePoste(noOrdre);
		adresse = (AdresseSuisse) tiersDAO.addAndSave(tiers, adresse);
		return adresse;
	}

    protected AdresseCivile addAdresseCivil(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, TypeAdresseCivil type) {
	    AdresseCivile adresse = new AdresseCivile();
        adresse.setDateDebut(debut);
        adresse.setDateFin(fin);
        adresse.setUsage(usage);
        adresse.setType(type);
        adresse = (AdresseCivile) tiersDAO.addAndSave(tiers, adresse);
        return adresse;
    }

    protected AdresseEtrangere addAdresseEtrangere(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, @Nullable String rue, @Nullable String numeroPostalEtLocalite, Pays pays) {
        return addAdresseEtrangere(tiers,usage,debut,fin,rue,numeroPostalEtLocalite,pays,null);
    }

    protected AdresseMandataireEtrangere addAdresseMandataireEtrangere(Contribuable ctb, RegDate debut, @Nullable RegDate fin, TypeMandat type, String nomMandataire, @Nullable String rue, @Nullable String numeroPostalEtLocalite, Pays pays) {
        return addAdresseMandataireEtrangere(ctb, debut, fin, type, nomMandataire, rue, numeroPostalEtLocalite, pays, null);
    }

	protected AdresseEtrangere addAdresseEtrangere(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, @Nullable String rue, @Nullable String numeroPostalEtLocalite,
	                                               Pays pays,CasePostale casePostale) {
		AdresseEtrangere adresse = new AdresseEtrangere();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setUsage(usage);
		adresse.setRue(rue);
		adresse.setNumeroPostalLocalite(numeroPostalEtLocalite);
		if (casePostale != null) {
			adresse.setTexteCasePostale(casePostale.getType());
			adresse.setNumeroCasePostale(casePostale.getNumero());
		}
		adresse.setNumeroOfsPays(pays.getNoOFS());
		adresse = (AdresseEtrangere) tiersDAO.addAndSave(tiers, adresse);
		return adresse;
	}

	protected AdresseMandataireEtrangere addAdresseMandataireEtrangere(Contribuable ctb, RegDate debut, @Nullable RegDate fin, TypeMandat type, String nomMandataire, @Nullable String rue, @Nullable String numeroPostalEtLocalite,
	                                                                   Pays pays, CasePostale casePostale) {
		AdresseMandataireEtrangere adresse = new AdresseMandataireEtrangere();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setNomDestinataire(nomMandataire);
		adresse.setTypeMandat(type);
		adresse.setRue(rue);
		adresse.setNumeroPostalLocalite(numeroPostalEtLocalite);
		if (casePostale != null) {
			adresse.setTexteCasePostale(casePostale.getType());
			adresse.setNumeroCasePostale(casePostale.getNumero());
		}
		adresse.setNumeroOfsPays(pays.getNoOFS());
		adresse = (AdresseMandataireEtrangere) tiersDAO.addAndSave(ctb, adresse);
		return adresse;
	}

    protected AdresseAutreTiers addAdresseAutreTiers(Tiers tiers, TypeAdresseTiers usage, RegDate debut, @Nullable RegDate fin, TypeAdresseTiers autreType, Tiers autreTiers) {
        AdresseAutreTiers adresse = new AdresseAutreTiers();
        adresse.setDateDebut(debut);
        adresse.setDateFin(fin);
        adresse.setUsage(usage);
        adresse.setAutreTiersId(autreTiers.getId());
        adresse.setType(autreType);
        adresse = (AdresseAutreTiers) tiersDAO.addAndSave(tiers, adresse);
        return adresse;
    }

    private CollectiviteAdministrative addCollAdm(MockCollectiviteAdministrative ca) {
        return addCollAdm(ca, null, null);
    }

    private CollectiviteAdministrative addCollAdm(MockOfficeImpot oid) {
        return addCollAdm(oid, oid.getIdentifiantDistrict(), oid.getIdentifiantRegion());
    }

    private CollectiviteAdministrative addCollAdm(MockCollectiviteAdministrative oid, @Nullable Integer identifiantDistrict, @Nullable Integer identifiantRegion) {
        CollectiviteAdministrative ca = new CollectiviteAdministrative();
        ca.setNumeroCollectiviteAdministrative(oid.getNoColAdm());
        if (identifiantDistrict != null) {
            ca.setIdentifiantDistrictFiscal(identifiantDistrict);
        }
        if (identifiantRegion != null) {
            ca.setIdentifiantRegionFiscale(identifiantRegion);
        }
        ca = hibernateTemplate.merge(ca);
        hibernateTemplate.flush();
        return ca;
    }

	protected DeclarationImpotSource addLRPeriodiciteUnique(DebiteurPrestationImposable debiteur, RegDate debut, RegDate fin, PeriodeFiscale periode) {
		return addLRPeriodiciteUnique(debiteur, debut, fin, periode, TypeEtatDeclaration.EMISE);
	}

	protected DeclarationImpotSource addLRPeriodiciteUnique(DebiteurPrestationImposable debiteur, RegDate debut, RegDate fin, PeriodeFiscale periode, TypeEtatDeclaration typeEtat) {
		DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.setDateDebut(debut);
		lr.setDateFin(fin);
		lr.setPeriode(periode);
		lr.setModeCommunication(ModeCommunication.PAPIER);
		lr.setPeriodicite(PeriodiciteDecompte.UNIQUE);

		// l'état "EMISE" si l'état demandé est autre (il faut au moins l'état "EMISE")
		if (typeEtat != TypeEtatDeclaration.EMISE) {
			final EtatDeclaration etatEmission = new EtatDeclarationEmise();
			etatEmission.setDateObtention(fin);
			lr.addEtat(etatEmission);
		}

		if (typeEtat != null) {
			final EtatDeclaration etat = EtatDeclarationHelper.getInstanceOfEtatDeclaration(typeEtat);
			etat.setDateObtention(fin);
			lr.addEtat(etat);
		}

		lr.setTiers(debiteur);
		lr = hibernateTemplate.merge(lr);
		debiteur.addDeclaration(lr);
		return lr;
	}

    protected DeclarationImpotSource addLR(DebiteurPrestationImposable debiteur, RegDate debut, PeriodiciteDecompte periodicite, PeriodeFiscale periode) {
        return addLR(debiteur, debut, periodicite, periode, TypeEtatDeclaration.EMISE);
    }

    protected DeclarationImpotSource addLR(DebiteurPrestationImposable debiteur, RegDate debut, PeriodiciteDecompte periodicite, PeriodeFiscale periode, TypeEtatDeclaration typeEtat) {
        DeclarationImpotSource lr = new DeclarationImpotSource();
        lr.setDateDebut(debut);

	    final RegDate fin = periodicite.getFinPeriode(debut);
        lr.setDateFin(fin);
        lr.setPeriode(periode);
        lr.setModeCommunication(ModeCommunication.PAPIER);
        lr.setPeriodicite(periodicite);

        // l'état "EMISE" si l'état demandé est autre (il faut au moins l'état "EMISE")
        if (typeEtat != TypeEtatDeclaration.EMISE) {
            final EtatDeclaration etatEmission = new EtatDeclarationEmise();
            etatEmission.setDateObtention(fin);
            lr.addEtat(etatEmission);
        }

        if (typeEtat != null) {
            final EtatDeclaration etat = EtatDeclarationHelper.getInstanceOfEtatDeclaration(typeEtat);
            etat.setDateObtention(fin);
            lr.addEtat(etat);
        }

        lr.setTiers(debiteur);
        lr = hibernateTemplate.merge(lr);
        debiteur.addDeclaration(lr);
        return lr;
    }


    protected DebiteurPrestationImposable addDebiteur(CategorieImpotSource categorie, PeriodiciteDecompte periodicite, RegDate debutValiditePeriodicite) {
        final DebiteurPrestationImposable debiteur = addDebiteur();
        debiteur.setCategorieImpotSource(categorie);
        final PeriodeDecompte periode = (periodicite == PeriodiciteDecompte.UNIQUE ? PeriodeDecompte.A : null);
        debiteur.addPeriodicite(new Periodicite(periodicite, periode, debutValiditePeriodicite, null));
        return debiteur;
    }

    protected ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable dpi, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture, TypeAutoriteFiscale typeAutorite, MockCommune commune) {
        ForDebiteurPrestationImposable f = createForDebiteur(dpi, dateDebut, motifOuverture, dateFin, motifFermeture, typeAutorite, commune);
        return tiersDAO.addAndSave(dpi, f);
    }

    protected ForDebiteurPrestationImposable createForDebiteur(DebiteurPrestationImposable dpi, RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture, TypeAutoriteFiscale typeAutorite, MockCommune commune) {
        final ForDebiteurPrestationImposable f = new ForDebiteurPrestationImposable();
        f.setTiers(dpi);
        f.setTypeAutoriteFiscale(typeAutorite);
        f.setNumeroOfsAutoriteFiscale(commune.getNoOFS());
        f.setDateDebut(dateDebut);
	    f.setMotifOuverture(motifOuverture);
        f.setDateFin(dateFin);
	    f.setMotifFermeture(motifFermeture);
        return f;
    }

    protected ForFiscalAutreElementImposable addForAutreElementImposable(Contribuable ctb, RegDate dateDebut, RegDate dateFin, MockCommune commune, TypeAutoriteFiscale taf,
                                                                         MotifRattachement rattachement) {
        ForFiscalAutreElementImposable f = new ForFiscalAutreElementImposable();
        f.setDateDebut(dateDebut);
        f.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
        f.setDateFin(dateFin);
        if (dateFin != null) {
            f.setMotifFermeture(MotifFor.FIN_EXPLOITATION);
        }
        f.setTypeAutoriteFiscale(taf);
        f.setNumeroOfsAutoriteFiscale(commune.getNoOFS());
        f.setMotifRattachement(rattachement);
        return tiersDAO.addAndSave(ctb, f);
    }

    /**
     * Ajoute une déclaration d'impôt ordinaire PP sur le contribuable spécifié.
     */
    protected DeclarationImpotOrdinairePP addDeclarationImpot(ContribuableImpositionPersonnesPhysiques tiers, PeriodeFiscale periode, RegDate debut, RegDate fin,
                                                              @Nullable TypeContribuable typeC, ModeleDocument modele) {

        final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
        assertNotNull("La collectivité administrative du CEDI n'a pas été définie", cedi);

        return addDeclarationImpot(tiers, periode, debut, fin, cedi, typeC, modele);
    }

    @Override
    protected <T extends DeclarationAvecNumeroSequence> T assignerNumeroSequenceEtSaveDeclarationImpot(Contribuable ctb, T di) {
        if (useTiersServiceToCreateDeclarationImpot()) {
            //noinspection unchecked
            return (T) tiersDAO.addAndSave(ctb, di);
        }
        else {
            return super.assignerNumeroSequenceEtSaveDeclarationImpot(ctb, di);
        }
    }

    /**
     * L'implémentation du tiersService est très bien, mais elle provoque une sauvegarde du tiers
     * et de la déclaration, ce qui n'est pas aprécié par tous les tests
     */
    protected boolean useTiersServiceToCreateDeclarationImpot() {
        return true;
    }

    protected SituationFamillePersonnePhysique addSituation(PersonnePhysique pp, RegDate debut, @Nullable RegDate fin, int nombreEnfants) {
        SituationFamille situation = new SituationFamillePersonnePhysique();
        situation.setDateDebut(debut);
        situation.setDateFin(fin);
        situation.setNombreEnfants(nombreEnfants);
        return (SituationFamillePersonnePhysique) tiersDAO.addAndSave(pp, situation);
    }

	protected SituationFamillePersonnePhysique addSituation(PersonnePhysique pp, RegDate debut, @Nullable RegDate fin, int nombreEnfants, ch.vd.uniregctb.type.EtatCivil etatCivil) {
		SituationFamille situation = new SituationFamillePersonnePhysique();
		situation.setDateDebut(debut);
		situation.setDateFin(fin);
		situation.setNombreEnfants(nombreEnfants);
		situation.setEtatCivil(etatCivil);
		return (SituationFamillePersonnePhysique) tiersDAO.addAndSave(pp, situation);
	}


	protected SituationFamilleMenageCommun addSituation(MenageCommun menage, RegDate debut, @Nullable RegDate fin, int nombreEnfants,
                                                        TarifImpotSource tarif) {
        SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
        situation.setDateDebut(debut);
        situation.setDateFin(fin);
        situation.setNombreEnfants(nombreEnfants);
        situation.setTarifApplicable(tarif);
        return (SituationFamilleMenageCommun) tiersDAO.addAndSave(menage, situation);
    }

	protected SituationFamilleMenageCommun addSituation(MenageCommun menage, RegDate debut, @Nullable RegDate fin, int nombreEnfants,
	                                                     TarifImpotSource tarif, ch.vd.uniregctb.type.EtatCivil etatCivil) {
		SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
		situation.setDateDebut(debut);
		situation.setDateFin(fin);
		situation.setNombreEnfants(nombreEnfants);
		situation.setTarifApplicable(tarif);
		situation.setEtatCivil(etatCivil);
		return (SituationFamilleMenageCommun) tiersDAO.addAndSave(menage, situation);
	}


	protected IdentificationPersonne addIdentificationPersonne(PersonnePhysique pp, CategorieIdentifiant categorie, String identifiant) {
        IdentificationPersonne ident = new IdentificationPersonne();
        ident.setCategorieIdentifiant(categorie);
        ident.setIdentifiant(identifiant);
        return tiersDAO.addAndSave(pp, ident);
    }

	protected IdentificationEntreprise addIdentificationEntreprise(Contribuable ctb, String numeroIDE) {
		final IdentificationEntreprise ie = new IdentificationEntreprise();
		ie.setNumeroIde(numeroIDE);
		return tiersDAO.addAndSave(ctb, ie);
	}

    private <T extends DonneeCivileEntreprise> T addDonneeCivileEntreprise(Entreprise e, T donneeCivile) {
        e.addDonneeCivile(donneeCivile);
        return donneeCivile;
    }

    protected RaisonSocialeFiscaleEntreprise addRaisonSociale(Entreprise e, RegDate dateDebut, @Nullable RegDate dateFin, String raisonSociale) {
        return addDonneeCivileEntreprise(e, new RaisonSocialeFiscaleEntreprise(dateDebut, dateFin, raisonSociale));
    }

    protected FormeJuridiqueFiscaleEntreprise addFormeJuridique(Entreprise e, RegDate dateDebut, @Nullable RegDate dateFin, FormeJuridiqueEntreprise formeJuridique) {
        return addDonneeCivileEntreprise(e, new FormeJuridiqueFiscaleEntreprise(dateDebut, dateFin, formeJuridique));
    }

    protected CapitalFiscalEntreprise addCapitalEntreprise(Entreprise e, RegDate dateDebut, RegDate dateFin, MontantMonetaire capital) {
        return addDonneeCivileEntreprise(e, new CapitalFiscalEntreprise(dateDebut, dateFin, capital));
    }

    protected DomicileEtablissement addDomicileEtablissement(Etablissement etb, RegDate dateDebut, @Nullable RegDate dateFin, MockCommune commune) {
        final DomicileEtablissement domicile = new DomicileEtablissement(dateDebut, dateFin, commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC, commune.getNoOFS(), etb);
        return tiersDAO.addAndSave(etb, domicile);
    }

    protected DomicileEtablissement addDomicileEtablissement(Etablissement etb, RegDate dateDebut, @Nullable RegDate dateFin, MockPays pays) {
        final DomicileEtablissement domicile = new DomicileEtablissement(dateDebut, dateFin, TypeAutoriteFiscale.PAYS_HS, pays.getNoOFS(), etb);
        return tiersDAO.addAndSave(etb, domicile);
    }

    protected RegimeFiscal addRegimeFiscalVD(Entreprise entreprise, RegDate dateDebut, @Nullable RegDate dateFin, TypeRegimeFiscal type) {
        final RegimeFiscal rf = new RegimeFiscal(dateDebut, dateFin, RegimeFiscal.Portee.VD, type.getCode());
        return tiersDAO.addAndSave(entreprise, rf);
    }

    protected RegimeFiscal addRegimeFiscalCH(Entreprise entreprise, RegDate dateDebut, @Nullable RegDate dateFin, TypeRegimeFiscal type) {
        final RegimeFiscal rf = new RegimeFiscal(dateDebut, dateFin, RegimeFiscal.Portee.CH, type.getCode());
        return tiersDAO.addAndSave(entreprise, rf);
    }

    protected DegrevementICI addDegrevementICI(Entreprise entreprise, ImmeubleRF immeuble, int pfDebut, @Nullable Integer pfFin, DonneesUtilisation locatif, DonneesUtilisation usagePropre, @Nullable DonneesLoiLogement loiLogement) {
    	final DegrevementICI deg = new DegrevementICI();
    	deg.setNonIntegrable(Boolean.FALSE);
    	deg.setImmeuble(immeuble);
    	deg.setContribuable(entreprise);
    	deg.setDateDebut(date(pfDebut, 1, 1));
    	if (pfFin != null) {
		    deg.setDateFin(date(pfFin, 12, 31));
	    }
    	deg.setLocation(locatif);
    	deg.setPropreUsage(usagePropre);
    	deg.setLoiLogement(loiLogement);
    	return tiersDAO.addAndSave(entreprise, deg);
    }

    protected AllegementFiscalConfederation addAllegementFiscalFederal(Entreprise entreprise,
                                                                       RegDate dateDebut,
                                                                       @Nullable RegDate dateFin,
                                                                       AllegementFiscal.TypeImpot typeImpot,
                                                                       @Nullable BigDecimal pourcentageAllegement,
                                                                       AllegementFiscalConfederation.Type type) {
        final AllegementFiscalConfederation af = new AllegementFiscalConfederation(dateDebut, dateFin, pourcentageAllegement, typeImpot, type);
        return tiersDAO.addAndSave(entreprise, af);
    }

    protected AllegementFiscalCanton addAllegementFiscalCantonal(Entreprise entreprise,
                                                                 RegDate dateDebut,
                                                                 @Nullable RegDate dateFin,
                                                                 AllegementFiscal.TypeImpot typeImpot,
                                                                 @Nullable BigDecimal pourcentageAllegement,
                                                                 AllegementFiscalCantonCommune.Type type) {
        final AllegementFiscalCanton af = new AllegementFiscalCanton(dateDebut, dateFin, pourcentageAllegement, typeImpot, type);
        return tiersDAO.addAndSave(entreprise, af);
    }

    protected AllegementFiscalCommune addAllegementFiscalCommunal(Entreprise entreprise,
                                                                  RegDate dateDebut,
                                                                  @Nullable RegDate dateFin,
                                                                  AllegementFiscal.TypeImpot typeImpot,
                                                                  @Nullable BigDecimal pourcentageAllegement,
                                                                  @Nullable MockCommune commune,
                                                                  AllegementFiscalCantonCommune.Type type) {
        final AllegementFiscalCommune af = new AllegementFiscalCommune(dateDebut, dateFin, pourcentageAllegement, typeImpot, type, commune != null ? commune.getNoOFS() : null);
        return tiersDAO.addAndSave(entreprise, af);
    }
}
