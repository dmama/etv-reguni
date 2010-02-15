package ch.vd.uniregctb.evenement;

import java.sql.SQLException;
import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

/**
 * DAO des événements fiscaux..
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class EvenementFiscalDAOImpl extends GenericDAOImpl<EvenementFiscal, Long> implements EvenementFiscalDAO {

	//private static final Logger LOGGER = Logger.getLogger(EvenementFiscalDAOImpl.class);

	public EvenementFiscalDAOImpl() {
		super(EvenementFiscal.class);
	}

	/**
	 * Créer une nouvelle instance d'un événement situation de famille transient.
	 * @param tiers
	 * @param typeEvenement
	 * @param dateEvenement
	 * @param id de la situation de famille
	 * @return Retourne une nouvelle instance d'un événement situation de famille transient.
	 */
	public EvenementFiscalSituationFamille creerEvenementSituationFamille(Tiers tiers, TypeEvenementFiscal typeEvenement,
												RegDate dateEvenement, Long id) {
		if( tiers == null) {
			throw new IllegalArgumentException("tiers ne peut être null.");
		}
		if( typeEvenement == null) {
			throw new IllegalArgumentException("typeEvenement ne peut être null.");
		}
		if( dateEvenement == null) {
			throw new IllegalArgumentException("dateEvenement ne peut être null.");
		}
		Assert.isEqual(TypeEvenementFiscal.CHANGEMENT_SITUATION_FAMILLE, typeEvenement);
		EvenementFiscalSituationFamille evenementSituationFamille = new EvenementFiscalSituationFamille();
		evenementSituationFamille.setTiers(tiers);
		evenementSituationFamille.setDateEvenement(dateEvenement);
		evenementSituationFamille.setType(typeEvenement);
		evenementSituationFamille.setNumeroTechnique(id);
		return evenementSituationFamille;
	}

	/**
	 * Créer une nouvelle instance d'un événement for transient.
	 * @param tiers
	 * @param typeEvenement
	 * @param dateEvenement
	 * @param motifFor
	 * @param id du for
	 * @return Retourne une nouvelle instance d'un événement for transient.
	 */
	public EvenementFiscalFor creerEvenementFor(Tiers tiers, TypeEvenementFiscal typeEvenement,
			RegDate dateEvenement, MotifFor motifFor, ModeImposition modeImposition, Long id) {
		if( tiers == null) {
			throw new IllegalArgumentException("tiers ne peut être null.");
		}
		if( typeEvenement == null) {
			throw new IllegalArgumentException("typeEvenement ne peut être null.");
		}
		if( dateEvenement == null) {
			throw new IllegalArgumentException("dateEvenement ne peut être null.");
		}
		Assert.isTrue(typeEvenement.equals(TypeEvenementFiscal.ANNULATION_FOR) ||
				typeEvenement.equals(TypeEvenementFiscal.CHANGEMENT_MODE_IMPOSITION) ||
				typeEvenement.equals(TypeEvenementFiscal.FERMETURE_FOR) ||
				typeEvenement.equals(TypeEvenementFiscal.OUVERTURE_FOR));
		EvenementFiscalFor evenementFor = new EvenementFiscalFor();
		evenementFor.setTiers(tiers);
		evenementFor.setDateEvenement(dateEvenement);
		evenementFor.setType(typeEvenement);
		if (typeEvenement.equals(TypeEvenementFiscal.CHANGEMENT_MODE_IMPOSITION)) {
			evenementFor.setModeImposition(modeImposition);
		}
		else if (typeEvenement.equals(TypeEvenementFiscal.ANNULATION_FOR)) {
			//pas de motif ni de mode d'imposition
		}
		else {
			evenementFor.setMotifFor(motifFor);
		}
		evenementFor.setNumeroTechnique(id);
		return evenementFor;
	}

	/**
	 * Créer une nouvelle instance d'un événement LR transient.
	 * @param tiers
	 * @param typeEvenement
	 * @param dateEvenement
	 * @param id de la LR
	 * @param dateDebutPeriode
	 * @param dateFinPeriode
	 * @return Retourne une nouvelle instance d'un événement LR transient.
	 */
	public EvenementFiscalLR creerEvenementLR(Tiers tiers, TypeEvenementFiscal typeEvenement,
			RegDate dateEvenement, Long id, RegDate dateDebutPeriode, RegDate dateFinPeriode) {
		if( tiers == null) {
			throw new IllegalArgumentException("tiers ne peut être null.");
		}
		if( typeEvenement == null) {
			throw new IllegalArgumentException("typeEvenement ne peut être null.");
		}
		if( dateEvenement == null) {
			throw new IllegalArgumentException("dateEvenement ne peut être null.");
		}
		if( dateDebutPeriode == null) {
			throw new IllegalArgumentException("dateDebutPeriode ne peut être null.");
		}
		if( dateFinPeriode == null) {
			throw new IllegalArgumentException("dateFinPeriode ne peut être null.");
		}
		Assert.isTrue(typeEvenement.equals(TypeEvenementFiscal.ANNULATION_LR) ||
				typeEvenement.equals(TypeEvenementFiscal.LR_MANQUANTE) ||
				typeEvenement.equals(TypeEvenementFiscal.OUVERTURE_PERIODE_DECOMPTE_LR) ||
				typeEvenement.equals(TypeEvenementFiscal.RETOUR_LR) ||
				typeEvenement.equals(TypeEvenementFiscal.SOMMATION_LR));
		EvenementFiscalLR evenementLR = new EvenementFiscalLR();
		evenementLR.setTiers(tiers);
		evenementLR.setDateEvenement(dateEvenement);
		evenementLR.setType(typeEvenement);
		evenementLR.setDateDebutPeriode(dateDebutPeriode);
		evenementLR.setDateFinPeriode(dateFinPeriode);
		evenementLR.setNumeroTechnique(id);
		return evenementLR;
	}

	/**
	 * Créer une nouvelle instance d'un événement DI transient.
	 * @param tiers
	 * @param typeEvenement
	 * @param dateEvenement
	 * @param id de la DI
	 * @param dateDebutPeriode
	 * @param dateFinPeriode
	 * @return Retourne une nouvelle instance d'un événement DI transient.
	 */
	public EvenementFiscalDI creerEvenementDI(Tiers tiers, TypeEvenementFiscal typeEvenement,
			RegDate dateEvenement, Long id, RegDate dateDebutPeriode, RegDate dateFinPeriode) {
		if( tiers == null) {
			throw new IllegalArgumentException("tiers ne peut être null.");
		}
		if( typeEvenement == null) {
			throw new IllegalArgumentException("typeEvenement ne peut être null.");
		}
		if( dateEvenement == null) {
			throw new IllegalArgumentException("dateEvenement ne peut être null.");
		}
		if( dateDebutPeriode == null) {
			throw new IllegalArgumentException("dateDebutPeriode ne peut être null.");
		}
		if( dateFinPeriode == null) {
			throw new IllegalArgumentException("dateFinPeriode ne peut être null.");
		}
		Assert.isTrue(typeEvenement.equals(TypeEvenementFiscal.ANNULATION_DI) ||
				typeEvenement.equals(TypeEvenementFiscal.ECHEANCE_DI) ||
				typeEvenement.equals(TypeEvenementFiscal.ENVOI_DI) ||
				typeEvenement.equals(TypeEvenementFiscal.RETOUR_DI) ||
				typeEvenement.equals(TypeEvenementFiscal.SOMMATION_DI) ||
				typeEvenement.equals(TypeEvenementFiscal.TAXATION_OFFICE));
		EvenementFiscalDI evenementDI = new EvenementFiscalDI();
		evenementDI.setTiers(tiers);
		evenementDI.setDateEvenement(dateEvenement);
		evenementDI.setType(typeEvenement);
		evenementDI.setDateDebutPeriode(dateDebutPeriode);
		evenementDI.setDateFinPeriode(dateFinPeriode);
		evenementDI.setNumeroTechnique(id);
		return evenementDI;
	}


	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public Collection<EvenementFiscal> getEvenementFiscals(final Tiers tiers)  {
		return (Collection<EvenementFiscal>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(EvenementFiscal.class);
				criteria.add(Restrictions.eq("tiers", tiers));
				return criteria.list();
			}
		});
	}

}
