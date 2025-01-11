package org.grobid.analyzers;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

public final class MyJapaneseFilterOrdinals extends TokenFilter {
	private static final int TermPos=0;
	@SuppressWarnings("unused")
	private static final int TypePos=1;
	private static final int StartPos=2;
	private static final int EndPos=3;
	private static final int MaxPos=4;

  private final LinkedList<String[]> tokenQueue = new LinkedList<String[]>();
  
  private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);;
  private final PositionIncrementAttribute positionAttr = addAttribute(PositionIncrementAttribute.class);
  private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);;

	public MyJapaneseFilterOrdinals (TokenStream input) {
		super(input);
	}
	
	
	public boolean incrementToken() throws IOException {

		if(tokenQueue.size()>0) {
			popQueue(); 
			return true;
		} 
		if (!input.incrementToken())  {
			return false; 
		}
		if (termAttr.charAt(0) == '第' && termAttr.length()==1) { // ordinal mark
			String[] tok = new String[MaxPos];
			tok[TermPos]="第";
			
			tok[StartPos]=String.valueOf(offsetAttr.startOffset());
			tok[EndPos]=String.valueOf(offsetAttr.endOffset());
			tokenQueue.add(tok);
		
			if (!input.incrementToken())  {
				return false; 
			}
			if (isDigit(termAttr.charAt(0))) {
				tokenQueue.removeFirst();
				char[] term=termAttr.buffer().clone(); int l = termAttr.length();
				
				termAttr.setEmpty();
				termAttr.append('第'); for (int i=0; i<l; i++) {termAttr.append(term[i]);}
				offsetAttr.setOffset(offsetAttr.startOffset()-1,offsetAttr.endOffset());
			} else {
				tok[TermPos]=termAttr.toString();			
				tok[StartPos]=String.valueOf(offsetAttr.startOffset());
				tok[EndPos]=String.valueOf(offsetAttr.endOffset());
				tokenQueue.add(tok);
				popQueue();
			}
		}
		return true;
	}

	private boolean isDigit(char c) {
		return ((c >='0' && c<='9'));

	}
	@SuppressWarnings("unused")
	private boolean isLatinChar(char c) {
		return ((c >='a' && c<='z') || (c >='A' && c<='Z') 	|| (c >='0' && c<='9') 			
				);

	}

	private void popQueue() {
		String[] popToken=tokenQueue.removeFirst();
		termAttr.setEmpty(); termAttr.append(popToken[TermPos]);
		positionAttr.setPositionIncrement(1);
		
		offsetAttr.setOffset(Integer.parseInt(popToken[StartPos]), Integer.parseInt(popToken[EndPos]));
	}
	
  @Override
  public void reset() throws IOException {
    super.reset();
    tokenQueue.clear();
  }
    		
}
