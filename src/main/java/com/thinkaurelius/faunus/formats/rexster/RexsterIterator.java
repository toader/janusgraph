package com.thinkaurelius.faunus.formats.rexster;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterIterator implements Iterator<JSONObject> {

    public static final int DEFAULT_BUFFER_SIZE = 1024;

    private long itemsIterated = 0;
    private long start;
    private long end;
    private final int bufferSize;

    private final long splitEnd;
    private final RexsterVertexLoader loader;

    private final Queue<JSONObject> queue = new LinkedList<JSONObject>();

    public RexsterIterator(final RexsterVertexLoader loader, final long start, final long end) {
        this(loader, start, end, DEFAULT_BUFFER_SIZE);
    }

    public RexsterIterator(final RexsterVertexLoader loader, final long start, final long end,
                           final int bufferSize) {
        this.bufferSize = bufferSize;
        this.splitEnd = end;
        this.start = start;
        this.end = this.bufferSize + this.start;
        this.loader = loader;
    }

    @Override
    public boolean hasNext() {
        if (!queue.isEmpty())
            return true;
        else {
            if (end > start) {
                // last buffer if start == end
                fillBuffer();
                this.update();
            }
            return !queue.isEmpty();
        }
    }

    @Override
    public JSONObject next() {
        if (!queue.isEmpty()) {
            this.itemsIterated++;
            return queue.remove();
        } else {
            if (end > start) {
                // last buffer if start == end
                fillBuffer();
                this.update();
            }

            if (!queue.isEmpty()) {
                this.itemsIterated++;
                return queue.remove();
            } else
                throw new NoSuchElementException();
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public long getItemsIterated() {
        return itemsIterated;
    }

    private void fillBuffer() {
        try {
            final JSONArray vertices = this.loader.getPagedVertices(this.start, this.end);
            for (int ix = 0; ix < vertices.length(); ix++) {
                queue.add(new JSONObject(vertices.optString(ix)));
            }

            //System.out.println("filling the buffer starting at " + this.start + " ending at " + this.end);
            //System.out.println("filling the buffer:" + vertices.length() + " queue at: " + queue.size());
        } catch (JSONException jse) {
            throw new RuntimeException(jse);
        }
    }

    private void update() {
        if (this.queue.size() == bufferSize) {
            // next buffer if full
            this.start = this.start + bufferSize;
            this.end = this.end + bufferSize;

            if (this.splitEnd < Long.MAX_VALUE) {
                // since the split end is less than the max we basically want to kill
                // the iterator at the split end otherwise we just let it exhaust itself
                if (this.end > this.splitEnd) {
                    this.end = this.splitEnd;
                }

                if (this.start > this.splitEnd) {
                    // last buffer
                    this.start = this.end;
                }
            }
        } else { // last buffer
            this.start = this.end;
        }
    }
}
