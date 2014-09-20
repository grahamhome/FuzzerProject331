import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class Fuzzer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
			System.out.println(authentication(webClient, url));
			List<HtmlAnchor> links = Fuzzer.discoverLinks(webClient,url);
			//TODO
			
			webClient.closeAllWindows();
		}
		else if(args[0].toLowerCase().equals("test")){
			System.out.println("Part 2 of project");
			if(opts.get("vector") == null
					|| opts.get("sensitive") == null
					|| opts.get("commonWords") == null){
				Fuzzer.usage();
			}
		}else{
			Fuzzer.usage();
		}

	}
	
	private static boolean authentication(WebClient webClient, String url){
		HtmlPage page;
		try {
			page = webClient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if (url.equals("127.0.0.1/dvwa/login.php")){
			final HtmlForm form = page.getForms().get(0);
		    final HtmlTextInput user = form.getInputByName("username");
		    final HtmlTextInput pass = form.getInputByName("password");
		    
		    user.setValueAttribute("admin");
		    pass.setValueAttribute("password");
		    
		    final HtmlSubmitInput button = form.getInputByName("Login");
		    try {
				button.click();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
		} else if(url.startsWith("127.0.0.1:8080/bodgeit/")){
			String email = "gmh5970@rit.edu";
			String password = "password";
			
			if (!url.equals("127.0.0.1:8080/bodgeit/register.jsp")){
				try {
					page = webClient.getPage("127.0.0.1:8080/bodgeit/register.jsp");
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			
			final HtmlForm form = page.getForms().get(0);
		    final HtmlTextInput user = form.getInputByName("username");
		    final HtmlTextInput pass1 = form.getInputByName("password1");
		    final HtmlTextInput pass2 = form.getInputByName("password2");	
		    
		    user.setValueAttribute(email);
		    pass1.setValueAttribute(password);
		    pass2.setValueAttribute(password);
		    
		    final HtmlSubmitInput submitButton = form.getInputByName("submit");
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
					page = webClient.getPage("127.0.0.1:8080/bodgeit/login.jsp");
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
				final HtmlForm form2 = page.getForms().get(0);
			    final HtmlTextInput user3 = form.getInputByName("username");
			    final HtmlTextInput pass3 = form.getInputByName("password2");	
			    
			    user.setValueAttribute(email);
			    pass3.setValueAttribute(password);
			    
			    final HtmlSubmitInput loginButton = form.getInputByName("Login");
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

	/**
	 * This code is for showing how you can get all the links on a given page, and visit a given URL
	 * @param webClient
	 */
	private static List<HtmlAnchor> discoverLinks(WebClient webClient,String url) {
		HtmlPage page;
		try {
			page = webClient.getPage(url);
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.<HtmlAnchor>emptyList();
		}
		List<HtmlAnchor> links = page.getAnchors();
		for (HtmlAnchor link : links) {
			System.out.println("Link discovered: " + link.asText() + " @URL=" + link.getHrefAttribute());
		}
		return links;
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
	 * Call this to print out usage guidelines if there is any errors.
	 */
	private static void usage() {
		System.out.println("java Fuzzer [discover | test] url OPTIONS");
		System.exit(0);
	}

}
