import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.util.*;

public class ReadNist {
    public static void main(String[] args) throws Exception {
        ReadNist reader=new ReadNist();
        String url;
        ParseHTML home = new ParseHTML("http://www.nist.gov/pml/data/xraycoef/");

        if(args.length!=0){
            String fn = args[0];
            if(!fn.equals("")){
            }
        }

        url = home.getLink("\\s*Table\\s*1.\\s*");
        ArrayList<Material> table1=reader.readTable1(url);
        if(fn

        url = home.getLink("\\s*Table\\s*2.\\s*");
        ArrayList<Material> table2=reader.readTable2(url);

        url = home.getLink("\\s*Table\\s*3.\\s*");
        ArrayList<ArrayList<double[]>> table3=reader.lookupTable(url,table1);

        url = home.getLink("\\s*Table\\s*4.\\s*");
        ArrayList<ArrayList<double[]>> table4=reader.lookupTable(url,table2);

        /*
        System.out.print("density=[\n");
        while(itr.hasNext()){
            System.out.println(itr.next().density+"; ");
        }
        System.out.println("];\n");

        url = ParseHTML.getLink(html,"\\s*Table 1.\\s*");
        ArrayList<Material> mcTable=reader.readTable1(url);
        Iterator<Material> itr=mcTable.iterator();
        System.out.print("density=[\n");
        while(itr.hasNext()){
            System.out.println(itr.group().density+"; ");
        }
        System.out.println("];\n");

        */
    }

    ArrayList<Material> readTable1(String url) throws Exception{
        String in=ParseHTML.readHTML(url);

        ParseHTML table=new ParseHTML(in,"table");
        if(! table.hasNext() )
            return null;
        table.refine("tr");

        Material singlerow;
        int col;
        //System.out.println(content);
        ArrayList<Material> mcTable=new ArrayList<Material>();
        int id=0;
        double za=0,i=0,density=0;
        String sym="", name="", cell;

        ParseHTML rowHTML;
        Iterator<String> itr;

        while(table.hasNext()){
            rowHTML=table.copy();
            rowHTML.refine("t(d|h)");
            itr=rowHTML.findAll().iterator();

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
        String in=ParseHTML.readHTML(url);

        ParseHTML table=new ParseHTML(in,"table");
        if(! table.hasNext() )
            return null;
        table.refine("tr");

        Composition singlerow;
        int col,row=0;
        //System.out.println(content);
        ArrayList<Material> mcTable=new ArrayList<Material>();
        int id=0;
        double za=0,i=0,density=0;
        String sym="", name="", cell;

        ParseHTML rowHTML;
        Iterator<String> itr;

        while(table.hasNext()){
            //if(id>1) break;
            row++;
            //System.out.println(table.group());
            if(row<=2) continue;
            rowHTML=table.copy();
            rowHTML.refine("t(d|h)");
            itr=rowHTML.findAll().iterator();

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

    /*
    ArrayList<ArrayList<double[]>> lookupTable(String url, ArrayList<Material> list){
        Iterator<double[]> itr3;
        double[] tmp;
        for(int i=1; i<1+mcTable.size(); i++){
            System.out.printf("mac{%d} = [\n",i);
            itr3=reader.readTable3(i).iterator();
            while(itr3.hasNext()){
                tmp=itr3.group();
                System.out.printf("%9e, %8e, %8e;\n", tmp[0],tmp[1],tmp[2]);
            }
            System.out.printf("];\n\n");
        }
        return res;
    }
    */

    ArrayList<ArrayList<double[]>> lookupTable(String url, ArrayList<Material> list){
        String in=ParseHTML.readHTML(url);
        ArrayList<ArrayList<double[]>> res = new ArrayList<ArrayList<double[]>>();
        for(int i=0; i<list.size(); i++) res.add(null);

        ParseHTML table=new ParseHTML(in,"table");
        if(! table.hasNext() ) return null;
        table.refine("tr");

        int row=0;
        String cell;

        ParseHTML rowHTML;
        Iterator<String> itr;
        String temp;
        URI tU;

        while(table.hasNext()){
            row++;
            //if(row>4) System.exit(0);
            //System.out.println(table.group().replaceAll("\\n",""));
            //System.out.println(table.group());
            rowHTML=table.copy();
            rowHTML.refine("t(d|h)");
            itr=rowHTML.findAll().iterator();
            while(itr.hasNext()){
                cell = itr.next().trim();
                //System.out.println(cell);
                temp = ParseHTML.getLink(cell,"[^><]+");
                if(temp!=null)
                    cell = cell.replaceAll("(?ims)<\\s*/?a[^>]*>","");
                else continue;
                //System.out.println(cell);

                boolean find = false;
                for(int i=0; i<list.size(); i++){
                    if(list.get(i).pairs(cell)){
                        //System.out.println(cell + " pairs " + 
                        //list.get(i).name);
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
        String in=ParseHTML.readHTML(url);

        ParseHTML table=new ParseHTML(in, "table");
        if(table.hasNext()) table.refine();
        if(table.hasNext()) table.refine("tr");

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
        //if(m[0].equals("polyethylene") && m[0].equals(n[0])){
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

/*
            table4=ParseHTML.readHTML(ParseHTML.getLink(html,"\\s*Table 4.\\s*"));
            table4 = table4.toLowerCase();

            */
