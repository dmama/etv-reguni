package ch.vd.uniregctb.security;

import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class DroitAccesServiceImpl implements DroitAccesService {

	private TiersDAO tiersDAO;
	private DroitAccesDAO droitAccesDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setDroitAccesDAO(DroitAccesDAO droitAccesDAO) {
		this.droitAccesDAO = droitAccesDAO;
	}

	/**
	 * {@inheritDoc}
	 */
	public DroitAcces addDroitAcces(long operateurId, long tiersId, TypeDroitAcces type, Niveau niveau) throws DroitAccesException {

		final RegDate aujourdhui = RegDate.get();

		final DroitAcces da = droitAccesDAO.getDroitAcces(operateurId, tiersId, aujourdhui);
		if (da != null && !da.isAnnule()) {
			throw new DroitAccesException("Un droit d'accès existe déjà entre l'opérateur n°" + operateurId + " et le tiers n°" + tiersId);
		}

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			throw new DroitAccesException("Le tiers n°" + tiersId + " n'existe pas.");
		}
		if (!(tiers instanceof PersonnePhysique)) {
			throw new DroitAccesException("Le tiers n°" + tiersId + " n'est pas une personne physique.");
		}

		final DroitAcces droitAcces = new DroitAcces();
		droitAcces.setDateDebut(aujourdhui);
		droitAcces.setNiveau(niveau);
		droitAcces.setType(type);
		droitAcces.setNoIndividuOperateur(operateurId);
		droitAcces.setTiers((PersonnePhysique) tiers);

		return droitAccesDAO.save(droitAcces);
	}

	/**
	 * {@inheritDoc}
	 */
	public void annuleDroitAcces(long id) throws DroitAccesException {

		final DroitAcces da = droitAccesDAO.get(id);
		if (da == null) {
			throw new DroitAccesException("Le droit d'accès n°" + id + " n'existe pas");
		}
		da.setAnnule(true);
		da.setDateFin(RegDate.get());
	}

	/**
	 * {@inheritDoc}
	 */
	public void copieDroitsAcces(long operateurSourceId, long operateurTargetId) throws DroitAccesException {
		copie(operateurSourceId, operateurTargetId, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public void transfereDroitsAcces(long operateurSourceId, long operateurTargetId) throws DroitAccesException {
		copie(operateurSourceId, operateurTargetId, true);
	}

	private void copie(long operateurSourceId, long operateurTargetId, final boolean fermeSource) throws DroitAccesException {

		final List<DroitAcces> source = droitAccesDAO.getDroitsAcces(operateurSourceId);
		final List<DroitAcces> target = droitAccesDAO.getDroitsAcces(operateurTargetId);

		final RegDate aujourdhui = RegDate.get();

		for (DroitAcces s : source) {
			if (!s.isValidAt(aujourdhui)) {
				// inutile de copier les droits antérieurs
				continue;
			}

			boolean copieNecessaire = true;

			// la spécification dit qu'un conflit est un droit qui serait à la fois en écriture et en lecture,
			// ou à la fois une autorisation et une interdiction
			for (DroitAcces t : target) {
				if (!t.isAnnule() && s.getTiers().getNumero().equals(t.getTiers().getNumero()) && DateRangeHelper.intersect(s, t)) {
					if (s.getNiveau() != t.getNiveau() || s.getType() != t.getType()) {
						final String msg = String.format(
								"Impossible de %s le droit d'accès de l'opérateur %d à l'opérateur %d sur le dossier %s car l'opérateur %d possède déjà un droit sur le même dossier qui entrerait en conflit",
								fermeSource ? "transférer" : "copier", operateurSourceId, operateurTargetId, FormatNumeroHelper.numeroCTBToDisplay(s.getTiers().getNumero()),
								operateurTargetId);
						throw new DroitAccesException(msg);
					}
					else {
						// pas de conflit, mais droit existant -> il est ignoré
						t.setDateFin(null);
						if (!RegDateHelper.isAfterOrEqual(aujourdhui, t.getDateDebut(), NullDateBehavior.EARLIEST)) {
							t.setDateDebut(aujourdhui);
						}
						copieNecessaire = false;
					}
				}
			}

			if (copieNecessaire) {
				// on copie le droit
				final DroitAcces da = s.duplicate();
				da.setNoIndividuOperateur(operateurTargetId);
				da.setDateDebut(aujourdhui);
				da.setDateFin(null);
				droitAccesDAO.save(da);
			}

			if (fermeSource) {
				s.setDateFin(aujourdhui);
			}
		}
	}

}
