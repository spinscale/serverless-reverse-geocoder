package de.spinscale.fst;

import io.javalin.Javalin;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Representation of a serialized on disk FST that handles authentication date
 */
public class AuthFST {

    private final FST<BytesRef> fst;

    // visible for testing
    AuthFST(final FST<BytesRef> fst) {
        this.fst = fst;
    }

    public static AuthFST readFrom(final Path path) throws IOException {
        final FST<BytesRef> fst = FST.read(path, ByteSequenceOutputs.getSingleton());
        return new AuthFST(fst);
    }

    public String find(final String input) {
        try {
            final BytesRef bytesRef = Util.get(fst, new BytesRef(input));
            if (bytesRef == null) {
                return "unknown";
            } else {
                return bytesRef.utf8ToString();
            }
        } catch (IOException e) {
            // TODO log and return null
            Javalin.log.error("Error finding input [{}] in FST", input, e);
            throw new RuntimeException("Error finding input [" + input + "]: [" + e.getMessage() + "]");
        }
    }
}
