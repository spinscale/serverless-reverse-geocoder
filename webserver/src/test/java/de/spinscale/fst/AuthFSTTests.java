package de.spinscale.fst;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthFSTTests {

    @TempDir
    public Path folder;

    private AuthFSTWriter writer = new AuthFSTWriter()
                .allowed("allowed-token")
                .rejected("rejected-token")
                .operations("operations-token");

    @Test
    public void testAuthFST() throws Exception {
        final AuthFST authfst = new AuthFST(writer.createFST());
        assertThat(authfst.find("allowed-token")).isEqualTo("allowed");
        assertThat(authfst.find("rejected-token")).isEqualTo("rejected");
        assertThat(authfst.find("operations-token")).isEqualTo("operations");
        assertThat(authfst.find("unknown-token")).isEqualTo("unknown");
    }

    @Test
    public void testWriteAndReadFromDisk() throws Exception {
        final Path fstFile = folder.resolve("file");
        writer.save(fstFile);

        final AuthFST authFST = AuthFST.readFrom(fstFile);
        assertThat(authFST.find("allowed-token")).isEqualTo("allowed");
    }
}
