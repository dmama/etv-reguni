package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.BusinessHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Debiteur", propOrder = {
		"raisonSociale", "categorie", "periodiciteDecompte", "periodeDecompte", "modeCommunication", "sansRappel",
		"sansListRecapitulative", "contribuableAssocie", "periodicites","logicielId"
})
public class Debiteur extends Tiers {

	@XmlElement(required = true)
	public String raisonSociale;

	@XmlElement(required = true)
	public CategorieDebiteur categorie;

	@XmlElement(required = true)
	public PeriodiciteDecompte periodiciteDecompte;

	@XmlElement(required = false)
	public PeriodeDecompte periodeDecompte;

	@XmlElement(required = true)
	public ModeCommunication modeCommunication;

	@XmlElement(required = true)
	public boolean sansRappel;

	@XmlElement(required = true)
	public boolean sansListRecapitulative;

	@XmlElement(required = false)
	public Long logicielId;

	/**
	 * Le numéro du contribuable associé à ce débiteur; ou <b>null</b> si le débiteur n'est pas lié à un contribuable.
	 */
	@XmlElement(required = false)
	public Long contribuableAssocie;

	/**
	 * Liste des périodicités
	 */
	@XmlElement(required = false)
	public List<Periodicite> periodicites = null;

	public Debiteur() {
	}

	public Debiteur(ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur, Set<TiersPart> parts, ch.vd.registre.base.date.RegDate date,
	                Context context) throws BusinessException {
		super(debiteur, parts, date, context);

		this.raisonSociale = BusinessHelper.getRaisonSociale(debiteur, date, context.adresseService);
		this.categorie = EnumHelper.coreToWeb(debiteur.getCategorieImpotSource());

		final ch.vd.uniregctb.declaration.Periodicite periodiciteAtDate = debiteur.getPeriodiciteAt(date);
		if (periodiciteAtDate != null) {
			this.periodeDecompte = EnumHelper.coreToWeb(periodiciteAtDate.getPeriodeDecompte());
			this.periodiciteDecompte = EnumHelper.coreToWeb(periodiciteAtDate.getPeriodiciteDecompte());
		}
		else {
			this.periodeDecompte = null;
			this.periodiciteDecompte = null;
		}

		this.modeCommunication = EnumHelper.coreToWeb(debiteur.getModeCommunication());
		this.sansRappel = DataHelper.coreToWeb(debiteur.getSansRappel());
		this.sansListRecapitulative = DataHelper.coreToWeb(debiteur.getSansListeRecapitulative());
		this.contribuableAssocie = debiteur.getContribuableId();
		if (this.modeCommunication == ModeCommunication.ELECTRONIQUE) {
			this.logicielId = debiteur.getLogicielId();
		}
		if (parts != null && parts.contains(TiersPart.PERIODICITES)) {
			this.periodicites = new ArrayList<Periodicite>();
			for (ch.vd.uniregctb.declaration.Periodicite periodicite : debiteur.getPeriodicitesNonAnnules(true)) {
				this.periodicites.add(new Periodicite(periodicite));
			}
		}
	}

	public Debiteur(Debiteur debiteur, Set<TiersPart> parts) {
		super(debiteur, parts);

		this.raisonSociale = debiteur.raisonSociale;
		this.categorie = debiteur.categorie;
		this.periodiciteDecompte = debiteur.periodiciteDecompte;
		this.periodeDecompte = debiteur.periodeDecompte;
		this.modeCommunication = debiteur.modeCommunication;
		this.sansRappel = debiteur.sansRappel;
		this.sansListRecapitulative = debiteur.sansListRecapitulative;
		this.contribuableAssocie = debiteur.contribuableAssocie;
		this.periodicites = debiteur.periodicites;
		this.logicielId = debiteur.logicielId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Tiers clone(Set<TiersPart> parts) {
		return new Debiteur(this, parts);
	}
}
