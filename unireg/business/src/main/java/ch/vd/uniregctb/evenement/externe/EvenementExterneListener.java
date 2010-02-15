package ch.vd.uniregctb.evenement.externe;

import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public interface EvenementExterneListener extends ApplicationListener {

}
