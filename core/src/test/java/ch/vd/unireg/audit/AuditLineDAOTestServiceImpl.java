package ch.vd.unireg.audit;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
public class AuditLineDAOTestServiceImpl implements AuditLineDAOTestService {

	@Override
	public void logAuditAndThrowException() {

		Audit.info("Blabla");
		throw new RuntimeException();
	}

}