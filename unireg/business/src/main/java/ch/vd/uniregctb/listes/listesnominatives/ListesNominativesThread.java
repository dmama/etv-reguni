package ch.vd.uniregctb.listes.listesnominatives;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.ListesThread;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Thread utilisé dans la génération des listes nominatives
 */
public class ListesNominativesThread extends ListesThread<ListesNominativesResults> {

	private final TypeAdresse adressesIncluses;

	private final Set<TiersDAO.Parts> partsFiscales;

    public ListesNominativesThread(BlockingQueue<List<Long>> queue, RegDate dateTraitement, int nombreThreads, TypeAdresse adressesIncluses,
                                   boolean avecContribuablesPP, boolean avecContribuablesPM, boolean avecDebiteurs, TiersService tiersService,
                                   AdresseService adresseService, ServiceCivilCacheWarmer serviceCivilCacheWarmer, StatusManager status, AtomicInteger compteur, PlatformTransactionManager transactionManager,
                                   TiersDAO tiersDAO, HibernateTemplate hibernateTemplate) {

        super(queue, status, compteur, serviceCivilCacheWarmer, transactionManager, tiersDAO, hibernateTemplate,
                new ListesNominativesResults(dateTraitement, nombreThreads, adressesIncluses, avecContribuablesPP, avecContribuablesPM, avecDebiteurs, tiersService, adresseService));

	    this.adressesIncluses = adressesIncluses;
	    if (adressesIncluses == TypeAdresse.AUCUNE) {
		    partsFiscales = PARTS_FISCALES;
	    }
	    else {
		    final Set<TiersDAO.Parts> parts = new HashSet<TiersDAO.Parts>(PARTS_FISCALES.size() + 1);
		    parts.addAll(PARTS_FISCALES);
		    parts.add(TiersDAO.Parts.ADRESSES);
		    partsFiscales = Collections.unmodifiableSet(parts);
	    }
    }

    @Override
    protected void traiteNonContribuable(Tiers t) {
        super.traiteNonContribuable(t);

        if (t instanceof DebiteurPrestationImposable) {
            final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) t;
            try {
                getResults().addDebiteurPrestationImposable(debiteur);
            }
            catch (Exception e) {
                getResults().addErrorException(debiteur, e);
            }
        }
    }

	@Override
	protected Set<TiersDAO.Parts> getFiscalPartsToFetch() {
		return partsFiscales;
	}

	@Override
	protected void fillAttributesIndividu(Set<AttributeIndividu> attributes) {
		super.fillAttributesIndividu(attributes);
		if (adressesIncluses != TypeAdresse.AUCUNE) {
			attributes.add(AttributeIndividu.ADRESSES);
		}
	}
}
