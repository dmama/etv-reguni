package ch.vd.unireg.wsclient.rcpers;

import java.util.Collection;

import ch.vd.evd0001.v2.ListOfPersons;
import ch.vd.registre.base.date.RegDate;

public interface RcPersClient {

	ListOfPersons getPeople(Collection<Long> ids, RegDate date);
}
