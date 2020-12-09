package de.spinscale.query;

import org.apache.lucene.util.SetOnce;

public class SearchResult {

    public enum Hint { Point, Shape }

    public final String city;
    public final Hint hint;
    private final SetOnce<Long> took = new SetOnce<>();

    SearchResult(String city, Hint hint) {
        this.city = city;
        this.hint = hint;
    }

    // empty result
    private SearchResult(Hint hint) {
        this.city = null;
        this.hint = hint;
    }

    public boolean isEmpty() {
        return city == null;
    }

    public long getTook() {
        return this.took.get();
    }

    public void setTook(long took) {
        this.took.set(took);
    }

    public static final SearchResult empty(Hint hint) {
        return new SearchResult(hint);
    }
}
