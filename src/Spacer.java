import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import de.saar.coli.salsa.reiter.framenet.*;
import de.saar.coli.salsa.reiter.framenet.FrameNet;
import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.CSVReader;

/**
 * Created by cliff on 01/02/14.
 */

//args
// 0=input folder
// 1=output folder
// 2=type
// 3=lu column
// 4=pos column
// 5=binary for delete content of output folder before starting parse
// 6=optional type
//    1=framenetfolder
//    2=supplied frame list
// optional 7=framenetfolder
// optional 7=supplied frame list


public class Spacer
{
    static String InputFileFolder = "";
    static String OutputFileFolder = "";
    static int type = 0;
    static FrameNet fn;
    static int parsedColumns = 22;
    static String[] row;
    static String[] prevRow;
    static String[] rowout;
    static String lastRow = "1";


    public static void main(String[] args)
    {
        try
        {
            if(args.length>0)
            {
                System.out.println(args[0]);
                InputFileFolder=args[0];
                System.out.println(args[1]);
                OutputFileFolder=args[1];
                System.out.println(args[2]);
                type=Integer.parseInt(args[2]);

                if(Integer.parseInt(args[5])==1)      //delete the output before parsing
                {
                    File f = new File(OutputFileFolder);
                    File[] matchingFiles = f.listFiles();

                    if(matchingFiles!=null)
                    {
                        for(File tf : matchingFiles)
                        {
                            tf.delete();
                        }
                    }
                }

                String FrameNetFolder = "";

                //FrameNetFolder = "/Users/cliff/Desktop/Dropbox/MSD/Tech/fndata-1.5/fndata-1.5"; //Mac
                FrameNetFolder = "G:\\ShareOne\\Cliff\\Dropbox\\MSD\\Tech\\fndata-1.5\\fndata-1.5"; //PC


                System.out.println(args[6]);
                if(Integer.parseInt(args[6])==1)
                {
                    FrameNetFolder=args[7];
                }

                int luCol = Integer.parseInt(args[3])-1;
                int posCol = Integer.parseInt(args[4])-1;
                File f = new File(InputFileFolder);
                File[] matchingFiles = f.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".csv");
                    }
                });
                System.out.println("Scanning ... " + InputFileFolder);
                int filecount=0;

                fn = new FrameNet();
                File fnHome = new File(FrameNetFolder);
                DatabaseReader reader = new FNDatabaseReader15(fnHome,true);
                fn.readData(reader);

                //create local list of LUs and POS
                System.out.println("Create local list of LUs and POS");
                List<String[]> LUs = new ArrayList<String[]>();

                for(Frame fr : fn.getFrames())
                {
                    for(LexicalUnit luv : fr.getLexicalUnits())
                    {
                        String[] tmpLU = new String[3];
                        tmpLU[0]=luv.getLexemeString();
                        tmpLU[1]=luv.getPartOfSpeechAbbreviation();
                        tmpLU[2]=fr.getName();
                        LUs.add(tmpLU);
                        //System.out.println("Added " + tmpLU[0] + ", " + tmpLU[1] + ", " + tmpLU[2]);
                    }
                }

                int rowcount = 0;

                for(File tf : matchingFiles)
                {
                    filecount++;
                    CSVReader csvinput = new CSVReader(new FileReader(tf.getAbsolutePath()));
                    System.out.println("Reading csv file ...");
                    List csvinputdata = csvinput.readAll();
                    csvinput.close();

                    CSVWriter csvout = new CSVWriter(new FileWriter(OutputFileFolder + File.separator + "ms-" + tf.getName()));

                    List<String> framesSoFar = new ArrayList<String>();

                    List<String> SuppliedFramesO  = new ArrayList<String>();

                    for(Object ob : csvinputdata)
                    {
                        List<String> frames = new ArrayList<String>();
                        row=(String[]) ob;
                        String tLU = row[luCol];
                        String tPOS = row[posCol];

                        boolean init = false;

                        for(String[] strlus : LUs)
                        {
                            if(strlus[0].trim().equals(tLU) && strlus[1].equals(tPOS))
                            {
                                //System.out.println("Checking " + strlus[0] + ", " + strlus[1] + ", " + strlus[2]);
                                frames.add(strlus[2]);
                                //System.out.println(strlus[2]);
                                framesSoFar.add(strlus[2]);
                                init=true;
                                //System.out.println("Testing " + tLU + " , " + tPOS);
                            }
                        }

                        if(Integer.parseInt(args[6])==2)
                        {

                            List<String> SuppliedFrames  = new ArrayList<String>();


                            String[] sfo = row[1].trim().split(" ");

                            for(String s : sfo)
                            {
                                SuppliedFrames.add(s);
                            }

                            if(!lastRow.equals(row[0]))
                            {
                                for(String s : SuppliedFramesO)
                                {
                                    if(!framesSoFar.contains(s))
                                    {
                                        frames.add(s);
                                    }
                                }

                                if(frames.size()>0)
                                {
                                    csvout.writeNext(writeFrameData(type, frames, tLU, "True"));
                                    rowcount++;
                                    System.out.println("Written a row: " + rowcount);
                                }
                                framesSoFar = new ArrayList<String>();
                            }
                            else
                            {
                                SuppliedFramesO = SuppliedFrames;
                            }

                        }


                        if(init)
                        {
                            csvout.writeNext(writeFrameData(type, frames, tLU, "False"));
                            rowcount++;
                            System.out.println("Written a row: " + rowcount);
                        }
                        else // no frames
                        {
                            //System.out.println("No frames!");
                            rowout = new String[row.length+parsedColumns];
                            int a = 0;

                            for(String s : row)
                            {
                                rowout[a] = s;
                                a++;
                            }

                            for(int n = 0 ; n<parsedColumns ; n++)
                            {
                                rowout[a+n] = "";
                            }

                            csvout.writeNext(rowout);
                            rowcount++;
                        }

                        lastRow = row[0];
                        prevRow = row;

                    }

                    csvout.close();
                }
            }
            else
            {
                System.out.println("Error - no parameters supplied");
            }
        }
        catch (Exception ex)
        {
            System.out.println("Error:-" + ex.toString() + ", " + ex.getMessage() + ", " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }


    static public List<String> getFrameElements(String frame)
    {
        List<String> outputFrameElements = new ArrayList<String>();
        try
        {
            Frame f = fn.getFrame(frame);
            for(FrameElement fe : f.getFrameElements().values())
            {
                outputFrameElements.add(fe.getName());
            }
        }
        catch (Exception ex)
        {
            System.out.println("Error:-" + ex.toString() + ", " + ex.getMessage() + ", " + ex.getLocalizedMessage());
            ex.printStackTrace();

        }
        return outputFrameElements;
    }

    static public List<String> getFrameLUs(String frame)
    {
        List<String> outputLUs = new ArrayList<String>();
        try
        {
            Frame f = fn.getFrame(frame);
            for(LexicalUnit lu : f.getLexicalUnits())
            {
                outputLUs.add(lu.getName());
            }
        }
        catch (Exception ex)
        {
            System.out.println("Error:-" + ex.toString() + ", " + ex.getMessage() + ", " + ex.getLocalizedMessage());
            ex.printStackTrace();

        }
        return outputLUs;
    }

    static public String[] writeFrameData(int type, List<String> frames, String tLU, String extraLine)
    {

        try
        {
            if(type==1)  //basic framenet spacers
            {
                rowout = new String[row.length + parsedColumns];

                for(int h=0 ; h<row.length + parsedColumns ; h++)
                {
                    rowout[h]="";
                }

                for(String frnm : frames)
                {

                    Frame fr = fn.getFrame(frnm);

                    int a = 0;

                    if(extraLine.equals("True"))
                    {
                        for(String s : prevRow)
                        {
                            rowout[a] = s;
                            a++;
                        }
                        rowout[prevRow.length-1]="";
                        rowout[prevRow.length-2]="";
                        rowout[prevRow.length-3]="";
                        rowout[prevRow.length-4]="";
                        rowout[prevRow.length-5]="";
                        rowout[prevRow.length-6]="0";
                    }
                    else
                    {
                        for(String s : row)
                        {
                            rowout[a] = s;
                            a++;
                        }
                    }

                    //add Frame name
                    rowout[a]=rowout[a] + " " + fr.getName();
                    a++;

                    //add Frame Elements
                    String FEs = "";
                    for(String FE : getFrameElements(fr.getName()))
                    {
                        FEs = FEs + FE + " ";
                    }
                    FEs.trim();
                    rowout[a]=rowout[a] + " " + FEs;
                    a++;

                    //add Frame LUs
                    String fLUs = "";
                    for(String fLU : getFrameLUs(fr.getName()))
                    {
                        fLUs = fLUs + fLU + " ";
                    }
                    fLUs.trim();
                    rowout[a]=rowout[a] + " " + fLUs;
                    a++;

                    String ibFs = "";
                    for(Frame IdF : fr.isInheritedBy())
                    {
                        ibFs = ibFs + IdF + " ";
                    }
                    ibFs.trim();
                    rowout[a]=rowout[a] + " " + ibFs;
                    a++;

                    String pFs = "";
                    for(Frame IdF : fr.perspectivized())
                    {
                        pFs = pFs + IdF + " ";
                    }
                    pFs.trim();
                    rowout[a]=rowout[a] + " " + pFs;
                    a++;

                    String uFs = "";
                    for(Frame IdF : fr.uses())
                    {
                        uFs = uFs + IdF + " ";
                    }
                    uFs.trim();
                    rowout[a]=rowout[a] + " " + uFs;
                    a++;

                    String ubFs = "";
                    for(Frame IdF : fr.usedBy())
                    {
                        ubFs = ubFs + IdF + " ";
                    }
                    ubFs.trim();
                    rowout[a]=rowout[a] + " " + ubFs;
                    a++;

                    String hsfFs = "";
                    for(Frame IdF : fr.hasSubframe())
                    {
                        hsfFs = hsfFs + IdF + " ";
                    }
                    hsfFs.trim();
                    rowout[a]=rowout[a] + " " + hsfFs;
                    a++;

                    String incFs = "";
                    for(Frame IdF : fr.inchoative())
                    {
                        incFs = incFs + IdF + " ";
                    }
                    incFs.trim();
                    rowout[a]=rowout[a] + " " + incFs;
                    a++;

                    String incsFs = "";
                    for(Frame IdF : fr.inchoativeStative())
                    {
                        incsFs = incsFs + IdF + " ";
                    }
                    incsFs.trim();
                    rowout[a]=rowout[a] + " " + incsFs;
                    a++;

                    String cauFs = "";
                    for(Frame IdF : fr.causative())
                    {
                        cauFs = cauFs + IdF + " ";
                    }
                    cauFs.trim();
                    rowout[a]=rowout[a] + " " + cauFs;
                    a++;

                    String caustFs = "";
                    for(Frame IdF : fr.causativeStative())
                    {
                        caustFs = caustFs + IdF + " ";
                    }
                    caustFs.trim();
                    rowout[a]=rowout[a] + " " + caustFs;
                    a++;

                    String aifFs = "";
                    for(Frame IdF : fr.allInheritedFrames())
                    {
                        aifFs = aifFs + IdF + " ";
                    }
                    aifFs.trim();
                    rowout[a]=rowout[a] + " " + aifFs;
                    a++;

                    String aigfFs = "";
                    for(Frame IdF : fr.allInheritingFrames())
                    {
                        aigfFs = aigfFs + IdF + " ";
                    }
                    aigfFs.trim();
                    rowout[a]=rowout[a] + " " + aigfFs;
                    a++;

                    String earFs = "";
                    for(Frame IdF : fr.earlier())
                    {
                        earFs = earFs + IdF + " ";
                    }
                    earFs.trim();
                    rowout[a]=rowout[a] + " " + earFs;
                    a++;

                    String ifFs = "";
                    for(Frame IdF : fr.inheritsFrom())
                    {
                        ifFs = ifFs + IdF + " ";
                    }
                    ifFs.trim();
                    rowout[a]=rowout[a] + " " + ifFs;
                    a++;

                    String lFs = "";
                    for(Frame IdF : fr.later())
                    {
                        lFs = lFs + IdF + " ";
                    }
                    lFs.trim();
                    rowout[a]=rowout[a] + " " + lFs;
                    a++;

                    String nFs = "";
                    for(Frame IdF : fr.neutral())
                    {
                        nFs = nFs + IdF + " ";
                    }
                    nFs.trim();
                    rowout[a]=rowout[a] + " " + nFs;
                    a++;

                    String refFs = "";
                    for(Frame IdF : fr.referred())
                    {
                        refFs = refFs + IdF + " ";
                    }
                    refFs.trim();
                    rowout[a]=rowout[a] + " " + refFs;
                    a++;

                    String refrFs = "";
                    for(Frame IdF : fr.referring())
                    {
                        refrFs = refrFs + IdF + " ";
                    }
                    refrFs.trim();
                    rowout[a]=rowout[a] + " " + refrFs;
                    a++;

                    String sfoFs = "";
                    for(Frame IdF : fr.subframeOf())
                    {
                        sfoFs = sfoFs + IdF + " ";
                    }
                    sfoFs.trim();
                    rowout[a]=rowout[a] + " " + sfoFs;
                    a++;

                    if(extraLine.equals("True"))
                    {
                        rowout[a]="";
                    }
                    else
                    {
                        //add evokingLU
                        rowout[a]=tLU;
                        //a++;
                    }


                }

            }
            if(type==2)  //some more advanced spacers
            {
            }
        }
        catch (Exception ex)
        {
            System.out.println("Error:-" + ex.toString() + ", " + ex.getMessage() + ", " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }

        return rowout;
    }
}
