package org.grobid.analyzers;

final class GrobidToken {
	
	String term;
	String type;
	int startOffset;
	int endOffset;
	
	public GrobidToken(String term, String type, int startOffset, int endOffset) {
		super();
		this.term = term;
		this.type = type;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}
	
	public String getTerm() {
		return term;
	}
	public String getType() {
		return type;
	}
	public int getStartOffset() {
		return startOffset;
	}
	public int getEndOffset() {
		return endOffset;
	}
}
