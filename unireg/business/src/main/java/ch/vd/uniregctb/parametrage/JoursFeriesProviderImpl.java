package ch.vd.uniregctb.parametrage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDate.WeekDay;

public class JoursFeriesProviderImpl implements JoursFeriesProvider {

	private ParametreAppService parametreAppService;

	/**
	 * Easter - compute the day on which Easter falls.
	 *
	 * In the Christian religion, Easter is possibly the most important holiday of
	 * the year, so getting its date <I>just so </I> is worthwhile.
	 *
	 * @author: Ian F. Darwin, http://www.darwinsys.com/, based on a detailed
	 *          algorithm in Knuth, vol 1, pg 155.
	 *
	 * @Version: $Id: Easter.java,v 1.5 2004/02/09 03:33:46 ian Exp $ Written in C,
	 *           Toronto, 1988. Java version 1996.
	 *
	 * @Note: It's not proven correct, although it gets the right answer for years
	 *        around the present.
	 */
  private static Calendar findEaster(int year) {
    if (year <= 1582) {
      throw new IllegalArgumentException(
          "Algorithm invalid before April 1583");
    }
    int golden, century, x, z, d, epact, n;

    golden = (year % 19) + 1; /* E1: metonic cycle */
    century = (year / 100) + 1; /* E2: e.g. 1984 was in 20th C */
    x = (3 * century / 4) - 12; /* E3: leap year correction */
    z = ((8 * century + 5) / 25) - 5; /* E3: sync with moon's orbit */
    d = (5 * year / 4) - x - 10;
    epact = (11 * golden + 20 + z - x) % 30; /* E5: epact */
    if ((epact == 25 && golden > 11) || epact == 24)
      epact++;
    n = 44 - epact;
    n += 30 * (n < 21 ? 1 : 0); /* E6: */
    n += 7 - ((d + n) % 7);
    if (n > 31) /* E7: */
      return new GregorianCalendar(year, 4 - 1, n - 31); /* April */
    else
      return new GregorianCalendar(year, 3 - 1, n); /* March */
  }

  private RegDate getPaques(int annee) {
	  return RegDate.get(findEaster(annee).getTime());
  }

  public RegDate getLundiPaques(int annee) {
	  return getPaques(annee).getOneDayAfter();
  }

  public RegDate getVendrediSaint(int annee) {
	  return getPaques(annee).addDays(-2);
  }

  public RegDate getJeudiAscension(int annee) {
	  return getPaques(annee).addDays(39);
  }

  public RegDate getLundiPentecote(int annee) {
	  return getPaques(annee).addDays(50);
  }

  public RegDate getJeuneFederal(int annee) {
	  // Le jeune fédéral correspond au lundi suivant le 3ème dimanche du mois de septembre
	  RegDate dateJeune = RegDate.get(annee, RegDate.SEPTEMBRE, 1);
	  while (!WeekDay.SUNDAY.equals(dateJeune.getWeekDay())) {
		  dateJeune = dateJeune.getOneDayAfter();
	  }
	  return dateJeune.addDays(15);
  }

  public  RegDate getNoel (int annee) {
	  if (parametreAppService == null) {
		  return RegDate.get(annee, RegDate.DECEMBRE, 25);
	  } else {
		  Integer[] noel = parametreAppService.getNoel();
		  return RegDate.get(annee, noel[1], noel[0]);
	  }
  }

  public  RegDate getNouvelAn (int annee) {
	  if (parametreAppService == null) {
		  return RegDate.get(annee, RegDate.JANVIER, 1);
	  } else {
		  Integer[] nouvelAn = parametreAppService.getNouvelAn();
		  return RegDate.get(annee, nouvelAn[1], nouvelAn[0]);
	  }
  }

  public  RegDate getLendemainNouvelAn(int annee) {
	  if (parametreAppService == null) {
		  return RegDate.get(annee, RegDate.JANVIER, 2);
	  } else {
		  Integer[] lendemainNouvelAn = parametreAppService.getLendemainNouvelAn();
		  return RegDate.get(annee, lendemainNouvelAn[1], lendemainNouvelAn[0]);
	  }
  }

  public  RegDate getFeteNationale(int annee) {
	  if (parametreAppService == null) {
		  return RegDate.get(annee, RegDate.AOUT, 1);
	  } else {
		  Integer[] feteNationale = parametreAppService.getFeteNationale();
		  return RegDate.get(annee, feteNationale[1], feteNationale[0]);
	  }

  }



	public List<RegDate> getDatesJoursFeries(int annee) {
		ArrayList<RegDate> joursFeries = new ArrayList<RegDate>(9);
		joursFeries.add(getNouvelAn(annee));
		joursFeries.add(getLendemainNouvelAn(annee));
		joursFeries.add(getVendrediSaint(annee));
		joursFeries.add(getLundiPaques(annee));
		joursFeries.add(getJeudiAscension(annee));
		joursFeries.add(getLundiPentecote(annee));
		joursFeries.add(getFeteNationale(annee));
		joursFeries.add(getJeuneFederal(annee));
		joursFeries.add(getNoel(annee));
		return joursFeries;
	}

	public ParametreAppService getParametreAppService() {
		return parametreAppService;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	public static void main(String[] argv) {
		for (String anArgv : argv) {
			int year = 0;
			JoursFeriesProviderImpl jfp = new JoursFeriesProviderImpl();
			try {
				year = Integer.parseInt(anArgv);
				System.out.println("1.na: " + jfp.getNouvelAn(year).asJavaDate());
				System.out.println("2.lna: " + jfp.getLendemainNouvelAn(year).asJavaDate());
				System.out.println("3.vs: " + jfp.getVendrediSaint(year).asJavaDate());
				System.out.println("4.lpa: " + jfp.getLundiPaques(year).asJavaDate());
				System.out.println("5.ja: " + jfp.getJeudiAscension(year).asJavaDate());
				System.out.println("6.lpe: " + jfp.getLundiPentecote(year).asJavaDate());
				System.out.println("7.fn: " + jfp.getFeteNationale(year).asJavaDate());
				System.out.println("8.jf: " + jfp.getJeuneFederal(year).asJavaDate());
				System.out.println("9.n: " + jfp.getNoel(year).asJavaDate());
				System.out.println("------");

			}
			catch (IllegalArgumentException e) {
				System.err.println("Year " + anArgv + " invalid ("
						+ e.getMessage() + ").");
			}
		}
	}

}
