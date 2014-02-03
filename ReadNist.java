import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.util.*;

public class ReadNist {
    public static void main(String[] args) throws Exception {
        ReadNist reader=new ReadNist();
        String url;
        PrintWriter out=null;
        ParseHTML home = new ParseHTML("http://www.nist.gov/pml/data/xraycoef/");

        if(args.length!=0){
            String fn = args[0];
            if(!fn.equals("")){
                out = new PrintWriter(new BufferedWriter(new FileWriter(fn)));
            }
        }

        url = home.getLink("\\s*Table\\s*1.\\s*");
        ArrayList<Material> table1=reader.readTable1(url);

        url = home.getLink("\\s*Table\\s*2.\\s*");
        ArrayList<Material> table2=reader.readTable2(url);

        url = home.getLink("\\s*Table\\s*3.\\s*");
        ArrayList<ArrayList<double[]>> table3=reader.lookupTable(url,table1);

        url = home.getLink("\\s*Table\\s*4.\\s*");
        ArrayList<ArrayList<double[]>> table4=reader.lookupTable(url,table2);

        if(out!=null){
            ArrayList<Material> namelist = new ArrayList<Material>();
            Material temp;
            namelist.addAll(table1);
            namelist.addAll(table2);
            for(int i=0; i< namelist.size(); i++){
                temp = namelist.get(i);
                out.printf("symbols{%3d}=%-15s   %% %s\n",i+1,"'"+temp.sym+"';", temp.name);
            }
            out.printf("\n\nzaid=[\t%% Z/A I and density");
            for(int i=0; i< namelist.size(); i++){
                temp = namelist.get(i);
                out.printf("\n\t%g, %g, %e;",temp.za, temp.i, temp.density);
            }
            out.println("];\n\n");
            for(int i=0; i< namelist.size(); i++){
                temp = namelist.get(i);
                if(temp instanceof Composition){
                    out.printf("comp{%3d}=[%s;\n%11s%s]';\n",i+1,((Composition)temp).comp, 
                            "",((Composition)temp).prop);
                }
            }

            ArrayList<ArrayList<double[]>> mac = new ArrayList<ArrayList<double[]>>();
            mac.addAll(table3);
            mac.addAll(table4);

            ArrayList<double[]> itr;
            double[] tmp;
            for(int i=0; i<mac.size(); i++){
                out.printf("mac{%d} = [",i+1);
                itr=mac.get(i);
                if(itr==null){
                    System.err.printf("i=%d, %s, itr is null\n",i,namelist.get(i).name);
                    out.printf("];\n\n");
                    continue;
                }
                for(int j=0; j<itr.size(); j++){
                    tmp=itr.get(j);
                    if(tmp==null || tmp.length!=3){
                        System.err.printf("(i,j)=(%d,%d), tmp has some problem\n",i,j);
                        continue;
                    }
                    out.printf("\n\t%9e, %8e, %8e;", tmp[0],tmp[1],tmp[2]);
                }
                out.printf("];\n\n");
            }
            out.close();
        }
    }

    ArrayList<Material> readTable1(String url) throws Exception{
        ParseHTML table=new ParseHTML(url,"table");
        if(! table.hasNext() )
            return null;
        table.refine(); table.setTag("tr");
        //System.out.println(table.getContent());

        Material singlerow;
        int col;
        //System.out.println(content);
        ArrayList<Material> mcTable=new ArrayList<Material>();
        int id=0;
        double za=0,i=0,density=0;
        String sym="", name="", cell;

        Iterator<String> itr;

        while(table.hasNext()){
            //System.err.println(table.group());
            table.stash(); table.refine(); table.setTag("t(d|h)");
            itr=table.findAll().iterator();
            table.stashPop();

            col=0;
            while(itr.hasNext()){
                cell=itr.next().trim();
                if(cell.matches(Material.heads[col])){
                    switch(col){
                        case 0: 
                            id=Integer.parseInt(cell);
                            break;
                        case 1: 
                            sym=cell;
                            break;
                        case 2: 
                            name=cell;
                            //System.out.println(name);
                            break;
                        case 3: 
                            za=Double.parseDouble(cell);
                            break;
                        case 4: 
                            i=Double.parseDouble(cell);
                            break;
                        case 5: 
                            density=Double.parseDouble(cell);
                            singlerow=new Material(id,sym,name,za,i,density);
                            mcTable.add(singlerow);
                            //System.out.println(singlerow);
                            break;
                    }
                    col++;
                }
            }
        }
        return mcTable;
    }

    ArrayList<Material> readTable2(String url){
        ParseHTML table=new ParseHTML(url,"table");
        if(! table.hasNext() )
            return null;
        table.refine(); table.setTag("tr");

        Composition singlerow;
        int col,row=0;
        //System.out.println(content);
        ArrayList<Material> mcTable=new ArrayList<Material>();
        int id=0;
        double za=0,i=0,density=0;
        String sym="", name="", cell;

        Iterator<String> itr;

        while(table.hasNext()){
            //if(id>1) break;
            row++;
            //System.out.println(table.group());
            if(row<=2) continue;
            table.stash(); table.refine(); table.setTag("t(d|h)");
            itr=table.findAll().iterator();
            table.stashPop();

            col=0;
            while(itr.hasNext()){
                cell=itr.next().trim();
                if(cell.matches(Composition.heads[col])){
                    //System.out.println(cell);
                    switch(col){
                        case 0: 
                            name=cell;
                            //System.out.println(name);
                            break;
                        case 1: 
                            za=Double.parseDouble(cell);
                            break;
                        case 2: 
                            i=Double.parseDouble(cell);
                            break;
                        case 3: 
                            density=Double.parseDouble(cell);
                            break;
                        case 4: 
                            sym=name.replaceFirst("^[^A-Z]*","").split("[\\(\\),/\\s]+")[0].replaceAll("-","");
                            singlerow=new Composition(id,sym,name,za,i,density);
                            id++;
                            Matcher components = Pattern.compile("(\\d{1,2})\\s*:\\s*([0-9.]+)").matcher(cell);
                            while(components.find())
                                singlerow.add(Integer.parseInt(components.group(1)),
                                        Float.parseFloat(components.group(2)));
                            //System.out.println(singlerow);
                            mcTable.add(singlerow);
                            break;
                    }
                    col++;
                }
            }
        }
        return mcTable;
    }

    ArrayList<ArrayList<double[]>> lookupTable(String url, ArrayList<Material> list){
        ArrayList<ArrayList<double[]>> res = new ArrayList<ArrayList<double[]>>();
        for(int i=0; i<list.size(); i++) res.add(null);

        ParseHTML table=new ParseHTML(url,"table");
        if(! table.hasNext() ) return null;
        table.refine(); table.setTag("tr");

        int row=0;
        String cell;

        Iterator<String> itr;
        String temp;
        URI tU;

        while(table.hasNext()){
            row++;
            //if(row>4) System.exit(0);
            //System.out.println(table.group().replaceAll("\\n",""));
            //System.out.println(table.group());
            table.stash(); table.refine(); table.setTag("t(d|h)");
            itr=table.findAll().iterator();
            table.stashPop();
            while(itr.hasNext()){
                cell = itr.next().trim();
                temp = ParseHTML.getLink(cell,".+?");
                //System.out.println(cell);
                if(temp!=null)
                    cell = cell.replaceAll("(?ims)<[^>]+>","");
                else continue;
                //System.out.println(cell);

                boolean find = false;
                for(int i=0; i<list.size(); i++){
                    if(list.get(i).pairs(cell)){
                        //System.out.println(cell + " pairs " + 
                        //list.get(i).name);
                        if(list.get(i) instanceof Composition)
                            list.get(i).sym = temp.replaceAll(".*/","").replaceAll("\\..*","");
                        //System.out.println(list.get(i).sym);
                        try{
                            tU = new URI(url).resolve(temp);
                            temp=tU.toString();
                            System.out.println(temp);
                        }catch(Exception e){
                            System.err.println(e);
                        }
                        find = true;
                        if(res.get(i)!=null)
                            System.err.println("ERR: \""+list.get(i).name+"\" has already been found");
                        else
                            res.set(i,readAttenTable(temp,3));
                        //System.out.println("finished reading 
                        //"+list.get(i).sym);
                        break;
                    }
                }
                if(!find) System.err.println("ERR: \""+cell+"\" not found");
            }
        }
        return res;
    }

    ArrayList<double[]> readAttenTable(String url, int nc){
        ParseHTML table=new ParseHTML(url, "table");
        if(table.hasNext()) table.refine();
        if(table.hasNext()) table.refine();
        table.setTag("tr");

        Matcher temp;
        ArrayList<double[]> macTable=new ArrayList<double[]>();
        int c;
        double[] data;
        String[] cell;
        while(table.hasNext()){
            //String test=table.group().replaceAll("\\n"," 
            //").replaceAll("(?ims)<[^>]*>"," ");
            cell = table.group().replaceAll("\\n"," ").replaceAll("(?ims)<[^>]*>"," ").trim().split("[\\s]+");
            //temp = Pattern.compile().matcher(cell);
            c=0;
            data=new double[nc];
            for(int i=0; i< cell.length && c<nc; i++){
                try{
                    if(cell[i].matches("(\\d\\.\\d*[eE\\+\\-]{1,2}\\d*)|([0-9\\.]+)"))
                        data[c]=Double.parseDouble(cell[i]);
                    else continue;
                }catch(Exception e){
                    continue;
                }
                c++;
            }
            if(c==nc){
                macTable.add(data);
                //System.out.println(Arrays.toString(data));
                //System.out.println(Arrays.toString(cell));
            }
        }
        return macTable;
    }
}

class Material{
    static final String[] heads={"\\d{1,2}", "[A-Z][a-z]?", "(\\w|,|\\s)+", 
        "[0-9.+\\-Ee]+", "[0-9.+\\-Ee]+", "[0-9.+\\-Ee]+"};
    int id;
    double za,i,density;
    String sym,name;
    Material(int id,String sym, String name, double za,
            double i, double density){
        this.id=id;
        this.sym=sym;
        this.name=name;
        this.za=za;
        this.i=i;
        this.density=density;
    }
    public String toString(){
        return String.format("%2d %4s %20s %9.5f %7.1f %18e",
            id,sym, name, za, i, density);
    }
    boolean pairs(String fn){
        String[] n = name.toLowerCase().replaceAll("(\\&[a-z]{4,4};)|(\\n)","")
            .split("[\\-\\(\\),/\\s]+");
        String[] m = fn.toLowerCase().replaceAll("(\\&[a-z]{4,4};)|(\\n)","")
            .split("[\\-\\(\\),/\\s]+");
        boolean res=true;
        if(m.length==n.length){
            for(int i=0; i< m.length; i++)
                if(!m[i].equals(n[i])) res=false;
        }else res=false;
        //if(m[0].equals("15") && m[0].equals(n[0])){
        //    System.out.println(Arrays.toString(n));
        //    System.out.println(Arrays.toString(m));
        //}
        return res;
    }
}

class Composition extends Material{
    static final String[] heads={"[a-zA-Z\\-0-9\\(\\),/\\s]+", 
        "[0-9.+\\-Ee]+", "[0-9.+\\-Ee]+", "[0-9.+\\-Ee]+", "((\\d{1,2})\\s*:\\s*([0-9.]+)[^0-9]*)+"};
    ArrayList<Integer> comp;
    ArrayList<Float> prop;
    Composition(int id, String sym, String name, double za,
            double i, double density){
        super(id,sym,name,za,i,density);
        comp = new ArrayList<Integer>();
        prop = new ArrayList<Float>();
    }
    public void add(int c, float p){
        comp.add(c); prop.add(p);
    }
    public String toString(){
        String str= String.format("%2d %10s %30s %9.5f %7.1f %18e",
            id,sym, name, za, i, density) + "\n";
        for(int i=0; i<comp.size(); i++)
            str += "\t\t\t" + comp.get(i) +" : " + prop.get(i) + "\n";
        return str;
    }
}

