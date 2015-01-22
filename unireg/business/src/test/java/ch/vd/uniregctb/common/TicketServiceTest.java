package ch.vd.uniregctb.common;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class TicketServiceTest extends WithoutSpringTest {

	private TicketService ticketService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		ticketService = new TicketServiceImpl();
	}

	@Test(timeout = 1000)
	public void testAcquisitionAndRelease() throws Exception {
		final Long keyAnswer = 42L;
		final Long keyOther = 777L;

		final TicketService.Ticket ticket1 = ticketService.getTicket(keyAnswer, 0);
		Assert.assertNotNull(ticket1);

		final TicketService.Ticket ticket2 = ticketService.getTicket(keyOther, 0);
		Assert.assertNotNull(ticket2);

		ticketService.releaseTicket(ticket1);
		ticketService.releaseTicket(ticket2);
	}

	@Test(timeout = 1000)
	public void testConcurrency() throws Exception {
		final Long key = 42L;

		// je prends le ticket
		final TicketService.Ticket ticket = ticketService.getTicket(key, 0);
		Assert.assertNotNull(ticket);

		// le ticket n'est pas ré-entrant...
		final long ts1 = System.nanoTime();
		try {
			ticketService.getTicket(key, 300);
			Assert.fail("Aurait dû échouer...");
		}
		catch (TicketTimeoutException e) {
			// c'est bien ce qu'on attend...
		}

		final long ts2 = System.nanoTime();
		Assert.assertTrue(Long.toString(ts2 - ts1), ts2 - ts1 > TimeUnit.MILLISECONDS.toNanos(300));
	}

	@Test
	public void testWrongTicket() throws Exception {
		// ticket bidon venu d'ailleurs!
		final TicketService.Ticket ticket = new TicketService.Ticket() {};
		try {
			ticketService.releaseTicket(ticket);
			Assert.fail("Aurait dû échouer...");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Wrong ticket!", e.getMessage());
		}

		try {
			ticketService.releaseTicket(null);
			Assert.fail("Aurait dû échouer...");
		}
		catch (NullPointerException e) {
			Assert.assertEquals("ticket", e.getMessage());
		}
	}

	@Test(timeout = 1000)
	public void testNullKey() throws Exception {
		try {
			ticketService.getTicket(null, 2000);
			Assert.fail("Aurait dû échouer...");
		}
		catch (NullPointerException e) {
			Assert.assertEquals("key", e.getMessage());
		}
	}

	@Test(timeout = 1000)
	public void testDoubleRelease() throws Exception {

		final Long key = 42L;

		// je prends le ticket
		final TicketService.Ticket ticket = ticketService.getTicket(key, 0);
		Assert.assertNotNull(ticket);

		// je le relâche
		ticketService.releaseTicket(ticket);

		// ... et encore une fois
		try {
			ticketService.releaseTicket(ticket);
			Assert.fail("Aurait dû échouer...");
		}
		catch (IllegalStateException e) {
			Assert.assertEquals("Already released!", e.getMessage());
		}
	}
}
