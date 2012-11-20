package ch.vd.uniregctb.tiers.manager;

import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class ForFiscalManagerTest extends WebTest {

    private ForFiscalManager manager;

    @Override
    public void onSetUp() throws Exception {
        super.onSetUp();
        manager = getBean(ForFiscalManager.class, "forFiscalManager");
    }

    @Test
    public void testFermetureForDebiteur() throws Exception {

        final RegDate dateDebut = date(2009, 1, 1);
        final RegDate dateFermeture = date(2010, 6, 30);

        // mise en place fiscale
        final ForFiscalView view = doInNewTransactionAndSession(new TransactionCallback<ForFiscalView>() {
            @Override
            public ForFiscalView doInTransaction(TransactionStatus status) {

                final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
                final ForDebiteurPrestationImposable ff = addForDebiteur(dpi, dateDebut, null, MockCommune.Bex);

                final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
                addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);

                return new ForFiscalView(ff, false, true);
            }
        });

        // fermeture du for
        view.setDateFermeture(dateFermeture);
        manager.updateFor(view);

        // vérification que le for est bien fermé et que le rapport de travail aussi
        doInNewTransactionAndSession(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(view.getNumeroCtb());
                assertNotNull(dpi);

                final ForDebiteurPrestationImposable ff = dpi.getForDebiteurPrestationImposableAt(dateFermeture);
                assertNotNull(ff);
                assertFalse(ff.isAnnule());
                assertEquals(dateDebut, ff.getDateDebut());
                assertEquals(dateFermeture, ff.getDateFin());

                final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
                assertNotNull(rapports);
                assertEquals(1, rapports.size());

                final RapportEntreTiers r = rapports.iterator().next();
                assertNotNull(r);
                assertInstanceOf(RapportPrestationImposable.class, r);
                assertEquals(dateDebut, r.getDateDebut());
                assertEquals(dateFermeture, r.getDateFin());
                assertFalse(r.isAnnule());
                return null;
            }
        });
    }
}
