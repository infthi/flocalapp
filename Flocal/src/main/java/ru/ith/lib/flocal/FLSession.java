package ru.ith.lib.flocal;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import ru.ith.lib.flocal.data.FLBoard;
import ru.ith.lib.flocal.data.FLMessage;
import ru.ith.lib.flocal.data.FLThread;
import ru.ith.lib.webcrawl.ConnectionFactory;
import ru.ith.lib.webcrawl.providers.HTMLResponce;
import ru.ith.lib.webcrawl.providers.ProviderEnum;

public class FLSession {
	private static final String FLOCAL_HOST = "forumbgz.ru";
	private final Map<String, String> sessionCookies;

	public static FLSession makeAnonymousSession() throws FLException {
		return new FLSession(null);
	}

	public FLSession(String login, String password) throws FLException {
		this(generateLoginData(login, password));
	}

	public FLSession(String sKey) throws FLException {
		Map<String, String> cookies = new TreeMap<String, String>();
		if (sKey != null) {
			String[] cookieData = sKey.split(":");
			cookies.put("w3t_w3t_key", cookieData[0]);
			cookies.put("w3t_w3t_mysess", cookieData[1]);
			cookies.put("w3t_w3t_myid", cookieData[2]);
		}
		this.sessionCookies = Collections.unmodifiableMap(cookies);
	}

	private static final String generateLoginData(String login, String password)
			throws FLException {
		try {
			HTMLResponce rdr = (HTMLResponce) ConnectionFactory.doQuery(
					FLOCAL_HOST, "/login.php?showlite=sl", null,
					ProviderEnum.HTML);
			Elements postKeyElement = rdr
					.getAll("form > input[name=postdata_protection_key]");
			if (postKeyElement.isEmpty())
				throw new FLException("Malformed server responce",
						"no postdata_protection_key");
			String postKey = postKeyElement.get(0).attr("value");

			Map<String, String> loginData = new TreeMap<String, String>();
			loginData.put("Loginname", login);
			loginData.put("Loginpass", password);
			loginData.put("rememberme", "1");
			loginData.put("firstlogin", "1");
			loginData.put("ipbind", "0");
			loginData.put("postdata_protection_key", postKey);
			loginData.put("buttlogin", "1");

			rdr = (HTMLResponce) ConnectionFactory.doQuery(FLOCAL_HOST,
					"/start_page.php?showlite=sl", null, loginData,
					rdr.metaData.getEncoding(), ProviderEnum.HTML);
			String mysess = rdr.metaData.getCookie("w3t_w3t_mysess");
			String key = rdr.metaData.getCookie("w3t_w3t_key");
			String myID = rdr.metaData.getCookie("w3t_w3t_myid");
			if ((key == null) || (mysess == null) || (myID == null)) {
				throw new FLException("Failed to login",
						"server returned no session cookies");
			}
			return key + ":" + mysess + ":" + myID;
		} catch (IOException e) {
			throw new FLException("Failed to login", e.getMessage());
		}
	}

	private HTMLResponce doQuery(String url) throws IOException {
		return (HTMLResponce) ConnectionFactory.doQuery(FLOCAL_HOST, url,
				sessionCookies, ProviderEnum.HTML);
	}

	public String getCurrentUser() {
		throw new RuntimeException("not implemented yet!");
	}

	public void logout() throws FLException {
		if (sessionCookies.isEmpty())
			return;
		try{
			HTMLResponce keyPage = doQuery("/logout.php?showlite=sl");
			Elements logoutLinkSet = keyPage.getAll("td i a[href]");
			if (!logoutLinkSet.isEmpty()) {
				String logoutLink = logoutLinkSet.first().attr("href");
				int index = logoutLink.indexOf("key=");
				if (index>0) {
					index+=4;
					int endIndex = logoutLink.indexOf('&', index);
					if (endIndex>0)
						logoutLink = logoutLink.substring(index, endIndex);
					else
						logoutLink = logoutLink.substring(index);
					HTMLResponce clean = doQuery("/logout.php?key="+logoutLink);
					return;
				}
			}
			throw new FLException("server error", "Failed to retrieve logout key");
		} catch (IOException e){
			throw new FLException("Failed to connect to server", e.getMessage());
		}
	}

	public LinkedList<FLBoard> listBoards() throws FLException {
		LinkedList<FLBoard> result = new LinkedList<FLBoard>();
		try {
			HTMLResponce mainPage = doQuery("/ubbthreads.php?showlite=sl");
			for (Element e : mainPage.getAll("a[href*=postlist.php]")) {
				boolean hasUnread = false;
				if (e.previousSibling().nodeName().startsWith("#")) {// #text:
																		// (*)
					hasUnread = true;
				}
				Node textNode = e.childNode(0);
				if (textNode instanceof TextNode) {
					String name, URIName;
					name = ((TextNode) textNode).text();
					String link = e.attr("href");
					int beginning = link.indexOf("Board=");
					if (beginning >= 0) {
						beginning += 6;
						int ending = link.indexOf('&', beginning);
						if (ending >= 0)
							URIName = link.substring(beginning, ending);
						else
							URIName = link.substring(beginning);
						FLBoard board = new FLBoard(name, URIName, hasUnread);
						result.add(board);
					}
				}
			}
			return result;
		} catch (IOException e) {
			throw new FLException("Failed to retrieve data", e.getMessage());
		}
	}

	public LinkedList<FLThread> listThreads(FLBoard board, int page)
			throws FLException {
		LinkedList<FLThread> result = new LinkedList<FLThread>();
		try {
			HTMLResponce mainPage = doQuery("/postlist.php?Board="
					+ board.boardURIName + "&sb=5&showlite=sl&page=" + page);
			for (Element e : mainPage.getAll("a[href*=showflat.php]")) {
				int numUnread, numUnreadDisc = 0;
				boolean isPinned = false;
				String name = "", author;
				String id;

				Node unreadNode = e.previousSibling();
				if (unreadNode instanceof TextNode) {
					String unreadText = ((TextNode) unreadNode).text();
					if (unreadText.endsWith("["))
						continue;
					else if (unreadText.startsWith("]")) {
						Node unreadDiscussion = unreadNode.previousSibling();
						String unreadDiscString = ((TextNode) (unreadDiscussion.childNode(0))).text();
						try {
							numUnreadDisc = Integer.valueOf(unreadDiscString);
						} catch (NumberFormatException ex) {
							//TODO: log
							continue;
						}
						unreadText = ((TextNode) unreadDiscussion.previousSibling()).text();
					}

					if (unreadText.startsWith("(")) {
						try {
							numUnread = Integer.valueOf(unreadText.substring(1,
									unreadText.length() - 2));
						} catch (NumberFormatException ex) {
							//TODO: log
							continue;
						}
					} else
						numUnread = 0;
				} else
					continue;

				if (e.childNodes().isEmpty())
					continue;
				Node nameNode = e.childNode(0);
				if (nameNode.nodeName().equals("img")) {
					isPinned = true;
					nameNode = nameNode.nextSibling();
				}
				if (nameNode instanceof TextNode) {
					name = ((TextNode) nameNode).text();
				} else
					continue;

				Node authorNode = e.nextSibling();
				if (authorNode instanceof TextNode) {
					author = ((TextNode) authorNode).text();
					if (author.length() < 3)
						continue;
					author = author.substring(2, author.length() - 1);
				} else
					continue;

				String link = e.attr("href");
				int beginning = link.indexOf("Number=");
				if (beginning >= 0) {
					beginning += 7;
					int ending = link.indexOf('&', beginning);
					if (ending >= 0)
						id = link.substring(beginning, ending);
					else
						id = link.substring(beginning);
					FLThread thread = new FLThread(name, author, numUnread, numUnreadDisc, 
							Integer.valueOf(id), isPinned);
					result.add(thread);
				}
			}
			return result;
		} catch (IOException e) {
			throw new FLException("Failed to retrieve data", e.getMessage());
		}
	}

	public LinkedList<FLMessage> listMessages(FLThread thread, int page)
			throws FLException {
		LinkedList<FLMessage> result = new LinkedList<FLMessage>();
		try {
			HTMLResponce mainPage = doQuery("/showflat.php?showlite=l&Number="
					+ thread.getID());
			for (Element headerElement : mainPage
					.getAll("td.subjecttable:not([style])")) {
				String userName, caption, postDate;
				int rating = 0;
				long ID;
				StringBuilder postHtml = new StringBuilder();

				Node linkNode = headerElement.childNode(0);
				if (linkNode.nodeName().equalsIgnoreCase("a")) {
					ID = Long.valueOf(linkNode.attr("name").substring(4));
				} else
					continue;

				Node nickNode = linkNode.nextSibling();
				if (nickNode instanceof TextNode) {
					// TODO: process layer
					nickNode = nickNode.nextSibling();
				}
				if (nickNode.nodeName().equalsIgnoreCase("b")) {
					userName = ((TextNode) nickNode.childNode(0)).text();
				} else
					continue;

				Node captionNodeWrapper = nickNode.nextSibling().nextSibling();
				if (captionNodeWrapper.childNodeSize() > 0) {
					Node captionNode = captionNodeWrapper.childNode(0);
					if (captionNode instanceof TextNode)
						caption = ((TextNode) captionNode).text().substring(2);
					else
						continue;
				} else {
					caption = "";
				}

				Node dateContainer = captionNodeWrapper.siblingNodes().get(
						captionNodeWrapper.siblingNodes().size() - 1);
				if (dateContainer.childNodeSize() == 0)
					continue;
				Node dateNode = dateContainer.childNode(0);
				if (dateNode instanceof TextNode) {
					postDate = ((TextNode) dateNode).text();
					int dateEndIndex = postDate.indexOf('\u00a0');// nbsp;
					if (dateEndIndex > 0)
						postDate = postDate.substring(0, dateEndIndex);
				} else
					continue;

				Element ratingNodeContainer = headerElement
						.nextElementSibling();
				if (ratingNodeContainer.childNodeSize()>0){
					Node ratingNodeSpan = ratingNodeContainer.childNode(0);
					if (ratingNodeSpan.childNodeSize()>0){
						Node ratingNode = ratingNodeSpan.childNode(0);
						if (ratingNode instanceof TextNode)
							rating = Integer.valueOf(((TextNode) ratingNode)
									.text());
						else
							continue;
					} else
						continue;
				} else
					continue;

				Element textContainer = headerElement.parent()
						.nextElementSibling();
				if (textContainer.children().size() > 0)
					postHtml.append(textContainer.child(0).html());
				else
					continue;
					
				FLMessage message = new FLMessage(userName,
						postHtml.toString(), caption, postDate, rating, ID);
				result.add(message);
			}
			return result;
		} catch (IOException e) {
			throw new FLException("Failed to retrieve data", e.getMessage());
		}
	}

	public boolean isAnonymous() {
		return sessionCookies.isEmpty();
	}

	public String getKey() {
		String key = sessionCookies.get("w3t_w3t_key");
		String mysess = sessionCookies.get("w3t_w3t_mysess");
		String myid = sessionCookies.get("w3t_w3t_myid");
		if ((key==null)||(mysess==null)||(myid==null))
			return null;
		return key+":"+mysess+":"+myid;
	}
}
