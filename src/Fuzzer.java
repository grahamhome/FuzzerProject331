import com.gargoylesoftware.htmlunit.WebClient;


public class Fuzzer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
		discoverLinks(webClient);
		webClient.closeAllWindows();

	}

}
