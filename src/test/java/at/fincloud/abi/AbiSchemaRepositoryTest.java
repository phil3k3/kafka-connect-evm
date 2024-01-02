package at.fincloud.abi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

class AbiSchemaRepositoryTest {

    @Test
    public void shouldLoadSchemasFromAbi() throws IOException {
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream("Streams.json")) {
            assert resourceAsStream != null;
            AbiSchemaRepository abiSchemaRepository = new AbiSchemaRepository(() -> {
                try {
                    return (ArrayNode) new ObjectMapper().readTree(resourceAsStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Assertions.assertNotNull(abiSchemaRepository.getSchema("StreamStarted"));
            Assertions.assertNotNull(abiSchemaRepository.getSchema("StreamStopped"));
            Assertions.assertNotNull(abiSchemaRepository.getSchema("StreamOutOfFunds"));
        }
    }

}
