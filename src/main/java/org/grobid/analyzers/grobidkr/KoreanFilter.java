package org.grobid.analyzers.grobidkr;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.grobid.analyzers.grobidkr.morph.AnalysisOutput;
import org.grobid.analyzers.grobidkr.morph.CompoundEntry;
import org.grobid.analyzers.grobidkr.morph.MorphAnalyzer;
import org.grobid.analyzers.grobidkr.morph.MorphException;
import org.grobid.analyzers.grobidkr.morph.PatternConstants;
import org.grobid.analyzers.grobidkr.morph.WordSpaceAnalyzer;
import org.grobid.analyzers.grobidkr.utils.DictionaryUtil;

public final class KoreanFilter extends TokenFilter {

	private static final boolean DECOMPOUND = false;
	
	private final LinkedList<Token> koreanQueue;
	
	private final LinkedList<Token> cjQueue;
	
	private final MorphAnalyzer morph;
	
	final WordSpaceAnalyzer wsAnal;
	
	private boolean bigrammable = true;
	
	private boolean hasOrigin = true;

	public boolean returnOnlyOne = true;
	
	private static final String APOSTROPHE_TYPE = KoreanTokenizerImpl.TOKEN_TYPES[KoreanTokenizerImpl.APOSTROPHE];
	private static final String ACRONYM_TYPE = KoreanTokenizerImpl.TOKEN_TYPES[KoreanTokenizerImpl.ACRONYM];

	private final CharTermAttribute termAttr;
	private final TypeAttribute typeAttr;
	private final PositionIncrementAttribute posAttr;
	private final OffsetAttribute offsetAttr;
	
	public KoreanFilter(TokenStream input) {
		super(input);
		
		termAttr = (CharTermAttribute) addAttribute(CharTermAttribute.class);
    	typeAttr=(TypeAttribute) addAttribute(TypeAttribute.class);
    	posAttr = (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class) ;
    	offsetAttr = (OffsetAttribute) addAttribute(OffsetAttribute.class) ;
		
		koreanQueue =  new LinkedList();
		cjQueue =  new LinkedList();
		morph = new MorphAnalyzer();
		wsAnal = new WordSpaceAnalyzer();
	}

	/**
	 * 
	 * @param input	input token stream
	 * @param bigram	Whether the bigram index term return or not.
	 */
	public KoreanFilter(TokenStream input, boolean bigram) {
		this(input);	
		bigrammable = bigram;
	}
	
	public KoreanFilter(TokenStream input, boolean bigram, boolean has) {
		this(input, bigram);
		hasOrigin = has;
	}
	
	/**
	 * 한글을 분석한다.
	 * @param token
	 * @param skipinc
	 * @return
	 * @throws MorphException
	 */
	private Token analysisKorean(Token token, int skipinc) throws MorphException {

		String input = token.toString();
		List<AnalysisOutput> outputs = morph.analyze(input);
		//		if(outputs.size()==0) return null;
		if(outputs.size()==0) return token;

		HashMap<String,Integer> map = new HashMap<String,Integer>();
		if(hasOrigin) map.put(input, new Integer(1));

		// BP look first for stem+josa in any of the outputs
		for (int k=0; k<outputs.size(); k++) {
			AnalysisOutput aElement=outputs.get(k);		
			if (! token.toString().equals(aElement.getStem())) {
				if (token.toString().equals(aElement.getStem()+aElement.getJosa())) {
					Token t = new Token(aElement.getStem(),
							token.startOffset(),token.startOffset()+aElement.getStem().length()
							,KoreanTokenizer.TOKEN_TYPES[KoreanTokenizer.KORNOUN]);
					koreanQueue.add(t);
					Token t2 = new Token("−"+aElement.getJosa(),
							token.startOffset()+aElement.getStem().length()+1,token.endOffset()
							,KoreanTokenizer.TOKEN_TYPES[KoreanTokenizer.POSTJOSA]);
					koreanQueue.add(t2);
					return koreanQueue.removeFirst();
				} else if (token.toString().equals(aElement.getStem()+aElement.getVsfx()+aElement.getEomi())) {
					Token t = new Token(aElement.getStem(),
							token.startOffset(),token.startOffset()+aElement.getStem().length()
							,KoreanTokenizer.TOKEN_TYPES[KoreanTokenizer.KORNOUN]);
					koreanQueue.add(t);
					Token t2 = new Token("−"+aElement.getVsfx()+aElement.getEomi(),
							token.startOffset()+aElement.getStem().length()+1,token.endOffset()
							,KoreanTokenizer.TOKEN_TYPES[KoreanTokenizer.POSTEOMI]);
					koreanQueue.add(t2);
					return koreanQueue.removeFirst();
				}			
			}
		}
		if (returnOnlyOne) {
			return token;
		} else {
		
		
		if(outputs.get(0).getScore()==AnalysisOutput.SCORE_CORRECT) {
			extractKeyword(outputs, map);
		} else {
			try{
				List<AnalysisOutput> list = wsAnal.analyze(input);

				List<AnalysisOutput> results = new ArrayList<AnalysisOutput>();			
				if(list.size()>1) {
					for(AnalysisOutput o : list) {
						if(hasOrigin) map.put(o.getSource(), new Integer(1));				
						results.addAll(morph.analyze(o.getSource()));
					}				
				} else {
					results.addAll(list);
				}

				extractKeyword(results, map);
			}catch(Exception e) {
				extractKeyword(outputs, map);
			}
		}

		Iterator<String> iter = map.keySet().iterator();
		int i=0;
		while(iter.hasNext()) {

			String text = iter.next();

			//		if(text.length()<=1) continue;

			int index = input.indexOf(text);
			Token t = new Token(text,
					token.startOffset()+(index!=-1?index:0),
					index!=-1?token.startOffset()+index+text.length():token.endOffset(),
							KoreanTokenizer.TOKEN_TYPES[KoreanTokenizer.KOROREAN]);
			if (i==0) 
				t.setPositionIncrement(token.getPositionIncrement()+skipinc);
			else 
				t.setPositionIncrement(0);

			koreanQueue.add(t);
			i++;
		}
	}
		if (koreanQueue.size()==0) 
			return null;

		return koreanQueue.removeFirst();
	}

	private void extractKeyword(List<AnalysisOutput> outputs, HashMap<String,Integer> map) throws MorphException {
		for(AnalysisOutput output : outputs) {			
			if(output.getPos()!=PatternConstants.POS_VERB) {
				map.put(output.getStem(), new Integer(1));	
			}				

			if(DECOMPOUND && output.getScore()>=AnalysisOutput.SCORE_COMPOUNDS) {
				List<CompoundEntry> cnouns = output.getCNounList();
				for(int jj=0;jj<cnouns.size();jj++) {
					CompoundEntry cnoun = cnouns.get(jj);
					if(cnoun.getWord().length()>1) map.put(cnoun.getWord(),  new Integer(0));
					if(jj==0 && cnoun.getWord().length()==1)
						map.put(cnoun.getWord()+cnouns.get(jj+1).getWord(),  new Integer(0));
					else if(jj>1 && cnoun.getWord().length()==1)
						map.put(cnouns.get(jj).getWord()+cnoun.getWord(),  new Integer(0));
				}
			} else if(bigrammable){
				addBiagramToMap(output.getStem(),map);
			}
		}
	}
	
	private void addBiagramToMap(String input, HashMap<String,Integer> map) {
		int offset = 0;
		int strlen = input.length();
		while(offset<strlen-1) {
			if(isAlphaNumChar(input.charAt(offset))) {
				String text = findAlphaNumeric(input.substring(offset));
				map.put(text,  new Integer(0));
				offset += text.length();
			} else {
				String text = input.substring(offset,
						offset+2>strlen?strlen:offset+2);
				map.put(text,  new Integer(0));
				offset++;
			}				
		}
	}
	
	private String findAlphaNumeric(String text) {
		int pos = 0;
		for(int i=0;i<text.length();i++) {
			if(!isAlphaNumChar(text.charAt(i))) break;
			pos++;
		}				
		return text.substring(0,pos);
	}
	
	private Token analysisCJ(Token token, int skipinc) throws MorphException {
		String input = token.toString();
		
		Token t = new Token(input,0,token.endOffset(),KoreanTokenizer.TOKEN_TYPES[KoreanTokenizer.CJ]);
		t.setPositionIncrement(token.getPositionIncrement()+skipinc);
		cjQueue.add(t);	
		return cjQueue.removeFirst();
	}
	
	private Token analysisETC(Token t) throws MorphException {
	    char[] buffer = t.buffer();
	    final int bufferLength = t.length();
	    final String type = t.type();

	    if (type == APOSTROPHE_TYPE &&
	    	bufferLength >= 2 &&
	        buffer[bufferLength-2] == '\'' &&
	        (buffer[bufferLength-1] == 's' || buffer[bufferLength-1] == 'S')) {
    		// remove 's
	      	// Strip last 2 characters off
	      	t.setLength(bufferLength - 2);
	    } else if (type == ACRONYM_TYPE) {		  
	    	// remove dots
		    int upto = 0;
		    for(int i=0;i<bufferLength;i++) {
		      	char c = buffer[i];
		      	if (c != '.')
		        	buffer[upto++] = c;
		    }
		    t.setLength(upto);
	    }

	    return t;
	}
	
	private boolean isAlphaNumChar(int c) {
		if ((c>=48&&c<=57)||(c>=65&&c<=122)) 
			return true;		
		
		return false;
	}
	
	public void setHasOrigin(boolean has) {
		hasOrigin = has;
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (koreanQueue.size()>0) {
			Token t = (Token)koreanQueue.removeFirst();
			termAttr.setEmpty(); termAttr.append(t.toString());
			typeAttr.setType(t.type());
			posAttr.setPositionIncrement(t.getPositionIncrement());
			offsetAttr.setOffset(t.startOffset(),t.endOffset());
			return true;
		}
		else if (cjQueue.size()>0) {
			Token t = (Token)cjQueue.removeFirst();
			termAttr.setEmpty(); termAttr.append(t.toString());
			typeAttr.setType(t.type());
			posAttr.setPositionIncrement(t.getPositionIncrement());
			offsetAttr.setOffset(t.startOffset(),t.endOffset());
			return true;
		}
		
	  	int skippedPositions = 0;
		try {
		    while(input.incrementToken()) {
		    	Token t = new Token();
		    	t.reinit(termAttr.toString(), offsetAttr.startOffset(), offsetAttr.endOffset(), typeAttr.type());
		    	if (typeAttr.type().equals(KoreanTokenizer.TOKEN_TYPES[KoreanTokenizer.KOREAN])) {		    		
		    		t = analysisKorean(t, skippedPositions);
		    	} else {
		    		t = analysisETC(t);
		    	}

		    	if(t==null) {
					skippedPositions++;			    		
		    		continue;
		    	}
		    	termAttr.setEmpty(); termAttr.append(t.toString());
				typeAttr.setType(t.type());
				posAttr.setPositionIncrement(t.getPositionIncrement());
				offsetAttr.setOffset(t.startOffset(),t.endOffset());
				return true;
			}
		} catch (MorphException e) {
			System.err.println(e.getMessage());
			throw new IOException(e.getMessage());
		}

		return false;
	}

  	@Override
  	public void reset() throws IOException {
	    super.reset();
	    cjQueue.clear();
	    koreanQueue.clear();
  	}
}
