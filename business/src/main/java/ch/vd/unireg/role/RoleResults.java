package ch.vd.unireg.role;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AbstractJobResults;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;

public abstract class RoleResults<R extends RoleResults<R>> extends AbstractJobResults<Long, R> {

	protected final AdresseService adresseService;
	protected final ServiceInfrastructureService infraService;
	protected final TiersService tiersService;
	protected final AssujettissementService assujettissementService;

	public final int annee;
	public final int nbThreads;

	private boolean interrupted = false;
	private int nbContribuablesTraites = 0;

	public final List<RoleError> errors = new LinkedList<>();
	public final List<RoleIgnore> ignores = new LinkedList<>();

	public static class RoleError {
		public final long noContribuable;
		public final String message;

		public RoleError(long noContribuable, String message) {
			this.noContribuable = noContribuable;
			this.message = message;
		}

		@Override
		public String toString() {
			return String.format("Contribuable %d, %s", noContribuable, message);
		}
	}

	public enum RaisonIgnore {
		PAS_CONCERNE_PAR_ROLE("Le contribuable n'apparaît pas sur le rôle annuel."),
		PAS_CONCERNE_PAR_ROLE_SUR_COMMUNE("Le contribuable n'apparaît pas sur le rôle annuel des communes ciblées."),
		DIPLOMATE_SUISSE("Diplomate suisse"),
		SOURCIER_GRIS("Sourcier gris");

		public final String displayLabel;

		RaisonIgnore(String displayLabel) {
			this.displayLabel = displayLabel;
		}
	}

	public static class RoleIgnore {
		public final long noContribuable;
		public final RaisonIgnore raison;

		public RoleIgnore(long noContribuable, RaisonIgnore raison) {
			this.noContribuable = noContribuable;
			this.raison = raison;
		}

		@Override
		public String toString() {
			return String.format("Contribuable %d, %s", noContribuable, raison);
		}
	}

	public RoleResults(int annee, int nbThreads, AdresseService adresseService, ServiceInfrastructureService infraService, TiersService tiersService, AssujettissementService assujettissementService) {
		this.annee = annee;
		this.nbThreads = nbThreads;
		this.adresseService = adresseService;
		this.infraService = infraService;
		this.tiersService = tiersService;
		this.assujettissementService = assujettissementService;
	}

	/**
	 * @return le type de population géré par cette extraction de rôle
	 */
	public abstract TypePopulationRole getTypePopulationRole();

	@Override
	public void addErrorException(Long element, Exception e) {
		errors.add(new RoleError(element, ExceptionUtils.extractCallStack(e)));
		addContribuableAuDecompte();
	}

	public void addIgnore(Contribuable contribuable, RaisonIgnore raison) {
		ignores.add(new RoleIgnore(contribuable.getNumero(), raison));
		addContribuableAuDecompte();
	}

	protected void addContribuableAuDecompte() {
		++ nbContribuablesTraites;
	}

	@Override
	public void end() {
		errors.sort(Comparator.comparingLong(e -> e.noContribuable));
		ignores.sort(Comparator.comparingLong(i -> i.noContribuable));
		super.end();
	}

	@Override
	public void addAll(R right) {
		nbContribuablesTraites += right.getNbContribuablesTraites();
		errors.addAll(right.errors);
		ignores.addAll(right.ignores);
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public int getNbContribuablesTraites() {
		return nbContribuablesTraites;
	}
}
