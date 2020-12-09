package de.spinscale.fst;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CollectionUtil;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.ByteSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthFSTWriter {

    private final Map<String, String> tokens = new HashMap<>();

    public AuthFSTWriter allowed(final String token) {
        return add("allowed", token);
    }

    public AuthFSTWriter rejected(final String token) {
        return add("rejected", token);
    }

    public AuthFSTWriter operations(final String token) {
        return add("operations", token);
    }

    AuthFSTWriter add(final String operation, final String token) {
        tokens.put(token, operation);
        return this;
    }

    // visible for testing
    FST<BytesRef> createFST() throws IOException {
        Builder<BytesRef> builder = new Builder<>(FST.INPUT_TYPE.BYTE1, ByteSequenceOutputs.getSingleton());
        final IntsRefBuilder scratch = new IntsRefBuilder();
        final List<String> tokenList = new ArrayList<>(tokens.keySet());

        CollectionUtil.timSort(tokenList);
        for (String uuid : tokenList) {
            builder.add(Util.toIntsRef(new BytesRef(uuid), scratch), new BytesRef(tokens.get(uuid)));
        }

        return builder.finish();
    }

    public void save(final Path path) throws IOException {
        final FST<BytesRef> fst = createFST();
        fst.save(path);
    }
}
