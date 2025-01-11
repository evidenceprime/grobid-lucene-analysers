package org.grobid.nlp;

/**
 * A class to handle a language identification
 * 
 * @author DevBpo
 *
 */
public class Language implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3592384958120874106L;
	private String languageIdentification;
	private String lg;

	private String dir="ltr"; 
	private String textAlign="left"; 
	private boolean containsCases=true;
	private boolean containsSpaces=true;
	private boolean standardCases=true;
	private boolean reordered=false;
	private boolean spaceNeededBeforeColon = false; // a space is required before colon in some languages (eg: fr)

	public enum LanguageCode {AR, DE, ES, EN, FR, HE, JA, KO, NL, PT, RU, ZH, IT, SV, XX};
	public LanguageCode languageCode;
	

	/**
	 * create the new instance
	 * @param languageIdentification usually a two-letter iso-636 code (but it could also be something else)
	 */
	public Language (String languageIdentification) {
		this.languageIdentification=languageIdentification;
		lg = languageIdentification.substring(0,2).toLowerCase();
		
		standardCases=true;
		
		if (lg.startsWith("ar")) {
			dir= "rtl";
			textAlign="right"; 
			containsCases=false; //standardCases=false;
			languageCode = LanguageCode.AR;		
		} else if (lg.startsWith("he")) {	
			dir= "rtl";
			textAlign="right"; 
			containsCases=false;standardCases=false;
			languageCode = LanguageCode.HE;	
		} else if (lg.equals("ja")) {
			containsCases=false;
			reordered=true;
			containsSpaces = false;standardCases=false;
			languageCode = LanguageCode.JA;	
		} else if (lg.equals("jp")) { // jp=Japanese without reordering
			containsCases=false;
			reordered=false;
			containsSpaces = false;standardCases=false;
			languageCode = LanguageCode.JA;	
		} else if (lg.startsWith("j")) {
			containsCases=false;
			reordered=true;
			containsSpaces = false;standardCases=false;
			languageCode = LanguageCode.JA;	
		} else if (lg.equals("ko")) {
			containsCases=false;
			languageCode = LanguageCode.KO;	
		} else if (lg.equals("zh")) {
			containsCases=false; standardCases=false;
			containsSpaces = false;
			languageCode = LanguageCode.ZH;
		} else if (lg.startsWith("en"))	{
			languageCode = LanguageCode.EN;
		} else if (lg.startsWith("es"))	{
			languageCode = LanguageCode.ES;
		} else if (lg.startsWith("pt"))	{
			languageCode = LanguageCode.PT;
		} else if (lg.startsWith("fr"))	{
			this.spaceNeededBeforeColon=true;
			languageCode = LanguageCode.FR;
		} else if (lg.startsWith("it"))	{
			languageCode = LanguageCode.IT;
		} else if (lg.startsWith("nl"))	{
			languageCode = LanguageCode.NL;
		} else if (lg.startsWith("sv"))	{
			languageCode = LanguageCode.SV;
		} else if (lg.startsWith("de")) {
			standardCases=false;
			languageCode = LanguageCode.DE;
		} else if (lg.startsWith("ru")) {
			languageCode = LanguageCode.RU;
		} else if (lg.startsWith("xx")) {
			languageCode = LanguageCode.XX;
		} else {
			System.err.println("What language is this:?"+lg);
			languageCode = LanguageCode.XX;
		}
	}
	
	/** returns the writing direction of the language (ar, he => right) others left
	 * @return "right" or "left"
	 */
	public String getTextAlign() {
		return textAlign;
	}

	/** returns the ccs entry contaning writing direction of the language (ar, he => right) others left
	 * @return "text-align: right" or "text-align: left"
	 */
	public String getTextAlignStyle() {
		return "text-align: "+textAlign+";";
	}

	/**
	 * @return the full designation of the language code (eg pt_s)
	 */
	public String getLanguageIdentification() {
		return languageIdentification;
	}

	/**
	 * @return the two-letter code
	 */
	public String getLg() {
		return lg;
	}

	/** returns the writing direction of the language (ar, he => rtl) others ltr
	 * @return "rtl" or "ltr"
	 */
	public String getDir() {
		return dir;
	}
	
	/** returns the html attribute to add (or empty) to reproduce the writing direction of the language 
	 * (ar, he => ' dir="rtl"') others empty
	 * @return " dir=\"rtl\"" or empty
	 */
	public String getDirAttribute() {
		if (dir.equals("ltr")) return "";
		return " dir=\"rtl\"";
	}
	/** returns the html dir attribute and the style addtibute to add (or empty) 
	 *  
	 * eg. (ar, he => ' dir="rtl" style=\"text-align: right\"') others empty
	 * @return " dir=... style=..." or empty
	 */
	public String getDirAndStyleAttribute() {
		if (dir.equals("ltr")) return "";
		return " dir=\"rtl\" style=\""+getTextAlignStyle()+"\"";
	}

	public String toString() {
		return languageIdentification;
		
	}
	/** true if language has cases
	 * @return true for French, English etc., false for Japanese, Korean etc.
	 */
	public boolean containsCases() {
		return this.containsCases;
	}
	
	/** true if language has spaces
	 * @return false for Japanese, chinese.
	 */
	public boolean containsSpaces() {
		return this.containsSpaces;
	}
	
	/** true if language has standard cases (not German)
	 * @return false for German, Japanese, chinese.
	 */
	public boolean standardCases() {
		return this.standardCases;
	}
	/** true if language is reordered (currently only Japanese)
	 * @return false except for Japanese.
	 */
	public boolean reordered() {
		return this.reordered;
	}
	public boolean spaceNeededBeforeColon() {
		return spaceNeededBeforeColon;
	}
}
