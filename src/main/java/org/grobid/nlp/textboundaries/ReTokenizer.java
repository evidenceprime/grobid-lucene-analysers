package org.grobid.nlp.textboundaries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.Version;

import org.grobid.analyzers.grobid.GrobidChineseAnalyzer;
import org.grobid.analyzers.grobid.GrobidJapaneseAnalyzer;
import org.grobid.analyzers.grobid.GrobidKoreanAnalyzer;
import org.grobid.nlp.Language;

/**
 * Class used to tokenize a sentence (a short text), usually simply used to return 
 * a sentences with tokens separated by spaces.
 * 
 * @author DevBpo
 *
 */
public class ReTokenizer {
	public static final String VERSION="1.10";
	public static final Version LUCENE_VERSION = Version.LUCENE_45;
	
	public Analyzer analyzer;

	private ArrayList<File> queue = new ArrayList<File>();

	private Language language_E=null;
	public String languageIdentification = "en";
	
	private String specialMsufBehaviour=null;

	private static String[] emptyToken = new String[1];
	static {emptyToken[0] = "";};
	
	public ReTokenizer (String lang) throws Exception {
		this(new Language(lang));
	}
	
	public ReTokenizer (Language language) throws Exception {
		
		if (System.getProperty("NLP.MsufBehaviour")!=null) {
			specialMsufBehaviour=System.getProperty("NLP.MsufBehaviour");
		}
		if (System.getenv().containsKey("NLPMsufBehaviour")) {
			specialMsufBehaviour=System.getenv().get("NLPMsufBehaviour");
		}

		this.language_E=language;
		String lang=language_E.getLanguageIdentification();
		this.languageIdentification=lang;	
				
		if (lang.startsWith("ja_g")) { // Japanese for Grobid
			analyzer = new GrobidJapaneseAnalyzer();
		} else if (lang.startsWith("zh_g")) { // Chinese for Grobid
			analyzer = new GrobidChineseAnalyzer();
		} else if (lang.startsWith("ko_g") || lang.startsWith("kr_g")) { // Korean for Grobid
			analyzer = new GrobidKoreanAnalyzer();
		}	
	}

	/**
	 * Indexes a file or directory
	 * @param fileName the name of a text file or a folder we wish to add to the index
	 * @throws java.io.IOException 
	 */
	public void indexFileOrDirectory(String fileName) throws IOException {
		//===================================================
		//gets the list of files in a folder (if user has submitted
		//the name of a folder) or gets a single file name (is user
		//has submitted only the file name) 
		//===================================================
		listFiles(new File(fileName));

		for (File f : queue) {
			try{
				tokenizeWithSpaceEachLineOfTextfile(f);
				System.err.println("Added: " + f);
			} catch (Exception e) {
				System.err.println("Could not add: " + f + "ex:"+e);
			} 
		}


		queue.clear();
	}
	private void tokenizeWithSpaceEachLineOfTextfile (File f) throws Exception {

		BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(f),Charset.forName("UTF-8")));
		System.err.println("converting: "+f.getAbsolutePath()+"...");

		String l;
		int lineNum=0;
		while ((l=buf.readLine()) != null) {
			String s = l.trim();
			System.out.println(tokenizeWithSpaces(s));
			if (lineNum++ % 1000 == 0) {System.err.print("STS: "+lineNum+" \r");}
		}
		buf.close();		
	}

	@SuppressWarnings("unused")
	private void indexEachLineOfTextfile (File f) throws Exception {

		BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(f),Charset.forName("UTF-8")));
		System.err.println("looking: "+f.getAbsolutePath()+"...");

		String l;

		while ((l=buf.readLine()) != null) {
			String s = l.trim();
			System.out.println(tokensAsString(s));
		}
		buf.close();		
	}

	private void listFiles(File file) {
		if (!file.exists()) {
			System.out.println(file + " does not exist.");
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				listFiles(f);
			}
		} else {
			String filename = file.getName().toLowerCase();
			//===================================================
			// Only index text files
			//===================================================
			if (filename.matches(".*\\.[a-z][a-z]") || filename.endsWith(".htm") || filename.endsWith(".html") || 
					filename.endsWith(".xml") || filename.endsWith(".txt")) {
				queue.add(file);
			} else {
				System.out.println("Skipped " + filename);
			}
		}
	}

	public String tokensAsString (String text) throws IOException {

		return tokenizeWithSpaces(text);
	}

	/**
	 * returns the string tokenized with space (be aware that tokens are usually lowercased!)
	 * Note also that some token may contain spaces converted to ‗ (unicode DOUBLE LOW LINE)
	 * "&lt;p color=red&gt;table&lt;/p&gt;" will be tokenized as "&lt;p‗color=red&gt; table &lt;/p&gt;"
	 * @param text input text 
	 * @return the text tokenized
	 * @throws IOException
	 */
	public String tokenizeWithSpaces (String text) throws IOException {
		return tokenizeWithSpaces(text, false);
	}

	/**
	 * returns the string tokenized with space (be aware that tokens are usually lowercased!)
	 * Note also that some token may contain spaces converted to ‗ (unicode DOUBLE LOW LINE)
	 * "<p color=red>table</p>" will be tokenized as "<p‗color=red> table </p>"
	 * @param text input text 
	 * @param withAttribute a boolean (usually false) if true adds info about attributes of token (ALPHANUM, PUNCTUATION...)
	 * eg. from the string ''Tony's "local"'' returns ''tony|<ALPHANUM> 's|<APOSTROPHE> \"|<PUNCT> local|<ALPHANUM> \"|<PUNCT>''
	 * @return the text tokenized
	 * @throws IOException
	 */
	public String tokenizeWithSpaces (String text, boolean withAttribute) throws IOException {
		
		String[] ts = tokensAsArray(text, withAttribute);
		boolean containsUnderscore = false;
		String s = "";
		for (int i=0; i<ts.length;i++) {
			StringBuffer token = new StringBuffer();
			int j = 0;
			boolean specialCaseOfParentheses = (ts[i].endsWith(")") || ts[i].startsWith("<"));
			boolean containSpaces=false;
			for (char c:ts[i].toCharArray()) {
				if (c=='\u00AD') { 
					// skip
				} else if (c=='\u0000') { // This char could create problem in C
					token.append('#');j++;
				} else if (c==' ') { // we have a space in a token: "(3, 4)" or "and / or" or "12 123 234"
					if (j >0) {
						token.append('\u2017'); j++; // ‗ (unicode DOUBLE LOW LINE)
						containSpaces=true;
					}
				} else if (c == ';' && specialCaseOfParentheses && j>0 && j<ts[i].length()-1) {
					token.append(','); j++;
				} else {
					token.append(c);j++;
					if (c == '_') 
						containsUnderscore=true;
				}
			}
			if (containSpaces && token.toString().contains("\u2017/\u2017")) {
				s += (i==0?"":" ")+token.toString().replaceAll("\u2017/\u2017","/");
			} else {
				s += (i==0?"":" ")+token;
			}
		}
		s = s.replaceAll("[ ]*\\.[ ]$", "");
		
		if (containsUnderscore) 
			s = s.replaceAll("_ TODEL _", "_TODEL_");

		s = s.replaceAll("[ ][ ]+", " ");
		if (specialMsufBehaviour!=null) {
			if (specialMsufBehaviour.equals("joinMsuf")) {
				s=s.replaceAll(" − ", " −");
			} else if (specialMsufBehaviour.equals("hideMsuf")) {
				s=s.replaceAll(" − ", " ");
			} else if (specialMsufBehaviour.equals("prefixMsuf")) {
				s=s.replaceAll(" − ", "− ");
			} else if (specialMsufBehaviour.equals("suffixMsuf")) {
				s=s.replaceAll(" − ", " −");
			} else if (specialMsufBehaviour.equals("keepMsuf")) {
//				s=s.replaceAll(" − ", " −");
			}
		} else {
			if (this.languageIdentification.startsWith("ko")) {
//				s=s.replaceAll(" − ", " −");
			}
		}
		return s;
	}

	public String[] tokenize(String[] texts) throws IOException {
		String[] res = new String[texts.length];
		for (int i=0; i < texts.length; i++) {
			res[i]=this.tokenizeWithSpaces(texts[i]);

		}
		return res;
	}

	public String[] tokensAsArray (String text) throws IOException {
		return tokensAsArray(text, false);
	}

	/**
	 * Tokenizes a sentences and returns an array of tokens
	 * @param text input sentence (do not use it with full text)
	 * @param withAttribute if true will return the "attributes" together with the tokens
	 * @return an array of tokens (returns an empty token if the input string is empty)
	 * @throws IOException
	 */
	public String[] tokensAsArray (String text, boolean withAttribute) throws IOException {
		if (text==null || text.isEmpty()) 
			return emptyToken;
		
		TokenStream stream = this.analyzer.tokenStream("contents", new StringReader(text));
		try {
  			CharTermAttribute termAtt = (CharTermAttribute) stream.getAttribute(CharTermAttribute.class);
  			OffsetAttribute offsetAtt = (OffsetAttribute) stream.getAttribute(OffsetAttribute.class);
  			TypeAttribute typeAttr = null;
  			PartOfSpeechAttribute posAttr = null;
  		
  			boolean all = (this.languageIdentification.endsWith("A") );
  			if (all) 
				withAttribute = true;
  		
  			if ((languageIdentification.equals("ko") || withAttribute) && stream.hasAttribute(TypeAttribute.class)) {
  				typeAttr = (TypeAttribute) stream.getAttribute(TypeAttribute.class);
  			} 
  			if (languageIdentification.equals("ja") && withAttribute) {
  				posAttr = (PartOfSpeechAttribute) stream.getAttribute(PartOfSpeechAttribute.class);
  			}
  
  		  	String previous="";
  			List<String> tokenList = new ArrayList<String>(); // The list of words (returned as String[])

  			stream.reset();
  			while (stream.incrementToken()) {
  				String currentTerm=termAtt.toString();
  				int currentLength=termAtt.length();
  				int currentStartOffset=offsetAtt.startOffset();
  				int currentEndOffset=offsetAtt.endOffset();
  			
	  			// Here the set of conditions tries not to output overlapping tokens (most of the time
	  			// this could simply be: tokenList.add(currentTerm);
	  			if (all) {
					tokenList.add(currentTerm+"("+currentStartOffset+"-"+currentEndOffset+
						"="+currentLength+")|"+typeAttr.type());
	  			} 
				else { 
					// here try to play with offsets in order to display only tokens which do not overlap 
	  				if (! previous.equals("")) tokenList.add(previous);
	  					previous = currentTerm;
  					
	  				if (withAttribute) { 
						// outputs also the POS attribute attached to the token	
	  					String pos=(typeAttr!=null?typeAttr.type():"[u]");
	  					if (posAttr!=null) {
	  						try {
								pos = "type=" + typeAttr.type() + "|" + 
					org.apache.lucene.analysis.ja.util.ToStringUtil.getPOSTranslation(posAttr.getPartOfSpeech());
	  						} 
							catch (Exception e) {
								pos=posAttr.getPartOfSpeech();
							}
	  					}	 
	  					previous+="|"+pos;
					}
	  			}
  			}
  			stream.end();
  		  	if (! previous.equals("")) 
				tokenList.add(previous);
      
	  		return (String[]) tokenList.toArray(new String[tokenList.size()]); 
		} 
		finally {
		  	stream.close();
		}
	}

	public Integer[] tokensAsOffsets (String text) throws IOException {

		TokenStream stream = this.analyzer.tokenStream("contents", new StringReader(text));
		CharTermAttribute termAtt = (CharTermAttribute) stream.getAttribute(CharTermAttribute.class);
		OffsetAttribute offsetAtt = (OffsetAttribute) stream.getAttribute(OffsetAttribute.class);

		int currOffset=-1;
		String previous="";int previousOffset=0;
		List<Integer> al = new ArrayList<Integer>();


		while (stream.incrementToken()) {
			if (offsetAtt.startOffset() == currOffset) {
				previous = termAtt.toString(); previousOffset = currOffset;
			} else if (offsetAtt.startOffset() > currOffset) {
				if (! previous.equals("")) al.add(previousOffset);
				previous = termAtt.toString();
				currOffset = offsetAtt.startOffset();previousOffset = currOffset;
			}
		}
		if (! previous.equals("")) 
			al.add(previousOffset);

		return al.toArray(new Integer[al.size()]); 
	}

	/**
	 * Tokenizes a sentences and returns a ArrayList of String tokens
	 * @param text input sentence (do not use it with full text)
	 * @param withAttribute if true will return the "attributes" together with the tokens
	 * @return a List of tokens (returns an empty List if the input string is empty)
	 * @throws IOException
	 */
	public List<String> tokensAsList(String text, boolean withAttribute) throws IOException {
		if (text==null || text.isEmpty()) 
			return new ArrayList<String>();
		
		TokenStream stream = this.analyzer.tokenStream("contents", new StringReader(text));
		
		try {
  			CharTermAttribute termAtt = (CharTermAttribute) stream.getAttribute(CharTermAttribute.class);
  			OffsetAttribute offsetAtt = (OffsetAttribute) stream.getAttribute(OffsetAttribute.class);
  			TypeAttribute typeAttr = null;
  			PartOfSpeechAttribute posAttr = null;
  		
  			boolean all = (this.languageIdentification.endsWith("A") );
  			if (all) 
				withAttribute=true;
  		
  			if ((languageIdentification.equals("ko") || withAttribute) && stream.hasAttribute(TypeAttribute.class)) {
  				typeAttr = (TypeAttribute) stream.getAttribute(TypeAttribute.class);
  			} 
  			if (languageIdentification.equals("ja") && withAttribute) {
  				posAttr = (PartOfSpeechAttribute) stream.getAttribute(PartOfSpeechAttribute.class);
  			}
  
  		  	String previous="";
  			List<String> tokenList = new ArrayList<String>(); // The list of tokens

  			stream.reset();
  			while (stream.incrementToken()) {
  				String currentTerm=termAtt.toString();
  				int currentLength=termAtt.length();
  				int currentStartOffset=offsetAtt.startOffset();
  				int currentEndOffset=offsetAtt.endOffset();
  			
	  			// Here the set of conditions tries not to output overlapping tokens (most of the time
	  			// this could simply be: tokenList.add(currentTerm);
	  			if (all) {
					tokenList.add(currentTerm+"("+currentStartOffset+"-"+currentEndOffset+
						"="+currentLength+")|"+typeAttr.type());
	  			} 
				else { 
					// here try to play with offsets in order to display only tokens which do not overlap 
	  				if (! previous.equals("")) tokenList.add(previous);
	  					previous = currentTerm;
  					
	  				if (withAttribute) { 
						// outputs also the POS attribute attached to the token	
	  					String pos=(typeAttr!=null?typeAttr.type():"[u]");
	  					if (posAttr!=null) {
	  						try {
								pos = "type=" + typeAttr.type() + "|" + 
					org.apache.lucene.analysis.ja.util.ToStringUtil.getPOSTranslation(posAttr.getPartOfSpeech());
	  						} 
							catch (Exception e) {
								pos=posAttr.getPartOfSpeech();
							}
	  					}	 
	  					previous+="|"+pos;
					}
	  			}
  			}
  			stream.end();
  		  	if (! previous.equals("")) 
				tokenList.add(previous);
			return tokenList;
		} 
		finally {
		  	stream.close();
		}
	}

	public List<String> tokensAsList(String text) throws IOException {
		return tokensAsList(text, false);
	}
}


