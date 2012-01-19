package ch.vd.uniregctb.evenement.civil.engine;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.annulation.arrivee.AnnulationArriveeTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.deces.AnnulationDecesTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.depart.SuppressionDepartTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.divorce.AnnulationDivorceTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.individu.SuppressionIndividuTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.mariage.AnnulationMariageTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.reconciliation.AnnulationReconciliationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.separation.AnnulationSeparationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulation.veuvage.AnnulationVeuvageTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulationpermis.AnnulationPermisTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulationpermis.SuppressionNationaliteTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulationtutelle.AnnulationLeveeTutelleTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.annulationtutelle.AnnulationTutelleTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.ArriveeTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification.CorrectionAdresseTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification.ModificationAdresseNotificationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.arrivee.CorrectionDateArriveeTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.conjoint.CorrectionConjointTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.dateEtatCivil.CorrectionDateEtatCivilTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.dateNaissance.CorrectionDateNaissanceTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.depart.CorrectionDateDepartTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.filiation.CorrectionFiliationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.identificateur.ChangementIdentificateurTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.nationalite.CorrectionDateFinNationaliteTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.nationalite.CorrectionDateObtentionNationaliteTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.nationalite.RemiseBlancDateFinNationaliteTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.nom.ChangementNomTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.origine.CorrectionOrigineTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.permis.CorrectionDebutValiditePermisTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.permis.CorrectionFinValiditePermisTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.permis.CorrectionPermisTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.changement.sexe.ChangementSexeTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.deces.DecesTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.demenagement.DemenagementTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.depart.DepartTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.divorce.DivorceTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.fin.nationalite.FinNationaliteTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.fin.permis.FinPermisTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.ignore.DeclarationEtatCompletIndividuECH99TranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.mariage.MariageTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.naissance.NaissanceTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.obtentionpermis.ObtentionNationaliteTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.obtentionpermis.ObtentionPermisTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.reconciliation.ReconciliationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.separation.SeparationTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.testing.TestingTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.tutelle.LeveeTutelleTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.tutelle.TutelleTranslationStrategy;
import ch.vd.uniregctb.evenement.civil.interne.veuvage.VeuvageTranslationStrategy;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class EvenementCivilTranslatorImpl implements EvenementCivilTranslator, InitializingBean {

	private static final Map<TypeEvenementCivil, EvenementCivilTranslationStrategy> strategies = new EnumMap<TypeEvenementCivil, EvenementCivilTranslationStrategy>(TypeEvenementCivil.class);

	static {
		strategies.put(TypeEvenementCivil.ANNUL_ARRIVEE_SECONDAIRE, new AnnulationArriveeTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER, new AnnulationPermisTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_DATE_FIN_NATIONALITE_NON_SUISSE, new RemiseBlancDateFinNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_DATE_FIN_NATIONALITE_SUISSE, new RemiseBlancDateFinNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_DECES, new AnnulationDecesTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_DIVORCE, new AnnulationDivorceTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_LEVEE_TUTELLE, new AnnulationLeveeTutelleTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_MARIAGE, new AnnulationMariageTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_MESURE_TUTELLE, new AnnulationTutelleTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_RECONCILIATION, new AnnulationReconciliationTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_SEPARATION, new AnnulationSeparationTranslationStrategy());
		strategies.put(TypeEvenementCivil.ANNUL_VEUVAGE, new AnnulationVeuvageTranslationStrategy());
		strategies.put(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE, new ArriveeTranslationStrategy());
		strategies.put(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, new ArriveeTranslationStrategy());
		strategies.put(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, new ArriveeTranslationStrategy());
		strategies.put(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, new ArriveeTranslationStrategy());
		strategies.put(TypeEvenementCivil.ARRIVEE_SECONDAIRE, new ArriveeTranslationStrategy());
		strategies.put(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, new ObtentionPermisTranslationStrategy());
		strategies.put(TypeEvenementCivil.CHGT_CORREC_IDENTIFICATION, new ChangementIdentificateurTranslationStrategy());
		strategies.put(TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, new ChangementNomTranslationStrategy());
		strategies.put(TypeEvenementCivil.CHGT_SEXE, new ChangementSexeTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_ADRESSE, new CorrectionAdresseTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_CONJOINT, new CorrectionConjointTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_DATE_ARRIVEE, new CorrectionDateArriveeTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_DATE_DEPART, new CorrectionDateDepartTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_DATE_ETAT_CIVIL, new CorrectionDateEtatCivilTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_DATE_FIN_NATIONALITE_NON_SUISSE, new CorrectionDateFinNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_DATE_FIN_NATIONALITE_SUISSE, new CorrectionDateFinNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_DATE_NAISSANCE, new CorrectionDateNaissanceTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_DATE_OBTENTION_NATIONALITE_NON_SUISSE, new CorrectionDateObtentionNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_DATE_OBTENTION_NATIONALITE_SUISSE, new CorrectionDateObtentionNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_DEBUT_VALIDITE_PERMIS, new CorrectionDebutValiditePermisTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_FILIATION, new CorrectionFiliationTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_FIN_VALIDITE_PERMIS, new CorrectionFinValiditePermisTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_ORIGINE, new CorrectionOrigineTranslationStrategy());
		strategies.put(TypeEvenementCivil.CORREC_PERMIS, new CorrectionPermisTranslationStrategy());
		strategies.put(TypeEvenementCivil.DECES, new DecesTranslationStrategy());
		strategies.put(TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, new DemenagementTranslationStrategy());
		strategies.put(TypeEvenementCivil.DEPART_COMMUNE, new DepartTranslationStrategy());
		strategies.put(TypeEvenementCivil.DEPART_SECONDAIRE, new DepartTranslationStrategy());
		strategies.put(TypeEvenementCivil.DIVORCE, new DivorceTranslationStrategy());
		strategies.put(TypeEvenementCivil.ETAT_COMPLET, new DeclarationEtatCompletIndividuECH99TranslationStrategy());
		strategies.put(TypeEvenementCivil.EVENEMENT_TESTING, new TestingTranslationStrategy());
		strategies.put(TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER, new FinPermisTranslationStrategy());
		strategies.put(TypeEvenementCivil.FIN_NATIONALITE_NON_SUISSE, new FinNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.FIN_NATIONALITE_SUISSE, new FinNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.LEVEE_TUTELLE, new LeveeTutelleTranslationStrategy());
		strategies.put(TypeEvenementCivil.MARIAGE, new MariageTranslationStrategy());
		strategies.put(TypeEvenementCivil.MESURE_TUTELLE, new TutelleTranslationStrategy());
		strategies.put(TypeEvenementCivil.MODIF_ADRESSE_NOTIFICATION, new ModificationAdresseNotificationTranslationStrategy());
		strategies.put(TypeEvenementCivil.NAISSANCE, new NaissanceTranslationStrategy());
		strategies.put(TypeEvenementCivil.NATIONALITE_NON_SUISSE, new ObtentionNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.NATIONALITE_SUISSE, new ObtentionNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.RECONCILIATION, new ReconciliationTranslationStrategy());
		strategies.put(TypeEvenementCivil.SEPARATION, new SeparationTranslationStrategy());
		strategies.put(TypeEvenementCivil.SUP_ARRIVEE_DANS_COMMUNE, new AnnulationArriveeTranslationStrategy());
		strategies.put(TypeEvenementCivil.SUP_DEPART_COMMUNE, new SuppressionDepartTranslationStrategy());
		strategies.put(TypeEvenementCivil.SUP_DEPART_SECONDAIRE, new SuppressionDepartTranslationStrategy());
		strategies.put(TypeEvenementCivil.SUP_INDIVIDU, new SuppressionIndividuTranslationStrategy());
		strategies.put(TypeEvenementCivil.SUP_NATIONALITE_NON_SUISSE, new SuppressionNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.SUP_NATIONALITE_SUISSE, new SuppressionNationaliteTranslationStrategy());
		strategies.put(TypeEvenementCivil.VEUVAGE, new VeuvageTranslationStrategy());
	}

	private ServiceCivilService serviceCivilService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private TiersDAO tiersDAO;
	private DataEventService dataEventService;
	private TiersService tiersService;
	private MetierService metierService;
	private AdresseService adresseService;
	private GlobalTiersIndexer indexer;
	private EvenementFiscalService evenementFiscalService;

	private EvenementCivilContext context;

	@Override
	public EvenementCivilInterne toInterne(EvenementCivilExterne event, EvenementCivilOptions options) throws EvenementCivilException {
		final EvenementCivilTranslationStrategy strategy = strategies.get(event.getType());
		if (strategy == null) {
			throw new EvenementCivilException("Aucune stratégie de traduction n'existe pour l'événement de type = [" + event.getType() + ']');
		}
		return strategy.create(event, context, options);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		context = new EvenementCivilContext(serviceCivilService, serviceInfrastructureService, dataEventService, tiersService, indexer, metierService, tiersDAO, adresseService, evenementFiscalService);
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setContext(EvenementCivilContext context) {
		this.context = context;
	}
}
