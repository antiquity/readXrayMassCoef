import java.util.regex.*;
import java.util.*;
import java.net.*;
import java.io.*;
public class ParseHTML{
    private String url;
    private String content;
    private String tag;
    private Matcher matcher;
    private boolean recursive;
    int[] innerRegion = new int[2], outerRegion= new int[2];
    private ArrayDeque<State> stack;

    static String readHTML(String url){
        StringBuilder content=new StringBuilder();
        try{
            URL html = new URL(url);
            BufferedReader inRead = new BufferedReader(
                    new InputStreamReader(html.openStream()));
            String inputLine;
            while ((inputLine = inRead.readLine()) != null)
                content.append(inputLine+"\n");
            inRead.close();
        }catch(Exception e){
            System.err.println(e);
        }
        //System.out.println(content);
        //System.out.println(parseMark("table",content.toString(),1)[0]);
        return content.toString();
    }

    ParseHTML(String url){
        this(url,"html");
    }

    ParseHTML(String url, String tag){
        this.url = url;
        if(url!=null)
            this.content=readHTML(url);
        this.tag=tag;
        String regex="(?ims)</?" + tag + "((\\s[^>]*)|([^>]*))>";
        matcher=Pattern.compile(regex).matcher(content);
        //System.out.println(pattern.flags()+" "
        //        + (Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
        recursive=setRec(tag);
        stack = new ArrayDeque<State>();
    }

    void setContent(String con){
        this.content = con; this.url = null;
    }

    void setTag(String tag){
        this.tag=tag;
        String regex="(?ims)</?" + tag + "((\\s[^>]*)|([^>]*))>";
        matcher.usePattern(Pattern.compile(regex));
        recursive=setRec(tag);
    }

    void refine(){
        //System.out.println(Arrays.toString(innerRegion));
        //System.out.println(content.length() + " " + matcher.regionStart() + " " + matcher.regionEnd());
        content = content.substring(innerRegion[0],innerRegion[1]);
        matcher.reset(content);
    }

    String getContent(){ return content;}

    String group(){
        //System.out.println("from group(): " + Arrays.toString(innerRegion));
        return content.substring(innerRegion[0],innerRegion[1]); }

    void stash(){
        stack.push(new State(content, matcher.start(), matcher.pattern()));
    }

    void stashPop(){
        content = stack.pop().recover(matcher);
    }

    boolean hasNext(){
        int[] openTag = new int[2], closeTag= new int[2];
        int depth=0;
        if(matcher.find()){
            if(matcher.group().charAt(1)=='/'){
                System.err.println("ERR: " + url);
                System.err.printf("L:%d, no opening tag for %s!\n",
                        getLineNumber(matcher.start()), matcher.group());
                //System.err.println("Search from: " +
                //        content.substring(matcher.regionStart(),matcher.regionEnd()));
                System.err.println("t1");
                innerRegion[0] = matcher.regionStart(); innerRegion[1] = matcher.start();
                return true;
            }
            openTag[0]=matcher.start(); openTag[1]=matcher.end();
            depth++;  
            while(depth>0){
                //System.out.println("depth=" + depth);
                if(matcher.find()){
                    if(matcher.group().charAt(1)=='/'){
                        depth--;
                        if(depth==0){
                            closeTag[0]=matcher.start(); closeTag[1]=matcher.end();
                        }
                    }else{
                        if(recursive)
                            depth++;
                        else{
                            System.err.println("ERR: " + url);
                            System.err.println("non-recursive tag: "+matcher.group() +
                                    "; stop at L:" + getLineNumber(matcher.start()));
                            System.err.println("t2");

                            closeTag[0]=matcher.start(); closeTag[1]=matcher.start();
                            matcher.find(openTag[0]);  // go back to the previous match in case
                            depth = 0;
                        }
                    }
                }else{
                    System.err.println("ERR: " + url);
                    System.err.printf("L:%d, no closing tag for %s!\n",
                            getLineNumber(openTag[0]), content.substring(openTag[0],openTag[1]));
                    System.err.println("depth: " + depth);
                    //System.err.printf("last found: " + content.substring(lastFind[0],lastFind[1]));
                    //System.err.println("Search from: " +
                    //        content.substring(matcher.regionStart(),matcher.regionEnd()));

                            System.err.println("t3");
                    closeTag[0]=matcher.regionEnd(); closeTag[1]=matcher.regionEnd();
                    depth = 0;
                }
            }
            innerRegion[0]=openTag[1]; innerRegion[1]=closeTag[0];
            return true;
        }
        return false;
    }

    ArrayList<String> findAll(){
        return findAll(0);
    }

    ArrayList<String> findAll(int limit){
        ArrayList<String> output = new ArrayList<String>();
        int count=0;
        while(hasNext()){
            output.add(group()); count++;
            if(limit>0 && count>=limit) break;
        }
        return output;
    }

    int getLineNumber(int pos){
        Matcher m=Pattern.compile("(?m)$").matcher(content);
        m.region(0,pos);
        int count=0;
        while(m.find()){
            //System.out.println("Find \"" + m.group() + "\" at " + m.start());
            count++;
        }
        return count;
    }

    // set recursive of the tags that can be used recursively
    boolean setRec(String tag){
        tag=tag.toLowerCase();
        if(tag.equals("html") || tag.equals("table"))
            return true;
        return false;
    }

    String getLink(String str){ return getLink(content,str); }

    static String getLink(String html, String str){
        String regex="(?ims)<\\s*a[^>]*href=[^>]*\"(?<url>[^\">]*)\"[^>]*>" + str + "</a>";
        Matcher matcher=Pattern.compile(regex).matcher(html);
        //System.out.println(matcher.pattern());
        if(matcher.find()){
            //System.out.println("found");
            //System.out.println(matcher.group());
            //System.out.println(matcher.group("url"));
            return matcher.group("url");
        }else return null;
    }
}

class State{
    String content;
    int cu;
    Pattern p;
    State(String con, int c, Pattern d){
        cu = c; p = d; content = con;
    }
    String recover(Matcher m){
        m.usePattern(p); m.reset(content); m.find(cu);
        return content;
    }
}
