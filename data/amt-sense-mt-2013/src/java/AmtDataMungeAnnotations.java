import java.io.*;
import java.util.*;

/** AmtDataMungeAnnotations
 *  Inputs: 1. original file of 5 columns of annotation data
 *          2. name of output directory
 *  Outputs:  multiple data files, 1 file per word
 *  Output 3 columns:  item annotator response
 *  Column 1 input -> name of output file
 *  Columns 2,3 input -> column 1 output ii
 *     renumber items  1:I, 
 *     output file of mappings -> original id, local id
 *  Column 4 input -> column 2 output
 *     must renumber annotatorIds  1:J, 
 *     output file of mappings -> original id, local id
 *  Column 5 input -> column 3 output
 */

public class AmtDataMungeAnnotations {

    static Integer addSymbol(Map<String,Integer> raterMap, String sym) {
        if (raterMap.containsKey(sym))
            return raterMap.get(sym);
        Integer nextId = raterMap.size() + 1;
        raterMap.put(sym,nextId);
        return nextId;
    }

    public static void main(String[] args) throws IOException {
        Map<String,Integer> catsPerWord = new LinkedHashMap<String,Integer>();
        Map<String,Integer> itemMap = new LinkedHashMap<String,Integer>();
        Map<String,Integer> raterMap = new LinkedHashMap<String,Integer>();

        String inputFileName = args[0];
        String mungeDirName = args[1];
        BufferedReader reader 
            = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), 
                                                       "ASCII"));

        // read through file 1 time, estable max senses per word
        String line;
        int lineNum = 0;
        while ((line = reader.readLine()) != null) {
            ++lineNum;
            if (lineNum == 1) {
                continue;
            }
            String[] fields = line.split("\t");
            if (fields.length != 5)
                throw new IOException("bad data line=" + line + "; line num=" + lineNum);

            int k = -1;
            try {
                k = Integer.parseInt(fields[4]);
            } catch (NumberFormatException e) {
                throw new IOException("bad data line=" + line + "; line num=" + lineNum);
            }
            Integer maxK = catsPerWord.get(fields[0]);
            if (maxK == null) {
                catsPerWord.put(fields[0],new Integer(k));
            } else if (maxK.intValue() < k) {
                    catsPerWord.put(fields[0],new Integer(k));
            }
        }
        reader.close();

        reader 
            = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), 
                                                       "ASCII"));
        BufferedWriter tsvWriter = null;
        String curWord = null;
        lineNum = 0;
        while ((line = reader.readLine()) != null) {
            ++lineNum;
            if (lineNum == 1) {
                continue;
            }
            String[] fields = line.split("\t");
            if (fields.length != 5)
                throw new IOException("bad data line=" + line + "; line num=" + lineNum);
            if (curWord == null || !(fields[0].equals(curWord))) {
                // encountered new WordPos
                if (curWord != null) {
                    // finish current WordPos
                    tsvWriter.close();
                    System.out.println("annotators for word " + curWord + ": " + raterMap.size());
                    // record mappings annotatorId -> jj
                    File curRaters = new File(mungeDirName,curWord+".map-a");
                    BufferedWriter mapWriter
                        = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(curRaters),
                                                                    "ASCII"));
                    mapWriter.write("amtId\tjj-"+curWord+"\n");
                    for (String key : raterMap.keySet())
                        mapWriter.write(key + "\t" + raterMap.get(key) + "\n");
                    mapWriter.close();
                    raterMap.clear();
                    File curItems = new File(mungeDirName,curWord+".map-i");
                    mapWriter
                        = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(curItems),
                                                                    "ASCII"));
                    // record mappings form,sentence -> ii
                    mapWriter.write("amtForm,Sentend\tii-"+curWord+"\n");
                    for (String key : itemMap.keySet())
                        mapWriter.write(key + "\t" + itemMap.get(key) + "\n");
                    mapWriter.close();
                    itemMap.clear();

                } 
                curWord = fields[0];
                System.out.println("#lines read=" + lineNum);
                System.out.println("processing annotations for word " + curWord);
                File curWordTsv = new File(mungeDirName,curWord+".tsv");
                tsvWriter
                    = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(curWordTsv),
                                                                "ASCII"));
                tsvWriter.write("item\tannotator\trating\n"); // header
            }
            Integer ii = addSymbol(itemMap,(fields[1] + "," + fields[2]));
            Integer jj = addSymbol(raterMap,fields[3]);
            int k = -1;
            try {
                k = Integer.parseInt(fields[4]);
            } catch (NumberFormatException e) {
                throw new IOException("bad data line=" + line + "; line num=" + lineNum);
            }
            if (k == 0) {
                Integer maxK = catsPerWord.get(fields[0]);
                k = maxK.intValue() + 1;
                System.out.println("line: " + lineNum + " word: " + fields[0] + ", cat 0 changed to " + k);
            }
            tsvWriter.write(ii + "\t" + jj + "\t" + k + "\n");
        }
        System.out.println("#lines read=" + lineNum);
        reader.close();

        if (curWord != null) {
            // finish current WordPos
            tsvWriter.close();
            // record mappings annotatorId -> jj
            File curRaters = new File(mungeDirName,curWord+".map");
            BufferedWriter mapWriter
                = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(curRaters),
                                                            "ASCII"));
            // record mappings form,sentence -> ii
            mapWriter.write("amtId\tjj-"+curWord+"\n");
            for (String key : raterMap.keySet())
                mapWriter.write(key + "\t" + raterMap.get(key) + "\n");
            mapWriter.close();
            // get mappings form,sentence ii
            File curItems = new File(mungeDirName,curWord+".map-i");
            mapWriter
                = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(curItems),
                                                            "ASCII"));
            mapWriter.write("amtForm,Sentend\tii-"+curWord+"\n");
            for (String key : itemMap.keySet())
                mapWriter.write(key + "\t" + itemMap.get(key) + "\n");
            mapWriter.close();
        }
    }
}