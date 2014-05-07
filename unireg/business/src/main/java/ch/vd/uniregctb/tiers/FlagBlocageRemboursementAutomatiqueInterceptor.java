package ch.vd.uniregctb.tiers;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSource;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSourceServiceException;

/**
 * Intercepteur de transaction qui recalcule de manière différée (juste avant le commit de la transaction)
 * le flag de blocage de remboursement automatique des tiers enregistrés pendant la transaction
 */
public class FlagBlocageRemboursementAutomatiqueInterceptor implements ModificationSubInterceptor, FlagBlocageRemboursementAutomatiqueCalculationRegister, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(FlagBlocageRemboursementAutomatiqueInterceptor.class);

	private final ThreadLocal<Set<Long>> idsTiersFlagACalculer = new ThreadLocal<Set<Long>>() {
		@Override
		protected Set<Long> initialValue() {
			return new HashSet<>();
		}
	};

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
		idsTiersFlagACalculer.get().add(tiersId);
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
		for (Long id : idsTiersFlagACalculer.get()) {
			final Tiers tiers = tiersDAO.get(id);
			if (tiers instanceof PersonnePhysique || tiers instanceof MenageCommun) {
				final Contribuable ctb = (Contribuable) tiers;
				final ForFiscalPrincipal forVaudois = ctb.getDernierForFiscalPrincipalVaudois();

				final boolean bloque;
				if (forVaudois != null && forVaudois.getDateFin() == null) {
					// for vaudois ouvert -> débloqué
					bloque = false;
				}
				else {
					// pas de for vaudois ouvert -> a prori bloqué, mais il y a des exceptions
					// SIFISC-9993 : si le tiers est une PP qui a un IBAN valide et qui a des PIIS (source uniquement), alors on laisse débloqué
					if (tiers instanceof PersonnePhysique && tiersService.getDateDeces((PersonnePhysique) tiers) == null && ibanValidator.isValidIban(tiers.getNumeroCompteBancaire())) {
						Boolean hasSeultSrc = null;
						try {
							final List<PeriodeImpositionImpotSource> piis = periodeImpositionImpotSourceService.determine((PersonnePhysique) tiers);
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
							                          tiers.getNumero()),
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
				tiersDAO.setFlagBlocageRemboursementAutomatique(tiers.getNumero(), bloque);

				// TODO JDE faut-il "évicter" le tiers de la session ici ? (en cas de multiple flushes dans la transaction, pour forcer un reload du tiers avec son flag modifié)
			}
		}

		cleanup();
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
		idsTiersFlagACalculer.remove();
	}
}
