package org.grobid.nlp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Set of static methods commonly used in NLP tools (join, tail etc.)
 * @author DevBpo
 *
 */
public class NLPUtils {
	
	// set of "join" commands (like in perl) usefull to create a String out of an array
	public static String join(char separator, String[] s) {
		return join(""+separator,s);
	}
	
	/**
	 * Unescape the special XMl chars in a string
	 * @param s input String e.g. "e &lt; mc 2"
	 * @return escaped string (e.g. "e < mc 2")
	 */
	public static String unEscapeXml(String s) {
		return s.replaceAll("&quote;", "\"")
			.replaceAll("&lt;","<")
			.replaceAll("&gt;",">");
	}
	
	/**
	 * Escape the special XMl chars in a string
	 * @param s input String e.g. "e > mc 2"
	 * @return escaped string (e.g. "e &gt; mc 2")
	 */
	public static String escapeXml(String s) {
		StringBuffer res=new StringBuffer();
		for (char c: s.toCharArray()) {
			switch (c) {
			case '"':res.append("&quot;"); break;
			case '<':res.append("&lt;");break;
			case '>':res.append("&gt;");break;
			default: res.append(c);
			}
		}
		return res.toString();
	}
	
	public static String join(String separator, String[] s) {
		StringBuffer res = new StringBuffer(); 
		boolean first=true;
		for (String e: s) {
			if (! first) {res.append(separator);}
			else first=false;
			res.append(e);
		}
		return res.toString();
	}
	public static String join(String separator, int[] s) {
		StringBuffer res = new StringBuffer();
		boolean first=true;
		for (int e: s) {
			if (! first) {
				res.append(separator);
			} else {
				first =false;
			}
			res.append(e);
		}
		return res.toString();
	}
	
	public static String join(String separator, char[] s) {
		StringBuffer res = new StringBuffer();
		for (char e: s) {
			if (res.length()!=0) {res.append(separator);}
			res.append(e);
		}
		return res.toString();
	}
	public static String join(char separator, String[] s, int start, int end) {
		return join(""+separator,s,start,end);
	}
	public static String join(String separator, String[] s, int start, int end) {
		StringBuffer res = new StringBuffer();
		for (int i=start; i<=end; i++) {
			if (res.length()!=0) {res.append(separator);}
			res.append(s[i]);
		}
		return res.toString();
	}
	
	public static String join(char separator, int[] is) {
		StringBuffer res = new StringBuffer();
		for (int e: is) {
			if (res.length()!=0) {res.append(separator);}
			res.append(e);
		}
		return res.toString();
	}
	public static String convertKanji2Latin (String s) {
		StringBuffer res= new StringBuffer();
		for (char c: s.toCharArray()) {
			res.append(convertKanji2Latin(c));
		}
		return res.toString();
	}
	public static char convertKanji2Latin (char c) {
		char newChar=c;

		if (c>=0xFF01 && c<=0xFF5E && c!= 0xFF0F) { 
			// This is a Kanji-latin char, let's convert all the string
			newChar=(char)(c-0xFF01+0x0021);
		} else {
			switch (c) {
			case '，': // Japanese comma
			case '、': // Chinese comma
				newChar=','; break;
			case '。': // Japanese/Chinese dot
				newChar='.'; break;	    	 
			case '>': // this is not the usual bracket symbol
			case '〉':
				newChar='>'; break;
			case '〈': 
			case '<': // this is not the usual bracket symbol
				newChar='<'; break;

			case  '【': // Japanese square or curly brackets
			case '〔':
			case '［':
			case '｛':
			case '〘':
			case '〚':
				newChar='['; break;
			case  '】':
			case  '〕':
			case '］':
			case '｝':
			case '〙':
			case '〛':
				newChar=']'; break;
			case '（':newChar='(';  break;
			case '）':newChar=')';  break;

			case  '「': // all Japanese chars for quotes
			case  '」':
			case '“':
			case '”':
			case '″': // this is not a usual double-quote!
			case '《':
			case '》':
			case '『': // double quotation marks
			case '』':
				newChar='"'; break;
			case '−': // these are not usual hyphens
			case '‐':
			case '-':
			case '─':
			case '〜':
			case '～':
				newChar='-'; break;
			case '：':
				newChar=':'; break;
			case '/':
				newChar='/'; break;
			case '！':newChar='!'; break;
			case '？':newChar='?'; break;
			case '…':
			case '‥':
			case '・': // non breakable space in names e.g. "M・K Sale CO., LTD."
			case '　': // <= this is not a usual space
				newChar=' '; break;				  
			}
		}
		return newChar;
	}

	/***
	 * Implementation of a unix-like 'tail' command
	 *
	 * @param aFileName a file name String
	 * @return An array of two strings is returned. At index 0 the String
	 *         representation of at most 10 last lines is located.
	 *         At index 1 there is an informational string about how large a
	 *         segment of the file is being returned.
	 *         Null is returned if errors occur (file not found or io exception)
	 */
	public static String[] tail(String aFileName) {
		return tail(aFileName, 10);
	}

	/***
	 * Implementation of a unix-like 'tail -n' command
	 *
	 * @param aFileName a file name String
	 * @param n int number of lines to be returned
	 * @return An array of two strings is returned. At index 0 the String
	 *         representation of at most n last lines is located.
	 *         At index 1 there is an informational string about how large a
	 *         segment of the file is being returned.
	 *         Null is returned if errors occur (file not found or io exception)
	 */
	public static String[] tail(String aFileName, int n) {
		try {
			return tail(new RandomAccessFile(new File(aFileName),"r"),n);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/***
	 * Implementation of a unix-like 'tail -n' command
	 *
	 * @param raf a RandomAccessFile to tail
	 * @param n int number of lines to be returned
	 * @return An array of two strings is returned. At index 0 the String
	 *         representation of at most n last lines is located.
	 *         At index 1 there is an informational string about how large a
	 *         segment of the file is being returned.
	 *         Null is returned if errors occur (file not found or io exception)
	 */
	public static String[] tail(RandomAccessFile raf, int n) {
		int BUFFERSIZE = 1024;
		long pos;
		long endPos;
		long lastPos;
		int numOfLines = 0;
		String info=null;
		byte[] buffer = new byte[BUFFERSIZE];
		StringBuffer sb = new StringBuffer();
		try {
			endPos = raf.length();
			lastPos = endPos;
//			System.err.println("buf:"+endPos+" , "+lastPos);
			// Check for non-empty file
			// Check for newline at EOF
			if (endPos > 0) {
				byte[] oneByte = new byte[1];
				raf.seek(endPos - 1);
				raf.read(oneByte);
				if ((char) oneByte[0] != '\n') {
					numOfLines++;
				}
			}

			do {
				// seek back BUFFERSIZE bytes
				// if length of the file if less then BUFFERSIZE start from BOF
				pos = 0;
				if ((lastPos - BUFFERSIZE) > 0) {
					pos = lastPos - BUFFERSIZE;
				}
				raf.seek(pos);
				// If less then BUFFERSIZE avaliable read the remaining bytes
				if ((lastPos - pos) < BUFFERSIZE) {
					int remainer = (int) (lastPos - pos);
					buffer = new byte[remainer];
				}
				raf.readFully(buffer);
				// in the buffer seek back for newlines
				for (int i = buffer.length - 1; i >= 0; i--) {
					if ((char) buffer[i] == '\n') {
						numOfLines++;
						// break if we have last n lines
						if (numOfLines > n) {
							pos += (i + 1);
							break;
						}
					}
				}
				// reset last postion
				lastPos = pos;
			} while ((numOfLines <= n) && (pos != 0));

			// print last n line starting from last postion
			for (pos = lastPos; pos < endPos; pos += buffer.length) {
				raf.seek(pos);
				if ((endPos - pos) < BUFFERSIZE) {
					int remainer = (int) (endPos - pos);
					buffer = new byte[remainer];
				}
				raf.readFully(buffer);
				sb.append(new String(buffer));
			}

			info = buildDisplayingHeader(sb.length(), raf.length());
		} catch (FileNotFoundException e) {
			sb = null;
		} catch (IOException e) {
			e.printStackTrace();
			sb = null;
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(sb==null){
			return null;
		}
		String[] tmp = {sb.toString(),info};
		return tmp;
	}
	public static String buildDisplayingHeader(int len, long logsize)
	{
		double percent = 0.0;
		if (logsize != 0) {
			percent = ((double) len/logsize) * 100;
		}
		return "Displaying:  " + percent+
		"% of " + logsize;
	}



	public static void main (String[] args) throws Exception {
		System.out.println("tail: "+join('\n', tail("d:/sw/apache-tomcat-6.0.24/logs/catalina.out")));
	}
	/** simply returns true if the string is in the array
	 * @param s string to look for
	 * @param a array
	 * @return true/false
	 */
	public static boolean searchStringInArray(String s, String[] a) {
		for (String l: a) {
			if (s.equals(l)) {
				return true;
			}
		}
		return false;
	}
	/** simply returns true if the string is in the array but not first or last position
	 * @param s string to look for
	 * @param a array
	 * @return true/false
	 */
	public static boolean searchStringInArrayExceptBoundaries(String s, String[] a) {
		for (int i=1; i<a.length-1;i++) {
			if (s.equals(a[i])) {
				return true;
			}
		}
		return false;
	}
	

	/**
	 * close the unclosed tags in xml, xhtml
	 * @param retS
	 * @return a string where every open tag is closed
	 */
	public static String closeUnclosedXmlTags(String retS) {
		final String header="<html lang=\"en\"><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/></p>";
		if (retS.startsWith(header)) {
			retS=retS.substring(header.length()+1);
		}
		// close unclosed xml tags
		char[] txt = retS.toCharArray();
		boolean inXmlTag=false; boolean endTag=false; String tag="";
		Stack<String> tagOpened = new Stack<String>();
		
		for (int i=0; i<txt.length; i++) {
			char c=txt[i];
			if (c=='<') {
				inXmlTag=true; tag=""; endTag=false;
				if (i<txt.length-1) {
					if (txt[i+1]=='/') {i+=1; endTag=true;}
				}
			} else if (inXmlTag) {
				if (c=='>' || c==' ') {		
					if (endTag) {
						if (tagOpened.size()>0 && tagOpened.peek().equals(tag)) { // Ok, we close the latest opened tag
							tagOpened.pop();
						} else {
							
						}
					} else { // We have an open tag, let's push it
						tagOpened.push(tag);
					}
					inXmlTag=false;tag="";
				} else {
					if (c=='/') {				
						endTag=false; inXmlTag=false; // we encountered a "<br/>" tag, let's ignore it!
					} else {
						tag+=c;
					}
				}		
			}
			
		}
		for (String opened:tagOpened) {
			retS += "</"+opened+">";
		}
		return retS;
	}
	

	/**
	 * returns the percentage of uppercase letters in a string (eg. percentageUppercase("HDMIs")=>80%)
	 * @param s
	 * @return int value between 0 and 100
	 */
	public static int percentageUppercase (String s) {
		
		int countLowercase=0; int countUppercase=0;
		for (char c:s.toCharArray()) {
			if (Character.isUpperCase(c)) countUppercase++;
			else if (Character.isLowerCase(c)) countLowercase++;
		}
		if ((countUppercase + countLowercase) == 0) return 0;
		else return (int) (100*((float)countUppercase / (float)((countUppercase + countLowercase))));		
	}
	/**
	 * inverse an array
	 * @param t an array of string
	 * @return array in reverse order
	 */
	public static String[] reverse(String[] t) {
		int l=t.length;
		String[] s = new String[l];
		for (int i=0; i<l; i++) s[i]=t[l-i-1];

		return s;
	}
	public static String join(String separator, ArrayList<String> s) {
		StringBuffer res = new StringBuffer();
		for (String e: s) {
			if (res.length()!=0) {res.append(separator);}
			res.append(e);
		}
		return res.toString();
	}
	public static String joinEscape(String separator, ArrayList<String> s) {
		StringBuffer res = new StringBuffer();
		for (String e: s) {
			if (res.length()!=0) {res.append(separator);}
			res.append(escapeXml(e));
		}
		return res.toString();
	}
	public static String join(char separator, ArrayList<String> s) {
		StringBuffer res = new StringBuffer();
		for (String e: s) {
			if (res.length()!=0) {res.append(separator);}
			res.append(e);
		}
		return res.toString();
	}
	/**
	 * Return the text content of a file
	 * @param file filename (should be a utf8 encoded text file)
	 * @return all the text in one single string
	 * @throws Exception
	 */
	public static String contentOfFile(String file) throws Exception {
		return head(new FileInputStream(file), -1);
	}
	
	/**
	 * Return the head of a text file
	 * @param fileName filename (should be a utf8 encoded text file)
	 * @param n number of lines to return 
	 * @return the first N lines of the given text file in one single string
	 * @throws Exception
	 */
	
	public static String head(String fileName, int n) throws Exception {
		return head(new FileInputStream(fileName), n);
	}
	public static String head(InputStream is, int l) throws Exception {
		BufferedReader bufInfoFile =null;
		try {
			bufInfoFile = new BufferedReader(new InputStreamReader(is
				,Charset.forName("UTF-8")));
		} catch (Exception e2) {
			throw e2;
		}
    	StringBuffer bs=new StringBuffer();
    	String s;
    	int count=0;
		while ((s=bufInfoFile.readLine()) != null) {
				if (count > 0) {bs.append("\n");}
				count++;			
				if (l > -1 && count > l) {break;}
				
				bs.append(s);
		}
		bufInfoFile.close();
		return bs.toString();
		
	}
	public static String getMd5OfString(String s) {
		MessageDigest digest=null;
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes("UTF-8"),0,s.length());
		} catch (UnsupportedEncodingException e) {
			return "[ERR MD5]";
		} catch (NoSuchAlgorithmException e) {
			return "[ERR MD5]";
		}
		return ""+new BigInteger(1,digest.digest()).toString(16)+"";
	}
	
	public static String[] inclusiveSplit(String input, String re, int limit) {
	    int index = 0;
	    boolean matchLimited = limit > 0;
	    ArrayList<String> matchList = new ArrayList<String>();

	    Pattern pattern = Pattern.compile(re);
	    Matcher m = pattern.matcher(input);

	    // Add segments before each match found
	    while (m.find()) {
	        int end = m.end();
	        if (!matchLimited || matchList.size() < limit - 1) {
	            int start = m.start();
	            String match = input.subSequence(index, start).toString();
	            matchList.add(match);
	            // add match to the list
	            matchList.add(input.subSequence(start, end).toString());
	            index = end;
	        } else if (matchList.size() == limit - 1) { // last one
	            String match = input.subSequence(index, input.length())
	                    .toString();
	            matchList.add(match);
	            index = end;
	        }
	    }

	    // If no match was found, return this
	    if (index == 0)
	        return new String[] { input.toString() };

	    // Add remaining segment
	    if (!matchLimited || matchList.size() < limit)
	        matchList.add(input.subSequence(index, input.length()).toString());

	    // Construct result
	    int resultSize = matchList.size();
	    if (limit == 0)
	        while (resultSize > 0 && matchList.get(resultSize - 1).equals(""))
	            resultSize--;
	    String[] result = new String[resultSize];
	    return matchList.subList(0, resultSize).toArray(result);
	}
	public static void outputStringInUtf8TextFile(String fileName, String text) throws IOException {
		BufferedWriter out = 
			new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName),"UTF8"));
		out.write(text);
		out.close();
	}

	public static String join(String separator, Object[] o) {
		if (o==null) return "";
		StringBuffer res = new StringBuffer(); 
		boolean first=true;
		for (Object e: o) {
			if (! first) {res.append(separator);}
			else first=false;
			res.append(e.toString());
		}
		return res.toString();
	}

	public static String join(String separator, Collection<Object> o) {
		if (o==null) return "";
		StringBuffer res = new StringBuffer(); 
		boolean first=true;
		for (Object e: o) {
			if (! first) {res.append(separator);}
			else first=false;
			res.append(e.toString());
		}
		return res.toString();
	}
}
