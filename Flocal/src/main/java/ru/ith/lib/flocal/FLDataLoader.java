package ru.ith.lib.flocal;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ru.ith.lib.flocal.data.FLBoard;
import ru.ith.lib.flocal.data.FLMessage;
import ru.ith.lib.flocal.data.FLMessageSet;
import ru.ith.lib.flocal.data.FLThreadHeader;
import ru.ith.lib.webcrawl.ConnectionFactory;
import ru.ith.lib.webcrawl.providers.HTMLResponce;
import ru.ith.lib.webcrawl.providers.ProviderEnum;

/**
 * Created by infthi on 6/26/13.
 */
public class FLDataLoader {
    private static final String FLOCAL_HOST = "forumbgz.ru";

    public static final String generateLoginData(String login, String password)
            throws FLException {
        try {
            HTMLResponce rdr = doQuery("/login.php?showlite=sl", null);
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

    private static HTMLResponce doQuery(String url, FLSession session) throws IOException {
        return (HTMLResponce) ConnectionFactory.doQuery(FLOCAL_HOST, url,
                (session==null)?null:session.getSessionCookies(), ProviderEnum.HTML);
    }

    public static void logout(FLSession session) throws FLException {
        if (session.isAnonymous())
            return;
        try{
            HTMLResponce keyPage = doQuery("/logout.php?showlite=sl", session);
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
                    HTMLResponce clean = doQuery("/logout.php?key="+logoutLink, session);
                    return;
                }
            }
            throw new FLException("server error", "Failed to retrieve logout key");
        } catch (IOException e){
            throw new FLException("Failed to connect to server", e.getMessage());
        }
    }

    public static LinkedList<FLBoard> listBoards(FLSession session) throws FLException {
        LinkedList<FLBoard> result = new LinkedList<FLBoard>();
        try {
            HTMLResponce mainPage = doQuery("/ubbthreads.php?showlite=sl", session);
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

                    final String src;
                    int srcBeginning = link.indexOf("src=");
                    if (srcBeginning>=0){
                        srcBeginning+=4;
                        int ending = link.indexOf('&', srcBeginning);
                        if (ending >= 0)
                            src = link.substring(srcBeginning, ending);
                        else
                            src = link.substring(srcBeginning);
                    } else
                        src = null;

                    int beginning = link.indexOf("Board=");
                    if (beginning >= 0) {
                        beginning += 6;
                        int ending = link.indexOf('&', beginning);
                        if (ending >= 0)
                            URIName = link.substring(beginning, ending);
                        else
                            URIName = link.substring(beginning);
                        FLBoard board = new FLBoard(name, URIName, hasUnread, src);
                        result.add(board);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new FLException("Failed to retrieve data", e.getMessage(), e);
        }
    }

    public static LinkedList<FLThreadHeader> listThreads(FLSession session, FLBoard board, int page)
            throws FLException {
        LinkedList<FLThreadHeader> result = new LinkedList<FLThreadHeader>();
        try {
            HTMLResponce mainPage = doQuery("/postlist.php?Board="
                    + board.boardURIName + "&sb=5&showlite=sl&page=" + page+((board.src==null)?"":("&src="+board.src)), session);
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

                    //in sl-mode, number points to first unread post if we have any unread posts. otherwise it points to first post.

                    final int first, firstUnread;
                    if (numUnread>0){
                        first = -1;
                        firstUnread = Integer.valueOf(id);
                    } else {
                        first = Integer.valueOf(id);
                        firstUnread = -1;
                    }


                    FLThreadHeader thread = new FLThreadHeader(name, author, numUnread, numUnreadDisc,
                            first, firstUnread, isPinned, board.src);
                    result.add(thread);
                }
            }
            return result;
        } catch (IOException e) {
            throw new FLException("Failed to retrieve data", e.getMessage());
        }
    }

    public static FLMessageSet listMessages(FLSession session, FLThreadHeader thread, int skip)
            throws FLException {
        LinkedList<FLMessage> result = new LinkedList<FLMessage>();
        try {
            long loadID = thread.getID();

            int threadOffset = 0;
            boolean hasMorePages = false;

            if (loadID<0)
                loadID = thread.getUnreadID();
            String URL = "/showflat.php?showlite=l&Number="+ loadID
                    +((skip>=0)?("&tistart="+skip):"")
                    +((thread.src==null)?"":("&src="+thread.src));
            HTMLResponce mainPage = doQuery(URL, session);

            boolean wasCounter = false;
            for (Element threadHeaderElement: mainPage.getAll("table > tbody> tr > td > a + br + br")){
                Node pageNavigationElement = threadHeaderElement.nextSibling(); //Pages:
                while (pageNavigationElement!=null){
                    boolean thisCounter = false;
                    if (pageNavigationElement instanceof TextNode){
                        String headerText = ((TextNode) pageNavigationElement).text().replaceAll(" ","");

                        int pagesSplit = headerText.indexOf(':');
                        if (pagesSplit>0){
                            headerText = "|"+headerText.substring(pagesSplit+1);
                        }
                        if ((headerText.length()>2)&&headerText.startsWith("|")&&headerText.endsWith("|")){
                            int extra = 1;
                            if (headerText.charAt(1)=='(')
                                extra++;
                            headerText = headerText.substring(extra, headerText.length()-extra);
                            threadOffset = Integer.valueOf(headerText);
                            thisCounter = true;
                        }
                    } else if (wasCounter&&pageNavigationElement.nodeName().equals("a")){
                        String href = pageNavigationElement.attr("href");
                        int tiStartIndex = href.indexOf("tistart=");
                        if (tiStartIndex>=0){
                            tiStartIndex+=8;

                            final String tistart;
                            int tiStartEnd = href.indexOf("&", tiStartIndex);
                            if (tiStartEnd>=0)
                                tistart = href.substring(tiStartIndex, tiStartEnd);
                            else
                                tistart = href.substring(tiStartIndex);

                            if (!tistart.equals("all"))
                                hasMorePages = true;
                        }
                    }
                    wasCounter = thisCounter;
                    pageNavigationElement = pageNavigationElement.nextSibling();
                }
                break;
            }

            for (Element mesageHeaderElement : mainPage
                    .getAll("td.subjecttable:not([style])")) {
                String userName, caption, postDate;
                int rating = 0;
                long messageID;
                StringBuilder postHtml = new StringBuilder();

                Node linkNode = mesageHeaderElement.childNode(0);
                if (linkNode.nodeName().equalsIgnoreCase("a")) {
                    messageID = Long.valueOf(linkNode.attr("name").substring(4));
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

                Element ratingNodeContainer = mesageHeaderElement
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

                Element textContainer = mesageHeaderElement.parent()
                        .nextElementSibling();
                if (textContainer.children().size() > 0){
                    Element textElement = textContainer.child(0);
                    List<Node> children = textElement.childNodes();
                    for (int i = children.size()-1; i>=0; i--){
                        Node lastNode = children.get(i);
                        if (lastNode.nodeName().equals("br"))
                            lastNode.remove();
                        else
                            break;
                    }
                    postHtml.append(textElement.html());
                }
                else
                    continue;

                FLMessage message = new FLMessage(userName,
                        postHtml.toString(), caption, postDate, rating, messageID);
                result.add(message);
            }


            FLMessageSet resultSet = new FLMessageSet(thread, result, hasMorePages, threadOffset);
            resultSet.URL = URL;
            return resultSet;
        } catch (IOException e) {
            throw new FLException("Failed to retrieve data", e.getMessage());
        }
    }

}
