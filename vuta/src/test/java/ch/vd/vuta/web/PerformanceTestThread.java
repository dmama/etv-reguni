package ch.vd.vuta.web;

public class PerformanceTestThread extends Thread {

	private boolean marcheOuMarchePas;
	private PerformanceTest test;
	
	public PerformanceTestThread(PerformanceTest test, boolean marcheOuMarchePas) {
		
		this.test = test;
		this.marcheOuMarchePas = marcheOuMarchePas;
	}
	
	public void run() {
		
		try {
			test.runSms(marcheOuMarchePas);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
