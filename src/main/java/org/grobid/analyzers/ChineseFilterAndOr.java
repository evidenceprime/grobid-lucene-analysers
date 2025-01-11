package org.grobid.analyzers;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

public final class ChineseFilterAndOr extends TokenFilter {
	private final LinkedList<GrobidToken> tokenQueue = new LinkedList<GrobidToken>();
	
	private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);;
	private final TypeAttribute typeAttr = addAttribute(TypeAttribute.class);
	private final PositionIncrementAttribute positionAttr = addAttribute(PositionIncrementAttribute.class);
	private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);;
	private final PartOfSpeechAttribute POSAttr = addAttribute(PartOfSpeechAttribute.class);
	
	private boolean inReference=false;
	private boolean inAndOr=false;
	
	public ChineseFilterAndOr (TokenStream input) {
		super(input);
	}
	
	public boolean incrementToken() throws IOException {

		String currentPos=org.apache.lucene.analysis.ja.util.ToStringUtil.getPOSTranslation(POSAttr.getPartOfSpeech());
		
		if(tokenQueue.size()>0) {
			popQueue(); 
	
			return true;
		} 
		if (!input.incrementToken())  {
		
			return false; 
		}
		String buffer = termAttr.toString();
		char firstChar=buffer.charAt(0);
		if (! inAndOr && (firstChar=='和') && termAttr.length()==1) {
			inAndOr=true;
		}
	
		while (inAndOr) {		
			currentPos=POSAttr.getPartOfSpeech();
			currentPos=org.apache.lucene.analysis.ja.util.ToStringUtil.getPOSTranslation(currentPos);
			boolean bufferIsPartOfToken=false;
			char c=buffer.charAt(0);
			if ((inAndOr && c=='或' && termAttr.length()==1)
				) {
				
				String term=""; 
				int start=offsetAttr.startOffset(); 
				int end=offsetAttr.endOffset();
				while (tokenQueue.size()>0) {
					GrobidToken popToken=tokenQueue.removeFirst();
					term += popToken.getTerm();
					if (popToken.getStartOffset() < start) {start=popToken.getStartOffset();}
					if (popToken.getEndOffset() > end) {end=popToken.getEndOffset();}
				}
				GrobidToken tok = new GrobidToken(term+c,"",start,end);
				tokenQueue.addFirst(tok);
				bufferIsPartOfToken=true;	
				inAndOr=false;
			}
			if (! bufferIsPartOfToken) {
				GrobidToken tok = new GrobidToken(new String(buffer), "", offsetAttr.startOffset(), offsetAttr.endOffset());
				tokenQueue.add(tok);
				if (inReference || inAndOr) {
					
					if (! (c=='/'|| c=='／' || c=='或'||c=='和')
							||  termAttr.length()!=1) inAndOr=false;
				}
			}
			if (inAndOr) { // go to next token
				if (!input.incrementToken())  {
					if(tokenQueue.size()>0) {
						String term=""; 
						int start=offsetAttr.startOffset(); 
						int end=offsetAttr.endOffset();
						while (tokenQueue.size()>0) {
							GrobidToken popToken=tokenQueue.removeFirst();
							term += popToken.getTerm(); 
							if (popToken.getStartOffset() < start) {start=popToken.getStartOffset();}
							if (popToken.getEndOffset() > end) {end=popToken.getEndOffset();}
						}
						GrobidToken tok;
						tok = new GrobidToken(term, "word", start, end);
						
						tokenQueue.addFirst(tok);
						inReference=false;
						
						popQueue(); 
			
						return true;
					} 
				
					return false; 
				}
				buffer = termAttr.toString(); 
			}
		}
		if(tokenQueue.size()>0) {
			popQueue();
		} 

		return true;
	}

	private void popQueue() {
		GrobidToken popToken=tokenQueue.removeFirst();
		termAttr.setEmpty(); termAttr.append(popToken.getTerm());
		positionAttr.setPositionIncrement(1);
		typeAttr.setType(popToken.getType());
		offsetAttr.setOffset(popToken.getStartOffset(), popToken.getEndOffset());
	}

  @Override
  public void reset() throws IOException {
    super.reset();
    tokenQueue.clear();
    inReference = inAndOr = false;
  }		
}
