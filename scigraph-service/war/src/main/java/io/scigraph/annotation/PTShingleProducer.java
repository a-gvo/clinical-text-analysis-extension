package io.scigraph.annotation;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;


/**
 * Splits text into shingles, with no shingle containing more than one grammatical clause
 * (ie they stop at punctuation).
 */
public class PTShingleProducer extends ShingleProducer
{
    /**
     * The analyzer.
     */
    private Analyzer analyzer;

    /**
     * The reader containing the text.
     */
    private Reader reader;

    /**
     * The maximum length of a shingle.
     */
    private int shingleCount;

    /**
     * The blocking queue to use to feed back to the annotating thread.
     */
    private final BlockingQueue<List<Token<String>>> queue;

    /**
     * Our logger.
     */
    private final static Logger logger = Logger.getLogger(ShingleProducer.class.getName());

    /**
     * CTOR.
     * @param analyzer the analyzer to use.
     * @param reader the reader containing text.
     * @param queue the queue to use to feed back to the annotating thread.
     */
    public PTShingleProducer(Analyzer analyzer, Reader reader, BlockingQueue<List<Token<String>>> queue)
    {
        this(analyzer, reader, queue, DEFAULT_SHINGLE_COUNT);
    }

    /**
     * CTOR.
     * @param analyzer the analyzer to use.
     * @param reader the reader containing text.
     * @param queue the queue to use to feed back to the annotating thread.
     * @param shingleCount the maximum length of any given shingle.
     */
    public PTShingleProducer(Analyzer analyzer, Reader reader, BlockingQueue<List<Token<String>>> queue,
                             int shingleCount)
    {
        super(analyzer, reader, queue, shingleCount);
        this.analyzer = analyzer;
        this.reader = reader;
        this.shingleCount = shingleCount;
        this.queue = queue;
    }

    @Override
    public void run() {
        Deque<Token<String>> buffer = new LinkedList<>();
        try {
            TokenStream stream = analyzer.tokenStream("", reader);
            OffsetAttribute offset = stream.getAttribute(OffsetAttribute.class);
            CharTermAttribute term = stream.getAttribute(CharTermAttribute.class);
            FlagsAttribute flags = stream.getAttribute(FlagsAttribute.class);
            boolean punctuation = false;

            try {
                while (punctuation || stream.incrementToken()) {
                    if (!punctuation) {
                        Token<String> token = new Token<String>(term.toString(), offset.startOffset(),
                        offset.endOffset());
                        buffer.offer(token);
                        punctuation = PunctuationFilter.isPunctuationSet(flags.getFlags());
                        if (!punctuation && buffer.size() < shingleCount) {
                            // Fill the buffer first, before offering anything to the queue
                            continue;
                        }
                    }
                    addBufferToQueue(buffer);
                    if (punctuation || shingleCount == buffer.size()) {
                        buffer.pop();
                    }
                    if (punctuation && buffer.isEmpty()) {
                        punctuation = false;
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to produces single", e);
            }
            while (!buffer.isEmpty()) {
                addBufferToQueue(buffer);
                buffer.pop();
            }
            queue.put(END_TOKEN);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
