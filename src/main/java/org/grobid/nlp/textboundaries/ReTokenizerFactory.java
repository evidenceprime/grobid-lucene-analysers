package org.grobid.nlp.textboundaries;
import java.util.HashMap;
import org.grobid.nlp.Language;

/**
 * Creates a ReTokenizer object per language and keeps it for future use. 
 * 
 * @author DevBpo
 *
 */
public class ReTokenizerFactory {

	private static HashMap <String,ReTokenizer> myPool = new HashMap <String,ReTokenizer>();

	public static ReTokenizer create(String lang) throws Exception {
		if (myPool.containsKey(lang)) {
			return myPool.get(lang);
		} else {
			ReTokenizer o = new ReTokenizer(lang);
			myPool.put(lang, o);
			return o;
		}
	}

	public static ReTokenizer create(Language language) throws Exception {
		return create(language.getLanguageIdentification());
	}
}