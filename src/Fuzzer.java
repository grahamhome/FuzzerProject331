import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class Fuzzer {
	
	// Default time for a response to be considered slow, in ms.
	private static long slowResponse = 500;
	
	//Default for random input checking
	private static boolean random = false;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		if(args.length < 3){
			Fuzzer.usage();
		}
		String url = args[1];
		HashMap<String,String> opts = options(args);
		
		if(args[0].toLowerCase().equals("discover")){
			if(opts.get("commonWords") == null){
				Fuzzer.usage();
			}
			
			List<String> words = Fuzzer.getCommonWords(opts.get("commonWords"));

			WebClient webClient = new WebClient();
			
			
			try {	
				webClient.getPage(url);
			} catch (FailingHttpStatusCodeException | IOException e) {
				e.printStackTrace();
			}
			
			//Discovery Steps:
			
			System.out.println("Authenticated: " + authentication(webClient, url, opts) + '\n');
			
			List<HtmlAnchor> links = Fuzzer.discoverLinks(webClient,words);
				
			HashMap<String, List<HtmlElement>> inputs = Fuzzer.discoverFormInputs(webClient, links);
			System.out.println('\n' + "Form Inputs: ");
			for (Map.Entry<String, List<HtmlElement>> e : inputs.entrySet()) {
				System.out.println(e.getKey() + ": " + e.getValue());
			}
			
			System.out.println(displayParams(parseURLs(webClient, links), webClient));
			
			System.out.println('\n' + "Cookies: " + getCookies(webClient));

			webClient.closeAllWindows();
		}
		else if(args[0].toLowerCase().equals("test")){
			System.out.println("Part 2 of project");
			if(opts.get("vector") == null
					|| opts.get("sensitive") == null
					|| opts.get("commonWords") == null){
				Fuzzer.usage();
			}
			List<String> vectors = Fuzzer.getVectors(opts.get("vector"));
			List<String> sensitiveWords = Fuzzer.getSensitiveWords(opts.get("sensitive"));
		}else{
			Fuzzer.usage();
		}

	}
	
	/**
	 * Discovers and returns the cookies used by a particular webpage.
	 * @param webClient : a webClient which has already been authenticated
	 * 
	 * @return : returns a list of cookies.
	 */
	
	private static ArrayList<Cookie> getCookies(WebClient webClient) {
		ArrayList<Cookie> cookies = new ArrayList<Cookie>();
		cookies.addAll(webClient.getCookies(webClient.getCurrentWindow().getEnclosedPage().getUrl()));
		return cookies;
	}
	
	/**
	 * 
	 * @param webClient : a webClient object which has already been authenticated,
	 * so that it can access all of the URLs specified in the 2nd parameter
	 * @param links: a List of HtmlAnchors representing links into the system
	 * 
	 * @return: a HashMap of Strings representing URL's linked to Lists of FormInputs.
	 */
	private static HashMap<String, List<HtmlElement>> discoverFormInputs(WebClient webClient, List<HtmlAnchor> links){
		HashMap<String, List<HtmlElement>> inputs = new HashMap<String, List<HtmlElement>>();
		HtmlPage page = (HtmlPage) webClient.getCurrentWindow().getEnclosedPage();

		for (HtmlAnchor a : links) {
			try {
				String url = HtmlAnchor.getTargetUrl(a.getHrefAttribute(), page).toString();
				page = webClient.getPage(url);
				List<HtmlForm> forms = page.getForms();
				if (forms.size() > 0) {
					List<HtmlElement> fields = new ArrayList<HtmlElement>();
					for (HtmlForm f : forms) {
						fields.addAll(f.getElementsByTagName("input"));
					}
					if (fields.size() > 0) {
						inputs.put(url, fields);
					}
				}
				
			} catch (Exception e) {
				//e.printStackTrace();
			}
			
		}
		return inputs;
	}
	
	private static boolean authentication(WebClient webClient, String url, HashMap<String, String> opts){
		HtmlPage page;
		try {
			page = webClient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if (! opts.containsKey("customAuth")) {
			return false;
		}
		if (url.equals("http://127.0.0.1/dvwa/login.php")){
			final HtmlForm form = page.getForms().get(0);
		    final HtmlTextInput user = form.getInputByName("username");
		    final HtmlPasswordInput pass = form.getInputByName("password");
		    
		    user.setValueAttribute("admin");
		    pass.setValueAttribute("password");
		    
		    final HtmlSubmitInput button = form.getInputByValue("Login");
		    try {
				button.click();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		    if (!webClient.getCurrentWindow().getEnclosedPage()
		    		.getUrl().toString().equals("http://127.0.0.1/dvwa/index.php")){
		    	return false;
		    } 
		    
		} else if(url.startsWith("http://127.0.0.1:8080/bodgeit/")){
			String email = "gmh5970@rit.edu";
			String password = "password";
			
			if (!url.equals("http://127.0.0.1:8080/bodgeit/register.jsp")){
				try {
					page = webClient.getPage("http://127.0.0.1:8080/bodgeit/register.jsp");
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			
			final HtmlForm form = page.getForms().get(0);
		    final HtmlTextInput user = form.getInputByName("username");
		    final HtmlPasswordInput pass1 = form.getInputByName("password1");
		    final HtmlPasswordInput pass2 = form.getInputByName("password2");	
		    
		    user.setValueAttribute(email);
		    pass1.setValueAttribute(password);
		    pass2.setValueAttribute(password);
		    
		    final HtmlSubmitInput submitButton = form.getInputByValue("Register");
		    try {
				submitButton.click();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		    
		    if (((HtmlPage) webClient.getCurrentWindow().getEnclosedPage())
		    		.getBody().asText().contains("successfully registered")){// registration passes
		    	return true;
		    } else { // registration fails due to user already existing
		    	try {
					page = webClient.getPage("http://127.0.0.1:8080/bodgeit/login.jsp");
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
				final HtmlForm form2 = page.getForms().get(0);
			    final HtmlTextInput user3 = form2.getInputByName("username");
			    final HtmlPasswordInput pass3 = form2.getInputByName("password");	
			    
			    user3.setValueAttribute(email);
			    pass3.setValueAttribute(password);
			    
			    final HtmlSubmitInput loginButton = form2.getInputByValue("Login");
			    try {
					loginButton.click();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
		    }
		    
		} else {
			return false;
		}
		return true;
	}

	/*
	 * Parses the query portion of the given URL to find possible input parameters
	 */
	private static HashMap<String, String> parseURL(URL url){
		HashMap<String, String> parameters = new HashMap<String, String>();

		String query = url.getQuery();
		if (query != null && query.indexOf("=") != -1){
			String[] pairs = query.split("&");
			
			for (int i = 0; i < pairs.length; i++){
				String[] split = pairs[i].split("=");
				String key = split[0];
				String value = split[1];
				parameters.put(key, value);
			}
		}
		return parameters;
	}
	
	/*
	 * Takes the list of all URLs to be parsed
	 */
	private static HashMap<HtmlAnchor, HashMap<String, String>>
			parseURLs(WebClient webClient, List<HtmlAnchor> URLs){
		
		HashMap<HtmlAnchor, HashMap<String, String>> parameters =
				new HashMap<HtmlAnchor, HashMap<String, String>>();
		
		HtmlPage page = (HtmlPage) webClient.getCurrentWindow().getEnclosedPage();
		
		for(int i = 0; i < URLs.size(); i++){
			HtmlAnchor tempAnchor = URLs.get(i);
			try {
				URL url = new URL(HtmlAnchor.getTargetUrl(tempAnchor.getHrefAttribute(), page).toString());
				parameters.put(tempAnchor, parseURL(url));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("Bad URL during parsing.");
				return parameters;
			}
		}
		
		return parameters;
	}
	
	/*
	 * Method to properly print the discovered url parameters. 
	 */
	private static String displayParams(HashMap<HtmlAnchor, HashMap<String, String>> params, WebClient webClient){
		String result = '\n' + "Discovered HTML Parameters:" + '\n';
		for (Map.Entry<HtmlAnchor, HashMap<String, String>> entry: params.entrySet()){			
			String temp = displayAnchor(entry.getKey());
			temp = temp + " || ";
			HashMap <String, String> workingMap = entry.getValue();
			int i = 0;
			for (Map.Entry<String, String> values : workingMap.entrySet()){
				temp = temp + "(" + values.getKey() + " = " + values.getValue() + ") ";
				i++;
			}
			if (i == 0){
				continue;
			}
			temp = temp + '\n';
			result = result + temp;
		}
		
		return result;
	}
	
	/*
	 * Pretty toString method for HtmlAnchors
	 */
	private static String displayAnchor(HtmlAnchor anchor) {
		String result = "";
		HtmlPage page = anchor.getHtmlPageOrNull();
		try {
			URL url = new URL(HtmlAnchor.getTargetUrl(anchor.getHrefAttribute(), page).toString());
			result = url.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	/**
	 * This code is for showing how you can get all the links on a given page, and visit a given URL
	 * @param webClient
	 */
	private static List<HtmlAnchor> discoverLinks(WebClient webClient, List<String> words) {
		HtmlPage page;
		try {
			page = (HtmlPage) webClient.getCurrentWindow().getEnclosedPage();
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.<HtmlAnchor>emptyList();
		}
		List<HtmlAnchor> links = page.getAnchors();
		List<HtmlAnchor> crawledLinks = new ArrayList<HtmlAnchor>();
		URL pageUrl = page.getUrl();
	
		for(int i = 0; i < links.size(); i++){
			HtmlAnchor link = links.get(i);
		
			String url = "";
			try {
				url = HtmlAnchor.getTargetUrl(link.getHrefAttribute(), page).toString();
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
			if (url.startsWith("http://" + pageUrl.getHost())){
				System.out.println("Link discovered: "+ url);
				
				try {
					page = webClient.getPage(url);
				} catch (FailingHttpStatusCodeException | IOException e) {
				}
		
				crawledLinks = page.getAnchors();
				crawledLinks.addAll(guessPages(webClient, link, words));
				for(HtmlAnchor ha : crawledLinks){
					if(!Fuzzer.containsLink(links, ha, page)){
						links.add(ha);
					}
				}
			}
		}
		
		return links;
	}
	
	private static List<HtmlAnchor> guessPages(WebClient webClient, HtmlAnchor link, List<String> words){
		HtmlPage page = (HtmlPage) webClient.getCurrentWindow().getEnclosedPage();
		
		List<HtmlAnchor> anchors = new ArrayList<HtmlAnchor>();
		
		String[] extensions = {".jsp",".php"};
		
		String url = "";
		try {
			url = FilenameUtils.removeExtension(HtmlAnchor.getTargetUrl(link.getHrefAttribute(), page).toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		for(String word : words){
			for(int i = 0; i < extensions.length; i++){
				try {
					page = webClient.getPage(url + "/" + word + extensions[i]);
					anchors.addAll(page.getAnchors());
				} catch (Exception e) {
				}				
			}
			
		}
		
		return anchors;
	}
	
	private static boolean containsLink(List<HtmlAnchor> anchors, HtmlAnchor link, HtmlPage page){
		for(HtmlAnchor anchor : anchors){
			try {
				if(HtmlAnchor.getTargetUrl(link.getHrefAttribute(), page).toString().equals(HtmlAnchor.getTargetUrl(anchor.getHrefAttribute(), page).toString())){
					return true;
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Creates a hash map of the options from the command line
	 * @param args
	 * @return
	 */
	private static HashMap<String,String> options(String[] args){		
		HashMap<String,String> opts = new HashMap<String,String>();
		
		opts.put("random", "false");
		opts.put("slow", "500");
		
		//Start at 2 to avoid discover|test flag and url
		for(int i = 2; i < args.length; i++){
			if(args[i].split("=")[0].equals("--custom-auth")){
				opts.put("customAuth", args[i].split("=")[1]);
			}
			if(args[i].split("=")[0].equals("--common-words")){
				opts.put("commonWords",args[i].split("=")[1]);
			}
			if(args[i].split("=")[0].equals("--vectors")){
				opts.put("vectors",args[i].split("=")[1]);
			}
			if(args[i].split("=")[0].equals("--sensitive")){
				opts.put("sensitive",args[i].split("=")[1]);
			}
			if(args[i].split("=")[0].equals("--random")){
				opts.put("random",args[i].split("=")[1]);
			}
			if(args[i].split("=")[0].equals("--slow")){
				opts.put("slow",args[i].split("=")[1]);
			}
		}
		
//		System.out.println("Custom Auth: "+opts.get("customAuth"));
//		System.out.println("Common Words File: "+opts.get("commonWords"));
		
		return opts;
	}
	
	/**
	 * Reads a file for common words
	 * @param path
	 * @return list of words
	 */
	private static List<String> getCommonWords(String path){
		File wordsFile = new File(path);
		List<String> words = new ArrayList<String>();
		BufferedReader reader = null;

		try {
		    reader = new BufferedReader(new FileReader(wordsFile));
		    String line = null;

		    while ((line = reader.readLine()) != null) {
		        words.add(line);
		    }
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
			    try {
			        if (reader != null) {
			            reader.close();
			        }
			    } catch (IOException e) {
		    }
		}
		//System.out.println(words);
		return words;
	}
	
	/**
	 * Reads a file for the vectors
	 * @param path
	 * @return list of vector strings
	 */
	private static List<String> getVectors(String path){
		File vectorsFile = new File(path);
		List<String> vectors = new ArrayList<String>();
		BufferedReader reader = null;

		try {
		    reader = new BufferedReader(new FileReader(vectorsFile));
		    String line = null;

		    while ((line = reader.readLine()) != null) {
		        vectors.add(line);
		    }
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
			    try {
			        if (reader != null) {
			            reader.close();
			        }
			    } catch (IOException e) {
		    }
		}
		//System.out.println(vectors);
		return vectors;
	}
	
	/**
	 * Reads a file for the sensitive words
	 * @param path
	 * @return list of sensitive words
	 */
	private static List<String> getSensitiveWords(String path){
		File sensitiveFile = new File(path);
		List<String> sensitive = new ArrayList<String>();
		BufferedReader reader = null;

		try {
		    reader = new BufferedReader(new FileReader(sensitiveFile));
		    String line = null;

		    while ((line = reader.readLine()) != null) {
		        sensitive.add(line);
		    }
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
			    try {
			        if (reader != null) {
			            reader.close();
			        }
			    } catch (IOException e) {
		    }
		}
		//System.out.println(sensitive);
		return sensitive;
	}
	
	/*
	 *  Tests if a given WebResponse gave back an "OK" status code
	 */
	private static boolean responseStatusOK(WebResponse response){
		if (response.getStatusCode() == 200){
			return true;
		}
		return false;
	}
	
	/*
	 *  Tests to see if a given WebResponse took too long to
	 *  come back (default = more than 500 milliseconds).
	 */
	private static boolean webResponseSlow(WebResponse response){
		if(response.getLoadTime() >= slowResponse){
			return true;
		}
		return false;
	}
	
	/**
	 * Call this to print out usage guidelines if there is any errors.
	 */
	private static void usage() {
		System.out.println("java Fuzzer [discover | test] url OPTIONS");
		System.exit(0);
	}

}
