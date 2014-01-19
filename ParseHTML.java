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
    ArrayDeque<State> stack;

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
        matcher.region(innerRegion[0],innerRegion[1]);
    }

    String getContent(){ return content;}

    String group(){
        //System.out.println("from group(): " + Arrays.toString(innerRegion));
        return content.substring(innerRegion[0],innerRegion[1]);
    }

    void stash(){
        stack.push(new State(matcher.regionStart(), matcher.regionEnd(),
                    innerRegion[1], tag, innerRegion, outerRegion));
    }

    boolean stashPop(){
        State tt = stack.pop();
        setTag(tt.tag);
        innerRegion = tt.innerRegion;
        outerRegion = tt.outerRegion;
        return tt.recover(matcher);
    }

    boolean hasNext(){
        int[] openTag = new int[2], closeTag= new int[2];
        int depth=0;
        if(matcher.find()){
            if(matcher.group().charAt(1)=='/'){
                System.err.println("ERR: " + url);
                System.err.printf("L:%d, no opening tag for %s!\n",
                        getLineNumber(matcher.start()), matcher.group());

                innerRegion[0] = matcher.end(); innerRegion[1] = matcher.end();
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

                            closeTag[0]=matcher.start(); closeTag[1]=matcher.start();
                            // go back to the previous match in case
                            matcher.region(matcher.regionStart(),matcher.regionEnd());
                            while(matcher.find()){
                                if(matcher.start()<openTag[0]) continue;
                                else{
                                    if(matcher.start()==openTag[0]){
                                        break;
                                    }else{  // pass the anchor point
                                        System.err.println("Couldn't find back exactly!");
                                        break;
                                    }
                                }
                            }
                            depth = 0;
                        }
                    }
                }else{
                    System.err.println("ERR: " + url);
                    System.err.printf("L:%d, depth:%d, no closing tag for %s!\n",
                            getLineNumber(openTag[0]), depth, content.substring(openTag[0],openTag[1]));

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
    int rs,re,cu;
    String tag;
    int[] innerRegion, outerRegion;
    State(int a, int b, int c, String tag, int[] innerRegion, int[] outerRegion){
        rs=a; re=b; cu = c; this.tag = tag;
        this.innerRegion = innerRegion;
        this.outerRegion = outerRegion;
    }
    boolean recover(Matcher m){
        m.region(rs,re);
        while(m.find()){
            if(m.start()<cu) continue;
            else{
               if(m.start()==cu) return true;
               else return false; // pass the anchor point
            }
        }
        return true;
    }
}
