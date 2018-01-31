package ch.vd.uniregctb.tiers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.StackedThreadLocal;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSource;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceServiceException;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Intercepteur de transaction qui recalcule de manière différée (juste avant le commit de la transaction)
 * le flag de blocage de remboursement automatique des tiers enregistrés pendant la transaction
 */
public class FlagBlocageRemboursementAutomatiqueInterceptor implements ModificationSubInterceptor, FlagBlocageRemboursementAutomatiqueCalculationRegister, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlagBlocageRemboursementAutomatiqueInterceptor.class);

	private final StackedThreadLocal<Set<Long>> idsTiersFlagACalculer = new StackedThreadLocal<>(HashSet::new);

	private ModificationInterceptor parent;
	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private PeriodeImpositionImpotSourceService periodeImpositionImpotSourceService;
	private IbanValidator ibanValidator;

	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setPeriodeImpositionImpotSourceService(PeriodeImpositionImpotSourceService periodeImpositionImpotSourceService) {
		this.periodeImpositionImpotSourceService = periodeImpositionImpotSourceService;
	}

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.parent.register(this);
	}

	@Override
	public void destroy() throws Exception {
		this.parent.unregister(this);
	}

	@Override
	public void enregistrerDemandeRecalcul(long tiersId) {
		final Set<Long> ids = idsTiersFlagACalculer.get();
		ids.add(tiersId);
	}

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {
		return false;
	}

	@Override
	public void postFlush() throws CallbackException {
		computeFlags();
	}

	private void computeFlags() {

		final Set<Long> ids = idsTiersFlagACalculer.get();
		if (!ids.isEmpty()) {

			// [SIFISC-12580] un re-calcul sur un couple doit forcer le re-calcul sur les personnes physiques qui le composent
			// -> phase 1 : on récupère tous les contribuables concernés
			final Map<Long, Contribuable> map = new HashMap<>(ids.size() * 3);      // un MC devient un MC + max 2 PP
			for (Long id : ids) {
				final Tiers tiers = tiersDAO.get(id);
				if (tiers instanceof PersonnePhysique || tiers instanceof Entreprise) {
					map.put(id, (Contribuable) tiers);
				}
				else if (tiers instanceof MenageCommun) {
					map.put(id, (Contribuable) tiers);

					// et les personnes physiques qui le composent
					final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, null);
					if (couple != null) {
						final PersonnePhysique principal = couple.getPrincipal();
						if (principal != null) {
							map.put(principal.getId(), principal);
						}

						final PersonnePhysique conjoint = couple.getConjoint();
						if (conjoint != null) {
							map.put(conjoint.getId(), conjoint);
						}
					}
				}
			}

			// et phase 2 : on lance le recalcul sur tous ces contribuables
			for (Contribuable ctb : map.values()) {

				// pour les entreprises, la seule présence d'un for vaudois ouvert est suffisante... alors qu'il faut
				// un for principal pour les assimilés PP
				final boolean hasForVaudoisOuvert = ctb.getForsFiscauxNonAnnules(false).stream()
						.filter(ff -> ff.getDateFin() == null)
						.filter(ff -> ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
						.anyMatch(ff -> ctb instanceof Entreprise || ff.isPrincipal());

				final boolean bloque;
				if (hasForVaudoisOuvert) {
					// for vaudois ouvert -> débloqué
					bloque = false;
				}
				else {
					// pas de for vaudois ouvert -> a prori bloqué, mais il y a des exceptions
					// SIFISC-9993 : si le tiers est une PP qui a un IBAN valide et qui a des PIIS (source uniquement), alors on laisse débloqué
					if (ctb instanceof PersonnePhysique && tiersService.getDateDeces((PersonnePhysique) ctb) == null && ibanValidator.isValidIban(ctb.getNumeroCompteBancaire())) {
						Boolean hasSeultSrc = null;
						try {
							final List<PeriodeImpositionImpotSource> piis = periodeImpositionImpotSourceService.determine((PersonnePhysique) ctb);
							for (PeriodeImpositionImpotSource pi : piis) {
								if (pi.getType() != PeriodeImpositionImpotSource.Type.SOURCE) {
									hasSeultSrc = Boolean.FALSE;
									break;
								}
								else if (hasSeultSrc == null) {
									hasSeultSrc = Boolean.TRUE;
								}
							}
						}
						catch (PeriodeImpositionImpotSourceServiceException e) {
							LOGGER.warn(String.format("Impossible de calculer les périodes d'imposition IS du contribuable %d sans for vaudois ouvert, les remboursements automatiques seront bloqués",
							                          ctb.getNumero()),
							            e);
						}

						// pas de piis, ou piis non-source trouvée -> bloqué
						bloque = hasSeultSrc == null || !hasSeultSrc;
					}
					else {
						// pas une personne physique vivante ou pas d'IBAN valide -> bloqué
						bloque = true;
					}
				}

				// sauvegarde du flag (directement en base parce que nous sommes ici en phase de "postFlush()" et que modifier l'entité se sert plus à rien)
				tiersDAO.setFlagBlocageRemboursementAutomatique(ctb.getNumero(), bloque);

				// TODO JDE faut-il "évicter" le tiers de la session ici ? (en cas de multiple flushes dans la transaction, pour forcer un reload du tiers avec son flag modifié)
			}
		}

		cleanup();
	}

	@Override
	public void suspendTransaction() {
		idsTiersFlagACalculer.pushState();
	}

	@Override
	public void resumeTransaction() {
		idsTiersFlagACalculer.popState();
	}

	@Override
	public void preTransactionCommit() {
	}

	@Override
	public void postTransactionCommit() {
		cleanup();
	}

	@Override
	public void postTransactionRollback() {
		cleanup();
	}

	private void cleanup() {
		final Set<Long> ids = idsTiersFlagACalculer.get();
		ids.clear();
	}
}
