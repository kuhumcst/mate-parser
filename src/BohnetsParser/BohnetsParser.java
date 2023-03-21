import java.io.*;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;
import java.util.ArrayList;
//import java.util.Enumeration;
import java.util.Properties;

import is2.data.Cluster;
import is2.data.DataF;
import is2.data.DataFES;
import is2.data.F2SF;
import is2.data.FV;
import is2.data.Instances;
import is2.data.Long2Int;
import is2.data.Long2IntInterface;
import is2.data.Parse;
import is2.data.PipeGen;
import is2.data.SentenceData09;
import is2.io.CONLLReader09;
import is2.io.CONLLWriter09;
import is2.tools.Retrainable;
import is2.tools.Tool;
import is2.util.DB;
import is2.util.OptionsSuper;
import is2.util.ParserEvaluator;

import is2.tag.Tagger;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.apache.commons.io.IOUtils;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
import java.io.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import is2.data.Cluster;
import is2.data.DataF;
import is2.data.DataFES;
import is2.data.F2SF;
import is2.data.FV;
import is2.data.Instances;
import is2.data.Long2Int;
import is2.data.Long2IntInterface;
import is2.data.Parse;
import is2.data.PipeGen;
import is2.data.SentenceData09;
import is2.io.CONLLReader09;
import is2.io.CONLLWriter09;
import is2.tools.Retrainable;
import is2.tools.Tool;
import is2.util.DB;
import is2.util.OptionsSuper;
import is2.util.ParserEvaluator;
*/
import is2.parser.*;
/*
import org.apache.commons.fileupload.Part;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.httpclient.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
*/
/*This file is 𝕌𝕋𝔽-𝟠 encoded*/

@SuppressWarnings("serial")
public class BohnetsParser extends HttpServlet 
    {    
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(BohnetsParser.class);
    private static final String TMP_DIR_PATH = "/tmp";
    

    public String readProperty(String property)
        {
        String ret = null;
        try
            {
            FileInputStream fis = new java.io.FileInputStream(getServletContext().getRealPath("/WEB-INF/classes/properties.xml"));
            Properties prop = new Properties();
            try 
                {
                prop.loadFromXML(fis);
                ret = prop.getProperty(property,"");
                }
            catch (IOException io)
                {
                logger.warn("could not read properties. Message is " + io.getMessage()); 
                }
            finally
                {
                fis.close();
                }
            }
        catch (IOException io)
            {
            logger.warn("Could not read properties file. Message is " + io.getMessage()); 
            }	
        return ret;
        }

    public void init(ServletConfig config) throws ServletException 
        {
        super.init(config);
        }

    public String modelFileName(String language)
        {
        if(language.length() > 3)
            return language; // Language is in fact path to model
        String modelPathHead = readProperty("modelPathHead");
        String modelPathTail = readProperty("modelPathTail");
        String[] models = readProperty("models").split(",");
        //logger.debug("language: "+language);

        for(int i = 0 ;i < models.length ; i++)
            {
            String lang = models[i];
            if(language.equals(lang))
                {
                return readProperty("modelPathHead")+lang+readProperty("modelPathTail")+readProperty(lang);
                }
            }
        //logger.debug("language not found");
        return "";
        }

    public void parseSentence(ArrayList<String> lines,PrintWriter out,String ModelFileName)
        {
        int l = lines.size();
        if(l == 0)
            return;

        //logger.debug("parse, l == {}",l);
                                            //    0:ID 1:FORM 2:LEMMA 3:PLEMMA 4:POS 5:PPOS 6:FEAT 7:PFEAT 8:HEAD 9:PHEAD 10:DEPREL
                                            //   11:PDEPREL 12:FILLPRED 13:PRED 14:APREDs
        String[] forms = new String[l];     // 1
        String[] lemmas = new String[l];    // 2
        String[] plemmas = new String[l];   // 3
        String[] gpos = new String[l];      // 4
        String[] ppos = new String[l];      // 5
        String[] ofeats=new String[l];      // 7
        String[] pfeats=new String[l];      // 7
        int[]    heads = new int[l];        // 9
        String[] labels = new String[l];    // 11
        String[] fillp = new String[l];     // 12
        for(int k=0;k<l;k++) 
            {
            String[] values = lines.get(k).toString().split("\\t");
            int cols = values.length;
            if(cols > 1)
                {
                forms[k] = values[1];
                if(cols > 2)
                    {
                    lemmas[k] = values[2];
                    if(cols > 3)
                        {
                        plemmas[k] = values[3];
                        if(cols > 4)
                            {
                            gpos[k] = values[4];
                            if(cols > 5)
                                {
                                ppos[k] = values[5];
                                if(cols > 7)
                                    {
                                    ofeats[k] = values[7];
                                    if(cols > 9)
                                        {
                                        if(values[9].equals("_"))
                                            {
                                            heads[k] = -1;
                                            }
                                        else
                                            {
                                            try {
                                                heads[k] = Integer.parseInt(values[9]);
                                                }
                                            catch(java.lang.NumberFormatException e)
                                                {
                                                heads[k] = -1;
                                                }
                                            }
                                        if(cols > 11)
                                            {
                                            labels[k] = values[11];
                                            if(cols > 12)
                                                {
                                                fillp[k] = values[12];
                                                }
                                            else
                                                {
                                                fillp[k] = "_";
                                                }
                                            }
                                        else
                                            {
                                            labels[k] = "_";
                                            }
                                        }
                                    else
                                        {
                                        heads[k] = -1;
                                        }
                                    }
                                else
                                    {
                                    ofeats[k] = "_";
                                    }
                                }
                            else
                                {
                                ppos[k] = "_";
                                }
                            }
                        else
                            {
                            gpos[k] = "_";
                            }
                        }
                    else
                        {
                        plemmas[k] = "_";
                        }
                    }
                else
                    {
                    lemmas[k] = "_";
                    }
                }
            else
                {
                forms[k] = "_";
                }
            }
        
        Parser aParser = new Parser();
        //logger.debug("ModelFileName: "+ModelFileName);
        aParser.options = new Options(new String[]{"-model", ModelFileName});
        aParser.pipe = new Pipe(aParser.options);
        aParser.params = new ParametersFloat(0);  // total should be zero and the parameters are later read 
        try {
            aParser.readModel(aParser.options, aParser.pipe, aParser.params);
            }
        catch(Exception e) 
            {          
            e.printStackTrace();
            }
        SentenceData09 rawSentence = new SentenceData09(forms,lemmas,gpos,ppos,labels,heads);

        boolean labelOnly = false;
        //logger.debug("raw: "+rawSentence.toString());
        SentenceData09 parsedSentence = aParser.parse(rawSentence,aParser.params,labelOnly,aParser.options);
        //logger.debug("syn: "+parsedSentence.toString());

        out.println(parsedSentence.toString());
        }

/**
Parse conll09 formatted input. It is important that the words are in the second
column (index 1) and the POS tags in the sixth (index 5). More than six columns
are not needed, but can be added. Unspecified strings must be written as _ and
unspecified integers as -1. The first line should contain the first word of the
first sentence, not ROOT. Each word is on a separate line. Sentences must be
separated by an empty line.

@param  arg the full path to the input file.
@param  out PrintWriter object to which output is written
@return     the parsed sentence, in conll09 format. The tenth column (index 11)
            contains the head, the twelfth column (index 13) the label.
*/
    public void parse(String arg,PrintWriter out,String ModelFileName)
    throws IOException    
        {
        //logger.debug("parse({},{})",arg,ModelFileName);
        try {
            BufferedReader dis = new BufferedReader(new InputStreamReader(new FileInputStream(arg),"UTF8"));
            String str;
            ArrayList<String> lines = new ArrayList<String>();
            lines.add(new String("0\t<root>\t_\t_\t_\t<root-POS>"));
            while ((str = dis.readLine()) != null) 
                {
                if (str.trim().length() == 0) 
                    {
                    parseSentence(lines,out,ModelFileName);
                    lines.clear();
                    lines.add(new String("0\t<root>\t_\t_\t_\t<root-POS>"));
                    }
                else
                    {
                    lines.add(str);
                    //logger.debug("str: "+str);
                    }
                }
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(arg));
            if(lines.size() > 1)
                parseSentence(lines,out,ModelFileName);
            }
        catch(UnsupportedEncodingException ue)
            {
            logger.error("Not supported : "+ue.getMessage());
            }
        catch(IOException e)
            {
            logger.error("FileNotFoundException: "+e.getMessage());
            }
        }


    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
        {
        PrintWriter out = response.getWriter();
        out.println("GET not supported");
        }
 
     public static String getParmFromMultipartFormData(HttpServletRequest request,Collection<Part> items,String parm)
        {
        logger.debug("getParmFromMultipartFormData:["+parm+"]");
        String ret = "";
        try 
            {
            logger.debug("items:"+items);
            Iterator<Part> itr = items.iterator();
            logger.debug("itr:"+itr);
            while(itr.hasNext()) 
                {
                logger.debug("in loop");
                Part item = itr.next();
                logger.debug("Field Name = "+item.getName()+", String = "+IOUtils.toString(item.getInputStream(),StandardCharsets.UTF_8));
                if(item.getName().equals(parm))
                    {
                    ret = IOUtils.toString(item.getInputStream(),StandardCharsets.UTF_8);
                    logger.debug("Found " + parm + " = " + ret);
                    break; // currently not interested in other fields than parm
                    }
                }
            }
        catch(Exception ex) 
            {
            logger.error("uploadHandler.parseRequest Exception");
            }
        logger.debug("value["+parm+"]="+ret);
        return ret;
        }
 
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
        {
        Collection<Part> items = getParmList(request);
        String language = getParmFromMultipartFormData(request,items,"model");
        request.setCharacterEncoding( "UTF-8" );
        response.setContentType("text/plain; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        //logger.debug("doPost, RemoteAddr == {} language == {}",request.getRemoteAddr(),language);

        java.lang.String arg  = getParmsAndFiles(items,response,out);
        //logger.debug("parse {}",arg);

        String ModelFileName = modelFileName(language);
        //logger.debug("ModelFileName {}",ModelFileName);
        parse(arg,out,ModelFileName);      
        }

    public static Collection<Part> getParmList(HttpServletRequest request) throws ServletException
        {
        Collection<Part> items = null;
        
        try 
            {
            items = request.getParts(); // throws ServletException if this request is not of type multipart/form-data
            }
        catch(IOException ex) 
            {
            logger.error("Error encountered while parsing the request: "+ex.getMessage());
            }
        catch(ServletException ex) 
            {
            logger.error("Error encountered while parsing the request: "+ex.getMessage());
            }
/*
        Collection<Part> items = null;
        
        Enumeration<?> parmNames = request.getParameterNames();
        for (Enumeration<?> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = (String)e.nextElement();
            //logger.debug("parmName: " + parmName);            
            String vals[] = request.getParameterValues(parmName);
            for(int j = 0;j < vals.length;++j)
                {
                //logger.debug("value: " + vals[j]);            
                }
            }
        
        boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);

        if(is_multipart_formData)
            {
            DiskFileItemFactory  fileItemFactory = new DiskFileItemFactory ();
            // Set the size threshold, above which content will be stored on disk.
            fileItemFactory.setSizeThreshold(1*1024*1024); //1 MB
            // Set the temporary directory to store the uploaded files of size above threshold.
            File tmpDir = new File(TMP_DIR_PATH);
            if(!tmpDir.isDirectory()) 
                {
                throw new ServletException("Trying to set \"" + TMP_DIR_PATH + "\" as temporary directory, but this is not a valid directory.");
                }
            fileItemFactory.setRepository(tmpDir);
            
            ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
            try {
                items = (Collection<Part>)uploadHandler.parseRequest(request);
                }
            catch(FileUploadException ex) 
                {
                logger.error("Error encountered while parsing the request: "+ex.getMessage());
                }
            }
*/
        return items;
        }

    public java.lang.String getParmsAndFiles(Collection<Part> items,HttpServletResponse response,PrintWriter out) throws ServletException
        {       
        logger.debug("getParmsAndFiles");

        java.lang.String arg = "";

        try {
            // Parse the request
            Iterator<Part> itr = items.iterator();
            while(itr.hasNext()) 
                {
                logger.debug("in loop");
                Part item = itr.next();
                // Handle Form Fields.
                if(item.getSubmittedFileName() != null)
                    {
                    //Handle Uploaded files.
                    String LocalFileName = item.getSubmittedFileName();
                    logger.debug("LocalFileName:"+LocalFileName);
                    // Write file to the ultimate location.

                    File tmpDir = new File(TMP_DIR_PATH);
                    if(!tmpDir.isDirectory()) 
                        {
                        throw new ServletException("Trying to set \"" + TMP_DIR_PATH + "\" as temporary directory, but this is not a valid directory.");
                        }

                    File file = File.createTempFile(LocalFileName,null,tmpDir);
                    String filename = file.getAbsolutePath();
                    logger.debug("LocalFileName:"+filename);
                    item.write(filename);
                    arg = filename;
                    }
                }
            }
        catch(Exception ex) 
            {
            }
        return arg;
        }
    }
